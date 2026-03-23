import SwiftUI
import FirebaseFirestore
import FirebaseAuth
import shared

struct ChatView: View {
    let emergencyId: String
    @EnvironmentObject var container: AppContainer
    @State private var messages: [(id: String, senderId: String, senderName: String, text: String, timestamp: Date)] = []
    @State private var messageText = ""
    @State private var listener: ListenerRegistration? = nil
    @State private var showResolveDialog = false
    @Environment(\.dismiss) private var dismiss

    private var currentUserId: String? { Auth.auth().currentUser?.uid }

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(messages, id: \.id) { msg in
                            MessageBubble(
                                text: msg.text,
                                senderName: msg.senderName,
                                isCurrentUser: msg.senderId == currentUserId
                            )
                            .id(msg.id)
                        }
                    }
                    .padding()
                }
                .onChange(of: messages.count) { _ in
                    if let last = messages.last { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }

            Divider()

            HStack(spacing: 8) {
                TextField("Digite uma mensagem...", text: $messageText)
                    .textFieldStyle(.roundedBorder)
                Button {
                    sendMessage()
                } label: {
                    Image(systemName: "paperplane.fill")
                }
                .disabled(messageText.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .padding(8)
        }
        .navigationTitle("Chat de Emergência")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button { showResolveDialog = true } label: {
                    Image(systemName: "chevron.left")
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Resolver") { showResolveDialog = true }
            }
        }
        .alert("Encerrar Emergência", isPresented: $showResolveDialog) {
            Button("Resolver", role: .destructive) { resolveEmergency() }
            Button("Continuar no Chat", role: .cancel) {}
        } message: { Text("Você precisa resolver a emergência antes de sair.") }
        .onAppear { startListening() }
        .onDisappear {
            listener?.remove()
            listener = nil
        }
    }

    private func resolveEmergency() {
        Firestore.firestore()
            .collection("emergency_requests")
            .document(emergencyId)
            .updateData(["status": "resolved", "active": false])
        container.emergency.clearEmergencyStateSwift()
        // dismiss() não necessário — ContentView faz pop ao detectar hasActiveEmergency=false
    }

    private func startListening() {
        listener = Firestore.firestore()
            .collection("emergency_chats")
            .document(emergencyId)
            .collection("messages")
            .order(by: "timestamp", descending: false)
            .addSnapshotListener { snapshot, _ in
                guard let docs = snapshot?.documents else { return }
                DispatchQueue.main.async {
                    messages = docs.compactMap { doc in
                        let d = doc.data()
                        guard let senderId = d["senderId"] as? String,
                              let senderName = d["senderName"] as? String,
                              let text = d["message"] as? String else { return nil }
                        let ts: Date
                        if let ms = d["timestamp"] as? Int64 { ts = Date(timeIntervalSince1970: Double(ms) / 1000) }
                        else if let ms = d["timestamp"] as? Double { ts = Date(timeIntervalSince1970: ms / 1000) }
                        else if let t = d["timestamp"] as? Timestamp { ts = t.dateValue() }
                        else { ts = Date() }
                        return (id: doc.documentID, senderId: senderId, senderName: senderName, text: text, timestamp: ts)
                    }
                }
            }
    }

    private func sendMessage() {
        let text = messageText.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty, let uid = currentUserId else { return }
        let name = Auth.auth().currentUser?.displayName ?? Auth.auth().currentUser?.email ?? "Usuário"
        messageText = ""
        let msgId = UUID().uuidString
        let timestampMs = Int64(Date().timeIntervalSince1970 * 1000)
        Firestore.firestore()
            .collection("emergency_chats")
            .document(emergencyId)
            .collection("messages")
            .document(msgId)
            .setData([
                "id": msgId,
                "emergencyId": emergencyId,
                "senderId": uid,
                "senderName": name,
                "message": text,
                "timestamp": timestampMs,
                "isFromHelper": false
            ])
    }
}

private struct MessageBubble: View {
    let text: String
    let senderName: String
    let isCurrentUser: Bool

    var body: some View {
        HStack {
            if isCurrentUser { Spacer() }
            VStack(alignment: isCurrentUser ? .trailing : .leading, spacing: 2) {
                Text(senderName)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                Text(text)
                    .padding(10)
                    .background(isCurrentUser ? Color.blue.opacity(0.2) : Color.gray.opacity(0.2))
                    .cornerRadius(12)
            }
            if !isCurrentUser { Spacer() }
        }
    }
}
