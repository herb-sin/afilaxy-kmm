import SwiftUI
import shared

enum AppRoute: Hashable {
    case home, emergency, history, profile, professionals
    case notifications, settings, about, terms, privacy, help
    case emergencyResponse(String)
    case chat(String)
    case professionalDetail(String)
}

struct ContentView: View {
    @EnvironmentObject var container: AppContainer
    @State private var isLoggedIn = false
    @State private var path = NavigationPath()
    @State private var emergencyRouteActive = false
    @State private var chatNavigatedId: String? = nil

    @State private var wasActiveEmergency = false
    @State private var resolvedChatId: String? = nil

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
                        .onDisappear { emergencyRouteActive = false }
                    case .emergencyResponse(let emergencyId):
                        EmergencyResponseView(emergencyId: emergencyId)
                        .onDisappear { emergencyRouteActive = false }
                    case .chat(let emergencyId):
                        ChatView(emergencyId: emergencyId)
                    case .history:     HistoryView()
                    case .profile:     ProfileView()
                    case .professionals: ProfessionalListView()
                    case .professionalDetail(let id): ProfessionalDetailView(professionalId: id)
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
            .onReceive(container.emergency.$state) { s in
                let isActive = s?.hasActiveEmergency == true
                if (wasActiveEmergency && !isActive) || (resolvedChatId != nil && !isActive) {
                    path.removeLast(path.count)
                    chatNavigatedId = nil
                    resolvedChatId = nil
                }
                wasActiveEmergency = isActive
            }
            .onReceive(container.$resolvedEmergencyId.compactMap { $0 }) { _ in
                resolvedChatId = chatNavigatedId
                container.resolvedEmergencyId = nil
            }
            .onReceive(NotificationCenter.default.publisher(for: .init("AfilaxyOpenEmergency"))) { notification in
                guard let emergencyId = notification.userInfo?["emergencyId"] as? String,
                      !emergencyRouteActive else { return }
                emergencyRouteActive = true
                container.dismissIncomingEmergency(id: emergencyId)
                path.append(AppRoute.emergencyResponse(emergencyId))
            }
            .onReceive(container.$pendingChatId.compactMap { $0 }) { emergencyId in
                guard chatNavigatedId != emergencyId else { return }
                chatNavigatedId = emergencyId
                resolvedChatId = nil
                path.append(AppRoute.chat(emergencyId))
                container.pendingChatId = nil
            }
        } else {
            LoginView(onLoginSuccess: {
                DispatchQueue.main.async { isLoggedIn = true }
            })
        }
    }
}
