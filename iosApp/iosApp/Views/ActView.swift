import SwiftUI
import shared

// MARK: - ACT (Asthma Control Test) — instrumento validado de 5 perguntas, janela
// retrospectiva de 4 semanas. Perguntas e opções reproduzidas literalmente do teste
// público de um pneumologista parceiro (Dr. Carlos Nogueira). Cortes (20-25/15-19/5-14)
// conferidos empiricamente contra a mesma fonte.
//
// Depois das 5 perguntas vêm 2 perguntas complementares de sim/não sobre crise grave
// nas mesmas 4 semanas (definição da GINA). Elas NÃO fazem parte do ACT e não entram
// no escore — alimentam o motor de risco e o relatório (espelha ActScreen.kt).

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

private let severeExacerbationQuestions: [String] = [
    "Nas últimas 4 semanas, você precisou tomar corticoide em comprimido ou xarope (ex.: prednisolona) por 3 dias ou mais por causa da asma?",
    "Nas últimas 4 semanas, você foi ao pronto-socorro/UPA ou ficou internado por causa da asma?"
]

/// Intervalo recomendado entre dois ACTs (janela de 4 semanas do instrumento) —
/// mesmo valor de ACT_INTERVAL_DAYS no shared.
private let actIntervalDays = 28

private func actDaysSince(_ result: AsthmaControlTestResult) -> Int {
    let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
    return max(0, Int((nowMs - result.timestamp) / (24 * 60 * 60 * 1000)))
}

private func daysAgoText(_ days: Int) -> String {
    switch days {
    case 0: return "hoje"
    case 1: return "ontem"
    default: return "há \(days) dias"
    }
}

struct ActView: View {
    @StateObject private var wrapper: ActViewModelWrapper = {
        if let vm = ViewModelProvider.shared.getActViewModel() {
            return ActViewModelWrapper(vm)
        }
        return ActViewModelWrapper.empty()
    }()
    @State private var showQuestionnaire = false
    @State private var showRecentWarning = false

    /// Refazer antes de 28 dias repete boa parte da mesma janela do ACT — só avisa, não bloqueia.
    private func startTest() {
        if let latest = wrapper.state?.latestResult, actDaysSince(latest) < actIntervalDays {
            showRecentWarning = true
        } else {
            wrapper.startNewTest()
            showQuestionnaire = true
        }
    }

    var body: some View {
        Group {
            if wrapper.state?.isSubmitted == true, let result = wrapper.state?.latestResult {
                ActResultContent(
                    result: result,
                    onRedo: { startTest() }
                )
            } else if showQuestionnaire {
                ActQuestionnaireContent(
                    isLoading: wrapper.state?.isLoading == true,
                    onSubmit: { answers, steroid, er in
                        wrapper.submitTest(answers: answers, oralSteroidCourse4w: steroid, emergencyVisit4w: er)
                    }
                )
            } else if wrapper.state?.isLoading == true && wrapper.state?.latestResult == nil {
                ProgressView()
            } else {
                ActEntryContent(latestResult: wrapper.state?.latestResult, onStart: { startTest() })
            }
        }
        .alert("Refazer o teste agora?", isPresented: $showRecentWarning) {
            Button("Refazer mesmo assim") {
                wrapper.startNewTest()
                showQuestionnaire = true
            }
            Button("Agora não", role: .cancel) {}
        } message: {
            let days = wrapper.state?.latestResult.map { actDaysSince($0) } ?? 0
            Text(
                "O ACT avalia as últimas 4 semanas, e seu último teste foi \(daysAgoText(days)). "
                    + "Refazer agora repete boa parte do mesmo período. O ideal é refazer a cada 4 semanas, "
                    + "mas você pode refazer antes se algo mudou (por exemplo, depois de uma crise ou de uma troca de remédio)."
            )
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
                        let days = actDaysSince(result)
                        Text("Feito \(daysAgoText(days))" + (days < actIntervalDays
                            ? " · próximo em \(max(1, actIntervalDays - days)) dia(s)"
                            : " · já pode refazer"))
                            .font(.caption)
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
    /// Respostas P1–P5, crise grave por corticoide oral, crise grave por pronto-socorro/internação.
    let onSubmit: ([Int32], Bool, Bool) -> Void
    @State private var answers: [Int?] = [nil, nil, nil, nil, nil]
    @State private var severeAnswers: [Bool?] = [nil, nil]

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
                Text("Para o seu médico (não entram na pontuação do ACT)")
                    .font(.headline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                ForEach(severeExacerbationQuestions.indices, id: \.self) { index in
                    YesNoQuestionCard(
                        number: actQuestions.count + index + 1,
                        text: severeExacerbationQuestions[index],
                        selected: severeAnswers[index],
                        onSelect: { severeAnswers[index] = $0 }
                    )
                }
                Button {
                    let values = answers.compactMap { $0 }
                    guard values.count == 5,
                          let steroid = severeAnswers[0],
                          let er = severeAnswers[1] else { return }
                    onSubmit(values.map { Int32($0) }, steroid, er)
                } label: {
                    if isLoading {
                        ProgressView()
                    } else {
                        Text("Calcular pontuação").frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(isLoading || answers.contains(nil) || severeAnswers.contains(nil))
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

private struct YesNoQuestionCard: View {
    let number: Int
    let text: String
    let selected: Bool?
    let onSelect: (Bool) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\(number). \(text)")
                .font(.subheadline)
                .fontWeight(.semibold)
            ForEach([true, false], id: \.self) { value in
                Button {
                    onSelect(value)
                } label: {
                    HStack {
                        Image(systemName: selected == value ? "largecircle.fill.circle" : "circle")
                            .foregroundColor(selected == value ? .accentColor : .secondary)
                        Text(value ? "Sim" : "Não")
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

                if result.hadSevereExacerbation {
                    Text(
                        "Você relatou uma crise grave nas últimas 4 semanas (corticoide em comprimido, pronto-socorro ou internação). "
                            + "Leve essa informação ao seu médico: uma crise grave aumenta o risco de novas crises, mesmo quando os sintomas melhoram."
                    )
                        .font(.subheadline)
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.red.opacity(0.12))
                        .cornerRadius(16)
                }

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
