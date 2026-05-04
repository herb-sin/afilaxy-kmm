import SwiftUI
import FirebaseAuth
import FirebaseCore

/// Consulta o CRM de um médico no Conselho Federal de Medicina.
/// Chama a Cloud Function `validateCrm` via URLSession (sem FirebaseFunctions pod).
struct CrmLookupView: View {
    @Environment(\.dismiss) private var dismiss

    private let ufList = [
        "AC","AL","AM","AP","BA","CE","DF","ES","GO","MA","MG","MS","MT",
        "PA","PB","PE","PI","PR","RJ","RN","RO","RR","RS","SC","SE","SP","TO"
    ]

    @State private var crm = ""
    @State private var selectedUF = ""
    @State private var isLoading = false
    @State private var result: CrmResultData? = nil
    @State private var notFound = false
    @State private var errorMessage: String? = nil

    private var canSearch: Bool {
        !crm.trimmingCharacters(in: .whitespaces).isEmpty && selectedUF.count == 2 && !isLoading
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {

                    Text("Verifique se um médico está regularmente inscrito no Conselho Federal de Medicina.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    // Campo CRM
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Número do CRM")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                        TextField("Ex: 123456", text: $crm)
                            .keyboardType(.numberPad)
                            .textFieldStyle(.roundedBorder)
                            .onChange(of: crm) { _ in
                                crm = String(crm.filter { $0.isNumber }.prefix(10))
                                resetResult()
                            }
                    }

                    // Picker de UF
                    VStack(alignment: .leading, spacing: 6) {
                        Text("UF")
                            .font(.caption)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                        Picker("Selecione a UF", selection: $selectedUF) {
                            Text("Selecione").tag("")
                            ForEach(ufList, id: \.self) { uf in
                                Text(uf).tag(uf)
                            }
                        }
                        .pickerStyle(.menu)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color(.systemGray6))
                        .cornerRadius(8)
                        .onChange(of: selectedUF) { _ in resetResult() }
                    }

                    // Botão Consultar
                    Button {
                        Task { await search() }
                    } label: {
                        HStack {
                            if isLoading {
                                ProgressView().scaleEffect(0.85)
                            } else {
                                Image(systemName: "magnifyingglass")
                            }
                            Text("Consultar")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(!canSearch)

                    // Resultados
                    if let r = result {
                        CrmResultCard(result: r)
                    } else if notFound {
                        CrmNotFoundCard()
                    } else if let err = errorMessage {
                        CrmErrorCard(message: err)
                    }

                    Spacer(minLength: 40)
                }
                .padding()
            }
            .navigationTitle("Consultar CRM")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fechar") { dismiss() }
                }
            }
        }
    }

    private func resetResult() {
        result = nil
        notFound = false
        errorMessage = nil
    }

    private func search() async {
        guard canSearch else { return }
        isLoading = true
        resetResult()
        defer { isLoading = false }

        do {
            guard let user = Auth.auth().currentUser else {
                errorMessage = "É necessário estar autenticado para consultar."
                return
            }
            let token = try await user.getIDToken()

            guard let projectId = FirebaseApp.app()?.options.projectID,
                  let url = URL(string: "https://us-central1-\(projectId).cloudfunctions.net/validateCrm")
            else {
                errorMessage = "Erro de configuração do Firebase."
                return
            }

            var req = URLRequest(url: url)
            req.httpMethod = "POST"
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

            let payload: [String: Any] = ["data": ["crm": crm.trimmingCharacters(in: .whitespaces), "uf": selectedUF]]
            req.httpBody = try JSONSerialization.data(withJSONObject: payload)

            let (data, response) = try await URLSession.shared.data(for: req)
            guard let http = response as? HTTPURLResponse else {
                errorMessage = "Erro inesperado. Tente novamente."
                return
            }

            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]

            if http.statusCode == 200, let res = json["result"] as? [String: Any] {
                if res["found"] as? Bool == true {
                    result = CrmResultData(
                        name:      res["name"]      as? String ?? "",
                        specialty: res["specialty"] as? String ?? "",
                        situation: res["situation"] as? String ?? "",
                        uf:        res["uf"]        as? String ?? selectedUF,
                        crm:       res["crm"]       as? String ?? crm
                    )
                } else {
                    notFound = true
                }
            } else if let errObj = json["error"] as? [String: Any],
                      let msg = errObj["message"] as? String {
                errorMessage = msg.contains("UNAVAILABLE")
                    ? "Serviço do CFM indisponível. Tente novamente mais tarde."
                    : "Erro ao consultar CRM. Verifique os dados e tente novamente."
            } else {
                errorMessage = "Erro ao consultar CRM. Verifique os dados e tente novamente."
            }
        } catch {
            errorMessage = "Erro inesperado. Tente novamente."
        }
    }
}

// MARK: - Data Model

struct CrmResultData {
    let name: String
    let specialty: String
    let situation: String
    let uf: String
    let crm: String
}

// MARK: - Sub-views

private struct CrmResultCard: View {
    let result: CrmResultData

    private var situationColor: Color {
        let s = result.situation.lowercased()
        return (s.contains("ativo") || s.contains("regular")) ? .green : .red
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.shield.fill")
                    .foregroundColor(.green)
                    .font(.title3)
                Text("Médico encontrado")
                    .font(.headline)
            }
            Divider()
            CrmLabelRow(label: "Nome",         value: result.name)
            CrmLabelRow(label: "CRM",          value: "\(result.crm)/\(result.uf)")
            CrmLabelRow(label: "Especialidade", value: result.specialty.isEmpty ? "Não informada" : result.specialty)
            HStack(alignment: .top) {
                Text("Situação: ").foregroundColor(.secondary)
                Text(result.situation.isEmpty ? "Não informada" : result.situation)
                    .foregroundColor(situationColor)
                    .fontWeight(.medium)
            }
            .font(.subheadline)
            Text("Fonte: Conselho Federal de Medicina (CFM)")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

private struct CrmNotFoundCard: View {
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.slash.fill")
                .foregroundColor(.red)
            VStack(alignment: .leading, spacing: 4) {
                Text("CRM não encontrado")
                    .font(.subheadline).fontWeight(.semibold).foregroundColor(.red)
                Text("Verifique o número e a UF informados.")
                    .font(.caption).foregroundColor(.secondary)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.red.opacity(0.1))
        .cornerRadius(12)
    }
}

private struct CrmErrorCard: View {
    let message: String
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.orange)
            Text(message)
                .font(.subheadline).foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.orange.opacity(0.1))
        .cornerRadius(12)
    }
}

private struct CrmLabelRow: View {
    let label: String
    let value: String
    var body: some View {
        HStack(alignment: .top) {
            Text("\(label): ").foregroundColor(.secondary)
            Text(value)
        }
        .font(.subheadline)
    }
}

#Preview {
    CrmLookupView()
}
