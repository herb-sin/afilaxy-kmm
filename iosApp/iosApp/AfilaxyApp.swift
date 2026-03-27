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

    // FCM data-only em background — acorda o app e dispara notificação local
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        let type = userInfo["type"] as? String ?? "unknown"
        FileLogger.shared.write(level: "INFO", tag: "AppDelegate", message: "didReceiveRemoteNotification type=\(type) appState=\(application.applicationState.rawValue)")
        guard let emergencyId = userInfo["emergencyId"] as? String,
              type == "emergency_request" else {
            completionHandler(.noData)
            return
        }
        let name = userInfo["requesterName"] as? String ?? "Alguém"
        NotificationCenter.default.post(
            name: .init("AfilaxyIncomingEmergency"),
            object: nil,
            userInfo: ["emergencyId": emergencyId, "requesterName": name]
        )
        completionHandler(.newData)
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
        let emergencyId = userInfo["emergencyId"] as? String ?? "nil"
        FileLogger.shared.write(level: "INFO", tag: "AppDelegate", message: "didReceive notificationResponse emergencyId=\(emergencyId) action=\(response.actionIdentifier)")
        if let eid = userInfo["emergencyId"] as? String {
            NotificationCenter.default.post(
                name: .init("AfilaxyOpenEmergency"),
                object: nil,
                userInfo: ["emergencyId": eid]
            )
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
    lazy var auth      = AuthViewModelWrapper(ViewModelProvider.shared.getAuthViewModel())
    lazy var emergency = EmergencyViewModelWrapper(ViewModelProvider.shared.getEmergencyViewModel())
    lazy var history   = HistoryViewModelWrapper(ViewModelProvider.shared.getHistoryViewModel())
    lazy var profile   = ProfileViewModelWrapper(ViewModelProvider.shared.getProfileViewModel())
    lazy var professionals = ProfessionalListViewModelWrapper(ViewModelProvider.shared.getProfessionalListViewModel())
    lazy var professionalDetail = ProfessionalDetailViewModelWrapper(ViewModelProvider.shared.getProfessionalDetailViewModel())
    lazy var loginViewModel: LoginViewModel = ViewModelProvider.shared.getLoginViewModel()

    private var cancellables = Set<AnyCancellable>()
    @Published var pendingEmergencyId: String? = nil
    @Published var pendingChatId: String? = nil
    @Published var resolvedEmergencyId: String? = nil
    @Published var pendingIncomingEmergencies: [(id: String, name: String)] = []
    private var emergencyListener: ListenerRegistration?
    private var notifiedEmergencyIds = Set<String>()

    func freezeAll() {
        emergency.freezeSwift()
        auth.signOutSwift()
        history.freeze()
        profile.freeze()
        professionals.freeze()
        professionalDetail.freeze()
    }

    func observeChildren() {
        emergency.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }.store(in: &cancellables)
        auth.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }.store(in: &cancellables)

        // Emergências recebidas via FCM em background (didReceiveRemoteNotification)
        // Sem receive(on:) — post já ocorre na main thread, enfileirar criaria janela pós-freezeAll
        NotificationCenter.default.publisher(for: .init("AfilaxyIncomingEmergency"))
            .sink { [weak self] notification in
                guard let self,
                      let emergencyId = notification.userInfo?["emergencyId"] as? String,
                      !self.notifiedEmergencyIds.contains(emergencyId) else { return }
                self.notifiedEmergencyIds.insert(emergencyId)
                let name = notification.userInfo?["requesterName"] as? String ?? "Alguém"
                FileLogger.shared.write(level: "INFO", tag: "AppContainer", message: "incoming emergency via FCM from \(name)")
                self.pendingIncomingEmergencies.append((id: emergencyId, name: name))
                self.sendLocalNotification(title: "🆘 Nova Emergência", body: "\(name) precisa de ajuda!", emergencyId: emergencyId)
            }
            .store(in: &cancellables)
    }

    /// Inicia listener Firestore nativo para emergências próximas (iOS-safe, sem KMM Flow)
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
                guard let self, let snapshot else { return }
                let uid = Auth.auth().currentUser?.uid
                snapshot.documentChanges
                    .filter { $0.type == .added }
                    .forEach { change in
                        let data = change.document.data()
                        let docId = change.document.documentID
                        guard let requesterId = data["requesterId"] as? String,
                              requesterId != uid,
                              !self.notifiedEmergencyIds.contains(docId),
                              (data["status"] as? String) == "waiting" else { return }
                        // Ignorar documentos sem timestamp ou anteriores ao início do listener
                        let docDate: Date
                        if let ts = data["timestamp"] as? Timestamp {
                            // Firestore nativo Timestamp (criado por Android SDK)
                            docDate = ts.dateValue()
                        } else if let ms = (data["timestamp"] as? Int64)
                                    ?? (data["timestamp"] as? Int).map(Int64.init)
                                    ?? (data["timestamp"] as? Double).map(Int64.init) {
                            guard ms > 0 else { return }
                            docDate = Date(timeIntervalSince1970: Double(ms) / 1000)
                        } else {
                            return  // sem timestamp válido → descarta
                        }
                        if docDate < startTime { return }  // doc antigo (pré-listener) → ignora
                        self.notifiedEmergencyIds.insert(docId)
                        let name = data["requesterName"] as? String ?? "Alguém"
                        FileLogger.shared.write(level: "INFO", tag: "AppContainer", message: "incoming emergency from \(name)")
                        // Atualiza diretamente — callback do Firestore já executa na main thread
                        self.pendingIncomingEmergencies.append((id: docId, name: name))
                        self.sendLocalNotification(title: "🆘 Nova Emergência", body: "\(name) precisa de ajuda!", emergencyId: docId)
                    }
            }
    }

    func stopObservingNearbyEmergencies() {
        emergencyListener?.remove()
        emergencyListener = nil
    }

    func navigateToChat(emergencyId: String) {
        pendingChatId = emergencyId
    }

    func dismissIncomingEmergency(id: String) {
        pendingIncomingEmergencies.removeAll { $0.id == id }
    }

    private func sendLocalNotification(title: String, body: String, emergencyId: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = ["emergencyId": emergencyId]
        let request = UNNotificationRequest(
            identifier: emergencyId,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }
}

// MARK: - App Entry Point

@main
struct AfilaxyApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) private var scenePhase
    let container = AppContainer()

    init() {
        FirebaseApp.configure()
        
        // Inicializar Google Maps SDK
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
        
        KoinHelperKt.doInitKoin()
        LocationManagerBridge.shared.start()
        container.observeChildren()
        Logger.shared.fileLogHook = { level, tag, message in
            FileLogger.shared.write(level: level, tag: tag, message: message)
        }
        FileLogger.shared.write(level: "INFO", tag: "AfilaxyApp", message: "App iniciado")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(container)
        }
        .onChange(of: scenePhase) { phase in
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
}

