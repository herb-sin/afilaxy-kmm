import SwiftUI
import shared

struct HomeView: View {
    @Binding var path: NavigationPath
    let onLogout: () -> Void

    @EnvironmentObject var container: AppContainer
    @State private var vmState: EmergencyState? = nil
    @State private var showLogoutAlert = false

    var body: some View {
        let isHelperMode = vmState?.isHelperMode ?? false

        List {
            Section {
                Toggle(isOn: Binding(
                    get: { isHelperMode },
                    set: { container.emergencyViewModel.onToggleHelperMode(enable: $0) }
                )) {
                    Label("Modo Ajudante", systemImage: "heart.fill")
                }
                Text(isHelperMode ? "Você está disponível para ajudar" : "Ative para receber pedidos")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Section {
                Button {
                    path.append(AppRoute.emergency)
                } label: {
                    Label("🆘 EMERGÊNCIA", systemImage: "exclamationmark.triangle.fill")
                        .frame(maxWidth: .infinity)
                        .foregroundColor(.white)
                        .padding(.vertical, 8)
                }
                .listRowBackground(Color.red)
            }

            Section {
                NavigationLink(value: AppRoute.history) {
                    Label("Histórico", systemImage: "clock.fill")
                }
                NavigationLink(value: AppRoute.professionals) {
                    Label("Profissionais", systemImage: "stethoscope")
                }
                NavigationLink(value: AppRoute.notifications) {
                    Label("Notificações", systemImage: "bell.fill")
                }
                NavigationLink(value: AppRoute.profile) {
                    Label("Meu Perfil", systemImage: "person.fill")
                }
                NavigationLink(value: AppRoute.settings) {
                    Label("Configurações", systemImage: "gearshape.fill")
                }
            }

            if let error = vmState?.error {
                Section {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }
            }
        }
        .navigationTitle("Afilaxy")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showLogoutAlert = true
                } label: {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                }
            }
        }
        .alert("Sair", isPresented: $showLogoutAlert) {
            Button("Cancelar", role: .cancel) {}
            Button("Sair", role: .destructive) {
                container.authViewModel.onLogout()
                onLogout()
            }
        } message: {
            Text("Deseja realmente sair?")
        }
        .task {
            for await state in container.emergencyViewModel.state {
                vmState = state
            }
        }
    }
}
