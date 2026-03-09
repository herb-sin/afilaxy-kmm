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
        isLoading = true
        error = nil
        DispatchQueue.main.async {
            vm.onEmailChange(email: self.email)
            vm.onPasswordChange(password: self.password)
            vm.onLoginClick()
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                self.isLoading = false
                if let state = vm.state.value as? LoginState {
                    if state.isLoggedIn {
                        self.onLoginSuccess()
                    } else if let err = state.error {
                        self.error = err
                    }
                }
            }
        }
    }
}
