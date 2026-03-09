import SwiftUI
import shared
import FirebaseCore

class AppContainer: ObservableObject {
    let loginViewModel: LoginViewModel
    let emergencyViewModel: EmergencyViewModel
    let authViewModel: AuthViewModel

    init() {
        loginViewModel = ViewModelProvider.shared.getLoginViewModel()
        emergencyViewModel = ViewModelProvider.shared.getEmergencyViewModel()
        authViewModel = ViewModelProvider.shared.getAuthViewModel()
    }
}

@main
struct AfilaxyApp: App {
    let container: AppContainer

    init() {
        FirebaseApp.configure()
        KoinHelperKt.doInitKoin()
        container = AppContainer()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(container)
        }
    }
}
