import SwiftUI
import shared

struct SettingsView: View {
    @Environment(\.dismiss) var dismiss
    @State private var notificationsEnabled = true
    @State private var locationAlways = false
    @State private var showLogoutAlert = false
    @State private var showAbout = false
    @State private var showTerms = false
    @State private var showPrivacy = false
    @State private var showHelp = false
    
    @State private var authViewModel: AuthViewModel? = nil
    
    var body: some View {
        NavigationView {
            List {
                Section(header: Text("Notificações")) {
                    Toggle("Ativar Notificações", isOn: $notificationsEnabled)
                }
                
                Section(header: Text("Localização")) {
                    Toggle("Permitir o Tempo Todo", isOn: $locationAlways)
                    
                    Text("Necessário para receber emergências próximas")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                Section(header: Text("Informações")) {
                    Button(action: { showHelp = true }) {
                        HStack {
                            Image(systemName: "questionmark.circle")
                            Text("Ajuda")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }
                    
                    Button(action: { showAbout = true }) {
                        HStack {
                            Image(systemName: "info.circle")
                            Text("Sobre")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }
                    
                    Button(action: { showTerms = true }) {
                        HStack {
                            Image(systemName: "doc.text")
                            Text("Termos de Uso")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }
                    
                    Button(action: { showPrivacy = true }) {
                        HStack {
                            Image(systemName: "lock.shield")
                            Text("Política de Privacidade")
                            Spacer()
                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.gray)
                        }
                    }
                }
                
                Section {
                    Button(action: {
                        showLogoutAlert = true
                    }) {
                        HStack {
                            Image(systemName: "arrow.right.square")
                            Text("Sair")
                        }
                        .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("Configurações")
            .navigationBarTitleDisplayMode(.inline)
            .task {
                if authViewModel == nil {
                    authViewModel = ViewModelProvider.shared.getAuthViewModel()
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Fechar") {
                        dismiss()
                    }
                }
            }
            .alert("Sair", isPresented: $showLogoutAlert) {
                Button("Cancelar", role: .cancel) { }
                Button("Sair", role: .destructive) {
                    authViewModel?.onLogout()
                    dismiss()
                }
            } message: {
                Text("Deseja realmente sair?")
            }
            .sheet(isPresented: $showAbout) {
                AboutView()
            }
            .sheet(isPresented: $showTerms) {
                TermsView()
            }
            .sheet(isPresented: $showPrivacy) {
                PrivacyView()
            }
            .sheet(isPresented: $showHelp) {
                HelpView()
            }
        }
    }
}
