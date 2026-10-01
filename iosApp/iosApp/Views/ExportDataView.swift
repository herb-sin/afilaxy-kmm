import SwiftUI
import UIKit
import FirebaseAuth
import FirebaseCore
import FirebaseFirestore

// MARK: - Main View

struct ExportDataView: View {
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
            .navigationTitle("Exportar Meus Dados")
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
                    Image(systemName: "lock.shield.fill")
                        .foregroundColor(.afiPrimary)
                        .font(.title2)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Seus dados, no seu controle")
                            .font(.headline)
                        Text("Gere um arquivo com os dados pessoais que você forneceu ao Afilaxy. Direito garantido pela LGPD.")
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
                        Text("Conteúdo do Arquivo").font(.headline)
                    }
                    ForEach([
                        "Perfil e Perfil Médico (alergias, comorbidades, contato de emergência)",
                        "Histórico completo de check-ins",
                        "Histórico de score de risco",
                        "Emergências em que você participou (como solicitante ou helper)",
                        "Avaliações enviadas e recebidas"
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
                    Task { await generateExport() }
                } label: {
                    HStack {
                        Image(systemName: "arrow.down.doc.fill")
                        Text("Gerar Arquivo de Dados")
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

    // MARK: - Generating / Failed

    private var generatingView: some View {
        VStack(spacing: 24) {
            Spacer()
            ProgressView().scaleEffect(1.5)
            Text("Reunindo seus dados...").font(.headline)
            Text("Isso pode levar alguns segundos.")
                .font(.subheadline).foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding()
    }

    private func failedView(message: String) -> some View {
        VStack(spacing: 20) {
            Spacer()
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48)).foregroundColor(.orange)
            Text("Erro ao gerar arquivo").font(.headline)
            Text(message)
                .font(.subheadline).foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            Button("Tentar novamente") { step = .idle }
                .buttonStyle(.borderedProminent)
            Spacer()
        }
        .padding()
    }

    // MARK: - Export Generation

    private func generateExport() async {
        step = .generating
        guard let uid = Auth.auth().currentUser?.uid else {
            step = .failed("Sessão expirada. Faça login novamente.")
            return
        }
        let db = Firestore.firestore()
        var root: [String: Any] = [:]
        root["exportadoEm"] = isoString(from: Date())
        root["usuarioId"] = uid

        let userDoc = try? await db.collection("users").document(uid).getDocument()
        root["perfil"] = sanitizeForJson(userDoc?.data())

        let checkIns = try? await db.collection("checkins").document(uid)
            .collection("responses").getDocuments()
        root["checkins"] = docsToArray(checkIns?.documents ?? [])

        let riskSnapshots = try? await db.collection("risk_scores").document(uid)
            .collection("snapshots").getDocuments()
        root["historicoDeRisco"] = docsToArray(riskSnapshots?.documents ?? [])

        let statsDoc = try? await db.collection("user_stats").document(uid).getDocument()
        root["estatisticas"] = sanitizeForJson(statsDoc?.data())

        let helperDoc = try? await db.collection("helpers").document(uid).getDocument()
        root["perfilHelper"] = sanitizeForJson(helperDoc?.data())

        let asRequester = try? await db.collection("emergency_requests")
            .whereField("requesterId", isEqualTo: uid).getDocuments()
        root["emergenciasComoSolicitante"] = docsToArray(asRequester?.documents ?? [])

        let asHelper = try? await db.collection("emergency_requests")
            .whereField("helperId", isEqualTo: uid).getDocuments()
        root["emergenciasComoHelper"] = docsToArray(asHelper?.documents ?? [])

        let reviewsAsReviewer = (try? await db.collection("reviews")
            .whereField("reviewerId", isEqualTo: uid).getDocuments())?.documents ?? []
        let reviewsAsReviewed = (try? await db.collection("reviews")
            .whereField("reviewedId", isEqualTo: uid).getDocuments())?.documents ?? []
        var seenReviewIds = Set<String>()
        let allReviews = (reviewsAsReviewer + reviewsAsReviewed)
            .filter { seenReviewIds.insert($0.documentID).inserted }
        root["avaliacoes"] = docsToArray(allReviews)

        guard JSONSerialization.isValidJSONObject(root),
              let data = try? JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted]),
              let fileURL = writeExportFile(data) else {
            step = .failed("Não foi possível gerar o arquivo. Tente novamente.")
            return
        }
        shareItems = [fileURL]
        showShareSheet = true
        step = .idle
    }

    private func writeExportFile(_ data: Data) -> URL? {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("meus_dados_afilaxy_\(Int(Date().timeIntervalSince1970)).json")
        return (try? data.write(to: url)) != nil ? url : nil
    }

    // MARK: - JSON Sanitization (tipos nativos do Firestore → tipos serializáveis em JSON)

    private func docsToArray(_ docs: [QueryDocumentSnapshot]) -> [[String: Any]] {
        docs.map { doc in
            var obj = sanitizeForJson(doc.data()) as? [String: Any] ?? [:]
            obj["id"] = doc.documentID
            return obj
        }
    }

    private func sanitizeForJson(_ value: Any?) -> Any {
        guard let value, !(value is NSNull) else { return NSNull() }
        switch value {
        case let geoPoint as GeoPoint:
            return ["latitude": geoPoint.latitude, "longitude": geoPoint.longitude]
        case let timestamp as Timestamp:
            return isoString(from: timestamp.dateValue())
        case let dict as [String: Any]:
            var result: [String: Any] = [:]
            for (k, v) in dict { result[k] = sanitizeForJson(v) }
            return result
        case let array as [Any]:
            return array.map { sanitizeForJson($0) }
        default:
            return value
        }
    }

    private func isoString(from date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "UTC")
        return formatter.string(from: date)
    }
}
