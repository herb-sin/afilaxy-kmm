import UserNotifications
import shared

/// Equivalente iOS do Android CheckInWorkers.kt.
/// Agenda notificações locais diárias para check-in matinal (07:30) e noturno (21:00)
/// usando UNCalendarNotificationTrigger com repeats=true.
///
/// Diferença vs. Android WorkManager:
/// - Android: OneTimeWorkRequest reagendável, executa código (filtra por riskScore ≥ 45)
/// - iOS: UNNotification puramente declarativa, sempre exibida (filtragem via Service Extension futura)
class CheckInNotificationScheduler {
    static let shared = CheckInNotificationScheduler()

    private let morningId = "checkin_morning"
    private let eveningId = "checkin_evening"
    private let categoryMorning = "CHECKIN_MORNING"
    private let categoryEvening = "CHECKIN_EVENING"
    private let actReminderId = "act_reminder"

    private init() {}

    // MARK: - Public API

    /// Agenda ambas as notificações diárias e registra as categorias de ação.
    /// Seguro para chamar múltiplas vezes — remove pendentes antes de reagendar.
    func scheduleDaily() {
        registerCategories()
        scheduleMorning()
        scheduleEvening()
        FileLogger.shared.write(level: "INFO", tag: "CheckInScheduler", message: "Notificações de check-in agendadas (07:30 + 21:00)")
    }

    /// Lembrete do ACT a cada 28 dias — janela retrospectiva do instrumento é de 4
    /// semanas, por isso não é diário como os check-ins. Seguro chamar múltiplas vezes.
    func scheduleActReminder() {
        let content = UNMutableNotificationContent()
        content.title = "📋 Teste de Controle da Asma (ACT)"
        content.body = "Faz um mês — hora de reavaliar como sua asma esteve nas últimas 4 semanas."
        content.sound = .default
        content.userInfo = ["type": "act_reminder"]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 28 * 24 * 60 * 60, repeats: true)
        let request = UNNotificationRequest(identifier: actReminderId, content: content, trigger: trigger)

        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [actReminderId])
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                FileLogger.shared.write(level: "ERROR", tag: "CheckInScheduler",
                    message: "Erro ao agendar lembrete do ACT: \(error.localizedDescription)")
            }
        }
    }

    /// Remove todas as notificações de check-in pendentes (usado no logout).
    func cancelAll() {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [morningId, eveningId, actReminderId])
        FileLogger.shared.write(level: "INFO", tag: "CheckInScheduler", message: "Notificações de check-in canceladas")
    }

    // MARK: - Private

    private func registerCategories() {
        let yesAction = UNNotificationAction(
            identifier: "CHECKIN_YES",
            title: "✅ Sim",
            options: [.foreground]
        )
        let noAction = UNNotificationAction(
            identifier: "CHECKIN_NO",
            title: "❌ Não",
            options: [.foreground]
        )

        let morningCategory = UNNotificationCategory(
            identifier: categoryMorning,
            actions: [yesAction, noAction],
            intentIdentifiers: [],
            options: []
        )

        let yesNoCrisis = UNNotificationAction(
            identifier: "CHECKIN_NO_CRISIS",
            title: "✅ Bem",
            options: [.foreground]
        )
        let yesCrisis = UNNotificationAction(
            identifier: "CHECKIN_HAD_CRISIS",
            title: "⚠️ Dia difícil",
            options: [.foreground]
        )

        let eveningCategory = UNNotificationCategory(
            identifier: categoryEvening,
            actions: [yesNoCrisis, yesCrisis],
            intentIdentifiers: [],
            options: []
        )

        UNUserNotificationCenter.current()
            .setNotificationCategories([morningCategory, eveningCategory])
    }

    private func scheduleMorning() {
        let content = UNMutableNotificationContent()
        content.title = "☀️ Check-in Matinal"
        content.body = "Como está seu bem-estar esta manhã?"
        content.sound = .default
        content.categoryIdentifier = categoryMorning
        content.userInfo = ["type": "checkin", "checkInType": "MORNING"]

        var dateComponents = DateComponents()
        dateComponents.hour = 7
        dateComponents.minute = 30

        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(identifier: morningId, content: content, trigger: trigger)

        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [morningId])
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                FileLogger.shared.write(level: "ERROR", tag: "CheckInScheduler",
                    message: "Erro ao agendar morning: \(error.localizedDescription)")
            }
        }
    }

    private func scheduleEvening() {
        let content = UNMutableNotificationContent()
        content.title = "🌙 Check-in Noturno"
        content.body = "Como foi seu dia hoje?"
        content.sound = .default
        content.categoryIdentifier = categoryEvening
        content.userInfo = ["type": "checkin", "checkInType": "EVENING"]

        var dateComponents = DateComponents()
        dateComponents.hour = 21
        dateComponents.minute = 0

        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(identifier: eveningId, content: content, trigger: trigger)

        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [eveningId])
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                FileLogger.shared.write(level: "ERROR", tag: "CheckInScheduler",
                    message: "Erro ao agendar evening: \(error.localizedDescription)")
            }
        }
    }
}
