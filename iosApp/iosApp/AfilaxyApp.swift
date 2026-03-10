import SwiftUI
import shared
import FirebaseCore

class AppContainer: ObservableObject {
    lazy var auth = AuthViewModelWrapper(ViewModelProvider.shared.getAuthViewModel())
    lazy var emergency = EmergencyViewModelWrapper(ViewModelProvider.shared.getEmergencyViewModel())
    lazy var history = HistoryViewModelWrapper(ViewModelProvider.shared.getHistoryViewModel())
    lazy var profile = ProfileViewModelWrapper(ViewModelProvider.shared.getProfileViewModel())
    lazy var professionals = ProfessionalListViewModelWrapper(ViewModelProvider.shared.getProfessionalListViewModel())
    lazy var loginViewModel: LoginViewModel = ViewModelProvider.shared.getLoginViewModel()
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
