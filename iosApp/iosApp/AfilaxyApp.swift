import SwiftUI
import shared
import FirebaseCore

class AppContainer: ObservableObject {
    lazy var loginViewModel: LoginViewModel = ViewModelProvider.shared.getLoginViewModel()
    lazy var authViewModel: AuthViewModel = ViewModelProvider.shared.getAuthViewModel()
    lazy var emergencyViewModel: EmergencyViewModel = ViewModelProvider.shared.getEmergencyViewModel()
    lazy var profileViewModel: ProfileViewModel = ViewModelProvider.shared.getProfileViewModel()
    lazy var historyViewModel: HistoryViewModel = ViewModelProvider.shared.getHistoryViewModel()
    lazy var professionalListViewModel: ProfessionalListViewModel = ViewModelProvider.shared.getProfessionalListViewModel()
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
