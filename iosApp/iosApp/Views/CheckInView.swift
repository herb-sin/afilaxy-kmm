import SwiftUI
import shared

// MARK: - CheckInView

struct CheckInView: View {
    let type: CheckInType
    let quickAnswer: Bool?
    let riskScore: Int32?
    let aqi: Int32?
    let temperature: Float?
    let humidity: Float?
    let onDone: () -> Void

    @StateObject private var wrapper: CheckInViewModelWrapper = {
        if let vm = ViewModelProvider.shared.getCheckInViewModel() {
            return CheckInViewModelWrapper(vm)
        }
        return CheckInViewModelWrapper.empty()
    }()

    init(
        type: CheckInType,
        quickAnswer: Bool? = nil,
        riskScore: Int32? = nil,
        aqi: Int32? = nil,
        temperature: Float? = nil,
        humidity: Float? = nil,
        onDone: @escaping () -> Void
    ) {
        self.type = type
        self.quickAnswer = quickAnswer
        self.riskScore = riskScore
        self.aqi = aqi
        self.temperature = temperature
        self.humidity = humidity
        self.onDone = onDone
    }

    var body: some View {
        Group {
            if wrapper.state?.isLoading == true {
                CheckInLoadingView()
            } else if wrapper.state?.showCriticalWellbeingCard == true {
                CriticalWellbeingCardView(
                    onDismiss: { wrapper.dismissCriticalCard(); onDone() }
                )
            } else if wrapper.state?.alreadyDoneToday == true || wrapper.state?.isSubmitted == true {
                CheckInDoneView(type: type, onDone: onDone)
            } else if type == .morning {
                MorningCheckInContent(
                    riskScore: wrapper.state?.riskScore?.int32Value,
                    onSubmit: { mood, energy, awakening in
                        wrapper.submitMorning(morningMoodGood: mood, morningEnergyGood: energy, nighttimeAwakening: awakening)
                    }
                )
            } else {
                EveningCheckInContent(
                    onSubmit: { selfCare, rescue, breathing, activity in
                        wrapper.submitEvening(EveningCheckInAnswers(
                            selfCareGood: selfCare,
                            rescueInhalerUses: Int32(rescue),
                            daytimeBreathingEase: breathing, activityAsPlanned: activity
                        ))
                    }
                )
            }
        }
        .onAppear {
            wrapper.initialize(
                type: type,
                riskScore: riskScore != nil ? KotlinInt(value: riskScore!) : nil,
                aqi: aqi != nil ? KotlinInt(value: aqi!) : nil,
                temperature: temperature != nil ? KotlinFloat(value: temperature!) : nil,
                humidity: humidity != nil ? KotlinFloat(value: humidity!) : nil
            )
            // Só "está tudo bem" (true) grava direto; false abre o formulário normal
            // para o usuário relatar o dia — igual ao Android.
            if quickAnswer == true,
               wrapper.state?.isLoading != true,
               wrapper.state?.alreadyDoneToday != true,
               wrapper.state?.isSubmitted != true {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    if type == .morning {
                        wrapper.submitMorning(morningMoodGood: true, morningEnergyGood: true, nighttimeAwakening: true)
                    } else {
                        wrapper.submitEvening(EveningCheckInAnswers(
                            selfCareGood: true, rescueInhalerUses: 0,
                            daytimeBreathingEase: true, activityAsPlanned: true
                        ))
                    }
                }
            }
        }
        .navigationBarHidden(true)
    }
}

// MARK: - Morning Check-in

private struct MorningCheckInContent: View {
    let riskScore: Int32?
    let onSubmit: (Bool, Bool, Bool) -> Void

    // Sem resposta pré-marcada (nil): só vale o que o usuário escolher. Antes as respostas
    // começavam em "Sim" e um toque direto em "Confirmar" gravava um dia bom.
    @State private var morningMoodGood: Bool? = nil    // "Me sinto bem esta manhã"
    @State private var morningEnergyGood: Bool? = nil  // "Estou com boa energia"
    @State private var nighttimeAwakening: Bool? = nil // "Meu sono foi tranquilo, sem interrupções?"

    private var canSubmit: Bool {
        morningMoodGood != nil && morningEnergyGood != nil && nighttimeAwakening != nil
    }
    private static let accent = Color(red: 0.9, green: 0.32, blue: 0)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.9, green: 0.32, blue: 0),
                         Color(red: 1, green: 0.56, blue: 0)],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: 40)

                    Text("🌅")
                        .font(.system(size: 64))

                    Text("Check-in Matinal")
                        .foregroundColor(.white.opacity(0.8))
                        .font(.footnote.weight(.medium))
                        .padding(.top, 16)

                    Text("Como foi sua noite e manhã?")
                        .foregroundColor(.white)
                        .font(.title2.bold())
                        .multilineTextAlignment(.center)
                        .padding(.top, 4)

                    if let score = riskScore, score >= 45 {
                        HStack {
                            Text("⚠️ Qualidade do ar baixa hoje: \(score)/100")
                                .foregroundColor(.white)
                                .font(.caption)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.white.opacity(0.2))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .padding(.top, 12)
                    }

                    Spacer().frame(height: 20)

                    VStack(spacing: 0) {
                        YesNoRow(label: "Me sinto bem esta manhã", value: $morningMoodGood, accent: Self.accent)
                        Divider().background(Color.white.opacity(0.2))
                        YesNoRow(label: "Estou com boa energia", value: $morningEnergyGood, accent: Self.accent)
                        Divider().background(Color.white.opacity(0.2))
                        YesNoRow(label: "Meu sono foi tranquilo, sem interrupções", value: $nighttimeAwakening, accent: Self.accent)
                    }
                    .padding(16)
                    .background(Color.white.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    Spacer().frame(height: 24)

                    Button(action: {
                        guard let mood = morningMoodGood, let energy = morningEnergyGood,
                              let sleep = nighttimeAwakening else { return }
                        onSubmit(mood, energy, sleep)
                    }) {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                            Text("Confirmar")
                                .fontWeight(.bold)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(Color.white.opacity(canSubmit ? 1 : 0.3))
                        .foregroundColor(canSubmit ? Self.accent : .white.opacity(0.6))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .disabled(!canSubmit)

                    if !canSubmit {
                        Text("Responda todas as perguntas para confirmar.")
                            .foregroundColor(.white.opacity(0.8))
                            .font(.caption)
                            .multilineTextAlignment(.center)
                            .padding(.top, 8)
                    }

                    Text("Sua resposta ajuda a entender seu padrão de risco.")
                        .foregroundColor(.white.opacity(0.6))
                        .font(.caption2)
                        .multilineTextAlignment(.center)
                        .padding(.top, 12)

                    Spacer().frame(height: 40)
                }
                .padding(.horizontal, 28)
            }
        }
    }
}

// MARK: - Evening Check-in

private struct EveningCheckInContent: View {
    let onSubmit: (Bool, Int, Bool, Bool) -> Void

    // Sem resposta pré-marcada (nil), inclusive na bombinha: antes o padrão era "Sim" e
    // 0 usos, e um toque direto em "Confirmar" gravava um dia bom sem uso de resgate.
    @State private var selfCareGood: Bool? = nil         // "Me cuidei bem hoje"
    @State private var rescueInhalerUses: Int? = nil
    @State private var daytimeBreathingEase: Bool? = nil // "Respirei com facilidade ao longo do dia?"
    @State private var activityAsPlanned: Bool? = nil    // "Consegui fazer tudo que tinha planejado hoje?"

    private var canSubmit: Bool {
        selfCareGood != nil && rescueInhalerUses != nil &&
            daytimeBreathingEase != nil && activityAsPlanned != nil
    }
    private static let accent = Color(red: 0.1, green: 0.14, blue: 0.49)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.1, green: 0.14, blue: 0.49),
                         Color(red: 0.16, green: 0.21, blue: 0.58)],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: 40)

                    Text("🌙")
                        .font(.system(size: 64))

                    Text("Check-in Noturno")
                        .foregroundColor(.white.opacity(0.7))
                        .font(.footnote)
                        .padding(.top, 16)

                    Text("Como foi seu dia?")
                        .foregroundColor(.white)
                        .font(.title2.bold())
                        .multilineTextAlignment(.center)
                        .padding(.top, 4)

                    Spacer().frame(height: 20)

                    VStack(spacing: 0) {
                        YesNoRow(label: "Me cuidei bem hoje", value: $selfCareGood, accent: Self.accent)
                        Divider().background(Color.white.opacity(0.2))
                        YesNoRow(label: "Respirei com facilidade ao longo do dia", value: $daytimeBreathingEase, accent: Self.accent)
                        Divider().background(Color.white.opacity(0.2))
                        YesNoRow(label: "Consegui fazer tudo que tinha planejado hoje", value: $activityAsPlanned, accent: Self.accent)
                    }
                    .padding(16)
                    .background(Color.white.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    Spacer().frame(height: 12)

                    RescueInhalerUsesCard(value: $rescueInhalerUses)

                    Spacer().frame(height: 24)

                    Button(action: {
                        guard let selfCare = selfCareGood, let rescue = rescueInhalerUses,
                              let breathing = daytimeBreathingEase, let activity = activityAsPlanned else { return }
                        onSubmit(selfCare, rescue, breathing, activity)
                    }) {
                        Text("Confirmar")
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.white.opacity(canSubmit ? 1 : 0.3))
                            .foregroundColor(canSubmit ? Self.accent : .white.opacity(0.6))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .disabled(!canSubmit)

                    if !canSubmit {
                        Text("Responda todas as perguntas para confirmar.")
                            .foregroundColor(.white.opacity(0.8))
                            .font(.caption)
                            .multilineTextAlignment(.center)
                            .padding(.top, 8)
                    }

                    Text("Seus dados ajudam a melhorar seu cuidado.")
                        .foregroundColor(.white.opacity(0.5))
                        .font(.caption2)
                        .multilineTextAlignment(.center)
                        .padding(.top, 12)

                    Spacer().frame(height: 40)
                }
                .padding(.horizontal, 28)
            }
        }
    }
}

// MARK: - Critical Wellbeing Card

private struct CriticalWellbeingCardView: View {
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color(red: 0.13, green: 0.13, blue: 0.13).ignoresSafeArea()
            VStack(spacing: 16) {
                Text("⚠️").font(.system(size: 48))

                Text("Percebemos um dia difícil")
                    .foregroundColor(.white)
                    .font(.title3.bold())
                    .multilineTextAlignment(.center)

                Text("Você registrou bem-estar muito baixo. Sabe como agir se precisar de apoio?")
                    .foregroundColor(.white.opacity(0.7))
                    .font(.subheadline)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 8)

                Button(action: {
                    if let url = URL(string: "https://afilaxy.com/guia") {
                        UIApplication.shared.open(url)
                    }
                    onDismiss()
                }) {
                    Text("Ver Guia de Suporte")
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color(red: 0.3, green: 0.69, blue: 0.31))
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                Button(action: onDismiss) {
                    Text("Estou bem")
                        .foregroundColor(.white.opacity(0.5))
                        .font(.subheadline)
                }
            }
            .padding(28)
            .background(Color(red: 0.17, green: 0.17, blue: 0.17))
            .clipShape(RoundedRectangle(cornerRadius: 20))
            .padding(28)
        }
    }
}

// MARK: - Pergunta Sim/Não sem resposta pré-marcada

private struct YesNoRow: View {
    let label: String
    @Binding var value: Bool? // nil = ainda não respondido
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .foregroundColor(.white)
                .font(.subheadline.weight(.medium))
                .frame(maxWidth: .infinity, alignment: .leading)
            HStack(spacing: 8) {
                option(true, "Sim")
                option(false, "Não")
            }
        }
        .padding(.vertical, 8)
    }

    private func option(_ answer: Bool, _ text: String) -> some View {
        let selected = value == answer
        return Button(action: { value = answer }) {
            Text(text)
                .fontWeight(.bold)
                .foregroundColor(selected ? accent : .white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(selected ? Color.white : Color.white.opacity(0.12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.white.opacity(0.3), lineWidth: selected ? 0 : 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

// MARK: - Rescue inhaler uses (só no check-in noturno)
// Alimenta o motor de risco (RiskScoreEngine, shared) como aproximação do critério
// da GINA de uso de resgate >2x/semana.

private struct RescueInhalerUsesCard: View {
    @Binding var value: Int? // nil = ainda não respondido

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                Text("💊").font(.title3)
                Text("Quantas vezes usou a bombinha de resgate hoje?")
                    .foregroundColor(.white)
                    .font(.subheadline.weight(.medium))
            }
            Text("Isso ajuda a acompanhar se o uso está dentro do esperado.")
                .foregroundColor(.white.opacity(0.6))
                .font(.caption2)

            HStack(spacing: 8) {
                ForEach([0, 1, 2, 3], id: \.self) { option in
                    let selected = value == option
                    let label = option == 3 ? "3+" : "\(option)"
                    Button(action: { value = option }) {
                        Text(label)
                            .fontWeight(.bold)
                            .foregroundColor(selected ? Color(red: 0.1, green: 0.14, blue: 0.49) : .white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(selected ? Color.white : Color.white.opacity(0.12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.white.opacity(0.3), lineWidth: selected ? 0 : 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
        }
        .padding(16)
        .background(Color.white.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 16))
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
                     ? "Obrigado!\nContinue cuidando de você durante o dia."
                     : "Obrigado pelo registro!\nSeus dados ajudam a entender seu bem-estar.")
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
