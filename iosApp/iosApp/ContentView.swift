import SwiftUI
import shared

// MARK: - App Routes
enum AppRoute: Hashable {
    case home, emergency, history, profile, professionals, map, portal
    case notifications, settings, about, terms, privacy, help
    case autocuidado, education
    case emergencyResponse(String)
    case chat(String)
    case professionalDetail(String)
}

// MARK: - Tab Selection
enum Tab: String, CaseIterable {
    case home = "home"
    case map = "map"
    case profile = "profile"
    case portal = "portal"
    
    var title: String {
        switch self {
        case .home: return "Home"
        case .map: return "Mapa"
        case .profile: return "Perfil"
        case .portal: return "Portal"
        }
    }
    
    var systemImage: String {
        switch self {
        case .home: return "house.fill"
        case .map: return "location.fill"
        case .profile: return "person.fill"
        case .portal: return "stethoscope"
        }
    }
}

struct ContentView: View {
    @EnvironmentObject var container: AppContainer
    @State private var isLoggedIn = false
    @State private var selectedTab: Tab = .home
    
    // Navigation paths for each tab
    @State private var homeNavigationPath = NavigationPath()
    @State private var mapNavigationPath = NavigationPath()
    @State private var profileNavigationPath = NavigationPath()
    @State private var portalNavigationPath = NavigationPath()
    
    // Emergency handling
    @State private var emergencyRouteActive = false
    @State private var chatNavigatedId: String? = nil
    @State private var wasActiveEmergency = false
    @State private var resolvedChatId: String? = nil
    /// IDs de emergências já encerradas nesta sessão — bloqueia FCM tardio
    /// de reabrir o chat após o usuário ter resolvido e voltado à home.
    @State private var resolvedEmergencyIds = Set<String>()
    
    // MARK: - Adaptive Tab View Style
    private var adaptiveTabViewStyle: DefaultTabViewStyle {
        return DefaultTabViewStyle()
    }

    var body: some View {
        if isLoggedIn {
            TabView(selection: $selectedTab) {
                // MARK: - Home Tab
                NavigationStack(path: $homeNavigationPath) {
                    HomeView(navigationPath: $homeNavigationPath)
                        .navigationDestination(for: AppRoute.self) { route in
                            destinationView(for: route)
                        }
                }
                .tabItem {
                    Label(Tab.home.title, systemImage: Tab.home.systemImage)
                }
                .tag(Tab.home)
                
                // MARK: - Map Tab
                NavigationStack(path: $mapNavigationPath) {
                    MapView()
                        .navigationDestination(for: AppRoute.self) { route in
                            destinationView(for: route)
                        }
                }
                .tabItem {
                    Label(Tab.map.title, systemImage: Tab.map.systemImage)
                }
                .tag(Tab.map)
                
                // MARK: - Profile Tab
                NavigationStack(path: $profileNavigationPath) {
                    ProfileView()
                        .navigationDestination(for: AppRoute.self) { route in
                            destinationView(for: route)
                        }
                }
                .tabItem {
                    Label(Tab.profile.title, systemImage: Tab.profile.systemImage)
                }
                .tag(Tab.profile)
                
                // MARK: - Portal Tab
                NavigationStack(path: $portalNavigationPath) {
                    PortalView()
                        .navigationDestination(for: AppRoute.self) { route in
                            destinationView(for: route)
                        }
                }
                .tabItem {
                    Label(Tab.portal.title, systemImage: Tab.portal.systemImage)
                }
                .tag(Tab.portal)
            }
            .tint(.blue)
            // iOS 18+ adaptive sidebar for iPad
            .tabViewStyle(adaptiveTabViewStyle)
            .onAppear {
                setupLocationPermissions()
            }
            .onReceive(container.emergency.objectWillChange) { _ in
                handleEmergencyStateChange(container.emergency.state)
            }
            .onReceive(container.$resolvedEmergencyId.compactMap { $0 }) { _ in
                handleEmergencyResolved()
            }
            .onReceive(NotificationCenter.default.publisher(for: .init("AfilaxyOpenEmergency"))) { notification in
                handleEmergencyNotification(notification)
            }
            .onReceive(NotificationCenter.default.publisher(for: .init("AfilaxyOpenChat"))) { notification in
                guard let emergencyId = notification.userInfo?["emergencyId"] as? String else { return }
                handlePendingChat(emergencyId)
            }
            .onReceive(container.$pendingChatId.compactMap { $0 }) { emergencyId in
                handlePendingChat(emergencyId)
            }
            .onReceive(container.$pendingIncomingEmergencies) { emergencies in
                guard let first = emergencies.first else { return }

                // Guard de self-match: descarta se o ID é da própria emergência do usuário.
                // Isso pode chegar aqui por timing — o FCM da Cloud Function chega antes
                // de emergency.state?.emergencyId ser atualizado pelo KMM StateFlow,
                // pelo que o guard no AppContainer.observeChildren não bloqueou a tempo.
                let ownEmergencyId = container.emergency.state?.emergencyId as? String
                let isRequester = container.emergency.state?.isRequester == true
                if first.id == ownEmergencyId || isRequester {
                    FileLogger.shared.write(level: "WARN", tag: "ContentView",
                        message: "pendingIncomingEmergencies: self-match descartado emergencyId=\(first.id)")
                    container.dismissIncomingEmergency(id: first.id)
                    return
                }

                if emergencyRouteActive {
                    // Já navegamos para EmergencyResponseView (via notificação ou Firestore listener).
                    // Descarta o pending sem navegar novamente — evita que pendingEmergencies
                    // fique preso em 1 para sempre após o aceite.
                    container.dismissIncomingEmergency(id: first.id)
                } else {
                    // Navega diretamente para EmergencyResponseView sem dialog intermediário.
                    handleEmergencyNotification(
                        Notification(name: .init("AfilaxyOpenEmergency"),
                                     object: nil,
                                     userInfo: ["emergencyId": first.id])
                    )
                }
            }
            .onReceive(container.auth.$state) { authState in
                // Detecta logout — performLogout() sinaliza via Firebase → Kotlin → StateFlow → polling
                if authState?.isAuthenticated == false {
                    isLoggedIn = false
                    resolvedEmergencyIds = []  // limpa na saída de sessão
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: .init("AfilaxyEmergencyResolved"))) { notification in
                // ChatView disparou 'Resolver' — limpa toda a pilha de navegação de volta à home
                homeNavigationPath.removeLast(homeNavigationPath.count)
                // Guarda o ID resolvido ANTES de zerar chatNavigatedId, para que FCM tardios
                // de type=chat não reabram o chat encerrado.
                if let resolvedId = notification.userInfo?["emergencyId"] as? String {
                    resolvedEmergencyIds.insert(resolvedId)
                } else if let id = chatNavigatedId {
                    // Fallback: usa chatNavigatedId se o userInfo não trouxer o ID
                    resolvedEmergencyIds.insert(id)
                }
                chatNavigatedId = nil
                emergencyRouteActive = false
            }
        } else {
            LoginView(onLoginSuccess: {
                isLoggedIn = true
            })
            .onReceive(container.auth.$state) { authState in
                // Auto-navega ao entrar (auth detectado via polling StateFlow)
                if authState?.isAuthenticated == true {
                    isLoggedIn = true
                }
            }
        }
    }
    
    // MARK: - Navigation Destination Builder
    @ViewBuilder
    private func destinationView(for route: AppRoute) -> some View {
        switch route {
        case .emergency:
            EmergencyView()
                .onDisappear { emergencyRouteActive = false }
        case .emergencyResponse(let emergencyId):
            EmergencyResponseView(emergencyId: emergencyId)
                .onDisappear { emergencyRouteActive = false }
        case .chat(let emergencyId):
            ChatView(emergencyId: emergencyId)
        case .history:
            HistoryView()
        case .profile:
            ProfileView()
        case .professionals:
            ProfessionalListView()
        case .professionalDetail(let id):
            ProfessionalDetailView(professionalId: id)
        case .notifications:
            NotificationsView()
        case .settings:
            SettingsView()
        case .about:
            AboutView()
        case .terms:
            TermsView()
        case .privacy:
            PrivacyView()
        case .help:
            HelpView()

        case .autocuidado:
            AutocuidadoView()
        case .education:
            EducationView()
        case .map:
            MapView()
        case .portal:
            PortalView()
        case .home:
            EmptyView()
        }
    }
    
    // MARK: - Helper Methods
    private func setupLocationPermissions() {
        FileLogger.shared.write(
            level: "INFO",
            tag: "ContentView",
            message: "requestWhenInUse authStatus=\(LocationManager.shared.authorizationStatus.rawValue)"
        )
        if !LocationManager.shared.hasPermission {
            LocationManager.shared.requestWhenInUse()
        }
    }
    
    private func handleEmergencyStateChange(_ state: EmergencyState?) {
        let isActive = state?.hasActiveEmergency == true
        if (wasActiveEmergency && !isActive) || (resolvedChatId != nil && !isActive) {
            // Clear navigation paths
            homeNavigationPath.removeLast(homeNavigationPath.count)
            chatNavigatedId = nil
            resolvedChatId = nil
        }
        wasActiveEmergency = isActive
    }
    
    private func handleEmergencyResolved() {
        resolvedChatId = chatNavigatedId
        container.resolvedEmergencyId = nil
    }
    
    private func handleEmergencyNotification(_ notification: Notification) {
        guard let emergencyId = notification.userInfo?["emergencyId"] as? String,
              !emergencyRouteActive else { return }
        
        emergencyRouteActive = true
        container.dismissIncomingEmergency(id: emergencyId)
        
        // Navigate to emergency response in home tab
        selectedTab = .home
        homeNavigationPath.append(AppRoute.emergencyResponse(emergencyId))
    }
    
    private func handlePendingChat(_ emergencyId: String) {
        // Bloqueia FCM tardio de reabrir um chat cujo emergency já foi encerrado nesta sessão.
        guard !resolvedEmergencyIds.contains(emergencyId) else {
            FileLogger.shared.write(level: "DEBUG", tag: "ContentView",
                message: "handlePendingChat ignorado — emergência já encerrada emergencyId=\(emergencyId)")
            container.pendingChatId = nil
            return
        }
        guard chatNavigatedId != emergencyId else { return }
        
        chatNavigatedId = emergencyId
        resolvedChatId = nil
        
        // Navigate to chat in home tab
        selectedTab = .home
        homeNavigationPath.append(AppRoute.chat(emergencyId))
        container.pendingChatId = nil
    }
}
