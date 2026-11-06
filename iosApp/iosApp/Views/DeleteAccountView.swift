import SwiftUI
import FirebaseAuth
import FirebaseCore
import AuthenticationServices
import shared

/// Exclusão de conta (LGPD art. 18, VI; App Store Review Guideline 5.1.1(v)).
/// A exclusão em si é feita pela Cloud Function `deleteAccount`. Para contas com
/// Sign in with Apple, o token da Apple é revogado antes (exigência da App Store),
/// o que pede uma nova autorização da Apple ao usuário.
struct DeleteAccountView: View {
    @EnvironmentObject var container: AppContainer
    @Environment(\.dismiss) private var dismiss

    private static let confirmationWord = "EXCLUIR"

    @State private var confirmation = ""
    @State private var isDeleting = false
    @State private var errorMessage: String?
    @State private var appleReauth = AppleReauthenticator()

    private var hasActiveEmergency: Bool {
        container.emergency.state?.hasActiveEmergency == true
    }

    private var isConfirmed: Bool {
        confirmation.trimmingCharacters(in: .whitespaces)
            .caseInsensitiveCompare(Self.confirmationWord) == .orderedSame
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Esta ação é permanente e não pode ser desfeita.")
                        .font(.headline)
                        .foregroundColor(.red)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("O que será apagado").font(.subheadline).fontWeight(.semibold)
                        Text("""
                        • Seu login e seu perfil
                        • Perfil médico, medicações, exames e contatos de emergência
                        • Check-ins, Teste de Controle da Asma e histórico de risco
                        • Seus pedidos de ajuda e as conversas deles
                        • Suas mensagens e seu nome nos atendimentos que você fez
                        • Avaliações, foto de perfil e assinatura de profissional (cancelada)
                        """)
                        .font(.body)
                        Text("Se quiser guardar uma cópia, use antes \"Exportar Dados\".")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)

                    if hasActiveEmergency {
                        Text("Você tem uma emergência em andamento. Encerre-a antes de excluir a conta.")
                            .foregroundColor(.red)
                    } else {
                        TextField("Digite \(Self.confirmationWord) para confirmar", text: $confirmation)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                            .textFieldStyle(.roundedBorder)
                            .disabled(isDeleting)
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .accessibilityAddTraits(.updatesFrequently)
                    }

                    Button(role: .destructive) {
                        Task { await deleteAccount() }
                    } label: {
                        HStack {
                            if isDeleting { ProgressView().tint(.white) }
                            Text(isDeleting ? "Excluindo…" : "Excluir minha conta definitivamente")
                        }
                        .frame(maxWidth: .infinity, minHeight: 44)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.red)
                    .disabled(!isConfirmed || isDeleting || hasActiveEmergency)

                    Button("Cancelar") { dismiss() }
                        .frame(maxWidth: .infinity, minHeight: 44)
                        .disabled(isDeleting)
                }
                .padding()
            }
            .navigationTitle("Excluir minha conta")
            .navigationBarTitleDisplayMode(.inline)
            .interactiveDismissDisabled(isDeleting)
        }
    }

    @MainActor
    private func deleteAccount() async {
        guard let user = Auth.auth().currentUser else {
            errorMessage = "Sessão expirada. Entre novamente para excluir a conta."
            return
        }
        isDeleting = true
        errorMessage = nil
        defer { isDeleting = false }

        do {
            // 1. Sign in with Apple: revoga o token na Apple (pede nova autorização).
            if user.providerData.contains(where: { $0.providerID == "apple.com" }) {
                let code = try await appleReauth.authorizationCode()
                try await Auth.auth().revokeToken(withAuthorizationCode: code)
            }

            // 2. Encerra modo ajudante/observers enquanto a sessão ainda vale (igual ao logout).
            container.emergency.clearEmergencyStateSwift()
            LocationManagerBridge.shared.disableHelperMode()

            // 3. Cloud Function apaga os dados e a conta.
            try await callDeleteAccountFunction(user: user)
        } catch AppleReauthenticator.Failure.cancelled {
            return // usuário fechou o pedido da Apple — nada foi feito
        } catch let DeleteAccountError.server(message) {
            errorMessage = message
            return
        } catch {
            errorMessage = "Não foi possível excluir a conta agora. Nada foi perdido. Tente novamente."
            return
        }

        // 4. Sucesso: mesma sequência do logout (SettingsView.performLogout) — o delay drena a
        //    main queue antes de invalidar o Firebase. freezeAll() já faz o signOut.
        dismiss()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak container] in
            container?.freezeAll()
        }
    }

    private enum DeleteAccountError: Error { case server(String) }

    private func callDeleteAccountFunction(user: FirebaseAuth.User) async throws {
        let token = try await user.getIDToken()
        guard let projectId = FirebaseApp.app()?.options.projectID,
              let url = URL(string: "https://us-central1-\(projectId).cloudfunctions.net/deleteAccount")
        else { throw DeleteAccountError.server("Erro de configuração do Firebase.") }

        var req = URLRequest(url: url, timeoutInterval: 90)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.httpBody = try JSONSerialization.data(withJSONObject: ["data": [String: Any]()])

        let (data, response) = try await URLSession.shared.data(for: req)
        let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
        if (response as? HTTPURLResponse)?.statusCode == 200, json["result"] != nil { return }

        // A única mensagem do servidor pensada para o usuário é a da assinatura.
        if let err = json["error"] as? [String: Any],
           let msg = err["message"] as? String, msg.localizedCaseInsensitiveContains("assinatura") {
            throw DeleteAccountError.server(msg)
        }
        throw DeleteAccountError.server("Não foi possível excluir a conta agora. Nada foi perdido. Tente novamente.")
    }
}

/// Pede uma nova autorização do Sign in with Apple só para obter o authorizationCode
/// exigido por `Auth.revokeToken(withAuthorizationCode:)`.
final class AppleReauthenticator: NSObject, ASAuthorizationControllerDelegate,
                                  ASAuthorizationControllerPresentationContextProviding {
    enum Failure: Error { case cancelled, missingCode }

    private var continuation: CheckedContinuation<String, Error>?
    private var controller: ASAuthorizationController? // referência forte até os delegates dispararem

    @MainActor
    func authorizationCode() async throws -> String {
        try await withCheckedThrowingContinuation { cont in
            continuation = cont
            let request = ASAuthorizationAppleIDProvider().createRequest()
            let controller = ASAuthorizationController(authorizationRequests: [request])
            controller.delegate = self
            controller.presentationContextProvider = self
            self.controller = controller
            controller.performRequests()
        }
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.keyWindow ?? ASPresentationAnchor()
    }

    func authorizationController(controller: ASAuthorizationController,
                                 didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let data = credential.authorizationCode,
              let code = String(data: data, encoding: .utf8) else {
            finish(.failure(Failure.missingCode))
            return
        }
        finish(.success(code))
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let cancelled = (error as? ASAuthorizationError)?.code == .canceled
        finish(.failure(cancelled ? Failure.cancelled : error))
    }

    private func finish(_ result: Result<String, Error>) {
        continuation?.resume(with: result)
        continuation = nil
        controller = nil
    }
}
