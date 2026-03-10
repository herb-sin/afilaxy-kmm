import SwiftUI

struct LoginView: View {
    let onLoginSuccess: () -> Void
    @EnvironmentObject var container: AppContainer

    @State private var email = ""
    @State private var password = ""
    @State private var showRegister = false

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

                    if let error = state.error {
                        Text(error).foregroundColor(.red).font(.caption)
                    }

                    Button(action: login) {
                        if state.isLoading {
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
                    .disabled(state.isLoading || email.isBlank || password.isBlank)

                    Button { showRegister = true } label: {
                        Text("Criar Conta").foregroundColor(.red)
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
            if s.isAuthenticated { onLoginSuccess() }
        }
    }

    private func login() {
        container.auth.vm.onLogin(email: email, password: password)
    }
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespaces).isEmpty }
}
