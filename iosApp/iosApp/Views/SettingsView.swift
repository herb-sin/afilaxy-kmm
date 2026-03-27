import SwiftUI
import FirebaseFirestore
import FirebaseAuth
import shared

struct SettingsView: View {
    @EnvironmentObject var container: AppContainer
    @State private var showLogoutAlert = false
    @State private var showShareSheet = false
    @State private var logFileURLs: [URL] = []
    @State private var showClearLogsAlert = false
    @State private var showNoLogsAlert = false
    @State private var notificationsEnabled = true
    @State private var helperModeAutoActivate = false
    @State private var emergencyRadius = 5.0

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Profile Summary Card
                ProfileSummaryCard()
                
                // Preferences Section
                SettingsSectionCard(title: "Preferências", icon: "slider.horizontal.3") {
                    VStack(spacing: 12) {
                        SettingsToggleRow(
                            title: "Notificações",
                            subtitle: "Receber alertas de emergência",
                            icon: "bell.fill",
                            isOn: $notificationsEnabled
                        )
                        
                        SettingsToggleRow(
                            title: "Modo Helper Automático",
                            subtitle: "Ativar automaticamente ao abrir o app",
                            icon: "heart.fill",
                            isOn: $helperModeAutoActivate
                        )
                        
                        SettingsSliderRow(
                            title: "Raio de Busca",
                            subtitle: "Distância para encontrar helpers",
                            icon: "location.circle.fill",
                            value: $emergencyRadius,
                            range: 1...10,
                            unit: "km"
                        )
                    }
                }
                
                // Account Section
                SettingsSectionCard(title: "Conta", icon: "person.circle.fill") {
                    VStack(spacing: 8) {
                        SettingsNavigationRow(
                            title: "Editar Perfil",
                            subtitle: "Alterar informações pessoais",
                            icon: "pencil.circle.fill",
                            destination: .profile
                        )
                        
                        SettingsNavigationRow(
                            title: "Histórico",
                            subtitle: "Ver emergências anteriores",
                            icon: "clock.fill",
                            destination: .history
                        )
                        
                        SettingsActionRow(
                            title: "Exportar Dados",
                            subtitle: "Baixar seus dados pessoais",
                            icon: "square.and.arrow.up.fill",
                            action: { /* Export data */ }
                        )
                    }
                }
                
                // Support Section
                SettingsSectionCard(title: "Suporte", icon: "questionmark.circle.fill") {
                    VStack(spacing: 8) {
                        SettingsNavigationRow(
                            title: "Ajuda",
                            subtitle: "Perguntas frequentes e tutoriais",
                            icon: "questionmark.circle.fill",
                            destination: .help
                        )
                        
                        SettingsNavigationRow(
                            title: "Sobre o Afilaxy",
                            subtitle: "Informações sobre o aplicativo",
                            icon: "info.circle.fill",
                            destination: .about
                        )
                        
                        SettingsActionRow(
                            title: "Avaliar App",
                            subtitle: "Deixe sua avaliação na App Store",
                            icon: "star.fill",
                            action: { /* Rate app */ }
                        )
                        
                        SettingsActionRow(
                            title: "Compartilhar",
                            subtitle: "Indique o Afilaxy para amigos",
                            icon: "square.and.arrow.up.fill",
                            action: { /* Share app */ }
                        )
                    }
                }
                
                // Legal Section
                SettingsSectionCard(title: "Legal", icon: "doc.text.fill") {
                    VStack(spacing: 8) {
                        SettingsNavigationRow(
                            title: "Termos de Uso",
                            subtitle: "Condições de utilização",
                            icon: "doc.text.fill",
                            destination: .terms
                        )
                        
                        SettingsNavigationRow(
                            title: "Política de Privacidade",
                            subtitle: "Como protegemos seus dados",
                            icon: "lock.shield.fill",
                            destination: .privacy
                        )
                    }
                }
                
                // Developer Section (Debug)
                if ProcessInfo.processInfo.environment["DEBUG_MODE"] != nil {
                    SettingsSectionCard(title: "Desenvolvedor", icon: "hammer.fill") {
                        VStack(spacing: 8) {
                            SettingsActionRow(
                                title: "Exportar Logs",
                                subtitle: "\(formatBytes(FileLogger.shared.getTotalLogSize()))",
                                icon: "square.and.arrow.up.fill",
                                action: exportLogs
                            )
                            
                            SettingsActionRow(
                                title: "Limpar Logs",
                                subtitle: "Remover todos os arquivos de log",
                                icon: "trash.fill",
                                isDestructive: true,
                                action: { showClearLogsAlert = true }
                            )
                        }
                    }
                }
                
                // Logout Section
                AfilaxyCard {
                    Button(action: { showLogoutAlert = true }) {
                        HStack {
                            Image(systemName: "rectangle.portrait.and.arrow.right.fill")
                                .foregroundColor(.red)
                                .font(.title3)
                            
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Sair")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                    .foregroundColor(.red)
                                Text("Desconectar da sua conta")
                                    .font(.caption)
                                    .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                            }
                            
                            Spacer()
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 100)
        }
        .navigationTitle("Configurações")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Sair", isPresented: $showLogoutAlert) {
            Button("Cancelar", role: .cancel) {}
            Button("Sair", role: .destructive) {
                performLogout()
            }
        } message: {
            Text("Deseja realmente sair da sua conta?")
        }
        .alert("Limpar Logs", isPresented: $showClearLogsAlert) {
            Button("Cancelar", role: .cancel) {}
            Button("Limpar", role: .destructive) {
                FileLogger.shared.clearAllLogs()
            }
        } message: {
            Text("Todos os logs serão removidos permanentemente.")
        }
        .alert("Sem Logs", isPresented: $showNoLogsAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Nenhum arquivo de log encontrado. Use o app para gerar logs.")
        }
        .sheet(isPresented: $showShareSheet) {
            if !logFileURLs.isEmpty {
                ShareSheet(items: logFileURLs)
            }
        }
    }
    
    private func performLogout() {
        let state = container.emergency.state
        if state?.hasActiveEmergency == true, let eid = state?.emergencyId as? String {
            Firestore.firestore().collection("emergency_requests").document(eid)
                .updateData(["active": false, "status": "cancelled"])
        }
        if state?.isHelperMode == true, let uid = Auth.auth().currentUser?.uid {
            Firestore.firestore().collection("helpers").document(uid).delete()
            LocationManagerBridge.shared.disableHelperMode()
        }
        container.emergency.clearEmergencyStateSwift()
        container.freezeAll()
        try? Auth.auth().signOut()
    }
    
    private func exportLogs() {
        let urls = FileLogger.shared.getAllLogFileURLs()
        FileLogger.shared.write(level: "DEBUG", tag: "SettingsView", message: "Found \(urls.count) log files")

        let content = urls.compactMap { url in
            try? String(contentsOf: url, encoding: .utf8)
        }.joined(separator: "\n\n--- next file ---\n\n")

        if content.isEmpty {
            showNoLogsAlert = true
        } else {
            logFileURLs = urls
            let av = UIActivityViewController(activityItems: [content], applicationActivities: nil)
            if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let root = scene.windows.first?.rootViewController {
                root.present(av, animated: true)
            }
        }
    }
    
    private func formatBytes(_ bytes: Int64) -> String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .file
        return formatter.string(fromByteCount: bytes)
    }
}

// MARK: - Supporting Views

struct ProfileSummaryCard: View {
    @EnvironmentObject var container: AppContainer
    
    var body: some View {
        HeroGradientCard {
            HStack(spacing: 16) {
                Circle()
                    .fill(AfilaxyColors.surface)
                    .frame(width: 60, height: 60)
                    .overlay {
                        Image(systemName: "person.fill")
                            .font(.title2)
                            .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                    }
                
                VStack(alignment: .leading, spacing: 4) {
                    if let profile = container.profile.state?.profile {
                        Text(profile.name) // Using name instead of displayName
                            .font(.headline)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                        
                        Text(profile.email)
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.8))
                        
                        if profile.isHealthProfessional {
                            StatusBadge(text: "Profissional de Saúde", style: .success)
                        }
                    } else {
                        Text("Carregando...")
                            .font(.headline)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
                
                Spacer()
            }
        }
    }
}

struct SettingsSectionCard<Content: View>: View {
    let title: String
    let icon: String
    @ViewBuilder let content: Content
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(AfilaxyColors.primary)
                    .font(.title3)
                
                Text(title)
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
            }
            .padding(.horizontal, 16)
            
            AfilaxyCard {
                content
            }
        }
    }
}

struct SettingsToggleRow: View {
    let title: String
    let subtitle: String
    let icon: String
    @Binding var isOn: Bool
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(AfilaxyColors.primary)
                .font(.title3)
                .frame(width: 24)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Text(subtitle)
                    .font(.caption)
                    .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
            }
            
            Spacer()
            
            Toggle("", isOn: $isOn)
                .toggleStyle(SwitchToggleStyle(tint: AfilaxyColors.primary))
        }
        .padding(.vertical, 4)
    }
}

struct SettingsSliderRow: View {
    let title: String
    let subtitle: String
    let icon: String
    @Binding var value: Double
    let range: ClosedRange<Double>
    let unit: String
    
    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(AfilaxyColors.primary)
                    .font(.title3)
                    .frame(width: 24)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                }
                
                Spacer()
                
                Text("\(Int(value)) \(unit)")
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(AfilaxyColors.primary)
            }
            
            Slider(value: $value, in: range, step: 1)
                .tint(AfilaxyColors.primary)
        }
        .padding(.vertical, 4)
    }
}

struct SettingsNavigationRow: View {
    let title: String
    let subtitle: String
    let icon: String
    let destination: AppRoute
    
    var body: some View {
        NavigationLink(value: destination) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(AfilaxyColors.primary)
                    .font(.title3)
                    .frame(width: 24)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(AfilaxyColors.onSurface.opacity(0.4))
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct SettingsActionRow: View {
    let title: String
    let subtitle: String
    let icon: String
    let isDestructive: Bool
    let action: () -> Void
    
    init(title: String, subtitle: String, icon: String, isDestructive: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.isDestructive = isDestructive
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(isDestructive ? .red : AfilaxyColors.primary)
                    .font(.title3)
                    .frame(width: 24)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(isDestructive ? .red : AfilaxyColors.onSurface)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(AfilaxyColors.onSurface.opacity(0.6))
                }
                
                Spacer()
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
