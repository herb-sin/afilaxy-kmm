import SwiftUI
import FirebaseAuth

struct LoginView: View {
    let onLoginSuccess: () -> Void

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
                    
                    Button(action: { showRegister = true }) {
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
        guard !email.isBlank, !password.isBlank else {
            error = email.isBlank ? "Email não pode estar vazio" : "Senha não pode estar vazia"
            return
        }
        isLoading = true
        error = nil
        Auth.auth().signIn(withEmail: email, password: password) { _, err in
            isLoading = false
            if let err = err {
                error = err.localizedDescription
            } else {
                onLoginSuccess()
            }
        }
    }
}

private extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespaces).isEmpty }
}
