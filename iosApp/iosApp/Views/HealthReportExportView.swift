import SwiftUI
import UIKit
import FirebaseAuth
import FirebaseFirestore

// MARK: - Main View

struct HealthReportExportView: View {
    @Environment(\.dismiss) private var dismiss

    enum Step {
        case idle
        case generating
        case failed(String)
    }

    @State private var step: Step = .idle
    @State private var shareItems: [Any] = []
    @State private var showShareSheet = false

    var body: some View {
        NavigationStack {
            Group {
                switch step {
                case .idle:
                    idleView
                case .generating:
                    generatingView
                case .failed(let message):
                    failedView(message: message)
                }
            }
            .navigationTitle("Relatório de Saúde")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fechar") { dismiss() }
                }
            }
        }
        .sheet(isPresented: $showShareSheet) {
            ActivityShareSheet(items: shareItems)
        }
    }

    // MARK: - Idle

    private var idleView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "doc.text.fill")
                        .foregroundColor(.afiPrimary)
                        .font(.title2)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Relatório completo")
                            .font(.headline)
                        Text("Gere um PDF com seus check-ins e pedidos de ajuda das últimas 4 semanas, "
                             + "seus Testes de Controle da Asma e seu Perfil Médico — pronto para compartilhar "
                             + "com quem você quiser, sem precisar informar um profissional específico.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemGray6))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 8) {
                        Image(systemName: "doc.text.fill").foregroundColor(.afiPrimary)
                        Text("Conteúdo do Relatório (4 semanas)").font(.headline)
                    }
                    ForEach([
                        "Controle de sintomas pelos critérios da GINA",
                        "Teste de Controle da Asma (último resultado e histórico)",
                        "Crises, pedidos de ajuda e fatores de risco",
                        "Adesão ao monitoramento e uso de bombinha de resgate",
                        "Check-ins matinal e noturno",
                        "Perfil Médico (alergias, medicamentos, acessibilidade)"
                    ], id: \.self) { item in
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark")
                                .foregroundColor(.afiPrimary).font(.caption2)
                            Text(item).font(.subheadline).foregroundColor(.secondary)
                        }
                    }
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemGray6))
                .cornerRadius(12)

                Button {
                    Task { await generateReport() }
                } label: {
                    HStack {
                        Image(systemName: "arrow.down.doc.fill")
                        Text("Gerar Relatório PDF")
                            .fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity).frame(height: 54)
                    .background(Color.afiPrimary)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                Spacer(minLength: 40)
            }
            .padding()
        }
    }

    // MARK: - Step 3: Generating

    private var generatingView: some View {
        VStack(spacing: 24) {
            Spacer()
            ProgressView().scaleEffect(1.5)
            Text("Gerando relatório...").font(.headline)
            Text("Coletando seus dados das últimas 4 semanas.")
                .font(.subheadline).foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding()
    }

    // MARK: - Step 4: Failed

    private func failedView(message: String) -> some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48)).foregroundColor(.orange)
            Text("Erro ao gerar relatório").font(.headline)
            Text(message)
                .font(.subheadline).foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Button("Tentar novamente") { step = .idle }
                .buttonStyle(.borderedProminent)
            Spacer()
        }
        .padding()
    }

    // MARK: - Report Generation

    private func generateReport() async {
        step = .generating
        guard let uid = Auth.auth().currentUser?.uid else {
            step = .failed("Sessão expirada.")
            return
        }
        do {
            // Janela de 4 semanas (28 dias): é o período dos critérios de controle da GINA.
            let now = Date()
            // Em dias de calendário: de 00:00 de 27 dias atrás até agora (hoje incluso), para que
            // o período, as semanas S1–S4 e os dados do PDF usem as mesmas datas.
            let cal = Calendar.current
            let windowStart = cal.date(byAdding: .day, value: -(ReportWindow.days - 1),
                                       to: cal.startOfDay(for: now)) ?? now
            let cutoffMs = Int64(windowStart.timeIntervalSince1970 * 1000)

            let checkInDocs = try await Firestore.firestore()
                .collection("checkins").document(uid)
                .collection("responses")
                .whereField("timestamp", isGreaterThanOrEqualTo: cutoffMs)
                .getDocuments()

            let statsDoc = try? await Firestore.firestore()
                .collection("user_stats").document(uid).getDocument()
            let userDoc = try? await Firestore.firestore()
                .collection("users").document(uid).getDocument()
            let emergencyCount = emergencyCountInWindow(statsDoc: statsDoc)
            let patientName = Auth.auth().currentUser?.displayName ?? "Usuário"

            // ACT: histórico completo (não só as 4 semanas) — o teste é mensal, então a janela do
            // relatório mostraria no máximo um resultado e esconderia a tendência.
            let actDocs = try? await Firestore.firestore()
                .collection("act").document(uid)
                .collection("results")
                .getDocuments()
            let actResults = (actDocs?.documents ?? [])
                .map { ActResultData(doc: $0) }
                .sorted { $0.timestamp > $1.timestamp }
                .prefix(ActResultData.historyLimit)

            // Filtro só por requesterId (exigido pela regra de list) e período aplicado no
            // cliente — evita índice composto, e o volume de pedidos por usuário é pequeno.
            let emergencyDocs = try? await Firestore.firestore()
                .collection("emergency_requests")
                .whereField("requesterId", isEqualTo: uid)
                .getDocuments()
            let emergencies = (emergencyDocs?.documents ?? [])
                .map { EmergencyEventData(doc: $0) }
                .filter { $0.timestamp >= cutoffMs }
                .sorted { $0.timestamp > $1.timestamp }

            let report = WellbeingReportData(
                patientName: patientName,
                healthProfile: HealthProfileData(doc: userDoc),
                checkIns: checkInDocs.documents.map { WellbeingCheckIn(doc: $0) },
                emergencyCount: emergencyCount,
                emergencies: emergencies,
                actResults: Array(actResults),
                windowStartMs: cutoffMs,
                generatedAt: now
            )

            guard let pdfData = WellbeingPDFGenerator.generate(report: report),
                  let fileURL = writePDF(pdfData) else {
                step = .failed("Não foi possível gerar o PDF. Tente novamente.")
                return
            }
            shareItems = [fileURL]
            showShareSheet = true
            step = .idle
        } catch {
            step = .failed("Erro ao carregar dados: \(error.localizedDescription)")
        }
    }

    private func emergencyCountInWindow(statsDoc: DocumentSnapshot?) -> Int {
        guard let data = statsDoc?.data(),
              let daily = data["dailyCount"] as? [String: Any] else {
            return (statsDoc?.data()?["totalEmergencies"] as? NSNumber)?.intValue ?? 0
        }
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "UTC")!
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        fmt.timeZone = TimeZone(identifier: "UTC")
        fmt.locale = Locale(identifier: "en_US_POSIX")
        return (0..<ReportWindow.days).reduce(0) { sum, offset in
            let d = cal.date(byAdding: .day, value: -offset, to: Date()) ?? Date()
            return sum + ((daily[fmt.string(from: d)] as? NSNumber)?.intValue ?? 0)
        }
    }

    private func writePDF(_ data: Data) -> URL? {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("relatorio_afilaxy_\(Int(Date().timeIntervalSince1970)).pdf")
        return (try? data.write(to: url)) != nil ? url : nil
    }
}

// MARK: - PDF Generator

enum WellbeingPDFGenerator {
    private static let margin: CGFloat   = 50
    private static let pageW: CGFloat    = 595.2
    private static let pageH: CGFloat    = 841.8
    private static let footerReserve: CGFloat = 60
    private static var contentW: CGFloat { pageW - margin * 2 }
    private static var bottomLimit: CGFloat { pageH - footerReserve }

    private static let primaryColor  = UIColor(red: 0,    green: 0.384, blue: 0.561, alpha: 1)
    private static let morningColor  = UIColor(red: 0.9,  green: 0.32,  blue: 0,     alpha: 1)
    private static let eveningColor  = UIColor(red: 0.1,  green: 0.14,  blue: 0.49,  alpha: 1)
    private static let criticalColor = UIColor(red: 0.73, green: 0.1,   blue: 0.1,   alpha: 1)

    // Cursor de paginação: cada seção/linha pede espaço antes de desenhar, e uma
    // nova página é aberta no mesmo UIGraphicsPDFRendererContext quando não há
    // espaço suficiente antes da área reservada ao rodapé. Autocontido (recebe
    // margin/contentW/bottomLimit no init) para não depender de acesso a
    // membros privados do tipo que o envolve.
    private final class PDFCursor {
        private let context: UIGraphicsPDFRendererContext
        private let margin: CGFloat
        private let bottomLimit: CGFloat
        var y: CGFloat = 0
        private var pageNumber = 1

        init(context: UIGraphicsPDFRendererContext, margin: CGFloat, bottomLimit: CGFloat) {
            self.context = context
            self.margin = margin
            self.bottomLimit = bottomLimit
            context.beginPage()
        }

        func breakIfNeeded(_ neededHeight: CGFloat) {
            if y + neededHeight > bottomLimit { newPage() }
        }

        func newPage() {
            context.beginPage()
            pageNumber += 1
            y = margin
            let text = "AFILAXY — Relatório de Monitoramento de Saúde (continuação, pág. \(pageNumber))"
            (text as NSString).draw(at: CGPoint(x: margin, y: y),
                withAttributes: [.font: UIFont.systemFont(ofSize: 9), .foregroundColor: UIColor.gray])
            y += 20
        }
    }

    static func generate(report: WellbeingReportData) -> Data? {
        let renderer = UIGraphicsPDFRenderer(
            bounds: CGRect(x: 0, y: 0, width: pageW, height: pageH)
        )
        return renderer.pdfData { ctx in
            let cursor = PDFCursor(context: ctx, margin: margin, bottomLimit: bottomLimit)
            // Ordem pensada para o médico: o que decide a consulta (GINA, ACT, crises e
            // fatores de risco) vem primeiro; os detalhes do monitoramento depois.
            drawHeader(report: report, cursor: cursor)
            drawInfoBox(report: report, cursor: cursor)
            drawGinaSection(report: report, cursor: cursor)
            drawActSection(report: report, cursor: cursor)
            drawRiskSection(report: report, cursor: cursor)
            drawAdherenceSection(report: report, cursor: cursor)
            drawMorningSection(report: report, cursor: cursor)
            drawEveningSection(report: report, cursor: cursor)
            drawHealthProfileSection(report: report, cursor: cursor)
            drawFooter(report: report, cursor: cursor)
        }
    }

    // MARK: Header

    private static func drawHeader(report: WellbeingReportData, cursor: PDFCursor) {
        let h: CGFloat = 88
        fillRect(CGRect(x: 0, y: 0, width: pageW, height: h), color: primaryColor)

        draw("AFILAXY", at: CGPoint(x: margin, y: 18),
             font: .boldSystemFont(ofSize: 22), color: .white)
        draw("Relatório de Monitoramento de Saúde", at: CGPoint(x: margin, y: 46),
             font: .systemFont(ofSize: 13, weight: .medium),
             color: UIColor.white.withAlphaComponent(0.9))

        let dateStr = "Gerado em \(dateFormatted(report.generatedAt))"
        let dW = measure(dateStr, font: .systemFont(ofSize: 10)).width
        draw(dateStr, at: CGPoint(x: pageW - margin - dW, y: 40),
             font: .systemFont(ofSize: 10), color: UIColor.white.withAlphaComponent(0.8))

        cursor.y = h + 22
    }

    // MARK: Info Box

    private static func drawInfoBox(report: WellbeingReportData, cursor: PDFCursor) {
        let h: CGFloat = 96
        cursor.breakIfNeeded(h + 20)
        let y = cursor.y
        fillRect(CGRect(x: margin, y: y, width: contentW, height: h),
                 color: UIColor(white: 0.96, alpha: 1))

        let lx = margin + 10
        draw("PACIENTE", at: CGPoint(x: lx, y: y + 10),
             font: .systemFont(ofSize: 8, weight: .medium), color: .gray)
        draw(report.patientName, at: CGPoint(x: lx, y: y + 22),
             font: .boldSystemFont(ofSize: 12), color: .black)
        draw("PERÍODO", at: CGPoint(x: lx, y: y + 46),
             font: .systemFont(ofSize: 8, weight: .medium), color: .gray)
        draw(periodString(report), at: CGPoint(x: lx, y: y + 58),
             font: .systemFont(ofSize: 10), color: .darkGray)

        let rx = pageW - margin - 205
        draw("ORIGEM", at: CGPoint(x: rx, y: y + 10),
             font: .systemFont(ofSize: 8, weight: .medium), color: .gray)
        draw("Autorrelato do paciente", at: CGPoint(x: rx, y: y + 22),
             font: .boldSystemFont(ofSize: 11), color: .black)
        draw("Sem revisão clínica prévia", at: CGPoint(x: rx, y: y + 40),
             font: .systemFont(ofSize: 10), color: .darkGray)
        draw("ANO DE NASCIMENTO", at: CGPoint(x: rx, y: y + 60),
             font: .systemFont(ofSize: 8, weight: .medium), color: .gray)
        draw(birthYearText(report), at: CGPoint(x: rx, y: y + 72),
             font: .boldSystemFont(ofSize: 11), color: .black)

        cursor.y = y + h + 20
    }

    // MARK: Sections

    private static func drawGinaSection(report: WellbeingReportData, cursor: PDFCursor) {
        let g = report.gina
        drawSectionHeader("1. CONTROLE DE SINTOMAS — CRITÉRIOS GINA (4 SEMANAS)", cursor: cursor, color: primaryColor)
        var present = "\(g.presentCount) de 4"
        if g.noDataCount > 0 { present += " (\(g.noDataCount) sem dados)" }
        let wf = DateFormatter()
        wf.dateFormat = "dd/MM"
        wf.locale = Locale(identifier: "pt_BR")
        let weeksText = g.weeks.enumerated().map { i, w in
            "S\(i + 1) \(wf.string(from: msDate(w.startMs)))–\(wf.string(from: msDate(w.startMs + ReportWindow.weekMs - ReportWindow.dayMs)))"
        }.joined(separator: " · ")
        let coverageText = g.weeks.enumerated().map { i, w in
            "S\(i + 1): \(w.morningCount) manhãs, \(w.eveningCount) noites"
        }.joined(separator: " · ")

        var rows: [(String, String)] = [("Critérios presentes", present)]
        for c in g.criteria { rows.append((c.label, c.valueText)) }
        rows.append(("Semanas", weeksText))
        rows.append(("Check-ins por semana", coverageText))
        rows.append(("Regra", "Sintomas diurnos e resgate: presente se mais de 2 ocorrências em alguma semana. Despertar e limitação: presente se ocorreu ao menos uma vez. — = semana sem resposta."))
        rows.append(("Fontes",
                     "Sintomas diurnos: \"Respirei com facilidade ao longo do dia?\" = Não. "
                     + "Despertar: \"Meu sono foi tranquilo, sem interrupções?\" = Não. "
                     + "Resgate: nº de usos informado no check-in noturno. "
                     + "Limitação: \"Consegui fazer tudo que tinha planejado?\" = Não."))
        rows.append(("Limitações",
                     "Perguntas de bem-estar, não específicas de asma: despertar e limitação podem ter outras causas. "
                     + "O uso de resgate antes de exercício não é separado. Dias sem check-in não entram nas contagens."))
        rows.append(("Referência GINA", "0 critérios: bem controlada; 1–2: parcialmente controlada; 3–4: não controlada. A interpretação cabe ao profissional de saúde."))
        drawPlainRows(rows, cursor: cursor)
    }

    private static func drawAdherenceSection(report: WellbeingReportData, cursor: PDFCursor) {
        let total = report.checkIns.count
        let possible = ReportWindow.possibleCheckIns
        let adherence = Int(Double(total) / Double(possible) * 100)
        let critical = report.checkIns.filter { $0.isCritical }.count
        let rescueAnswers = report.checkIns.compactMap { $0.rescueInhalerUses }
        let rescueValue = rescueAnswers.isEmpty
            ? "Não informado"
            : "\(rescueAnswers.reduce(0, +)) vez(es) em \(rescueAnswers.count) dia(s) informado(s)"
        drawSectionHeader("4. ADESÃO AO MONITORAMENTO", cursor: cursor, color: primaryColor)
        drawPlainRows([
            ("Check-ins realizados", "\(total) de \(possible) possíveis"),
            ("Taxa de adesão ao monitoramento", "\(adherence)%"),
            ("Uso de bombinha de resgate", rescueValue),
            ("Dias com bem-estar mínimo registrado", "\(critical) ocorrência(s)")
        ], cursor: cursor)
    }

    private static func drawActSection(report: WellbeingReportData, cursor: PDFCursor) {
        drawSectionHeader("2. CONTROLE DA ASMA — ACT", cursor: cursor, color: primaryColor)
        guard let latest = report.actResults.first else {
            drawPlainRows([("Teste de Controle da Asma (ACT)", "Nunca respondido")], cursor: cursor)
            return
        }
        var rows: [(String, String)] = [
            ("Último resultado (\(dateFormatted(msDate(latest.timestamp))))",
             "\(latest.score)/25 — \(latest.levelLabel)")
        ]
        for (i, label) in ActResultData.answerLabels.enumerated() {
            let answer = i < latest.answers.count ? latest.answers[i] : 0
            rows.append((label, "\(answer)/5"))
        }
        rows.append(("Corticoide oral ≥3 dias (4 semanas)", yesNo(latest.oralSteroidCourse4w)))
        rows.append(("Pronto-socorro/UPA ou internação (4 semanas)", yesNo(latest.emergencyVisit4w)))
        for previous in report.actResults.dropFirst() {
            let severe = previous.hadSevereExacerbation ? " · crise grave relatada" : ""
            rows.append(("Anterior (\(dateFormatted(msDate(previous.timestamp))))",
                         "\(previous.score)/25 — \(previous.levelLabel)\(severe)"))
        }
        rows.append(("Crise grave",
                     "Definição da GINA: corticoide oral por 3 dias ou mais, pronto-socorro ou internação. Perguntas complementares, fora da pontuação do ACT."))
        rows.append(("Interpretação",
                     "Itens de 1 (pior) a 5 (melhor). Total ≥20: bem controlada; 15–19: parcialmente; ≤14: não controlada. Respondido pelo paciente."))
        drawPlainRows(rows, cursor: cursor)
    }

    private static func drawMorningSection(report: WellbeingReportData, cursor: PDFCursor) {
        let morning = report.checkIns.filter { $0.isMorning }
        drawSectionHeader("5. CHECK-IN MATINAL — % DE RESPOSTAS \"SIM\"", cursor: cursor, color: morningColor)
        drawBarRows([
            ("Registros de manhã: \(morning.count)", nil),
            answeredRow("\"Meu sono foi tranquilo, sem interrupções\"", morning, \.nighttimeAwakening),
            answeredRow("\"Me sinto bem esta manhã\"", morning, \.morningMoodGood),
            answeredRow("\"Estou com boa energia\"", morning, \.morningEnergyGood)
        ], cursor: cursor, barColor: morningColor)
    }

    private static func drawEveningSection(report: WellbeingReportData, cursor: PDFCursor) {
        let evening = report.checkIns.filter { $0.isEvening }
        drawSectionHeader("6. CHECK-IN NOTURNO — % DE RESPOSTAS \"SIM\"", cursor: cursor, color: eveningColor)
        drawBarRows([
            ("Registros noturnos: \(evening.count)", nil),
            answeredRow("\"Me cuidei bem hoje\"", evening, \.selfCareGood),
            answeredRow("\"Respirei com facilidade ao longo do dia\"", evening, \.daytimeBreathingEase),
            answeredRow("\"Consegui fazer tudo que tinha planejado\"", evening, \.activityAsPlanned)
        ], cursor: cursor, barColor: eveningColor)
    }

    private static func drawRiskSection(report: WellbeingReportData, cursor: PDFCursor) {
        let p = report.healthProfile
        drawSectionHeader("3. CRISES E FATORES DE RISCO", cursor: cursor, color: criticalColor)

        var rows: [(String, String)] = [("Pedidos de ajuda (4 semanas)", "\(report.emergencyCount)")]
        for event in report.emergencies {
            rows.append((dateTimeFormatted(msDate(event.timestamp)), event.detail))
        }

        let recentAct: ActResultData? = {
            guard let first = report.actResults.first, first.timestamp >= report.windowStartMs else { return nil }
            return first
        }()
        var recentSevere: [String] = []
        if let act = recentAct, act.hadSevereExacerbation {
            recentSevere.append("relatada no ACT de \(dateFormatted(msDate(act.timestamp)))")
        }
        for event in report.emergencies where event.emergencyVisitAfter == true {
            recentSevere.append("pronto-socorro/UPA ou internação após pedido de ajuda em \(dateFormatted(msDate(event.timestamp)))")
        }
        let recentSevereText: String
        if !recentSevere.isEmpty {
            recentSevereText = "Sim — " + recentSevere.joined(separator: "; ")
        } else if let act = recentAct {
            recentSevereText = act.oralSteroidCourse4w == nil && act.emergencyVisit4w == nil
                ? "Não perguntado no ACT de \(dateFormatted(msDate(act.timestamp)))"
                : "Não relatada"
        } else {
            recentSevereText = "Nenhuma registrada (sem ACT no período)"
        }
        rows.append(("Crise grave (4 semanas)", recentSevereText))

        let severeHistory: String
        if p.severeExacerbationsUpdatedAt <= 0 {
            severeHistory = "Não informado"
        } else if p.severeExacerbations12m == 0 {
            severeHistory = "Nenhuma (informado em \(dateFormatted(msDate(p.severeExacerbationsUpdatedAt))))"
        } else {
            severeHistory = "\(p.severeExacerbations12m) nos 12 meses anteriores a \(dateFormatted(msDate(p.severeExacerbationsUpdatedAt)))"
        }
        rows.append(("Crises graves em 12 meses", severeHistory))
        rows.append(("UTI ou intubação (alguma vez)", p.everIcuOrIntubated ? "Sim" : "Não informado"))

        var comorbidities: [String] = []
        if p.hasObesity { comorbidities.append("Obesidade") }
        if p.hasRhinitis { comorbidities.append("Rinite alérgica") }
        if p.hasGerd { comorbidities.append("Refluxo / DRGE") }
        if p.hasFoodAllergy { comorbidities.append("Alergia alimentar") }
        if p.hasSleepApnea { comorbidities.append("Apneia do sono") }
        rows.append(("Comorbidades relatadas", comorbidities.isEmpty ? "Nenhuma" : comorbidities.joined(separator: ", ")))
        rows.append(("Não coletado pelo app", "Tabagismo ou exposição à fumaça, adesão ao tratamento de manutenção, técnica inalatória, gestação e função pulmonar."))
        drawPlainRows(rows, cursor: cursor)
    }

    private static func drawHealthProfileSection(report: WellbeingReportData, cursor: PDFCursor) {
        let p = report.healthProfile
        var specificAllergies: [String] = []
        if p.hasFoodAllergy { specificAllergies.append("Alimentar") }
        if p.hasNsaidAllergy { specificAllergies.append("AINEs/aspirina") }
        if p.hasInhalantAllergy { specificAllergies.append("Inalantes (ácaros/pólen/mofo/pelos)") }

        var accessibility: [String] = []
        if p.hasWheelchair { accessibility.append("Cadeirante") }
        if p.hasLowVision { accessibility.append("Baixa visão ou cegueira") }
        if p.hasSpecialCondition { accessibility.append("Outra condição especial") }

        let emergencyContact: String
        if !p.emergencyContactName.isEmpty {
            emergencyContact = p.emergencyContactPhone.isEmpty
                ? p.emergencyContactName
                : "\(p.emergencyContactName) — \(p.emergencyContactPhone)"
        } else {
            emergencyContact = "Não informado"
        }

        // Comorbidades, crises graves e UTI ficam na seção 3 (fatores de risco).
        drawSectionHeader("7. PERFIL MÉDICO", cursor: cursor, color: primaryColor)
        drawPlainRows([
            ("Alergias conhecidas", p.allergiesText.isEmpty ? "Nenhuma relatada" : p.allergiesText.joined(separator: ", ")),
            ("Medicamentos em uso", p.medications.isEmpty ? "Nenhum relatado" : p.medications.joined(separator: ", ")),
            ("Condições relatadas", p.conditions.isEmpty ? "Nenhuma" : p.conditions.joined(separator: ", ")),
            ("Alergias específicas", specificAllergies.isEmpty ? "Nenhuma" : specificAllergies.joined(separator: ", ")),
            ("Acessibilidade", accessibility.isEmpty ? "Nenhuma" : accessibility.joined(separator: ", ")),
            ("Detalhes complementares", p.notes.isEmpty ? "Nenhum" : p.notes),
            ("Contato de emergência", emergencyContact)
        ], cursor: cursor)
    }

    private static func drawFooter(report: WellbeingReportData, cursor: PDFCursor) {
        let footerY = pageH - 52
        if cursor.y > footerY - 10 { cursor.newPage() }
        fillRect(CGRect(x: margin, y: footerY, width: contentW, height: 0.5),
                 color: .lightGray)
        let text = "Este relatório foi gerado automaticamente pelo aplicativo Afilaxy com base nas respostas "
            + "fornecidas pelo próprio usuário no período de \(periodString(report)) (check-ins e pedidos de ajuda; "
            + "o ACT inclui testes anteriores ao período). Não substitui avaliação clínica presencial. "
            + "Gerado em \(dateFormatted(report.generatedAt))."
        let style = NSMutableParagraphStyle()
        style.lineBreakMode = .byWordWrapping
        (text as NSString).draw(
            in: CGRect(x: margin, y: footerY + 6, width: contentW, height: 42),
            withAttributes: [.font: UIFont.systemFont(ofSize: 8),
                             .foregroundColor: UIColor.gray,
                             .paragraphStyle: style]
        )
    }

    // MARK: Row Drawers

    private static func drawSectionHeader(_ title: String, cursor: PDFCursor, color: UIColor) {
        cursor.breakIfNeeded(22 + 21)
        let y = cursor.y
        draw(title, at: CGPoint(x: margin, y: y), font: .boldSystemFont(ofSize: 11), color: color)
        fillRect(CGRect(x: margin, y: y + 16, width: contentW, height: 1),
                 color: color.withAlphaComponent(0.25))
        cursor.y = y + 22
    }

    private static func drawPlainRows(_ rows: [(String, String)], cursor: PDFCursor) {
        let labelFont = UIFont.systemFont(ofSize: 10)
        let valueFont = UIFont.boldSystemFont(ofSize: 10)
        let valueMaxW = contentW - 150
        let lineH: CGFloat = 12
        for (i, (label, value)) in rows.enumerated() {
            let lines = wrapText(value, font: valueFont, maxWidth: valueMaxW)
            let rh = max(lineH * CGFloat(lines.count) + 9, 21)
            cursor.breakIfNeeded(rh)
            let y = cursor.y
            if i % 2 == 0 { fillRect(CGRect(x: margin, y: y, width: contentW, height: rh),
                                     color: UIColor(white: 0.97, alpha: 1)) }
            draw(label, at: CGPoint(x: margin + 8, y: y + 5),
                 font: labelFont, color: .darkGray)
            for (li, line) in lines.enumerated() {
                let vW = measure(line, font: valueFont).width
                draw(line, at: CGPoint(x: margin + contentW - vW - 8, y: y + 5 + CGFloat(li) * lineH),
                     font: valueFont, color: .black)
            }
            cursor.y = y + rh
        }
        cursor.y += 16
    }

    private static func drawBarRows(_ rows: [(String, Double?)], cursor: PDFCursor, barColor: UIColor) {
        let rh: CGFloat = 24
        let barMaxW: CGFloat = 110
        let barRight = margin + contentW - 8
        for (i, (label, pctVal)) in rows.enumerated() {
            cursor.breakIfNeeded(rh)
            let y = cursor.y
            if i % 2 == 0 { fillRect(CGRect(x: margin, y: y, width: contentW, height: rh),
                                     color: UIColor(white: 0.97, alpha: 1)) }
            draw(label, at: CGPoint(x: margin + 8, y: y + 7),
                 font: .systemFont(ofSize: 10), color: .darkGray)
            if let p = pctVal {
                let valStr = "\(Int(p))%"
                let vW = measure(valStr, font: .boldSystemFont(ofSize: 10)).width
                let barX = barRight - vW - 6 - barMaxW - 6
                // Track background
                fillRoundedRect(CGRect(x: barX, y: y + 9, width: barMaxW, height: 6),
                                radius: 3, color: UIColor(white: 0.88, alpha: 1))
                // Track fill
                let fillW = max(barMaxW * CGFloat(p) / 100, 2)
                fillRoundedRect(CGRect(x: barX, y: y + 9, width: fillW, height: 6),
                                radius: 3, color: barColor)
                draw(valStr, at: CGPoint(x: barRight - vW, y: y + 7),
                     font: .boldSystemFont(ofSize: 10), color: .black)
            }
            cursor.y = y + rh
        }
        cursor.y += 16
    }

    private static func wrapText(_ text: String, font: UIFont, maxWidth: CGFloat) -> [String] {
        if text.isEmpty { return [text] }
        let words = text.split(separator: " ").map(String.init)
        var lines: [String] = []
        var current = ""
        for word in words {
            let candidate = current.isEmpty ? word : "\(current) \(word)"
            if current.isEmpty || measure(candidate, font: font).width <= maxWidth {
                current = candidate
            } else {
                lines.append(current)
                current = word
            }
        }
        if !current.isEmpty { lines.append(current) }
        return lines.isEmpty ? [""] : lines
    }

    // MARK: Primitives

    private static func draw(_ text: String, at point: CGPoint, font: UIFont, color: UIColor) {
        (text as NSString).draw(at: point, withAttributes: [.font: font, .foregroundColor: color])
    }

    private static func measure(_ text: String, font: UIFont) -> CGSize {
        (text as NSString).size(withAttributes: [.font: font])
    }

    private static func fillRect(_ rect: CGRect, color: UIColor) {
        color.setFill()
        UIGraphicsGetCurrentContext()?.fill(rect)
    }

    private static func fillRoundedRect(_ rect: CGRect, radius: CGFloat, color: UIColor) {
        color.setFill()
        UIBezierPath(roundedRect: rect, cornerRadius: radius).fill()
    }

    /// Linha de barra com o % de "Sim" calculado só sobre quem respondeu a pergunta.
    /// O nº de respostas vai no rótulo; sem nenhuma resposta, a barra não é desenhada.
    private static func answeredRow(_ label: String, _ items: [WellbeingCheckIn],
                                    _ kp: KeyPath<WellbeingCheckIn, Bool?>) -> (String, Double?) {
        let answers = items.compactMap { $0[keyPath: kp] }
        guard !answers.isEmpty else { return ("\(label) (sem respostas)", nil) }
        return ("\(label) (\(answers.count) resp.)",
                Double(answers.filter { $0 }.count) / Double(answers.count) * 100)
    }

    private static func yesNo(_ value: Bool?) -> String {
        switch value {
        case .some(true): return "Sim"
        case .some(false): return "Não"
        case .none: return "Não perguntado (teste anterior)"
        }
    }

    private static func dateFormatted(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "dd/MM/yyyy"
        f.locale = Locale(identifier: "pt_BR")
        return f.string(from: date)
    }

    private static func dateTimeFormatted(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "dd/MM/yyyy HH:mm"
        f.locale = Locale(identifier: "pt_BR")
        return f.string(from: date)
    }

    private static func msDate(_ ms: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(ms) / 1000)
    }

    /// Ano de nascimento com a idade aproximada (só o ano é coletado: pode variar 1 ano).
    private static func birthYearText(_ report: WellbeingReportData) -> String {
        let year = report.healthProfile.birthYear
        guard year > 0 else { return "Não informado" }
        let currentYear = Calendar.current.component(.year, from: report.generatedAt)
        return "\(year) (cerca de \(currentYear - year) anos)"
    }

    /// Período dos dados: do início da janela (00:00) até a data de geração.
    private static func periodString(_ report: WellbeingReportData) -> String {
        "\(dateFormatted(msDate(report.windowStartMs))) a \(dateFormatted(report.generatedAt))"
    }
}
