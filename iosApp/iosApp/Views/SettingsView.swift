import SwiftUI
import shared

struct SettingsView: View {
    @EnvironmentObject var container: AppContainer
    @State private var showLogoutAlert = false

    var body: some View {
        List {
            Section("Informações") {
                NavigationLink(value: AppRoute.help) {
                    Label("Ajuda", systemImage: "questionmark.circle")
                }
                NavigationLink(value: AppRoute.about) {
                    Label("Sobre", systemImage: "info.circle")
                }
                NavigationLink(value: AppRoute.terms) {
                    Label("Termos de Uso", systemImage: "doc.text")
                }
                NavigationLink(value: AppRoute.privacy) {
                    Label("Política de Privacidade", systemImage: "lock.shield")
                }
            }

            Section {
                Button(role: .destructive) {
                    showLogoutAlert = true
                } label: {
                    Label("Sair", systemImage: "rectangle.portrait.and.arrow.right")
                }
            }
        }
        .navigationTitle("Configurações")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Sair", isPresented: $showLogoutAlert) {
            Button("Cancelar", role: .cancel) {}
            Button("Sair", role: .destructive) {
                container.authViewModel.onLogout()
            }
        } message: {
            Text("Deseja realmente sair?")
        }
    }
}
