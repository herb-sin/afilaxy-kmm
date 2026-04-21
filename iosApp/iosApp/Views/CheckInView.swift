import SwiftUI
import shared

// MARK: - CheckInView

/// Porta 1:1 do Android CheckInScreen para SwiftUI.
/// Usa o shared CheckInViewModel via CheckInViewModelWrapper (factory — nova instância por abertura).
struct CheckInView: View {
    let type: CheckInType
    let quickAnswer: Bool?
    let onDone: () -> Void

    @StateObject private var wrapper: CheckInViewModelWrapper = {
        if let vm = ViewModelProvider.shared.getCheckInViewModel() {
            return CheckInViewModelWrapper(vm)
        }
        return CheckInViewModelWrapper.empty()
    }()

    init(type: CheckInType, quickAnswer: Bool? = nil, onDone: @escaping () -> Void) {
        self.type = type
        self.quickAnswer = quickAnswer
        self.onDone = onDone
    }

    var body: some View {
        Group {
            if wrapper.state?.isLoading == true {
                CheckInLoadingView()
            } else if wrapper.state?.alreadyDoneToday == true || wrapper.state?.isSubmitted == true {
                CheckInDoneView(type: type, onDone: onDone)
            } else if type == .morning {
                MorningCheckInContent(
                    inhalerName: wrapper.state?.rescueInhalerName,
                    riskScore: wrapper.state?.riskScore?.int32Value,
                    onYes: { wrapper.submitMorning(hasInhaler: true) },
                    onNo: { wrapper.submitMorning(hasInhaler: false) }
                )
            } else {
                EveningCheckInContent(
                    onNoCrisis: { wrapper.submitEvening(hadCrisis: false) },
                    onHadCrisis: { severity, usedInhaler in
                        wrapper.submitEvening(
                            hadCrisis: true,
                            severity: severity,
                            usedRescueInhaler: usedInhaler.map { KotlinBoolean(value: $0) }
                        )
                    }
                )
            }
        }
        .onAppear {
            wrapper.initialize(type: type)
        }
        .onChange(of: wrapper.state?.isSubmitted) { isSubmitted in
            // Resposta rápida via action da notificação
            // (handled inline — isSubmitted triggers CheckInDoneView)
        }
        .onAppear {
            // Quick answer via notification action
            if let answer = quickAnswer,
               wrapper.state?.isLoading != true,
               wrapper.state?.alreadyDoneToday != true,
               wrapper.state?.isSubmitted != true {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    if type == .morning {
                        wrapper.submitMorning(hasInhaler: answer)
                    } else {
                        wrapper.submitEvening(hadCrisis: answer)
                    }
                }
            }
        }
        .navigationBarHidden(true)
    }
}

// MARK: - Morning Check-in

private struct MorningCheckInContent: View {
    let inhalerName: String?
    let riskScore: Int32?
    let onYes: () -> Void
    let onNo: () -> Void

    private var inhalerDisplay: String {
        inhalerName ?? "broncodilatador de resgate"
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.9, green: 0.32, blue: 0),
                         Color(red: 1, green: 0.56, blue: 0)],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                Text("💊")
                    .font(.system(size: 72))

                Text("Check-in Matinal")
                    .foregroundColor(.white.opacity(0.8))
                    .font(.footnote.weight(.medium))
                    .padding(.top, 20)

                Text("Você está com seu\n\(inhalerDisplay)?")
                    .foregroundColor(.white)
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                    .lineSpacing(6)
                    .padding(.top, 8)

                if let score = riskScore, score >= 45 {
                    HStack {
                        Text("⚠️ Risco de crise hoje: \(score)/100")
                            .foregroundColor(.white)
                            .font(.caption)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color.white.opacity(0.2))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.top, 16)
                }

                Spacer().frame(height: 40)

                Button(action: onYes) {
                    HStack {
                        Image(systemName: "checkmark.circle.fill")
                        Text("✅  Sim, estou com ela")
                            .fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(Color.white)
                    .foregroundColor(Color(red: 0.9, green: 0.32, blue: 0))
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                Button(action: onNo) {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                        Text("❌  Não tenho comigo")
                            .fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color.white, lineWidth: 2)
                    )
                    .foregroundColor(.white)
                }
                .padding(.top, 12)

                Text("Sua resposta ajuda a entender seu padrão de risco.")
                    .foregroundColor(.white.opacity(0.6))
                    .font(.caption2)
                    .multilineTextAlignment(.center)
                    .padding(.top, 16)

                Spacer()
            }
            .padding(.horizontal, 28)
        }
    }
}

// MARK: - Evening Check-in

private struct EveningCheckInContent: View {
    let onNoCrisis: () -> Void
    let onHadCrisis: (_ severity: String?, _ usedInhaler: Bool?) -> Void

    @State private var showCrisisDetail = false
    @State private var selectedSeverity: String? = nil
    @State private var usedInhaler: Bool? = nil

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.1, green: 0.14, blue: 0.49),
                         Color(red: 0.16, green: 0.21, blue: 0.58)],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                Text("🌙")
                    .font(.system(size: 64))

                Text("Check-in Noturno")
                    .foregroundColor(.white.opacity(0.7))
                    .font(.footnote)
                    .padding(.top, 16)

                Text("Você teve alguma\ncrise de asma hoje?")
                    .foregroundColor(.white)
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                    .lineSpacing(6)
                    .padding(.top, 8)

                Spacer().frame(height: 40)

                if !showCrisisDetail {
                    // Initial question
                    VStack(spacing: 12) {
                        Button(action: { withAnimation { showCrisisDetail = true } }) {
                            Text("⚠️  Sim, tive uma crise")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(Color(red: 0.94, green: 0.33, blue: 0.31))
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }

                        Button(action: onNoCrisis) {
                            Text("✅  Não, dia tranquilo")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(Color(red: 0.26, green: 0.63, blue: 0.28))
                                .foregroundColor(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                } else {
                    // Crisis detail form
                    VStack(spacing: 12) {
                        Text("Qual foi a intensidade?")
                            .foregroundColor(.white)
                            .fontWeight(.semibold)

                        ForEach([("leve", "🟡 Leve"), ("moderada", "🟠 Moderada"), ("grave", "🔴 Grave")], id: \.0) { value, label in
                            let isSelected = selectedSeverity == value
                            Button(action: { selectedSeverity = value }) {
                                Text(label)
                                    .fontWeight(isSelected ? .bold : .regular)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(isSelected ? Color.white.opacity(0.2) : Color.clear)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(Color.white, lineWidth: isSelected ? 2 : 1)
                                    )
                                    .foregroundColor(.white)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }

                        Text("Usou o broncodilatador de resgate?")
                            .foregroundColor(.white.opacity(0.8))
                            .font(.subheadline)
                            .padding(.top, 4)

                        HStack(spacing: 12) {
                            ForEach([(true, "Sim"), (false, "Não")], id: \.0) { value, label in
                                let isSelected = usedInhaler == value
                                Button(action: { usedInhaler = value }) {
                                    Text(label)
                                        .padding(.horizontal, 24)
                                        .padding(.vertical, 10)
                                        .background(isSelected ? Color.white.opacity(0.2) : Color.clear)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 12)
                                                .stroke(Color.white, lineWidth: isSelected ? 2 : 1)
                                        )
                                        .foregroundColor(.white)
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                }
                            }
                        }

                        Button(action: { onHadCrisis(selectedSeverity, usedInhaler) }) {
                            Text("Salvar")
                                .fontWeight(.bold)
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(selectedSeverity != nil ? Color.white : Color.white.opacity(0.3))
                                .foregroundColor(Color(red: 0.1, green: 0.14, blue: 0.49))
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        .disabled(selectedSeverity == nil)
                        .padding(.top, 8)
                    }
                    .transition(.opacity)
                }

                Spacer()
            }
            .padding(.horizontal, 28)
        }
    }
}

// MARK: - Done & Loading

private struct CheckInDoneView: View {
    let type: CheckInType
    let onDone: () -> Void

    var body: some View {
        ZStack {
            Color(red: 0.11, green: 0.37, blue: 0.13)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                Text("✅")
                    .font(.system(size: 72))

                Text(type == .morning
                     ? "Obrigado!\nFique com sua bombinha sempre que sair."
                     : "Obrigado pelo registro!\nSeus dados ajudam a melhorar seu cuidado.")
                    .foregroundColor(.white)
                    .font(.title3)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)

                Button(action: onDone) {
                    Text("Voltar")
                        .fontWeight(.bold)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 12)
                        .background(Color.white)
                        .foregroundColor(Color(red: 0.11, green: 0.37, blue: 0.13))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .padding(.top, 16)
            }
            .padding(.horizontal, 28)
        }
    }
}

private struct CheckInLoadingView: View {
    var body: some View {
        ZStack {
            Color(.systemBackground).ignoresSafeArea()
            ProgressView()
                .scaleEffect(1.5)
        }
    }
}
