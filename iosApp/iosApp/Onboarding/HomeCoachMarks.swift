import SwiftUI

/// Um alvo do tour de boas-vindas: o texto exibido e o id (usado tanto para o
/// ScrollViewReader quanto para casar com os bounds reportados via PreferenceKey).
struct CoachMarkTarget: Identifiable {
    let id: String
    let title: String
    let description: String
}

/// Bounds (no espaço de coordenadas nomeado "homeCoachMarkSpace") de cada alvo já medido.
/// Chave = CoachMarkTarget.id.
struct CoachMarkBoundsPreferenceKey: PreferenceKey {
    static var defaultValue: [String: CGRect] = [:]
    static func reduce(value: inout [String: CGRect], nextValue: () -> [String: CGRect]) {
        value.merge(nextValue()) { _, new in new }
    }
}

/// Estado do tour de boas-vindas da Home. Como a Home usa `LazyVStack`, um alvo mais abaixo
/// na tela só é medido depois de rolarmos até ele — por isso `advance()` primeiro pede a
/// rolagem (via `pendingScrollID`, observado pela view) e só troca de passo quando o alvo
/// seguinte de fato reportar um tamanho real. Um alvo que nunca existe nesta sessão
/// (ex.: card de check-in fora da janela horária) é pulado automaticamente.
@MainActor
final class HomeCoachMarkState: ObservableObject {
    let targets: [CoachMarkTarget]

    @Published private(set) var currentIndex: Int = 0
    @Published private(set) var isActive: Bool = false
    @Published private(set) var pendingScrollID: String? = nil
    private var bounds: [String: CGRect] = [:]

    init(targets: [CoachMarkTarget]) {
        self.targets = targets
    }

    var currentTarget: CoachMarkTarget? {
        guard isActive, targets.indices.contains(currentIndex) else { return nil }
        return targets[currentIndex]
    }

    var currentBounds: CGRect? {
        currentTarget.flatMap { bounds[$0.id] }
    }

    func reportBounds(id: String, rect: CGRect) {
        bounds[id] = rect
    }

    func start() {
        guard !targets.isEmpty else { return }
        isActive = true
        probe(from: 0)
    }

    func dismiss() {
        isActive = false
        pendingScrollID = nil
    }

    /// Avança para o próximo passo, ou encerra o tour se o atual já é o último.
    func advance() {
        guard isActive else { return }
        if currentIndex >= targets.count - 1 {
            dismiss()
        } else {
            probe(from: currentIndex + 1)
        }
    }

    /// Pede rolagem até `index` e, após um curto intervalo (tempo de rolar + medir),
    /// confirma o passo se o alvo existir de verdade; senão tenta o próximo.
    private func probe(from index: Int) {
        var next = index
        pendingScrollID = targets[next].id
        Task {
            try? await Task.sleep(nanoseconds: 350_000_000)
            while next < targets.count {
                let id = targets[next].id
                if let rect = bounds[id], rect.width > 1, rect.height > 1 {
                    currentIndex = next
                    return
                }
                next += 1
                if next < targets.count {
                    pendingScrollID = targets[next].id
                    try? await Task.sleep(nanoseconds: 350_000_000)
                }
            }
            dismiss()
        }
    }
}

/// Anexe a uma view da Home para que ela participe do tour como alvo identificado por [id].
extension View {
    func coachMarkTarget(_ id: String, state: HomeCoachMarkState) -> some View {
        self
            .id(id)
            .background(
                GeometryReader { geo in
                    Color.clear.preference(
                        key: CoachMarkBoundsPreferenceKey.self,
                        value: [id: geo.frame(in: .named("homeCoachMarkSpace"))]
                    )
                }
            )
    }
}

/// Overlay de "spotlight": escurece a tela inteira e recorta um retângulo arredondado
/// transparente sobre o alvo atual (regra par-ímpar de preenchimento), com um balão de
/// texto e navegação (Pular / Próximo/Entendi). Enquanto o alvo ainda não foi medido
/// (rolagem em andamento), mostra apenas o véu escuro, mantendo o passo anterior visível
/// até o próximo estar pronto (ver `HomeCoachMarkState.probe`).
struct HomeCoachMarkOverlay: View {
    @ObservedObject var state: HomeCoachMarkState

    var body: some View {
        if state.isActive {
            GeometryReader { screen in
                let target = state.currentBounds
                let padding: CGFloat = 10
                let cornerRadius: CGFloat = 16

                ZStack(alignment: .topLeading) {
                    Path { path in
                        path.addRect(CGRect(origin: .zero, size: screen.size))
                        if let target {
                            let hole = target.insetBy(dx: -padding, dy: -padding)
                            path.addRoundedRect(in: hole, cornerSize: CGSize(width: cornerRadius, height: cornerRadius))
                        }
                    }
                    .fill(Color.black.opacity(0.72), style: FillStyle(eoFill: true))
                    .allowsHitTesting(true)
                    .onTapGesture { state.advance() }

                    if let target, let step = state.currentTarget {
                        let bubbleBelowTarget = target.maxY < screen.size.height * 0.62
                        let bubbleWidth = screen.size.width - 40

                        CoachMarkBubble(
                            index: state.currentIndex,
                            count: state.targets.count,
                            title: step.title,
                            description: step.description,
                            isLastStep: state.currentIndex == state.targets.count - 1,
                            onSkip: { state.dismiss() },
                            onNext: { state.advance() }
                        )
                        .frame(width: bubbleWidth)
                        .position(
                            x: screen.size.width / 2,
                            y: bubbleBelowTarget
                                ? target.maxY + padding + 90
                                : target.minY - padding - 90
                        )
                    }
                }
            }
            .transition(.opacity)
            .animation(.easeInOut(duration: 0.25), value: state.isActive)
            .animation(.easeInOut(duration: 0.25), value: state.currentIndex)
            // Propositalmente SEM .ignoresSafeArea(): o overlay precisa ocupar exatamente
            // o mesmo frame da ScrollView (onde "homeCoachMarkSpace" foi declarado) para
            // que as coordenadas dos alvos batam com as do véu/recorte.
        }
    }
}

private struct CoachMarkBubble: View {
    let index: Int
    let count: Int
    let title: String
    let description: String
    let isLastStep: Bool
    let onSkip: () -> Void
    let onNext: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(index + 1)/\(count)")
                .font(.caption.bold())
                .foregroundColor(AfilaxyColors.primary)
            Text(title)
                .font(.headline)
                .foregroundColor(.primary)
            Text(description)
                .font(.subheadline)
                .foregroundColor(.secondary)
            HStack {
                Button("Pular", action: onSkip)
                    .font(.subheadline)
                Spacer()
                Button(isLastStep ? "Entendi" : "Próximo", action: onNext)
                    .font(.subheadline.bold())
                    .buttonStyle(.borderedProminent)
                    .tint(AfilaxyColors.primary)
            }
        }
        .padding(20)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(radius: 12)
        .onTapGesture { /* consome o toque — não avança ao tocar no próprio balão */ }
    }
}
