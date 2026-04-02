import SwiftUI
import CoreLocation
import FirebaseFirestore
import FirebaseAuth
import shared

struct HomeView: View {
    @EnvironmentObject var container: AppContainer
    @Environment(\.dismiss) private var dismiss
    @Binding var navigationPath: NavigationPath  // recebe homeNavigationPath do ContentView
    @State private var showLogoutAlert = false
    @State private var helperToggle = false
    @State private var isTogglingHelper = false
    @State private var helperIntendedValue = false  // valor visual enquanto opção está pendente
    // Consentimento LGPD persistido entre sessões (UserDefaults via @AppStorage)
    @AppStorage("helperMapConsentGiven") private var helperMapConsentGiven = false
    @State private var showHelperConsentAlert = false
    @State private var weeklyCount: Int = -1     // -1 = ainda carregando
    @State private var statsListener: ListenerRegistration? = nil

    var body: some View {
        // NOTA: ContentView já envolve HomeView em NavigationStack(path: $homeNavigationPath).
        // Não usar NavigationView nem NavigationStack aqui — aninhamento triplica os
        // níveis de navegação e oculta os itens de toolbar desta view na barra errada.
        ScrollView {
            LazyVStack(spacing: 20) {
                // Hero Section
                heroSection

                // Emergency Button
                emergencyButton

                // Helper Mode Toggle
                helperModeCard

                // Pending Emergencies (if any)
                if !container.pendingIncomingEmergencies.isEmpty {
                    pendingEmergenciesSection
                }

                // Quick Actions Grid
                quickActionsGrid

                // Community Feed Preview
                communityFeedPreview

                // Support Links
                supportLinksSection
            }
            .padding()
        }
        .background(Color.afiBackground)
        .navigationTitle("Afilaxy")
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button {
                        exportLogs()
                    } label: {
                        Label("Exportar Logs", systemImage: "doc.text.fill")
                    }

                    Divider()

                    Button(role: .destructive) {
                        showLogoutAlert = true
                    } label: {
                        Label("Sair", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle.fill")
                        .foregroundColor(.afiPrimary)
                }
            }
        }
        .alert("Sair", isPresented: $showLogoutAlert) {
            Button("Cancelar", role: .cancel) {}
            Button("Sair", role: .destructive) {
                performLogout()
            }
        } message: {
            Text("Deseja realmente sair?")
        }
        // Dialog LGPD: exibido apenas na primeira ativação do Modo Ajudante
        .alert("Sua localização será visível", isPresented: $showHelperConsentAlert) {
            Button("Concordar e Continuar") {
                helperMapConsentGiven = true
                activateHelperMode()
            }
            Button("Cancelar", role: .cancel) {
                helperIntendedValue = false  // reverte o toggle
                isTogglingHelper = false
            }
        } message: {
            Text(
                "Ao ativar o Modo Ajudante, sua posição aproximada (±100 m) ficará visível " +
                "no mapa para outros usuários Afilaxy.\nSeu nome de exibição é compartilhado; " +
                "nenhum endereço ou dado sensível.\nDesative o modo a qualquer momento."
            )
        }
        .onAppear { fetchWeeklyStatus() }
        .onDisappear { statsListener?.remove() }
    }
    
    // MARK: - Hero Section

    private var heroSection: some View {
        WeeklyStatusCard(weeklyCount: weeklyCount)
    }

    // MARK: - Fetch weekly stats (listener em tempo real)
    private func fetchWeeklyStatus() {
        guard let uid = Auth.auth().currentUser?.uid else {
            FileLogger.shared.write(level: "WARN", tag: "HomeView", message: "fetchWeeklyStatus: uid nil, não iniciando listener")
            return
        }

        // Cancela listener anterior se existir (evita duplicatas em reappear)
        statsListener?.remove()
        weeklyCount = -1  // mostra skeleton enquanto carrega

        // Semana ISO 8601 em UTC — mesma referência usada pela Cloud Function.
        var cal = Calendar(identifier: .iso8601)
        cal.timeZone = TimeZone(identifier: "UTC")!
        let week = cal.component(.weekOfYear, from: Date())
        let year = cal.component(.yearForWeekOfYear, from: Date())
        let weekKey = String(format: "%d-W%02d", year, week)
        FileLogger.shared.write(level: "DEBUG", tag: "HomeView", message: "fetchWeeklyStatus uid=\(uid) weekKey=\(weekKey)")

        statsListener = Firestore.firestore()
            .collection("user_stats")
            .document(uid)
            .addSnapshotListener { snapshot, error in
                if let error = error {
                    FileLogger.shared.write(level: "WARN", tag: "HomeView", message: "weeklyCount error: \(error.localizedDescription)")
                    weeklyCount = 0
                    return
                }
                guard let data = snapshot?.data() else {
                    FileLogger.shared.write(level: "WARN", tag: "HomeView", message: "weeklyCount: snapshot sem dados (uid=\(uid) weekKey=\(weekKey) exists=\(snapshot?.exists ?? false))")
                    weeklyCount = 0
                    return
                }
                let weekly = data["weeklyCount"] as? [String: Any]
                let raw = weekly?[weekKey]
                FileLogger.shared.write(level: "DEBUG", tag: "HomeView",
                    message: "weeklyCount weekKey=\(weekKey) weekly=\(String(describing: weekly)) raw=\(String(describing: raw)) rawType=\(type(of: raw))")
                if let r = raw {
                    let resolved = (r as? NSNumber)?.intValue ?? 0
                    weeklyCount = resolved
                    FileLogger.shared.write(level: "DEBUG", tag: "HomeView", message: "weeklyCount resolved=\(resolved)")
                } else {
                    weeklyCount = 0
                    FileLogger.shared.write(level: "DEBUG", tag: "HomeView", message: "weeklyCount: chave \(weekKey) não encontrada no mapa. Keys disponíveis: \(weekly?.keys.joined(separator: ",") ?? "mapa nil")")
                }
            }
    }
    
    // MARK: - Emergency Button
    private var emergencyButton: some View {
        let state = container.emergency.state
        let isActive = state?.hasActiveEmergency == true
        
        return EmergencyButton(
            title: isActive ? "Emergência Ativa" : "🆘 Solicitar Ajuda",
            isActive: isActive
        ) {
            if isActive {
                // Navega para a emergência ativa
                if let emergencyId = state?.emergencyId as? String {
                    NotificationCenter.default.post(
                        name: .init("AfilaxyOpenEmergency"),
                        object: nil,
                        userInfo: ["emergencyId": emergencyId]
                    )
                }
            } else {
                // Abre a tela de criação de emergência
                navigationPath.append(AppRoute.emergency)
            }
        }
    }
    
    private var helperModeCard: some View {
        let state = container.emergency.state
        let isHelperMode = state?.isHelperMode == true

        let helperBinding = Binding<Bool>(
            get: {
                isTogglingHelper ? helperIntendedValue : isHelperMode
            },
            set: { newValue in
                guard !isTogglingHelper else { return }
                helperIntendedValue = newValue
                if newValue {
                    if helperMapConsentGiven {
                        // Consentimento já dado — ativa diretamente
                        activateHelperMode()
                    } else {
                        // Primeira vez — exibe dialog LGPD antes de qualquer ação
                        showHelperConsentAlert = true
                    }
                } else {
                    deactivateHelperMode()
                }
            }
        )

        return ToggleCard(
            title: "Modo Ajudante",
            subtitle: isHelperMode ? "Você está disponível para ajudar" : "Ative para receber pedidos de ajuda",
            icon: "heart.fill",
            isOn: helperBinding
        ) { _ in } // binding set: já cuida da lógica
    }
    
    // MARK: - Pending Emergencies
    private var pendingEmergenciesSection: some View {
        AfilaxyCard(backgroundColor: .afiErrorContainer) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(.afiError)
                    
                    Text("Emergências Pendentes")
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.afiOnErrorContainer)
                    
                    Spacer()
                }
                
                ForEach(container.pendingIncomingEmergencies, id: \.id) { emergency in
                    PendingEmergencyRow(emergency: emergency) {
                        // Accept emergency
                        container.dismissIncomingEmergency(id: emergency.id)
                        // TODO: Navigate to emergency response
                    } onDismiss: {
                        // Dismiss emergency
                        container.dismissIncomingEmergency(id: emergency.id)
                    }
                }
            }
        }
    }
    
    // MARK: - Quick Actions Grid
    private var quickActionsGrid: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
            ActionCard(
                title: "Histórico",
                subtitle: "Suas emergências anteriores",
                icon: "clock.fill"
            ) {
                navigationPath.append(AppRoute.history)
            }
            
            ActionCard(
                title: "Profissionais",
                subtitle: "Encontre especialistas",
                icon: "stethoscope"
            ) {
                navigationPath.append(AppRoute.professionals)
            }
            
            ActionCard(
                title: "Educação",
                subtitle: "Aprenda sobre asma",
                icon: "graduationcap.fill"
            ) {
                navigationPath.append(AppRoute.education)
            }
            
            ActionCard(
                title: "Comunidade",
                subtitle: "Conecte-se com outros",
                icon: "person.3.fill"
            ) {
                navigationPath.append(AppRoute.community)
            }
        }
    }
    
    // MARK: - Community Feed Preview
    private var communityFeedPreview: some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Comunidade")
                        .font(.headline)
                        .fontWeight(.bold)
                        .foregroundColor(.afiTextPrimary)
                    
                    Spacer()
                    
                    Button("Ver Mais") {
                        navigationPath.append(AppRoute.community)
                    }
                    .font(.subheadline)
                    .foregroundColor(.afiPrimary)
                }
                
                VStack(spacing: 8) {
                    CommunityPostPreview(
                        author: "Maria S.",
                        content: "Consegui controlar melhor minha asma seguindo as dicas do app! 💪",
                        timeAgo: "2h"
                    )
                    
                    CommunityPostPreview(
                        author: "João P.",
                        content: "Alguém sabe onde encontrar bombinha mais barata na região?",
                        timeAgo: "4h"
                    )
                }
            }
        }
    }
    
    // MARK: - Support Links
    private var supportLinksSection: some View {
        AfilaxyCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Suporte Rápido")
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.afiTextPrimary)
                
                VStack(spacing: 8) {
                    SupportLinkRow(
                        title: "Farmácias 24h",
                        subtitle: "Encontre medicamentos",
                        icon: "cross.fill",
                        color: .afiSuccess
                    ) {
                        // Open Maps app to search for pharmacies
                        if let url = URL(string: "maps://?q=farmácia") {
                            UIApplication.shared.open(url)
                        }
                    }
                    
                    SupportLinkRow(
                        title: "Protocolo de Crise",
                        subtitle: "Passos para emergência",
                        icon: "list.clipboard.fill",
                        color: .afiWarning
                    ) {
                        navigationPath.append(AppRoute.help)
                    }
                    
                    SupportLinkRow(
                        title: "SAMU 192",
                        subtitle: "Emergência médica",
                        icon: "phone.fill",
                        color: .afiError
                    ) {
                        if let url = URL(string: "tel://192") {
                            UIApplication.shared.open(url)
                        }
                    }
                }
            }
        }
    }
    
    // MARK: - Helper Methods
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
                    helperIntendedValue = false  // reverte visual se falhou
                    isTogglingHelper = false
                }
                return
            }
            
            LocationManagerBridge.shared.enableHelperMode(lat: lat, lon: lon) { success in
                if success {
                    self.container.emergency.setHelperMode(true)
                    self.container.startObservingNearbyEmergencies(lat: lat, lon: lon)
                    // Ativa background location em modo econômico (100m) para o helper
                    // conseguir detectar emergências mesmo com app em background.
                    // Solicita upgrade WhenInUse→Always com delay para não conflitar
                    // com rerender SwiftUI em andamento (crash se apresentado imediatamente)
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
                        LocationManager.shared.startBackgroundUpdating()
                        LocationManager.shared.requestAlwaysIfNeeded()
                    }
                } else {
                    // Falhou ou foi cancelado — reverte o visual
                    self.helperIntendedValue = false
                }
                self.isTogglingHelper = false
            }
        }
    }
    
    private func deactivateHelperMode() {
        helperIntendedValue = false
        LocationManagerBridge.shared.cancelPendingHelperActivation()
        LocationManagerBridge.shared.disableHelperMode()
        container.emergency.setHelperMode(false)
        container.stopObservingNearbyEmergencies()
    }
    
    private func performLogout() {
        let state = container.emergency.state

        // Firestore cleanup antes de congelar os ViewModels
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

        // Passo 1: limpa o estado Kotlin — pode agendar blocos na main queue via Dispatchers.Main
        container.emergency.clearEmergencyStateSwift()

        // Passo 2: aguarda 0.5s para drenar a main dispatch queue antes de invalidar o
        // contexto Firebase. Sem esse delay, os blocos Kotlin agendados executam APÓS
        // o signOut() e tentam acessar Firebase sem auth válida → SIGABRT.
        // freezeAll() já chama Auth.auth().signOut() internamente — sem duplicação.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak container] in
            container?.freezeAll()
        }
    }
    
    private func exportLogs() {
        let urls = FileLogger.shared.getAllLogFileURLs()
        let content = urls.compactMap { try? String(contentsOf: $0, encoding: .utf8) }
                          .joined(separator: "\n\n--- next file ---\n\n")
        guard !content.isEmpty else { return }
        let av = UIActivityViewController(activityItems: [content], applicationActivities: nil)
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root = scene.windows.first?.rootViewController {
            root.present(av, animated: true)
        }
    }
    
    // MARK: - Navigation Destination Builder
    @ViewBuilder
    private func destinationView(for route: AppRoute) -> some View {
        switch route {
        case .emergency:
            EmergencyView()
        case .history:
            HistoryView()
        case .professionals:
            ProfessionalListView()
        case .education:
            EducationView()
        case .community:
            CommunityView()
        case .help:
            HelpView()
        case .professionalDetail(let id):
            ProfessionalDetailView(professionalId: id)
        default:
            EmptyView()
        }
    }
}

// MARK: - Supporting Views

struct PendingEmergencyRow: View {
    let emergency: Any // Placeholder type
    let onAccept: () -> Void
    let onDismiss: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.afiError)
            
            VStack(alignment: .leading, spacing: 2) {
                Text("Pessoa precisa de ajuda") // Fallback since emergency.name doesn't exist
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundColor(.afiOnErrorContainer)
                
                Text("Toque para responder")
                    .font(.caption)
                    .foregroundColor(.afiOnErrorContainer.opacity(0.8))
            }
            
            Spacer()
            
            HStack(spacing: 8) {
                Button("Recusar") {
                    onDismiss()
                }
                .font(.caption)
                .foregroundColor(.afiError)
                
                Button("Ajudar") {
                    onAccept()
                }
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.afiSuccess)
            }
        }
        .padding(.vertical, 4)
    }
}

struct CommunityPostPreview: View {
    let author: String
    let content: String
    let timeAgo: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Circle()
                .fill(Color.afiPrimary.opacity(0.2))
                .frame(width: 32, height: 32)
                .overlay(
                    Text(String(author.prefix(1)))
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.afiPrimary)
                )
            
            VStack(alignment: .leading, spacing: 2) {
                HStack {
                    Text(author)
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.afiTextPrimary)
                    
                    Text(timeAgo)
                        .font(.caption2)
                        .foregroundColor(.afiTextSecondary)
                }
                
                Text(content)
                    .font(.caption)
                    .foregroundColor(.afiTextSecondary)
                    .lineLimit(2)
            }
            
            Spacer()
        }
    }
}

struct SupportLinkRow: View {
    let title: String
    let subtitle: String
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundColor(color)
                    .frame(width: 24)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.afiTextPrimary)
                    
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.afiTextSecondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.afiTextSecondary)
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
}
