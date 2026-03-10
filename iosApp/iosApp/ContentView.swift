import SwiftUI
import shared

enum AppRoute: Hashable {
    case home
    case emergency
    case history
    case profile
    case professionals
    case notifications
    case settings
    case about
    case terms
    case privacy
    case help
}

struct ContentView: View {
    @EnvironmentObject var container: AppContainer
    @State private var isLoggedIn = false
    @State private var path = NavigationPath()

    var body: some View {
        if isLoggedIn {
            NavigationStack(path: $path) {
                HomeView(
                    path: $path,
                    onLogout: { isLoggedIn = false }
                )
                .navigationDestination(for: AppRoute.self) { route in
                    switch route {
                    case .emergency:   EmergencyView()
                    case .history:     HistoryView()
                    case .profile:     ProfileView()
                    case .professionals: ProfessionalListView()
                    case .notifications: NotificationsView()
                    case .settings:    SettingsView()
                    case .about:       AboutView()
                    case .terms:       TermsView()
                    case .privacy:     PrivacyView()
                    case .help:        HelpView()
                    case .home:        EmptyView()
                    }
                }
            }
        } else {
            LoginView(onLoginSuccess: {
                DispatchQueue.main.async { isLoggedIn = true }
            })
        }
    }
}
