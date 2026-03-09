import SwiftUI
import shared
import FirebaseCore

class AppContainer: ObservableObject {
    lazy var loginViewModel: LoginViewModel = ViewModelProvider.shared.getLoginViewModel()
    lazy var emergencyViewModel: EmergencyViewModel = ViewModelProvider.shared.getEmergencyViewModel()
    lazy var authViewModel: AuthViewModel = ViewModelProvider.shared.getAuthViewModel()
}

@main
struct AfilaxyApp: App {
    let container = AppContainer()

    init() {
        FirebaseApp.configure()
        KoinHelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(container)
        }
    }
}
