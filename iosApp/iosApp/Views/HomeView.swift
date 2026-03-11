import SwiftUI
import shared

struct HomeView: View {
    @Binding var path: NavigationPath
    let onLogout: () -> Void
    @EnvironmentObject var container: AppContainer
    @State private var showLogoutAlert = false

    var body: some View {
        guard let state = container.emergency.state else {
            return AnyView(ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity))
        }
        return AnyView(homeBody(state: state))
    }

    @ViewBuilder
    private func homeBody(state: EmergencyState) -> some View {
        List {
            Section {
                Toggle(isOn: Binding(
                    get: { state.isHelperMode },
                    set: { container.emergency.vm.onToggleHelperMode(enable: $0) }
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
                container.auth.vm.onLogout()
                onLogout()
            }
        } message: { Text("Deseja realmente sair?") }
        .onReceive(container.auth.$state) { s in
            // Detecta logout disparado por views nested (ex: SettingsView)
            if s?.isAuthenticated == false { onLogout() }
        }
    }
}
