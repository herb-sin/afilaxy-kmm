import SwiftUI
import shared
import FirebaseCore
import FirebaseAuth
import FirebaseMessaging
import FirebaseFirestore
import UserNotifications
import Combine

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
    lazy var loginViewModel: LoginViewModel = ViewModelProvider.shared.getLoginViewModel()

    private var cancellables = Set<AnyCancellable>()
    private var emergencyListener: ListenerRegistration?
    private var notifiedEmergencyIds = Set<String>()

    func observeChildren() {
        emergency.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }.store(in: &cancellables)
        auth.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }.store(in: &cancellables)
    }

    /// Inicia listener Firestore nativo para emergências próximas (iOS-safe, sem KMM Flow)
    func startObservingNearbyEmergencies(lat: Double, lon: Double, radiusKm: Double = 5.0) {
        emergencyListener?.remove()
        notifiedEmergencyIds.removeAll()
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
                              !self.notifiedEmergencyIds.contains(docId) else { return }
                        // Ignorar documentos que já existiam antes do listener iniciar
                        let tsMillis = (data["timestamp"] as? Int64)
                            ?? (data["timestamp"] as? Int).map { Int64($0) }
                            ?? (data["timestamp"] as? Double).map { Int64($0) }
                            ?? 0
                        let docDate = Date(timeIntervalSince1970: Double(tsMillis) / 1000)
                        if tsMillis > 0 && docDate < startTime { return }
                        self.notifiedEmergencyIds.insert(docId)
                        let name = data["requesterName"] as? String ?? "Alguém"
                        FileLogger.shared.write(level: "INFO", tag: "AppContainer", message: "incoming emergency from \(name)")
                        self.sendLocalNotification(title: "🆘 Nova Emergência", body: "\(name) precisa de ajuda!")
                    }
            }
    }

    func stopObservingNearbyEmergencies() {
        emergencyListener?.remove()
        emergencyListener = nil
    }

    private func sendLocalNotification(title: String, body: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
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
    let container = AppContainer()

    init() {
        FirebaseApp.configure()
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
    }
}

