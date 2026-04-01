import SwiftUI
import shared
import FirebaseCore
import FirebaseAuth
import FirebaseMessaging
import FirebaseFirestore
import UserNotifications
import Combine
import GoogleMaps

// MARK: - AppDelegate (APNs + FCM)

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            guard granted else { return }
            DispatchQueue.main.async { application.registerForRemoteNotifications() }
        }
        Messaging.messaging().delegate = self
        return true
    }

    // FCM data-only em background / foreground — roteia por tipo
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        let type = userInfo["type"] as? String ?? "unknown"
        FileLogger.shared.write(level: "INFO", tag: "AppDelegate", message: "didReceiveRemoteNotification type=\(type) appState=\(application.applicationState.rawValue)")
        guard let emergencyId = userInfo["emergencyId"] as? String else {
            completionHandler(.noData)
            return
        }
        switch type {
        case "emergency_request":
            let name = userInfo["requesterName"] as? String ?? "Alguém"
            NotificationCenter.default.post(
                name: .init("AfilaxyIncomingEmergency"),
                object: nil,
                userInfo: ["emergencyId": emergencyId, "requesterName": name]
            )
            completionHandler(.newData)
        case "helper_matched":
            // Enviado ao REQUESTER: helper aceitou, navega para o chat
            NotificationCenter.default.post(
                name: .init("AfilaxyOpenChat"),
                object: nil,
                userInfo: ["emergencyId": emergencyId]
            )
            completionHandler(.newData)
        case "chat":
            // App em foreground: navega direto para ChatView
            NotificationCenter.default.post(
                name: .init("AfilaxyOpenChat"),
                object: nil,
                userInfo: ["emergencyId": emergencyId]
            )
            completionHandler(.newData)
        default:
            completionHandler(.noData)
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        FileLogger.shared.write(level: "ERROR", tag: "AppDelegate", message: "APNs registration failed: \(error.localizedDescription)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        saveFcmToken(token)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let type = userInfo["type"] as? String ?? "unknown"
        let emergencyId = userInfo["emergencyId"] as? String ?? "nil"
        FileLogger.shared.write(level: "INFO", tag: "AppDelegate", message: "didReceive notificationResponse type=\(type) emergencyId=\(emergencyId) action=\(response.actionIdentifier)")
        guard let eid = userInfo["emergencyId"] as? String else {
            completionHandler()
            return
        }
        // Delay de 1s: dá tempo ao SwiftUI de concluir a transição background→foreground
        // antes de processar a navegação. Sem o delay, NavigationPath.append pode
        // ser descartado durante a animação de ativação da cena.
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            if type == "chat" || type == "helper_matched" {
                // chat      → nova mensagem: abre o chat
                // helper_matched → REQUESTER foi aceito por um helper: abre o chat
                // (EmergencyResponseView é a tela do HELPER, não do requester)
                NotificationCenter.default.post(
                    name: .init("AfilaxyOpenChat"),
                    object: nil,
                    userInfo: ["emergencyId": eid]
                )
            } else {
                // emergency_request → HELPER recebeu pedido de ajuda: abre EmergencyResponse
                NotificationCenter.default.post(
                    name: .init("AfilaxyOpenEmergency"),
                    object: nil,
                    userInfo: ["emergencyId": eid]
                )
            }
        }
        completionHandler()
    }

    private func saveFcmToken(_ token: String) {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        Firestore.firestore().collection("users").document(uid)
            .setData(["fcmToken": token], merge: true) { error in
                if let error = error {
                    FileLogger.shared.write(level: "ERROR", tag: "AppDelegate", message: "Erro ao salvar FCM token: \(error.localizedDescription)")
                }
            }
    }
}

// MARK: - AppContainer

class AppContainer: ObservableObject {
    // ViewModels opcionais — nunca crasham na criação
    // São pré-aquecidos em warmUp() após o Koin inicializar
    private(set) var _auth: AuthViewModelWrapper?
    private(set) var _emergency: EmergencyViewModelWrapper?
    private(set) var _history: HistoryViewModelWrapper?
    private(set) var _profile: ProfileViewModelWrapper?
    private(set) var _professionals: ProfessionalListViewModelWrapper?
    private(set) var _professionalDetail: ProfessionalDetailViewModelWrapper?
    private(set) var _loginViewModel: LoginViewModel?

    // Estado de erro publicado — substituiu fatalError()
    @Published var initError: String? = nil

    // Acesso seguro com fallback mínimo (sem crash)
    var auth: AuthViewModelWrapper { _auth ?? AuthViewModelWrapper.empty() }
    var emergency: EmergencyViewModelWrapper { _emergency ?? EmergencyViewModelWrapper.empty() }
    var history: HistoryViewModelWrapper { _history ?? HistoryViewModelWrapper.empty() }
    var profile: ProfileViewModelWrapper { _profile ?? ProfileViewModelWrapper.empty() }
    var professionals: ProfessionalListViewModelWrapper { _professionals ?? ProfessionalListViewModelWrapper.empty() }
    var professionalDetail: ProfessionalDetailViewModelWrapper { _professionalDetail ?? ProfessionalDetailViewModelWrapper.empty() }
    var loginViewModel: LoginViewModel? { _loginViewModel }

    /// Pré-aquece todos os ViewModels logo após o Koin inicializar.
    /// Chamado no background thread — falhas são capturadas sem crash.
    /// Retorna `false` + popula `initError` se algum VM crítico falhar.
    @discardableResult
    func warmUp() -> Bool {
        var failed = [String]()

        if let vm = ViewModelProvider.shared.getAuthViewModel() {
            _auth = AuthViewModelWrapper(vm)
        } else { failed.append("AuthViewModel") }

        if let vm = ViewModelProvider.shared.getEmergencyViewModel() {
            _emergency = EmergencyViewModelWrapper(vm)
        } else { failed.append("EmergencyViewModel") }

        if let vm = ViewModelProvider.shared.getHistoryViewModel() {
            _history = HistoryViewModelWrapper(vm)
        } else { failed.append("HistoryViewModel") }

        if let vm = ViewModelProvider.shared.getProfileViewModel() {
            _profile = ProfileViewModelWrapper(vm)
        } else { failed.append("ProfileViewModel") }

        if let vm = ViewModelProvider.shared.getProfessionalListViewModel() {
            _professionals = ProfessionalListViewModelWrapper(vm)
        } else { failed.append("ProfessionalListViewModel") }

        if let vm = ViewModelProvider.shared.getProfessionalDetailViewModel() {
            _professionalDetail = ProfessionalDetailViewModelWrapper(vm)
        } else { failed.append("ProfessionalDetailViewModel") }

        _loginViewModel = ViewModelProvider.shared.getLoginViewModel()

        if !failed.isEmpty {
            let msg = "Falha ao inicializar: \(failed.joined(separator: ", "))"
            FileLogger.shared.write(level: "ERROR", tag: "AppContainer", message: msg)
            DispatchQueue.main.async { self.initError = msg }
            return false
        }

        FileLogger.shared.write(level: "INFO", tag: "AppContainer", message: "warmUp: todos os ViewModels prontos")
        return true
    }

    private var cancellables = Set<AnyCancellable>()
    @Published var pendingEmergencyId: String? = nil
    @Published var pendingChatId: String? = nil
    @Published var resolvedEmergencyId: String? = nil
    @Published var pendingIncomingEmergencies: [(id: String, name: String)] = []
    private var emergencyListener: ListenerRegistration?
    private var notifiedEmergencyIds = Set<String>()

    func freezeAll() {
        // 1. Cancela todos os sinks Combine do AppContainer (objectWillChange e notificações).
        //    Isso para qualquer update assíncrono que ainda possa estar em voo antes de limpar
        //    os ViewModels KMM. Sem isso, um evento Firestore que chega após o freeze tenta
        //    publicar num @Published ligado a um KMM object já destruído → SIGABRT.
        cancellables = []
        // 2. Para o listener Firestore de emergências próximas
        emergencyListener?.remove()
        emergencyListener = nil
        notifiedEmergencyIds = []
        pendingIncomingEmergencies = []
        // 3. Agora é seguro encerrar cada ViewModel (seus Combine cancellables já foram
        //    cancelados em passo 1; os freezes abaixo também têm seus próprios cancels)
        _emergency?.freezeSwift()
        _auth?.signOutSwift()
        _history?.freeze()
        _profile?.freeze()
        _professionals?.freeze()
        _professionalDetail?.freeze()
    }

    func observeChildren() {
        _emergency?.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }.store(in: &cancellables)
        _auth?.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }.store(in: &cancellables)

        NotificationCenter.default.publisher(for: .init("AfilaxyIncomingEmergency"))
            .sink { [weak self] notification in
                guard let self = self,
                      let emergencyId = notification.userInfo?["emergencyId"] as? String,
                      !self.notifiedEmergencyIds.contains(emergencyId) else { return }
                // Evita auto-match: ignora se o usuário é o requester desta emergência
                // (pode acontecer quando o iOS estava na coleção helpers e criou a própria emergência)
                let ownEmergencyId = self.emergency.state?.emergencyId as? String
                if (ownEmergencyId != nil && emergencyId == ownEmergencyId)
                    || self.emergency.state?.isRequester == true {
                    FileLogger.shared.write(level: "WARN", tag: "AppContainer",
                        message: "FCM: ignorando própria emergência emergencyId=\(emergencyId)")
                    return
                }
                self.notifiedEmergencyIds.insert(emergencyId)
                let name = notification.userInfo?["requesterName"] as? String ?? "Alguém"
                FileLogger.shared.write(level: "INFO", tag: "AppContainer", message: "incoming emergency via FCM from \(name)")
                self.pendingIncomingEmergencies.append((id: emergencyId, name: name))
                self.sendLocalNotification(title: "🆘 Nova Emergência", body: "\(name) precisa de ajuda!", emergencyId: emergencyId)
            }
            .store(in: &cancellables)
    }

    func startObservingNearbyEmergencies(lat: Double, lon: Double, radiusKm: Double = 5.0) {
        emergencyListener?.remove()
        let deltaLat = radiusKm / 111.0
        let startTime = Date()
        emergencyListener = Firestore.firestore()
            .collection("emergency_requests")
            .whereField("active", isEqualTo: true)
            .whereField("latitude", isGreaterThanOrEqualTo: lat - deltaLat)
            .whereField("latitude", isLessThanOrEqualTo: lat + deltaLat)
            .addSnapshotListener { [weak self] snapshot, error in
                guard let self = self, let snapshot = snapshot else { return }
                // guard let uid: se Firebase Auth ainda não restaurou a sessão (uid==nil),
                // não processa eventos — evita auto-match onde requesterId!=nil≡true passaria.
                guard let uid = Auth.auth().currentUser?.uid else { return }
                let ownEmergencyId = self.emergency.state?.emergencyId as? String
                snapshot.documentChanges
                    .filter { $0.type == .added }
                    .forEach { change in
                        let data = change.document.data()
                        let docId = change.document.documentID
                        guard let requesterId = data["requesterId"] as? String,
                              requesterId != uid,       // não é emergência criada pelo próprio usuário
                              docId != ownEmergencyId, // camada extra: ID não confere com ViewModel
                              !self.notifiedEmergencyIds.contains(docId),
                              (data["status"] as? String) == "waiting" else { return }
                        let docDate: Date
                        if let ts = data["timestamp"] as? Timestamp {
                            docDate = ts.dateValue()
                        } else if let ms = (data["timestamp"] as? Int64)
                                    ?? (data["timestamp"] as? Int).map(Int64.init)
                                    ?? (data["timestamp"] as? Double).map(Int64.init) {
                            guard ms > 0 else { return }
                            docDate = Date(timeIntervalSince1970: Double(ms) / 1000)
                        } else { return }
                        if docDate < startTime { return }
                        self.notifiedEmergencyIds.insert(docId)
                        let name = data["requesterName"] as? String ?? "Alguém"
                        FileLogger.shared.write(level: "INFO", tag: "AppContainer", message: "incoming emergency from \(name)")
                        self.pendingIncomingEmergencies.append((id: docId, name: name))
                        self.sendLocalNotification(title: "🆘 Nova Emergência", body: "\(name) precisa de ajuda!", emergencyId: docId)
                    }
            }
    }

    func stopObservingNearbyEmergencies() {
        emergencyListener?.remove()
        emergencyListener = nil
    }

    func navigateToChat(emergencyId: String) { pendingChatId = emergencyId }

    func dismissIncomingEmergency(id: String) {
        pendingIncomingEmergencies.removeAll { $0.id == id }
    }

    private func sendLocalNotification(title: String, body: String, emergencyId: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        // type é necessário: sem ele, didReceive notificationResponse recebe type=unknown
        content.userInfo = ["emergencyId": emergencyId, "type": "emergency_request"]
        let request = UNNotificationRequest(identifier: emergencyId, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}

// MARK: - App Entry Point

@main
struct AfilaxyApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var container = AppContainer()
    @State private var isKoinInitialized = false

    init() {
        // Initialize Firebase first
        FirebaseApp.configure()
        
        // Initialize Google Maps SDK
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let plist = NSDictionary(contentsOfFile: path),
           let apiKey = plist["API_KEY"] as? String {
            GMSServices.provideAPIKey(apiKey)
        } else {
            // Fallback: usar API key diretamente do Info.plist
            if let apiKey = Bundle.main.object(forInfoDictionaryKey: "GMSApiKey") as? String {
                GMSServices.provideAPIKey(apiKey)
            }
        }
        
        // Initialize other components that don't depend on Koin
        LocationManagerBridge.shared.start()
        Logger.shared.fileLogHook = { level, tag, message in
            FileLogger.shared.write(level: level, tag: tag, message: message)
        }
        FileLogger.shared.write(level: "INFO", tag: "AfilaxyApp", message: "App iniciado - Koin será inicializado após UI")
    }

    var body: some Scene {
        WindowGroup {
            if let errorMsg = container.initError {
                // Tela de erro recuperável — sem crash
                VStack(spacing: 24) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 56))
                        .foregroundColor(.orange)
                    Text("Erro ao inicializar")
                        .font(.title2.bold())
                    Text(errorMsg)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    Button("Tentar Novamente") {
                        container.initError = nil
                        isKoinInitialized = false
                        initializeKoin()
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
            } else if isKoinInitialized {
                ContentView()
                    .environmentObject(container)
            } else {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.5)
                    Text("Inicializando...")
                        .foregroundColor(.secondary)
                }
                .onAppear {
                    initializeKoin()
                }
            }
        }
        .onChange(of: scenePhase) { phase in
            guard isKoinInitialized else { return }
            
            if phase == .background {
                // Não remove o listener em background — FCM acorda o app via didReceiveRemoteNotification
                FileLogger.shared.write(level: "INFO", tag: "AfilaxyApp", message: "scenePhase=background")
            } else if phase == .active {
                let pendingCount = container.pendingIncomingEmergencies.count
                FileLogger.shared.write(level: "INFO", tag: "AfilaxyApp", message: "scenePhase=active pendingEmergencies=\(pendingCount)")
                // Garante que o FCM token está salvo para o uid atual ao voltar ao foreground
                if let token = Messaging.messaging().fcmToken,
                   let uid = Auth.auth().currentUser?.uid {
                    Firestore.firestore().collection("users").document(uid)
                        .setData(["fcmToken": token], merge: true)
                }
                let isHelper = container.emergency.state?.isHelperMode == true
                if isHelper {
                    container.stopObservingNearbyEmergencies()
                    if let loc = LocationManager.shared.currentLocation {
                        container.startObservingNearbyEmergencies(lat: loc.coordinate.latitude,
                                                              lon: loc.coordinate.longitude)
                        FileLogger.shared.write(level: "INFO", tag: "AfilaxyApp", message: "scenePhase=active — listener reiniciado lat=\(loc.coordinate.latitude)")
                    } else {
                        FileLogger.shared.write(level: "WARN", tag: "AfilaxyApp", message: "scenePhase=active — listener não reiniciado: sem localização")
                    }
                }
            }
        }
    }
    
    private func initializeKoin() {
        // CRÍTICO: TODO código Kotlin/Native deve rodar na main thread no iOS 26.
        // O dev.gitlive:firebase-auth acessa o Firebase iOS SDK durante a resolução
        // do Koin, e o Firebase iOS SDK exige main thread em iOS 26.
        // DispatchQueue.main.async é não-bloqueante: o SwiftUI continua renderizando
        // o ProgressView entre os ciclos do run loop — não há congelamento visual.
        assert(Thread.isMainThread, "initializeKoin deve ser chamado da main thread")

        // Passo 1: registra módulos Koin — @Throws no Kotlin → Swift try/catch captura
        // qualquer falha (startKoin, módulos) sem abort().
        do {
            try KoinHelperKt.doInitKoin()
            FileLogger.shared.write(level: "INFO", tag: "AfilaxyApp", message: "Koin modules registrados (main thread)")
        } catch {
            let msg = "doInitKoin falhou: \(error.localizedDescription)"
            FileLogger.shared.write(level: "ERROR", tag: "AfilaxyApp", message: msg)
            container.initError = msg
            return
        }

        // Passo 2: pré-aquece ViewModels — também na main thread (Firebase exige isso)
        DispatchQueue.main.async {
            let success = self.container.warmUp()
            if success {
                self.container.observeChildren()
                self.isKoinInitialized = true
                FileLogger.shared.write(level: "INFO", tag: "AfilaxyApp", message: "App totalmente inicializado")
            } else {
                FileLogger.shared.write(level: "ERROR", tag: "AfilaxyApp", message: "warmUp falhou — tela de erro exibida")
            }
        }
    }
}

