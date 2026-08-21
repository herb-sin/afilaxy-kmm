import SwiftUI
import UIKit
import FirebaseAuth
import FirebaseCore
import FirebaseFirestore

// MARK: - Main View

struct HealthReportExportView: View {
    @Environment(\.dismiss) private var dismiss

    enum Step {
        case crmEntry
        case crmValidating
        case crmValidated(CrmResultData)
        case generating
        case failed(String)
    }

    private let ufList = [
        "AC","AL","AM","AP","BA","CE","DF","ES","GO","MA","MG","MS","MT",
        "PA","PB","PE","PI","PR","RJ","RN","RO","RR","RS","SC","SE","SP","TO"
    ]

    @State private var step: Step = .crmEntry
    @State private var crm = ""
    @State private var selectedUF = ""
    @State private var shareItems: [Any] = []
    @State private var showShareSheet = false

    var body: some View {
        NavigationStack {
            Group {
                switch step {
                case .crmEntry, .crmValidating:
                    crmEntryView
                case .crmValidated(let doctor):
                    crmValidatedView(doctor: doctor)
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

    // MARK: - Step 1: CRM Entry

    private var crmEntryView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "lock.shield.fill")
                        .foregroundColor(.afiPrimary)
                        .font(.title2)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Acesso por CRM")
                            .font(.headline)
                        Text("Para gerar o relatório, informe o CRM do profissional de saúde que irá recebê-lo. O nome e número serão registrados no documento como destinatário.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemGray6))
                .cornerRadius(12)

                VStack(alignment: .leading, spacing: 6) {
                    Text("Número do CRM")
                        .font(.caption).fontWeight(.medium).foregroundColor(.secondary)
                    TextField("Ex: 123456", text: $crm)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)
                        .onChange(of: crm) { _ in
                            crm = String(crm.filter { $0.isNumber }.prefix(10))
                        }
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("UF de registro")
                        .font(.caption).fontWeight(.medium).foregroundColor(.secondary)
                    Picker("UF", selection: $selectedUF) {
                        Text("Selecione a UF").tag("")
                        ForEach(ufList, id: \.self) { uf in Text(uf).tag(uf) }
                    }
                    .pickerStyle(.menu)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .background(Color(.systemGray6)).cornerRadius(8)
                }

                Button {
                    Task { await validateCrm() }
                } label: {
                    HStack {
                        if case .crmValidating = step {
                            ProgressView().scaleEffect(0.85).tint(.white)
                        } else {
                            Image(systemName: "checkmark.shield")
                        }
                        Text("Validar e Continuar")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity).frame(height: 50)
                    .background(canValidate ? Color.afiPrimary : Color(.systemGray4))
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .disabled(!canValidate)

                Spacer(minLength: 40)
            }
            .padding()
        }
    }

    private var canValidate: Bool {
        if case .crmValidating = step { return false }
        return !crm.trimmingCharacters(in: .whitespaces).isEmpty && selectedUF.count == 2
    }

    // MARK: - Step 2: CRM Validated

    private func crmValidatedView(doctor: CrmResultData) -> some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(alignment: .leading, spacing: 10) {
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark.shield.fill").foregroundColor(.green).font(.title3)
                        Text("Médico confirmado no CFM").font(.headline)
                    }
                    Divider()
                    labelRow("Nome", value: doctor.name)
                    labelRow("CRM", value: "\(doctor.crm)/\(doctor.uf)")
                    labelRow("Especialidade", value: doctor.specialty.isEmpty ? "Não informada" : doctor.specialty)
                    HStack {
                        Text("Situação: ").foregroundColor(.secondary)
                        let active = doctor.situation.lowercased().contains("ativo")
                            || doctor.situation.lowercased().contains("regular")
                        Text(doctor.situation.isEmpty ? "Não informada" : doctor.situation)
                            .foregroundColor(active ? .green : .red)
                            .fontWeight(.medium)
                    }.font(.subheadline)
                    Text("Fonte: Conselho Federal de Medicina (CFM)")
                        .font(.caption).foregroundColor(.secondary)
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
                        "Atividade física e autocuidado",
                        "Eventos de emergência acionados"
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
                    Task { await generateReport(doctor: doctor) }
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

                Button("Trocar profissional") { step = .crmEntry }
                    .font(.subheadline).foregroundColor(.secondary)

                Spacer(minLength: 40)
            }
            .padding()
        }
    }

    private func labelRow(_ label: String, value: String) -> some View {
        HStack(alignment: .top) {
            Text("\(label): ").foregroundColor(.secondary)
            Text(value).fontWeight(.medium)
        }.font(.subheadline)
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
            Button("Tentar novamente") { step = .crmEntry }
                .buttonStyle(.borderedProminent)
            Spacer()
        }
        .padding()
    }

    // MARK: - CRM Validation

    private func validateCrm() async {
        step = .crmValidating
        guard let user = Auth.auth().currentUser else {
            step = .failed("Sessão expirada. Faça login novamente.")
            return
        }
        do {
            let token = try await user.getIDToken()
            guard let projectId = FirebaseApp.app()?.options.projectID,
                  let url = URL(string: "https://us-central1-\(projectId).cloudfunctions.net/validateCrm")
            else {
                step = .failed("Erro de configuração do Firebase.")
                return
            }
            var req = URLRequest(url: url)
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            req.httpBody = try JSONSerialization.data(withJSONObject: [
                "data": ["crm": crm.trimmingCharacters(in: .whitespaces), "uf": selectedUF]
            ])
            let (data, response) = try await URLSession.shared.data(for: req)
            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
            if (response as? HTTPURLResponse)?.statusCode == 200,
               let res = json["result"] as? [String: Any],
               res["found"] as? Bool == true {
                let doctor = CrmResultData(
                    name:      res["name"]      as? String ?? "",
                    specialty: res["specialty"] as? String ?? "",
                    situation: res["situation"] as? String ?? "",
                    uf:        res["uf"]        as? String ?? selectedUF,
                    crm:       res["crm"]       as? String ?? crm
                )
                step = .crmValidated(doctor)
            } else {
                step = .failed("CRM \(crm)/\(selectedUF) não encontrado no CFM.\nVerifique o número e a UF informados.")
            }
        } catch {
            step = .failed("Erro de conexão. Verifique sua internet e tente novamente.")
        }
    }

    // MARK: - Report Generation

    private func generateReport(doctor: CrmResultData) async {
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
            let emergencyCount = emergency30d(statsDoc: statsDoc)
            let patientName = Auth.auth().currentUser?.displayName ?? "Usuário"

            let report = WellbeingReportData(
                patientName: patientName,
                doctor: doctor,
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
            step = .crmValidated(doctor)
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
    let doctor: CrmResultData
    let checkIns: [WellbeingCheckIn]
    let emergencyCount30d: Int
    let generatedAt: Date = Date()
}

struct WellbeingCheckIn {
    let type: String
    let wellbeingA: Bool
    let wellbeingB: Bool
    let wellbeingC: Bool

    init(doc: QueryDocumentSnapshot) {
        type      = doc["type"]      as? String ?? ""
        wellbeingA = doc["wellbeingA"] as? Bool ?? false
        wellbeingB = doc["wellbeingB"] as? Bool ?? false
        wellbeingC = doc["wellbeingC"] as? Bool ?? false
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
            _ = drawCriticalSection(report: report, y: y)
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
        draw("DESTINATÁRIO", at: CGPoint(x: rx, y: y + 10),
             font: .systemFont(ofSize: 8, weight: .medium), color: .gray)
        draw(report.doctor.name.isEmpty ? "—" : report.doctor.name,
             at: CGPoint(x: rx, y: y + 22),
             font: .boldSystemFont(ofSize: 11), color: .black)
        draw("CRM \(report.doctor.crm)/\(report.doctor.uf)", at: CGPoint(x: rx, y: y + 40),
             font: .systemFont(ofSize: 10), color: .darkGray)
        if !report.doctor.specialty.isEmpty {
            draw(report.doctor.specialty, at: CGPoint(x: rx, y: y + 56),
                 font: .systemFont(ofSize: 9), color: .gray)
        }

        return y + h + 20
    }

    // MARK: Sections

    private static func drawSummary(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let total = report.checkIns.count
        let possible = 60
        let adherence = possible > 0 ? Int(Double(total) / Double(possible) * 100) : 0
        let critical = report.checkIns.filter { !$0.wellbeingA && !$0.wellbeingB && !$0.wellbeingC }.count
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
            ("\"Dormi bem esta noite\"",      pct(morning, \.wellbeingA)),
            ("\"Me sinto bem esta manhã\"",   pct(morning, \.wellbeingB)),
            ("\"Estou com boa energia\"",     pct(morning, \.wellbeingC))
        ], y: y, barColor: morningColor)
    }

    private static func drawEveningSection(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let evening = report.checkIns.filter { $0.type == "EVENING" }
        var y = drawSectionHeader("3. CHECK-IN NOTURNO", y: y, color: eveningColor)
        return drawBarRows([
            ("Registros noturnos: \(evening.count)", nil),
            ("\"Tive um bom dia\"",           pct(evening, \.wellbeingA)),
            ("\"Pratiquei atividade física\"", pct(evening, \.wellbeingB)),
            ("\"Me cuidei bem hoje\"",        pct(evening, \.wellbeingC))
        ], y: y, barColor: eveningColor)
    }

    private static func drawCriticalSection(report: WellbeingReportData, y: CGFloat) -> CGFloat {
        let critical = report.checkIns.filter { !$0.wellbeingA && !$0.wellbeingB && !$0.wellbeingC }.count
        var y = drawSectionHeader("4. EVENTOS CRÍTICOS", y: y, color: criticalColor)
        return drawPlainRows([
            ("Pedidos de ajuda emergencial (período)", "\(report.emergencyCount30d)"),
            ("Dias com bem-estar mínimo registrado",   "\(critical)")
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
