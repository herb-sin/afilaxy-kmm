import SwiftUI
import shared

struct RegisterView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var container: AppContainer
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""

    private var passwordsMatch: Bool { password == confirmPassword }
    private var isFormValid: Bool {
        !name.isBlank && !email.isBlank && password.count >= 6 && passwordsMatch
    }

    var body: some View {
        let state = container.auth.state
        Form {
            Section {
                TextField("Nome completo", text: $name)
                TextField("Email", text: $email)
                    .textInputAutocapitalization(.never).keyboardType(.emailAddress)
                SecureField("Senha (mín. 6 caracteres)", text: $password)
                SecureField("Confirmar Senha", text: $confirmPassword)
            }
            if !passwordsMatch && !confirmPassword.isEmpty {
                Section { Text("As senhas não coincidem").foregroundColor(.red).font(.caption) }
            }
            if let error = state?.error {
                Section { Text(friendlyError(error)).foregroundColor(.red).font(.caption) }
            }
            Section {
                Button(action: register) {
                    if state?.isLoading == true { ProgressView().frame(maxWidth: .infinity) }
                    else { Text("Criar Conta").frame(maxWidth: .infinity) }
                }
                .disabled(!isFormValid || state?.isLoading == true)
            }
        }
        .navigationTitle("Criar Conta")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Cancelar") { dismiss() }
            }
        }
        .onAppear {
            container.auth.vm.clearError()
        }
        .onReceive(container.auth.$state) { s in
            if s?.isAuthenticated == true { dismiss() }
        }
    }

    private func register() {
        container.auth.vm.onRegister(email: email, password: password, name: name)
    }

    private func friendlyError(_ raw: String) -> String {
        if raw.contains("email-already-in-use") || raw.contains("already in use") { return "Este e-mail já está cadastrado." }
        if raw.contains("badly formatted") || raw.contains("invalid-email") { return "E-mail inválido." }
        if raw.contains("weak-password") || raw.contains("weak password") { return "Senha muito fraca. Use ao menos 6 caracteres." }
        if raw.contains("network") || raw.contains("Network") { return "Sem conexão. Verifique sua internet." }
        return "Erro ao criar conta. Tente novamente."
    }
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespaces).isEmpty }
}
