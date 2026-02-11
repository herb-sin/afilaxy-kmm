import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var authObserver = ObservableAuthViewModel()
    
    var body: some View {
        Group {
            if authObserver.isAuthenticated {
                HomeView()
            } else {
                LoginView()
            }
        }
        .onAppear {
            authObserver.checkAuth()
        }
    }
}

class ObservableAuthViewModel: ObservableObject {
    private let viewModel: AuthViewModel
    @Published var isAuthenticated = false
    
    init() {
        viewModel = ViewModelProvider.shared.getAuthViewModel()
        observeState()
    }
    
    private func observeState() {
        viewModel.state.watch { [weak self] state in
            guard let state = state as? AuthState else { return }
            DispatchQueue.main.async {
                self?.isAuthenticated = state.user != nil
            }
        }
    }
    
    func checkAuth() {
        viewModel.checkAuthState()
    }
}
