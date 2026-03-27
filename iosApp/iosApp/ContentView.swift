import SwiftUI
import shared

// MARK: - App Routes
enum AppRoute: Hashable {
    case home, emergency, history, profile, professionals, map, portal
    case notifications, settings, about, terms, privacy, help
    case community, autocuidado, education
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
    
    // MARK: - Adaptive Tab View Style
    private var adaptiveTabViewStyle: any TabViewStyle {
        if #available(iOS 18.0, *) {
            return .sidebarAdaptable
        } else {
            return .automatic
        }
    }

    var body: some View {
        if isLoggedIn {
            TabView(selection: $selectedTab) {
                // MARK: - Home Tab
                NavigationStack(path: $homeNavigationPath) {
                    HomeView()
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
            .tint(.afiPrimary)
            // iOS 18+ adaptive sidebar for iPad
            .tabViewStyle(adaptiveTabViewStyle)
            .onAppear {
                setupLocationPermissions()
            }
            .onReceive(container.emergency.$state) { state in
                handleEmergencyStateChange(state)
            }
            .onReceive(container.$resolvedEmergencyId.compactMap { $0 }) { _ in
                handleEmergencyResolved()
            }
            .onReceive(NotificationCenter.default.publisher(for: .init("AfilaxyOpenEmergency"))) { notification in
                handleEmergencyNotification(notification)
            }
            .onReceive(container.$pendingChatId.compactMap { $0 }) { emergencyId in
                handlePendingChat(emergencyId)
            }
        } else {
            LoginView(onLoginSuccess: {
                isLoggedIn = true
            })
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
        case .community:
            CommunityView()
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
        guard chatNavigatedId != emergencyId else { return }
        
        chatNavigatedId = emergencyId
        resolvedChatId = nil
        
        // Navigate to chat in home tab
        selectedTab = .home
        homeNavigationPath.append(AppRoute.chat(emergencyId))
        container.pendingChatId = nil
    }
}
