import SwiftUI
import shared
import Combine

struct LoginView: View {
    @StateObject private var viewModel = ObservableLoginViewModel()
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
                    TextField("Email", text: $viewModel.email)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                        .onChange(of: viewModel.email) { newValue in
                            viewModel.onEmailChange(newValue)
                        }
                    
                    SecureField("Senha", text: $viewModel.password)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .onChange(of: viewModel.password) { newValue in
                            viewModel.onPasswordChange(newValue)
                        }
                    
                    if let error = viewModel.error {
                        Text(error)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                    
                    Button(action: {
                        viewModel.login()
                    }) {
                        if viewModel.isLoading {
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
                    .disabled(viewModel.isLoading)
                    
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
}

class ObservableLoginViewModel: ObservableObject {
    private let viewModel: LoginViewModel
    private var cancellables = Set<AnyCancellable>()
    
    @Published var email = ""
    @Published var password = ""
    @Published var isLoading = false
    @Published var error: String?
    @Published var isAuthenticated = false
    
    init() {
        viewModel = ViewModelProvider.shared.getLoginViewModel()
        observeState()
    }
    
    private func observeState() {
        viewModel.state.watch { [weak self] state in
            guard let state = state as? LoginState else { return }
            DispatchQueue.main.async {
                self?.email = state.email
                self?.password = state.password
                self?.isLoading = state.isLoading
                self?.error = state.error
            }
        }
    }
    
    func onEmailChange(_ email: String) {
        viewModel.onEmailChange(email: email)
    }
    
    func onPasswordChange(_ password: String) {
        viewModel.onPasswordChange(password: password)
    }
    
    func login() {
        viewModel.onLoginClick()
    }
}
