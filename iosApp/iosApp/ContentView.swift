import SwiftUI
import shared

struct ContentView: View {
    @State private var isAuthenticated = false
    
    var body: some View {
        Group {
            if isAuthenticated {
                HomeView()
            } else {
                LoginView(onLoginSuccess: {
                    isAuthenticated = true
                })
            }
        }
        .onAppear {
            checkAuth()
        }
    }
    
    private func checkAuth() {
        let viewModel = ViewModelProvider.shared.getAuthViewModel()
        viewModel.checkAuthState()
        // Simplificado: verificar se há usuário
        isAuthenticated = viewModel.state.value.user != nil
    }
}
