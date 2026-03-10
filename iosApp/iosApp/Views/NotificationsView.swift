import SwiftUI

struct NotificationsView: View {
    private let notifications: [NotificationItem] = [
        NotificationItem(id: "1", title: "Emergência Aceita", message: "Um helper aceitou sua emergência", timestamp: "Há 2 horas", isRead: true),
        NotificationItem(id: "2", title: "Nova Emergência", message: "Emergência próxima a você", timestamp: "Há 5 horas", isRead: false),
        NotificationItem(id: "3", title: "Emergência Resolvida", message: "Sua emergência foi resolvida", timestamp: "Ontem", isRead: true)
    ]

    var body: some View {
        Group {
            if notifications.isEmpty {
                ContentUnavailableView("Nenhuma notificação", systemImage: "bell.slash")
            } else {
                List(notifications, id: \.id) { item in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(item.title)
                                .font(.headline)
                                .fontWeight(item.isRead ? .regular : .bold)
                            Spacer()
                            Text(item.timestamp)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        Text(item.message)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }
                    .padding(.vertical, 4)
                    .listRowBackground(item.isRead ? Color(.systemBackground) : Color.blue.opacity(0.05))
                }
            }
        }
        .navigationTitle("Notificações")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct NotificationItem {
    let id: String
    let title: String
    let message: String
    let timestamp: String
    let isRead: Bool
}
