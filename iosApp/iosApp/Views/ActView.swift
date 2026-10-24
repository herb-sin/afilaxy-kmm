import SwiftUI
import shared

// MARK: - ACT (Asthma Control Test) — instrumento validado de 5 perguntas, janela
// retrospectiva de 4 semanas. Perguntas e opções reproduzidas literalmente do teste
// público de um pneumologista parceiro (Dr. Carlos Nogueira). Cortes (20-25/15-19/5-14)
// conferidos empiricamente contra a mesma fonte.

private struct ActQuestion {
    let text: String
    let options: [String]
}

private let actQuestions: [ActQuestion] = [
    ActQuestion(
        text: "Nas últimas 4 semanas, quanto do tempo a asma prejudicou suas atividades no trabalho, escola ou em casa?",
        options: ["O tempo todo", "A maior parte do tempo", "Alguma parte do tempo", "Um pouco do tempo", "Nenhum tempo"]
    ),
    ActQuestion(
        text: "Durante as últimas 4 semanas, quantas vezes você teve falta de ar?",
        options: ["Mais do que uma vez ao dia", "1 vez por dia", "3 a 6 vezes por semana", "1 ou 2 vezes por semana", "Nenhuma vez"]
    ),
    ActQuestion(
        text: "Durante as últimas 4 semanas, com que frequência os sintomas de asma acordaram você à noite ou mais cedo do que o normal pela manhã?",
        options: ["4 ou mais noites por semana", "2 a 3 noites por semana", "1 vez por semana", "1 ou 2 vezes", "Nenhuma vez"]
    ),
    ActQuestion(
        text: "Durante as últimas 4 semanas, com que frequência você usou medicação de alívio (bombinha ou nebulizador)?",
        options: ["3 ou mais vezes por dia", "1 ou 2 vezes por dia", "2 ou 3 vezes por semana", "1 vez por semana ou menos", "Nenhuma vez"]
    ),
    ActQuestion(
        text: "Como você avaliaria o controle de sua asma nas últimas 4 semanas?",
        options: ["Nada controlada", "Pouco controlada", "Moderadamente controlada", "Bem controlada", "Totalmente controlada"]
    )
]

struct ActView: View {
    @StateObject private var wrapper: ActViewModelWrapper = {
        if let vm = ViewModelProvider.shared.getActViewModel() {
            return ActViewModelWrapper(vm)
        }
        return ActViewModelWrapper.empty()
    }()
    @State private var showQuestionnaire = false

    var body: some View {
        Group {
            if wrapper.state?.isSubmitted == true, let result = wrapper.state?.latestResult {
                ActResultContent(
                    result: result,
                    onRedo: { wrapper.startNewTest(); showQuestionnaire = true }
                )
            } else if showQuestionnaire {
                ActQuestionnaireContent(
                    isLoading: wrapper.state?.isLoading == true,
                    onSubmit: { a, b, c, d, e in
                        wrapper.submitTest(
                            activityImpairment: a, shortnessOfBreath: b, nighttimeSymptoms: c,
                            rescueMedicationUse: d, selfRatedControl: e
                        )
                    }
                )
            } else if wrapper.state?.isLoading == true && wrapper.state?.latestResult == nil {
                ProgressView()
            } else {
                ActEntryContent(latestResult: wrapper.state?.latestResult, onStart: { showQuestionnaire = true })
            }
        }
        .navigationTitle("Teste de Controle da Asma")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { wrapper.loadLatest() }
    }
}

private struct ActEntryContent: View {
    let latestResult: AsthmaControlTestResult?
    let onStart: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("O ACT é um questionário validado de 5 perguntas sobre as últimas 4 semanas, usado por pneumologistas pra avaliar o controle da sua asma.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                if let result = latestResult {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Último resultado").font(.caption).foregroundColor(.secondary)
                        Text("\(result.score) pontos").font(.largeTitle).fontWeight(.bold)
                        Text(result.level.label).fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(20)
                    .background(levelColor(result.level).opacity(0.15))
                    .cornerRadius(16)
                } else {
                    Text("Você ainda não fez o ACT.")
                        .padding(20)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(.secondarySystemBackground))
                        .cornerRadius(16)
                }

                Button(action: onStart) {
                    Text(latestResult == nil ? "Fazer teste" : "Refazer teste")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                if latestResult != nil {
                    NavigationLink(value: AppRoute.actHistory) {
                        Text("Ver histórico").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }

                Text("Este teste é informativo e não substitui avaliação médica. Leva menos de 2 minutos.")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(20)
        }
    }
}

private struct ActQuestionnaireContent: View {
    let isLoading: Bool
    let onSubmit: (Int32, Int32, Int32, Int32, Int32) -> Void
    @State private var answers: [Int?] = [nil, nil, nil, nil, nil]

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                ForEach(actQuestions.indices, id: \.self) { index in
                    ActQuestionCard(
                        index: index,
                        question: actQuestions[index],
                        selected: answers[index],
                        onSelect: { answers[index] = $0 }
                    )
                }
                Button {
                    let values = answers.compactMap { $0 }
                    guard values.count == 5 else { return }
                    onSubmit(Int32(values[0]), Int32(values[1]), Int32(values[2]), Int32(values[3]), Int32(values[4]))
                } label: {
                    if isLoading {
                        ProgressView()
                    } else {
                        Text("Calcular pontuação").frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(isLoading || answers.contains(nil))
            }
            .padding(20)
        }
    }
}

private struct ActQuestionCard: View {
    let index: Int
    let question: ActQuestion
    let selected: Int?
    let onSelect: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(index + 1). \(question.text)")
                .font(.subheadline)
                .fontWeight(.semibold)
            // Opção N vale (N+1) pontos — mesma ordem da fonte original (pior → melhor).
            ForEach(question.options.indices, id: \.self) { optionIndex in
                let value = optionIndex + 1
                Button {
                    onSelect(value)
                } label: {
                    HStack {
                        Image(systemName: selected == value ? "largecircle.fill.circle" : "circle")
                            .foregroundColor(selected == value ? .accentColor : .secondary)
                        Text(question.options[optionIndex])
                            .foregroundColor(.primary)
                        Spacer()
                    }
                }
                .buttonStyle(.plain)
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(16)
    }
}

private struct ActResultContent: View {
    let result: AsthmaControlTestResult
    let onRedo: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 8) {
                    Text("Seu resultado").font(.headline)
                    Text("\(result.score) pontos").font(.system(size: 40, weight: .bold))
                    Text(result.level.label).font(.title3).fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .padding(24)
                .background(levelColor(result.level).opacity(0.15))
                .cornerRadius(20)

                Text(levelMessage(result.level)).font(.subheadline)

                Text("Este resultado é informativo e não substitui consulta médica. Leve-o ao seu pneumologista.")
                    .font(.caption)
                    .foregroundColor(.secondary)

                NavigationLink(value: AppRoute.actHistory) {
                    Text("Ver histórico").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)

                Button(action: onRedo) {
                    Text("Refazer teste").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderless)
            }
            .padding(20)
        }
    }
}

private func levelMessage(_ level: AsthmaControlLevel) -> String {
    switch level {
    case .wellControlled:
        return "Sua asma parece estar bem controlada nas últimas 4 semanas. Continue com o tratamento e acompanhamento médico regular."
    case .partiallyControlled:
        return "Sua asma não está totalmente controlada. Considere conversar com seu pneumologista para ajustar o tratamento."
    case .notControlled:
        return "Sua asma parece estar fora de controle. Procure um pneumologista para reavaliar seu plano de tratamento."
    default:
        return ""
    }
}

private func levelColor(_ level: AsthmaControlLevel) -> Color {
    switch level {
    case .wellControlled: return .green
    case .partiallyControlled: return .orange
    case .notControlled: return .red
    default: return .gray
    }
}
