import SwiftUI
import FirebaseMessaging
import FirebaseAuth
import FirebaseFirestore
import GoogleSignIn
import AuthenticationServices
import CryptoKit

struct LoginView: View {
    let onLoginSuccess: () -> Void
    @EnvironmentObject var container: AppContainer

    @State private var email = ""
    @State private var password = ""
    @State private var showRegister = false
    @State private var resetSent = false
    @State private var resetError: String? = nil
    @State private var isSendingReset = false
    @State private var currentNonce: String?
    @StateObject private var appleCoordinator = AppleSignInCoordinator()
    @State private var appleController: ASAuthorizationController?
    @State private var showLogs = false

    var body: some View {
        let state = container.auth.state
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()
                Text("Afilaxy")
                    .font(.system(size: 48, weight: .bold))
                    .foregroundColor(.red)
                    .onTapGesture(count: 5) { showLogs = true }
                Text("Sistema de Emergência")
                    .font(.subheadline)
                    .foregroundColor(.gray)
                Spacer()

                VStack(spacing: 16) {
                    TextField("Email", text: $email)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)

                    SecureField("Senha", text: $password)
                        .textFieldStyle(RoundedBorderTextFieldStyle())

                    if let error = state?.error {
                        Text(friendlyError(error)).foregroundColor(.red).font(.caption)
                    }

                    Button(action: login) {
                        if state?.isLoading == true {
                            ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("Entrar").fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.red)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                    .disabled(state?.isLoading == true || email.isBlank || password.isBlank)

                    HStack {
                        Rectangle().frame(height: 1).foregroundColor(Color(.systemGray4))
                        Text("ou").font(.caption).foregroundColor(.secondary).fixedSize()
                        Rectangle().frame(height: 1).foregroundColor(Color(.systemGray4))
                    }

                    Button(action: signInWithGoogle) {
                        HStack(spacing: 8) {
                            Text("G")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.blue)
                            Text("Entrar com Google")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(.systemBackground))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color(.systemGray3), lineWidth: 1))
                    .cornerRadius(10)
                    .disabled(state?.isLoading == true)

                    Button(action: signInWithApple) {
                        HStack(spacing: 8) {
                            Image(systemName: "apple.logo")
                                .font(.system(size: 18, weight: .medium))
                            Text("Entrar com Apple")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(.label))
                    .foregroundColor(Color(.systemBackground))
                    .cornerRadius(10)
                    .disabled(state?.isLoading == true)

                    Button { showRegister = true } label: {
                        Text("Criar Conta").foregroundColor(.red)
                    }

                    // Esqueci minha senha
                    Button(action: sendPasswordReset) {
                        if isSendingReset {
                            ProgressView()
                        } else {
                            Text("Esqueci minha senha")
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        }
                    }
                    .disabled(isSendingReset)

                    if resetSent {
                        Text("✅ Email de redefinição enviado! Verifique sua caixa de entrada.")
                            .font(.caption)
                            .foregroundColor(.green)
                            .multilineTextAlignment(.center)
                    }
                    if let err = resetError {
                        Text(err).font(.caption).foregroundColor(.red).multilineTextAlignment(.center)
                    }
                }
                .padding(.horizontal, 32)
                Spacer()
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showRegister) {
                NavigationStack { RegisterView() }
            }
        }
        .sheet(isPresented: $showLogs) { LogViewerSheet() }
        .onReceive(container.auth.$state) { s in
            if s?.isAuthenticated == true {
                // Salva FCM token no login — garante que o token está associado ao uid atual
                // (o callback messaging:didReceiveRegistrationToken só dispara quando o token é novo)
                if let token = Messaging.messaging().fcmToken,
                   let uid = Auth.auth().currentUser?.uid {
                    Firestore.firestore().collection("users").document(uid)
                        .setData(["fcmToken": token], merge: true)
                }
                onLoginSuccess()
            }
        }
        .onAppear {
            appleCoordinator.onSuccess = { identityToken, nonce in
                container.auth.vm?.onAppleSignInResult(identityToken: identityToken, nonce: nonce)
            }
            appleCoordinator.onError = { message in
                container.auth.vm?.updateError(message: message)
            }
        }
    }

    private func login() {
        container.auth.vm?.onLogin(email: email, password: password)
    }

    private func signInWithGoogle() {
        guard let rootVC = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first?.keyWindow?.rootViewController else { return }

        GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { result, error in
            if let error = error {
                let nsErr = error as NSError
                let msg = error.localizedDescription
                FileLogger.shared.write(level: "ERROR", tag: "LoginView",
                    message: "GIDSignIn falhou domain=\(nsErr.domain) code=\(nsErr.code) msg=\(msg)")
                if msg.contains("account-exists") || msg.contains("different-credential") || msg.contains("17012") {
                    container.auth.vm?.updateError(message: "Esta conta foi criada com email e senha. Faça login com email e senha.")
                } else if !msg.contains("canceled") && !msg.contains("cancel") {
                    container.auth.vm?.updateError(message: "Erro ao entrar com Google. Tente novamente.")
                }
                return
            }
            guard let result = result,
                  let idToken = result.user.idToken?.tokenString else {
                FileLogger.shared.write(level: "ERROR", tag: "LoginView", message: "GIDSignIn: result ou idToken nulo")
                container.auth.vm?.updateError(message: "Não foi possível obter credenciais do Google.")
                return
            }
            let accessToken = result.user.accessToken.tokenString
            FileLogger.shared.write(level: "INFO", tag: "LoginView", message: "GIDSignIn ok, chamando onGoogleSignInResult")
            container.auth.vm?.onGoogleSignInResult(idToken: idToken, accessToken: accessToken)
        }
    }

    private func signInWithApple() {
        let nonce = randomNonceString()
        currentNonce = nonce
        appleCoordinator.currentNonce = nonce

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)

        // Mantém referência forte — sem isso o ARC libera o controller antes
        // dos delegates dispararem e o sheet da Apple nunca aparece.
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = appleCoordinator
        controller.presentationContextProvider = appleCoordinator
        appleController = controller
        controller.performRequests()
    }

    private func sendPasswordReset() {
        let trimmed = email.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else {
            resetError = "Digite seu e-mail acima para recuperar a senha."
            return
        }
        isSendingReset = true
        resetError = nil
        resetSent = false
        Auth.auth().sendPasswordReset(withEmail: trimmed) { error in
            isSendingReset = false
            if let error = error {
                resetError = "Não foi possível enviar o email. Verifique o endereço."
                _ = error
            } else {
                resetSent = true
            }
        }
    }

    private func friendlyError(_ raw: String) -> String {
        if raw.contains("badly formatted") || raw.contains("invalid-email") { return "E-mail inválido." }
        if raw.contains("no user record") || raw.contains("user-not-found") { return "Usuário não encontrado." }
        if raw.contains("wrong password") || raw.contains("invalid-credential") || raw.contains("INVALID_LOGIN_CREDENTIALS") { return "E-mail ou senha incorretos." }
        if raw.contains("too-many-requests") { return "Muitas tentativas. Tente novamente mais tarde." }
        if raw.contains("network") || raw.contains("Network") || raw.contains("conexão") { return "Sem conexão. Verifique sua internet." }
        if raw.contains("disabled") || raw.contains("not allowed") || raw.contains("OPERATION_NOT_ALLOWED") { return "Login social não configurado. Contate o suporte." }
        return "Erro ao entrar. Tente novamente."
    }

    private func randomNonceString(length: Int = 32) -> String {
        var randomBytes = [UInt8](repeating: 0, count: length)
        _ = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(randomBytes.map { byte in charset[Int(byte) % charset.count] })
    }

    private func sha256(_ input: String) -> String {
        let inputData = Data(input.utf8)
        let hashedData = SHA256.hash(data: inputData)
        return hashedData.compactMap { String(format: "%02x", $0) }.joined()
    }
}

class AppleSignInCoordinator: NSObject, ObservableObject, ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    var currentNonce: String?
    var onSuccess: ((String, String) -> Void)?
    var onError: ((String) -> Void)?

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?.keyWindow ?? ASPresentationAnchor()
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let identityTokenData = appleIDCredential.identityToken,
              let identityToken = String(data: identityTokenData, encoding: .utf8),
              let nonce = currentNonce else {
            FileLogger.shared.write(level: "ERROR", tag: "AppleSignIn", message: "Credenciais Apple nulas ou nonce ausente")
            onError?("Não foi possível obter credenciais da Apple. Tente novamente.")
            return
        }
        FileLogger.shared.write(level: "INFO", tag: "AppleSignIn", message: "Credenciais Apple obtidas, chamando onAppleSignInResult")
        onSuccess?(identityToken, nonce)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let nsError = error as NSError
        FileLogger.shared.write(level: "ERROR", tag: "AppleSignIn",
            message: "ASAuthorizationController erro domain=\(nsError.domain) code=\(nsError.code) msg=\(error.localizedDescription)")
        guard nsError.code != 1001 else { return }
        onError?("Erro ao entrar com Apple. Tente novamente.")
    }
}

struct LogViewerSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var logContent = ""

    var body: some View {
        NavigationStack {
            ScrollViewReader { proxy in
                ScrollView {
                    Text(logContent)
                        .font(.system(size: 11, design: .monospaced))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        .id("content")
                }
                .onAppear { proxy.scrollTo("content", anchor: .bottom) }
            }
            .navigationTitle("Logs de diagnóstico")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fechar") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    ShareLink(item: logContent) {
                        Image(systemName: "square.and.arrow.up")
                    }
                    .disabled(logContent.isEmpty)
                }
            }
        }
        .onAppear { loadLogs() }
    }

    private func loadLogs() {
        let urls = FileLogger.shared.getAllLogFileURLs()
        let combined = urls.compactMap { try? String(contentsOf: $0, encoding: .utf8) }.joined(separator: "\n--- arquivo anterior ---\n")
        logContent = combined.isEmpty ? "(nenhum log disponível)" : combined
    }
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespaces).isEmpty }
}
