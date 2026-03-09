import SwiftUI
import shared

struct LoginView: View {
    let onLoginSuccess: () -> Void

    @EnvironmentObject var container: AppContainer
    @State private var email = ""
    @State private var password = ""
    @State private var isLoading = false
    @State private var error: String?
    @State private var showRegister = false

    var body: some View {
        NavigationView {
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
                    
                    if let error = error {
                        Text(error)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                    
                    Button(action: login) {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("Entrar")
                                .fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.red)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                    .disabled(isLoading)
                    
                    Button(action: {
                        showRegister = true
                    }) {
                        Text("Criar Conta")
                            .foregroundColor(.red)
                    }
                }
                .padding(.horizontal, 32)
                
                Spacer()
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showRegister) {
                RegisterView()
            }
        }
    }
    
    private func login() {
        let vm = container.loginViewModel
        error = nil
        isLoading = true
        vm.onEmailChange(email: email)
        vm.onPasswordChange(password: password)
        vm.onLoginClick()
        observeLoginState(vm: vm)
    }

    private func observeLoginState(vm: LoginViewModel) {
        Task { @MainActor in
            while true {
                try? await Task.sleep(nanoseconds: 200_000_000)
                let state = vm.currentState()
                if !state.isLoading {
                    isLoading = false
                    if state.isLoggedIn {
                        onLoginSuccess()
                    } else if let err = state.error {
                        error = err
                    }
                    break
                }
            }
        }
    }
}
