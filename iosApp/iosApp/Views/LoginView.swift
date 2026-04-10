import SwiftUI
import FirebaseMessaging
import FirebaseAuth
import FirebaseFirestore

struct LoginView: View {
    let onLoginSuccess: () -> Void
    @EnvironmentObject var container: AppContainer

    @State private var email = ""
    @State private var password = ""
    @State private var showRegister = false
    @State private var resetSent = false
    @State private var resetError: String? = nil
    @State private var isSendingReset = false

    var body: some View {
        let state = container.auth.state
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()
                Text("Afilaxy")
                    .font(.system(size: 48, weight: .bold))
                    .foregroundColor(.red)
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
    }

    private func login() {
        container.auth.vm?.onLogin(email: email, password: password)
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
        if raw.contains("network") || raw.contains("Network") { return "Sem conexão. Verifique sua internet." }
        return "Erro ao entrar. Tente novamente."
    }
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespaces).isEmpty }
}
