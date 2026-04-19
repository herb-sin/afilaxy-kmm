import SwiftUI
import shared

/// Widget de risco de crise asmática — espelho do RiskWidget.kt do Android.
/// Exibido na HomeView quando a localização do usuário está disponível.
/// Toca para expandir/recolher fatores e recomendações.
struct RiskWidgetView: View {
    let riskState: RiskState?
    @State private var showDetails = false

    var body: some View {
        Group {
            if riskState?.isLoading == true {
                RiskWidgetSkeleton()
            } else if let score = riskState?.riskScore {
                RiskWidgetContent(score: score, showDetails: $showDetails)
            }
            // Se nil e não carregando: widget não exibido (sem localização ou erro)
        }
    }
}

// MARK: - Skeleton

private struct RiskWidgetSkeleton: View {
    @State private var animating = false

    var body: some View {
        RoundedRectangle(cornerRadius: 16)
            .fill(Color(.systemGray5).opacity(animating ? 0.7 : 0.3))
            .frame(height: 90)
            .animation(.easeInOut(duration: 0.9).repeatForever(autoreverses: true), value: animating)
            .onAppear { animating = true }
    }
}

// MARK: - Content

private struct RiskWidgetContent: View {
    let score: RiskScore
    @Binding var showDetails: Bool

    private var level: AsthmaRiskLevel { score.riskLevel }

    private var gradientColors: [Color] {
        switch level {
        case .low:      return [Color(hex: "#1B5E20"), Color(hex: "#2E7D32")]
        case .moderate: return [Color(hex: "#F57F17"), Color(hex: "#F9A825")]
        case .high:     return [Color(hex: "#E65100"), Color(hex: "#FF6D00")]
        case .veryHigh: return [Color(hex: "#B71C1C"), Color(hex: "#D32F2F")]
        default:        return [Color(hex: "#1B5E20"), Color(hex: "#2E7D32")]
        }
    }

    var body: some View {
        Button(action: { withAnimation(.easeInOut(duration: 0.25)) { showDetails.toggle() } }) {
            ZStack {
                LinearGradient(
                    colors: gradientColors,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))

                VStack(alignment: .leading, spacing: 10) {
                    // Header: emoji + label + score circular
                    HStack(alignment: .center) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(level.emoji) Risco \(level.label)")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                            Text("Toque para \(showDetails ? "ocultar" : "ver") detalhes")
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.7))
                        }
                        Spacer()
                        // Score circular
                        ZStack {
                            Circle()
                                .fill(Color.white.opacity(0.2))
                                .frame(width: 56, height: 56)
                            Text("\(score.score)")
                                .font(.system(size: 20, weight: .black))
                                .foregroundColor(.white)
                        }
                    }

                    // Barra de progresso
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(Color.white.opacity(0.3))
                                .frame(height: 6)
                            Capsule()
                                .fill(Color.white)
                                .frame(width: geo.size.width * CGFloat(score.score) / 100, height: 6)
                        }
                    }
                    .frame(height: 6)

                    // Detalhes expansíveis
                    if showDetails {
                        Divider().background(Color.white.opacity(0.3))

                        let factors = score.factors as? [String] ?? []
                        let recommendations = score.recommendations as? [String] ?? []

                        if !factors.isEmpty {
                            Text("⚠️ Fatores de risco")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.white)
                            ForEach(factors, id: \.self) { factor in
                                Text("• \(factor)")
                                    .font(.system(size: 11))
                                    .foregroundColor(.white.opacity(0.9))
                            }
                        }

                        if !recommendations.isEmpty {
                            if !factors.isEmpty { Spacer().frame(height: 4) }
                            Text("💡 Recomendações")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.white)
                            ForEach(recommendations, id: \.self) { rec in
                                Text("• \(rec)")
                                    .font(.system(size: 11))
                                    .foregroundColor(.white.opacity(0.9))
                            }
                        }
                    }
                }
                .padding(16)
            }
        }
        .buttonStyle(PlainButtonStyle())
        .shadow(color: gradientColors.first?.opacity(0.4) ?? .clear, radius: 10, x: 0, y: 5)
    }
}
