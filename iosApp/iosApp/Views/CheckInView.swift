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
                    onSubmit: { a, b, c in
                        wrapper.submitMorning(wellbeingA: a, wellbeingB: b, wellbeingC: c)
                    }
                )
            } else {
                EveningCheckInContent(
                    onSubmit: { a, b, c in
                        wrapper.submitEvening(wellbeingA: a, wellbeingB: b, wellbeingC: c)
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
            if let answer = quickAnswer,
               wrapper.state?.isLoading != true,
               wrapper.state?.alreadyDoneToday != true,
               wrapper.state?.isSubmitted != true {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    if type == .morning {
                        wrapper.submitMorning(wellbeingA: answer, wellbeingB: answer, wellbeingC: answer)
                    } else {
                        wrapper.submitEvening(wellbeingA: answer, wellbeingB: answer, wellbeingC: answer)
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

    @State private var wellbeingA = true  // "Dormi bem esta noite"
    @State private var wellbeingB = true  // "Me sinto bem esta manhã"
    @State private var wellbeingC = true  // "Estou com boa energia"

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
                        CheckInToggleRow(label: "Dormi bem esta noite", isOn: $wellbeingA)
                        Divider().background(Color.white.opacity(0.2))
                        CheckInToggleRow(label: "Me sinto bem esta manhã", isOn: $wellbeingB)
                        Divider().background(Color.white.opacity(0.2))
                        CheckInToggleRow(label: "Estou com boa energia", isOn: $wellbeingC)
                    }
                    .padding(16)
                    .background(Color.white.opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    Spacer().frame(height: 24)

                    Button(action: { onSubmit(wellbeingA, wellbeingB, wellbeingC) }) {
                        HStack {
                            Image(systemName: "checkmark.circle.fill")
                            Text("Confirmar")
                                .fontWeight(.bold)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(Color.white)
                        .foregroundColor(Color(red: 0.9, green: 0.32, blue: 0))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
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
    let onSubmit: (Bool, Bool, Bool) -> Void

    @State private var wellbeingA = true  // "Tive um bom dia"
    @State private var wellbeingB = false // "Pratiquei atividade física"
    @State private var wellbeingC = true  // "Me cuidei bem hoje"

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
                        CheckInToggleRow(label: "Tive um bom dia", isOn: $wellbeingA)
                        Divider().background(Color.white.opacity(0.2))
                        CheckInToggleRow(label: "Pratiquei atividade física", isOn: $wellbeingB)
                        Divider().background(Color.white.opacity(0.2))
                        CheckInToggleRow(label: "Me cuidei bem hoje", isOn: $wellbeingC)
                    }
                    .padding(16)
                    .background(Color.white.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    Spacer().frame(height: 24)

                    Button(action: { onSubmit(wellbeingA, wellbeingB, wellbeingC) }) {
                        Text("Confirmar")
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .frame(height: 54)
                            .background(Color.white)
                            .foregroundColor(Color(red: 0.1, green: 0.14, blue: 0.49))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
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

// MARK: - Shared toggle row

private struct CheckInToggleRow: View {
    let label: String
    @Binding var isOn: Bool

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.white)
                .font(.subheadline)
                .fontWeight(isOn ? .semibold : .regular)
                .frame(maxWidth: .infinity, alignment: .leading)
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(.white)
        }
        .padding(.vertical, 10)
        .contentShape(Rectangle())
        .onTapGesture { isOn.toggle() }
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
