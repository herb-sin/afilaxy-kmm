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
                        Text("Gere um PDF com seus check-ins dos últimos 30 dias e seu Perfil Médico — pronto para compartilhar com quem você quiser, sem precisar informar um profissional específico.")
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
                        Text("Conteúdo do Relatório (30 dias)").font(.headline)
                    }
                    ForEach([
                        "Resumo executivo e adesão ao monitoramento",
                        "Qualidade do sono (check-in matinal)",
                        "Bem-estar e energia matinal",
                        "Avaliação de bem-estar noturno",
                        "Eventos de emergência acionados",
                        "Perfil Médico (comorbidades, alergias, acessibilidade)"
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
            Text("Coletando seus dados dos últimos 30 dias.")
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
            let cutoffMs = Int64(Date().timeIntervalSince1970 * 1000) - 30 * 24 * 3_600_000

            let checkInDocs = try await Firestore.firestore()
                .collection("checkins").document(uid)
                .collection("responses")
                .whereField("timestamp", isGreaterThanOrEqualTo: cutoffMs)
                .getDocuments()

            let statsDoc = try? await Firestore.firestore()
                .collection("user_stats").document(uid).getDocument()
            let userDoc = try? await Firestore.firestore()
                .collection("users").document(uid).getDocument()
            let emergencyCount = emergency30d(statsDoc: statsDoc)
            let patientName = Auth.auth().currentUser?.displayName ?? "Usuário"

            let report = WellbeingReportData(
                patientName: patientName,
                healthProfile: HealthProfileData(doc: userDoc),
                checkIns: checkInDocs.documents.map { WellbeingCheckIn(doc: $0) },
                emergencyCount30d: emergencyCount
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

    private func emergency30d(statsDoc: DocumentSnapshot?) -> Int {
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
        return (0..<30).reduce(0) { sum, offset in
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

// MARK: - Report Data Models

struct WellbeingReportData {
    let patientName: String
    let healthProfile: HealthProfileData
    let checkIns: [WellbeingCheckIn]
    let emergencyCount30d: Int
    let generatedAt: Date = Date()
}

struct HealthProfileData {
    let bloodType: String
    let allergiesText: [String]
    let hasGerd: Bool
    let hasSleepApnea: Bool
    let hasRhinitis: Bool
    let hasObesity: Bool
    let hasFoodAllergy: Bool
    let hasNsaidAllergy: Bool
    let hasInhalantAllergy: Bool
    let hasWheelchair: Bool
    let hasLowVision: Bool
    let hasSpecialCondition: Bool
    let emergencyContactName: String
    let emergencyContactPhone: String

    init(doc: DocumentSnapshot?) {
        bloodType = doc?["healthData.bloodType"] as? String ?? ""
        allergiesText = (doc?["healthData.allergies"] as? [String]) ?? []
        hasGerd = doc?["healthData.hasGerd"] as? Bool ?? false
        hasSleepApnea = doc?["healthData.hasSleepApnea"] as? Bool ?? false
        hasRhinitis = doc?["healthData.hasRhinitis"] as? Bool ?? false
        hasObesity = doc?["healthData.hasObesity"] as? Bool ?? false
        hasFoodAllergy = doc?["healthData.hasFoodAllergy"] as? Bool ?? false
        hasNsaidAllergy = doc?["healthData.hasNsaidAllergy"] as? Bool ?? false
        hasInhalantAllergy = doc?["healthData.hasInhalantAllergy"] as? Bool ?? false
        hasWheelchair = doc?["healthData.hasWheelchair"] as? Bool ?? false
        hasLowVision = doc?["healthData.hasLowVision"] as? Bool ?? false
        hasSpecialCondition = doc?["healthData.hasSpecialCondition"] as? Bool ?? false
        emergencyContactName = doc?["emergencyContact.name"] as? String ?? ""
        emergencyContactPhone = doc?["emergencyContact.phone"] as? String ?? ""
    }
}

struct WellbeingCheckIn {
    let type: String
    let nighttimeAwakening: Bool
    let morningMoodGood: Bool
    let morningEnergyGood: Bool
    let selfCareGood: Bool
    let daytimeBreathingEase: Bool
    let activityAsPlanned: Bool

    init(doc: QueryDocumentSnapshot) {
        type = doc["type"] as? String ?? ""
        nighttimeAwakening = doc["nighttimeAwakening"] as? Bool ?? false
        morningMoodGood = doc["morningMoodGood"] as? Bool ?? false
        morningEnergyGood = doc["morningEnergyGood"] as? Bool ?? false
        selfCareGood = doc["selfCareGood"] as? Bool ?? false
        daytimeBreathingEase = doc["daytimeBreathingEase"] as? Bool ?? false
        activityAsPlanned = doc["activityAsPlanned"] as? Bool ?? false
    }

    var isCritical: Bool {
        type == "MORNING"
            ? !nighttimeAwakening && !morningMoodGood && !morningEnergyGood
            : !selfCareGood && !daytimeBreathingEase && !activityAsPlanned
    }
}

// MARK: - PDF Generator

enum WellbeingPDFGenerator {
    private static let margin: CGFloat   = 50
    private static let pageW: CGFloat    = 595.2
    private static let pageH: CGFloat    = 841.8
    private static var contentW: CGFloat { pageW - margin * 2 }

    private static let primaryColor  = UIColor(red: 0,    green: 0.384, blue: 0.561, alpha: 1)
    private static let morningColor  = UIColor(red: 0.9,  green: 0.32,  blue: 0,     alpha: 1)
    private static let eveningColor  = UIColor(red: 0.1,  green: 0.14,  blue: 0.49,  alpha: 1)
    private static let criticalColor = UIColor(red: 0.73, green: 0.1,   blue: 0.1,   alpha: 1)

    static func generate(report: WellbeingReportData) -> Data? {
        let renderer = UIGraphicsPDFRenderer(
            bounds: CGRect(x: 0, y: 0, width: pageW, height: pageH)
        )
        return renderer.pdfData { ctx in
            ctx.beginPage()
            var y = drawHeader(report: report)
            y = drawInfoBox(report: report, y: y)
            y = drawSummary(report: report, y: y)
            y = drawMorningSection(report: report, y: y)
            y = drawEveningSection(report: report, y: y)
            y = drawCriticalSection(report: report, y: y)
            _ = drawHealthProfileSection(report: report, y: y)
            drawFooter(report: report)
        }
    }

    // MARK: Header

    private static func drawHeader(report: WellbeingReportData) -> CGFloat {
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

        return h + 22
    }

    // MARK: Info Box

    private static func drawInfoBox(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let h: CGFloat = 78
        fillRect(CGRect(x: margin, y: y, width: contentW, height: h),
                 color: UIColor(white: 0.96, alpha: 1))

        let lx = margin + 10
        draw("PACIENTE", at: CGPoint(x: lx, y: y + 10),
             font: .systemFont(ofSize: 8, weight: .medium), color: .gray)
        draw(report.patientName, at: CGPoint(x: lx, y: y + 22),
             font: .boldSystemFont(ofSize: 12), color: .black)
        draw("PERÍODO", at: CGPoint(x: lx, y: y + 46),
             font: .systemFont(ofSize: 8, weight: .medium), color: .gray)
        draw(periodString(), at: CGPoint(x: lx, y: y + 58),
             font: .systemFont(ofSize: 10), color: .darkGray)

        let rx = pageW - margin - 205
        draw("ORIGEM", at: CGPoint(x: rx, y: y + 10),
             font: .systemFont(ofSize: 8, weight: .medium), color: .gray)
        draw("Autorrelato do paciente", at: CGPoint(x: rx, y: y + 22),
             font: .boldSystemFont(ofSize: 11), color: .black)
        draw("Sem revisão clínica prévia", at: CGPoint(x: rx, y: y + 40),
             font: .systemFont(ofSize: 10), color: .darkGray)

        return y + h + 20
    }

    // MARK: Sections

    private static func drawSummary(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let total = report.checkIns.count
        let possible = 60
        let adherence = possible > 0 ? Int(Double(total) / Double(possible) * 100) : 0
        let critical = report.checkIns.filter { $0.isCritical }.count
        var y = drawSectionHeader("1. RESUMO EXECUTIVO", y: y, color: primaryColor)
        return drawPlainRows([
            ("Check-ins realizados (30 dias)", "\(total) de \(possible) possíveis"),
            ("Taxa de adesão ao monitoramento", "\(adherence)%"),
            ("Pedidos de ajuda emergencial", "\(report.emergencyCount30d) ocorrência(s)"),
            ("Dias com bem-estar crítico registrado", "\(critical) ocorrência(s)")
        ], y: y)
    }

    private static func drawMorningSection(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let morning = report.checkIns.filter { $0.type == "MORNING" }
        var y = drawSectionHeader("2. CHECK-IN MATINAL", y: y, color: morningColor)
        return drawBarRows([
            ("Registros de manhã: \(morning.count)", nil),
            ("\"Meu sono foi tranquilo, sem interrupções\"", pct(morning, \.nighttimeAwakening)),
            ("\"Me sinto bem esta manhã\"",   pct(morning, \.morningMoodGood)),
            ("\"Estou com boa energia\"",     pct(morning, \.morningEnergyGood))
        ], y: y, barColor: morningColor)
    }

    private static func drawEveningSection(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let evening = report.checkIns.filter { $0.type == "EVENING" }
        var y = drawSectionHeader("3. CHECK-IN NOTURNO", y: y, color: eveningColor)
        return drawBarRows([
            ("Registros noturnos: \(evening.count)", nil),
            ("\"Me cuidei bem hoje\"",        pct(evening, \.selfCareGood)),
            ("\"Respirei com facilidade ao longo do dia\"", pct(evening, \.daytimeBreathingEase)),
            ("\"Consegui fazer tudo que tinha planejado\"",  pct(evening, \.activityAsPlanned))
        ], y: y, barColor: eveningColor)
    }

    private static func drawCriticalSection(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let critical = report.checkIns.filter { $0.isCritical }.count
        var y = drawSectionHeader("4. EVENTOS CRÍTICOS", y: y, color: criticalColor)
        return drawPlainRows([
            ("Pedidos de ajuda emergencial (período)", "\(report.emergencyCount30d)"),
            ("Dias com bem-estar mínimo registrado",   "\(critical)")
        ], y: y)
    }

    private static func drawHealthProfileSection(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let p = report.healthProfile
        var comorbidities: [String] = []
        if p.hasGerd { comorbidities.append("Refluxo / DRGE") }
        if p.hasSleepApnea { comorbidities.append("Apneia do sono") }
        if p.hasRhinitis { comorbidities.append("Rinite alérgica") }
        if p.hasObesity { comorbidities.append("Obesidade") }

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

        var y = drawSectionHeader("5. PERFIL MÉDICO", y: y, color: primaryColor)
        return drawPlainRows([
            ("Tipo sanguíneo", p.bloodType.isEmpty ? "Não informado" : p.bloodType),
            ("Alergias conhecidas", p.allergiesText.isEmpty ? "Nenhuma relatada" : p.allergiesText.joined(separator: ", ")),
            ("Comorbidades", comorbidities.isEmpty ? "Nenhuma" : comorbidities.joined(separator: ", ")),
            ("Alergias específicas", specificAllergies.isEmpty ? "Nenhuma" : specificAllergies.joined(separator: ", ")),
            ("Acessibilidade", accessibility.isEmpty ? "Nenhuma" : accessibility.joined(separator: ", ")),
            ("Contato de emergência", emergencyContact)
        ], y: y)
    }

    private static func drawFooter(report: WellbeingReportData) {
        let footerY = pageH - 52
        fillRect(CGRect(x: margin, y: footerY, width: contentW, height: 0.5),
                 color: .lightGray)
        let text = "Este relatório foi gerado automaticamente pelo aplicativo Afilaxy com base nas respostas fornecidas pelo próprio usuário. Não substitui avaliação clínica presencial. Gerado em \(dateFormatted(report.generatedAt))."
        let style = NSMutableParagraphStyle()
        style.lineBreakMode = .byWordWrapping
        (text as NSString).draw(
            in: CGRect(x: margin, y: footerY + 6, width: contentW, height: 36),
            withAttributes: [.font: UIFont.systemFont(ofSize: 8),
                             .foregroundColor: UIColor.gray,
                             .paragraphStyle: style]
        )
    }

    // MARK: Row Drawers

    private static func drawSectionHeader(_ title: String, y: CGFloat, color: UIColor) -> CGFloat {
        draw(title, at: CGPoint(x: margin, y: y), font: .boldSystemFont(ofSize: 11), color: color)
        fillRect(CGRect(x: margin, y: y + 16, width: contentW, height: 1),
                 color: color.withAlphaComponent(0.25))
        return y + 22
    }

    private static func drawPlainRows(_ rows: [(String, String)], y: CGFloat) -> CGFloat {
        var y = y
        let rh: CGFloat = 21
        for (i, (label, value)) in rows.enumerated() {
            if i % 2 == 0 { fillRect(CGRect(x: margin, y: y, width: contentW, height: rh),
                                     color: UIColor(white: 0.97, alpha: 1)) }
            draw(label, at: CGPoint(x: margin + 8, y: y + 5),
                 font: .systemFont(ofSize: 10), color: .darkGray)
            let vW = measure(value, font: .boldSystemFont(ofSize: 10)).width
            draw(value, at: CGPoint(x: margin + contentW - vW - 8, y: y + 5),
                 font: .boldSystemFont(ofSize: 10), color: .black)
            y += rh
        }
        return y + 16
    }

    private static func drawBarRows(_ rows: [(String, Double?)], y: CGFloat, barColor: UIColor) -> CGFloat {
        var y = y
        let rh: CGFloat = 24
        let barMaxW: CGFloat = 110
        let barRight = margin + contentW - 8
        for (i, (label, pctVal)) in rows.enumerated() {
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
            y += rh
        }
        return y + 16
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

    private static func pct(_ items: [WellbeingCheckIn], _ kp: KeyPath<WellbeingCheckIn, Bool>) -> Double? {
        guard !items.isEmpty else { return nil }
        return Double(items.filter { $0[keyPath: kp] }.count) / Double(items.count) * 100
    }

    private static func dateFormatted(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "dd/MM/yyyy"
        f.locale = Locale(identifier: "pt_BR")
        return f.string(from: date)
    }

    private static func periodString() -> String {
        let cal = Calendar.current
        let end = Date()
        let start = cal.date(byAdding: .day, value: -29, to: end) ?? end
        let f = DateFormatter()
        f.dateFormat = "dd/MM/yyyy"
        f.locale = Locale(identifier: "pt_BR")
        return "\(f.string(from: start)) – \(f.string(from: end))"
    }
}
