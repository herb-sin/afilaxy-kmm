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
    @AppStorage("nps_shown") private var npsShown = false
    @State private var showHelperConsentAlert = false
    @State private var showNps = false
    @State private var weeklyCount: Int = -1     // -1 = ainda carregando
    // Total acumulado de todas as semanas — nunca zera na virada de semana ISO.
    // Exibido no pill do WeeklyStatusCard para que o usuário veja seu histórico real.
    @State private var totalEmergencies: Int = -1
    @State private var showPharmacyMap = false
    @State private var statsListener: ListenerRegistration? = nil
    // Localização para o RiskWidget — nil enquanto não obtida ou sem permissão
    @State private var riskLocation: CLLocationCoordinate2D? = nil

    var body: some View {
        // NOTA: ContentView já envolve HomeView em NavigationStack(path: $homeNavigationPath).
        // Não usar NavigationView nem NavigationStack aqui — aninhamento triplica os
        // níveis de navegação e oculta os itens de toolbar desta view na barra errada.
        ScrollView {
            LazyVStack(spacing: 20) {
                // Hero Section
                heroSection

                // Risk Widget — visível apenas se localização disponível
                if riskLocation != nil || container.risk.state?.isLoading == true {
                    RiskWidgetView(riskState: container.risk.state)
                }

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

                // Support Links
                supportLinksSection
            }
            .padding()
        }
        .background(Color.afiBackground)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(role: .destructive) {
                    showLogoutAlert = true
                } label: {
                    Label("Sair", systemImage: "rectangle.portrait.and.arrow.right")
                        .foregroundColor(.afiError)
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
        .onAppear {
            fetchWeeklyStatus()
            checkNps()
            fetchLocationForRisk()
        }
        .onDisappear { statsListener?.remove() }
        .sheet(isPresented: $showNps) {
            NpsSheetView(
                onSubmit: { score in
                    npsShown = true
                    showNps = false
                    submitNps(score: score)
                },
                onSkip: {
                    npsShown = true
                    showNps = false
                }
            )
        }
        .sheet(isPresented: $showPharmacyMap) {
            NavigationStack {
                MapView(pharmacyMode: true)
                    .environmentObject(container)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Fechar") { showPharmacyMap = false }
                        }
                    }
            }
        }
    }
    
    // MARK: - Hero Section

    private var heroSection: some View {
        WeeklyStatusCard(weeklyCount: weeklyCount, totalEmergencies: totalEmergencies)
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
                } else {
                    weeklyCount = 0
                    FileLogger.shared.write(level: "DEBUG", tag: "HomeView", message: "weeklyCount: chave \(weekKey) não encontrada no mapa. Keys disponíveis: \(weekly?.keys.joined(separator: ",") ?? "mapa nil")")
                }
                // Lê o total acumulado de emergências (campo escrito pela Cloud Function)
                let rawTotal = data["totalEmergencies"] as? NSNumber
                totalEmergencies = rawTotal?.intValue ?? 0
                FileLogger.shared.write(level: "DEBUG", tag: "HomeView",
                    message: "weeklyCount resolved=\(weeklyCount) totalEmergencies=\(totalEmergencies)")
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
                // Requester clicou em "Emergência Ativa" — abre EmergencyView com countdown.
                // NÃO usa AfilaxyOpenEmergency pois esse canal sempre abre EmergencyResponseView
                // (tela do helper), o que causava o loop de self-match dismiss quando o próprio
                // usuário era o requester.
                navigationPath.append(AppRoute.emergency)
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
        HStack(spacing: 16) {
            ActionCard(
                title: "Autocuidado",
                subtitle: "Saúde e bem-estar",
                icon: "heart.text.square.fill"
            ) {
                navigationPath.append(AppRoute.education)
            }

            ActionCard(
                title: "Comunidade",
                subtitle: "Grupo no WhatsApp",
                icon: "person.3.fill"
            ) {
                if let url = URL(string: "https://chat.whatsapp.com/BmSp54ER4hHBeow0KYCedL") {
                    UIApplication.shared.open(url)
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
                        showPharmacyMap = true
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

    /// Obtém localização para alimentar o RiskWidget.
    /// Reutiliza o LocationManager já presente — não solicita nova permissão.
    /// Se a permissão não foi concedida, simplesmente não exibe o widget.
    private func fetchLocationForRisk() {
        // Usa localização cacheada se já disponível (evita delay perceptível)
        if let cached = LocationManager.shared.currentLocation {
            let coord = cached.coordinate
            riskLocation = coord
            container.risk.loadRiskScore(latitude: coord.latitude, longitude: coord.longitude)
            FileLogger.shared.write(level: "DEBUG", tag: "HomeView",
                message: "fetchLocationForRisk: usando cache lat=\(coord.latitude) lon=\(coord.longitude)")
            return
        }
        // Caso contrário, busca assíncrono
        Task {
            let location = await LocationManager.shared.fetchCurrentLocation()
            guard let coord = location?.coordinate, coord.latitude != 0 else {
                FileLogger.shared.write(level: "WARN", tag: "HomeView",
                    message: "fetchLocationForRisk: sem localização disponível — RiskWidget oculto")
                return
            }
            await MainActor.run {
                riskLocation = coord
                container.risk.loadRiskScore(latitude: coord.latitude, longitude: coord.longitude)
                FileLogger.shared.write(level: "DEBUG", tag: "HomeView",
                    message: "fetchLocationForRisk: OK lat=\(coord.latitude) lon=\(coord.longitude)")
            }
        }
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

// MARK: - NPS Helpers

extension HomeView {

    private func checkNps() {
        guard !npsShown else { return }
        guard let firstAtStr = UserDefaults.standard.string(forKey: "first_emergency_at"),
              let firstAt = Double(firstAtStr) else { return }
        let sevenDaysMs: Double = 7 * 24 * 60 * 60 * 1000
        if Date().timeIntervalSince1970 * 1000 - firstAt >= sevenDaysMs {
            showNps = true
        }
    }

    private func submitNps(score: Int) {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        Firestore.firestore().collection("nps_responses").addDocument(data: [
            "userId": uid,
            "score": score,
            "timestamp": nowMs
        ])
    }
}

// MARK: - NpsSheetView

struct NpsSheetView: View {
    var onSubmit: (Int) -> Void
    var onSkip: () -> Void
    @State private var selected = -1

    var body: some View {
        NavigationView {
            VStack(spacing: 28) {
                VStack(spacing: 8) {
                    Text("Você recomendaria o Afilaxy?")
                        .font(.title3).bold()
                    Text("De 0 (pouco provável) a 10 (com certeza).")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 24)

                VStack(spacing: 8) {
                    HStack(spacing: 6) {
                        ForEach(0...5, id: \.self) { i in npsButton(i) }
                    }
                    HStack(spacing: 6) {
                        ForEach(6...10, id: \.self) { i in npsButton(i) }
                    }
                }

                Spacer()

                HStack(spacing: 20) {
                    Button("Pular") { onSkip() }
                        .foregroundColor(.secondary)
                    Button("Enviar") { onSubmit(max(0, selected)) }
                        .buttonStyle(.borderedProminent)
                        .disabled(selected < 0)
                }
                .padding(.bottom, 32)
            }
            .padding(.horizontal)
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    @ViewBuilder
    private func npsButton(_ score: Int) -> some View {
        let isSelected = score == selected
        Button { selected = score } label: {
            Text("\(score)")
                .font(.subheadline).bold()
                .frame(width: 40, height: 40)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(isSelected ? Color.accentColor :
                              score <= 6  ? Color.red.opacity(0.15) :
                              score <= 8  ? Color.orange.opacity(0.15) :
                                            Color.green.opacity(0.15))
                )
                .foregroundColor(isSelected ? .white : .primary)
        }
    }
}

