import SwiftUI
import shared
import FirebaseCore
import FirebaseAuth
import FirebaseMessaging
import FirebaseFirestore
import UserNotifications

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
        print("APNs registration failed: \(error.localizedDescription)")
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
                    print("Erro ao salvar FCM token: \(error.localizedDescription)")
                }
            }
    }
}

// MARK: - AppContainer

class AppContainer: ObservableObject {
    // ViewModels são criados com proteção contra crash de Koin
    lazy var auth      = AuthViewModelWrapper(ViewModelProvider.shared.getAuthViewModel())
    lazy var emergency = EmergencyViewModelWrapper(ViewModelProvider.shared.getEmergencyViewModel())
    lazy var history   = HistoryViewModelWrapper(ViewModelProvider.shared.getHistoryViewModel())
    lazy var profile   = ProfileViewModelWrapper(ViewModelProvider.shared.getProfileViewModel())
    lazy var professionals = ProfessionalListViewModelWrapper(ViewModelProvider.shared.getProfessionalListViewModel())
    lazy var loginViewModel: LoginViewModel = ViewModelProvider.shared.getLoginViewModel()
}

// MARK: - App Entry Point

@main
struct AfilaxyApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    let container = AppContainer()

    init() {
        FirebaseApp.configure()
        KoinHelperKt.doInitKoin()
        // Iniciar rastreamento de localização
        LocationManagerBridge.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(container)
        }
    }
}

