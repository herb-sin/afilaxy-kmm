import Foundation
import FirebaseAnalytics

/// Espelha com.afilaxy.app.analytics.AnalyticsManager (Android) — mesma chave de
/// consentimento ("analytics_consent", lida via UserDefaults em ambas as plataformas)
/// e mesma política de opt-in: nada é enviado até o usuário consentir na ConsentView.
enum AnalyticsManager {
    private static let consentKey = "analytics_consent"

    private static var hasConsent: Bool {
        UserDefaults.standard.object(forKey: consentKey) != nil
            ? UserDefaults.standard.bool(forKey: consentKey)
            : true // valor padrão do @AppStorage em ConsentView é true
    }

    /// Aplica o consentimento salvo à coleta do SDK. Chamar no início do app
    /// (AfilaxyApp.swift) e sempre que o usuário alterar a preferência.
    static func applyConsent() {
        Analytics.setAnalyticsCollectionEnabled(hasConsent)
    }

    static func logEvent(_ name: String, params: [String: Any]? = nil) {
        guard hasConsent else { return }
        Analytics.logEvent(name, parameters: params)
    }

    static func logScreenView(_ screenName: String) {
        logEvent(AnalyticsEventScreenView, params: [AnalyticsParameterScreenName: screenName])
    }
}
