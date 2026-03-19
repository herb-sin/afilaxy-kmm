import SwiftUI
import shared

enum AppRoute: Hashable {
    case home, emergency, history, profile, professionals
    case notifications, settings, about, terms, privacy, help
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
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    FileLogger.shared.write(level: "INFO", tag: "ContentView", message: "requestWhenInUse authStatus=\(LocationManager.shared.authorizationStatus.rawValue)")
                    if !LocationManager.shared.hasPermission {
                        LocationManager.shared.requestWhenInUse()
                    }
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: .init("AfilaxyOpenEmergency"))) { notification in
                if notification.userInfo?["emergencyId"] is String {
                    path.append(AppRoute.emergency)
                }
            }
        } else {
            LoginView(onLoginSuccess: {
                DispatchQueue.main.async { isLoggedIn = true }
            })
        }
    }
}
