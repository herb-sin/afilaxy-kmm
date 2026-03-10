import SwiftUI
import shared

struct RegisterView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var container: AppContainer
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var vmState: AuthState? = nil

    private var passwordsMatch: Bool { password == confirmPassword }
    private var isFormValid: Bool {
        !name.isBlank && !email.isBlank && password.count >= 6 && passwordsMatch
    }

    var body: some View {
        Form {
            Section {
                TextField("Nome completo", text: $name)
                TextField("Email", text: $email)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
                SecureField("Senha (mín. 6 caracteres)", text: $password)
                SecureField("Confirmar Senha", text: $confirmPassword)
            }

            if !passwordsMatch && !confirmPassword.isEmpty {
                Section { Text("As senhas não coincidem").foregroundColor(.red).font(.caption) }
            }

            if let error = vmState?.error {
                Section { Text(error).foregroundColor(.red).font(.caption) }
            }

            Section {
                Button(action: register) {
                    if vmState?.isLoading == true {
                        ProgressView().frame(maxWidth: .infinity)
                    } else {
                        Text("Criar Conta").frame(maxWidth: .infinity)
                    }
                }
                .disabled(!isFormValid || vmState?.isLoading == true)
            }
        }
        .navigationTitle("Criar Conta")
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: vmState?.isAuthenticated) { _, isAuth in
            if isAuth == true { dismiss() }
        }
        .task {
            for await state in container.authViewModel.state {
                vmState = state
            }
        }
    }

    private func register() {
        container.authViewModel.onRegister(email: email, password: password, name: name)
    }
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespaces).isEmpty }
}
