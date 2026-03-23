import SwiftUI
import CoreLocation
import FirebaseFirestore
import FirebaseAuth
import shared

struct HomeView: View {
    @Binding var path: NavigationPath
    let onLogout: () -> Void
    @EnvironmentObject var container: AppContainer
    @State private var showLogoutAlert = false
    @State private var helperToggle = false
    @State private var isTogglingHelper = false

    var body: some View {
        guard let state = container.emergency.state else {
            return AnyView(ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity))
        }
        return AnyView(homeBody(state: state))
    }

    private func activateHelperMode() {
        guard !isTogglingHelper else { return }
        isTogglingHelper = true
        FileLogger.shared.write(level: "INFO", tag: "HomeView", message: "activateHelperMode start")
        LocationManager.shared.requestWhenInUse()
        Task {
            let location = await LocationManager.shared.fetchCurrentLocation()
            let lat = location?.coordinate.latitude ?? 0
            let lon = location?.coordinate.longitude ?? 0
            FileLogger.shared.write(level: "INFO", tag: "HomeView", message: "fetchCurrentLocation result: \(lat), \(lon)")
            guard LocationManager.shared.hasPermission, lat != 0 else {
                await MainActor.run {
                    FileLogger.shared.write(level: "WARN", tag: "HomeView", message: "No permission or location after fetch")
                    isTogglingHelper = false
                }
                return
            }
            // Firestore write via iOS SDK (não passa pelo Kotlin/Native)
            LocationManagerBridge.shared.enableHelperMode(lat: lat, lon: lon) { success in
                if success {
                    self.container.emergency.vm.onToggleHelperMode(enable: true)
                    self.container.startObservingNearbyEmergencies(lat: lat, lon: lon)
                }
                self.isTogglingHelper = false
            }
        }
    }

    private func performLogout() {
        let state = container.emergency.state
        // Writes via iOS SDK nativo — evita SIGABRT do KMM no iOS
        if state?.hasActiveEmergency == true, let eid = state?.emergencyId as? String {
            Firestore.firestore().collection("emergency_requests").document(eid)
                .updateData(["active": false, "status": "cancelled"])
        }
        if state?.isHelperMode == true {
            if let uid = Auth.auth().currentUser?.uid {
                Firestore.firestore().collection("helpers").document(uid).delete()
            }
            LocationManagerBridge.shared.disableHelperMode()
        }
        container.emergency.vm.onToggleHelperMode(enable: false)
        try? Auth.auth().signOut()
        onLogout()
    }
        List {
            Section {
                Toggle(isOn: Binding(
                    get: { state.isHelperMode },
                    set: { newValue in
                        guard !isTogglingHelper else { return }
                        if newValue {
                            activateHelperMode()
                        } else {
                            LocationManagerBridge.shared.disableHelperMode()
                            container.emergency.vm.onToggleHelperMode(enable: false)
                            container.stopObservingNearbyEmergencies()
                        }
                    }
                )) {
                    Label("Modo Ajudante", systemImage: "heart.fill")
                }
                Text(state.isHelperMode ? "Você está disponível para ajudar" : "Ative para receber pedidos")
                    .font(.caption).foregroundColor(.secondary)
            }

            Section {
                Button {
                    path.append(AppRoute.emergency)
                } label: {
                    Label("🆘 EMERGÊNCIA", systemImage: "exclamationmark.triangle.fill")
                        .frame(maxWidth: .infinity).foregroundColor(.white).padding(.vertical, 8)
                }
                .listRowBackground(Color.red)
            }

            Section {
                NavigationLink(value: AppRoute.history)      { Label("Histórico", systemImage: "clock.fill") }
                NavigationLink(value: AppRoute.professionals){ Label("Profissionais", systemImage: "stethoscope") }
                NavigationLink(value: AppRoute.notifications){ Label("Notificações", systemImage: "bell.fill") }
                NavigationLink(value: AppRoute.profile)      { Label("Meu Perfil", systemImage: "person.fill") }
                NavigationLink(value: AppRoute.settings)     { Label("Configurações", systemImage: "gearshape.fill") }
            }

            if let error = state.error {
                Section { Text(error).foregroundColor(.red).font(.caption) }
            }
        }
        .navigationTitle("Afilaxy")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button { showLogoutAlert = true } label: {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                }
            }
        }
        .alert("Sair", isPresented: $showLogoutAlert) {
            Button("Cancelar", role: .cancel) {}
            Button("Sair", role: .destructive) {
                performLogout()
            }
        } message: { Text("Deseja realmente sair?") }
        .onReceive(container.auth.$state) { s in
            // Detecta logout disparado por views nested (ex: SettingsView)
            if s?.isAuthenticated == false { onLogout() }
        }
    }
}
