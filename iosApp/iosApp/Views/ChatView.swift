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
    @State private var statusListener: ListenerRegistration? = nil
    @State private var showResolveDialog = false
    @State private var isResolved = false
    @State private var resolvedByOther = false
    @State private var didResolveLocally = false
    @State private var samuCalled = false
    @State private var showRatingSheet = false
    @State private var reviewedId: String? = nil
    @Environment(\.dismiss) private var dismiss

    private var currentUserId: String? { Auth.auth().currentUser?.uid }
    private var resolvedBannerText: String {
        resolvedByOther ? "✅ Emergência encerrada pela outra parte" : "✅ Emergência encerrada"
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(messages, id: \.id) { msg in
                            if msg.senderId == "system" {
                                // Mensagens de sistema: centralizadas, sem bubble
                                Text(msg.text)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 4)
                                    .background(Color(.systemGray6))
                                    .cornerRadius(8)
                                    .frame(maxWidth: .infinity)
                                    .padding(.horizontal)
                                    .id(msg.id)
                            } else {
                                MessageBubble(
                                    text: msg.text,
                                    senderName: msg.senderName,
                                    isCurrentUser: msg.senderId == currentUserId
                                )
                                .id(msg.id)
                            }
                        }
                    }
                    .padding()
                }
                .onChange(of: messages.count) { _ in
                    if let last = messages.last { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }

            // Banner de emergência encerrada
            if isResolved {
                HStack {
                    Text(resolvedBannerText)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    // Quando a outra parte encerrou: botão direto sem "Continuar no Chat"
                    if resolvedByOther {
                        Button("Encerrar") { dismissAndClearState() }
                            .font(.subheadline.bold())
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity)
                .background(Color(.systemGray6))
            }

            Divider()

            HStack(spacing: 8) {
                TextField(isResolved ? "Emergência encerrada" : "Digite uma mensagem...", text: $messageText)
                    .textFieldStyle(.roundedBorder)
                    .disabled(isResolved)
                Button {
                    sendMessage()
                } label: {
                    Image(systemName: "paperplane.fill")
                }
                .disabled(messageText.trimmingCharacters(in: .whitespaces).isEmpty || isResolved)
            }
            .padding(8)
        }
        .navigationTitle("Chat de Emergência")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    if resolvedByOther { dismissAndClearState() }
                    else { showResolveDialog = true }
                } label: {
                    Image(systemName: "chevron.left")
                }
            }
            // Botão SAMU — visível enquanto emergência ativa
            if !isResolved {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        markSamuCalled()
                    } label: {
                        Text(samuCalled ? "🚑 acionado" : "🚑 SAMU")
                            .font(.subheadline)
                            .foregroundColor(samuCalled ? .secondary : .red)
                    }
                    .disabled(samuCalled)
                }
            }
            if !resolvedByOther {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Resolver") { showResolveDialog = true }
                }
            }
        }
        .alert("Encerrar Emergência", isPresented: $showResolveDialog) {
            Button("Resolver", role: .destructive) { resolveEmergency() }
            Button("Continuar no Chat", role: .cancel) {}
        } message: { Text("A outra parte será avisada que a emergência foi encerrada.") }
        .onAppear {
            startListening()
            fetchParticipants()
        }        .onDisappear {
            listener?.remove()
            listener = nil
            statusListener?.remove()
            statusListener = nil
        }
        .sheet(isPresented: $showRatingSheet) {
            RatingSheetView(
                onSubmit: { rating, comment in
                    submitReview(reviewedId: reviewedId, rating: rating, comment: comment)
                    showRatingSheet = false
                },
                onSkip: { showRatingSheet = false }
            )
        }
    }

    private func resolveEmergency() {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        let displayName = Auth.auth().currentUser?.displayName ?? "Usuário"
        let db = Firestore.firestore()
        // Marca como resolvido localmente ANTES de escrever no Firestore —
        // impede que o statusListener reaja ao próprio echo de status=resolved.
        didResolveLocally = true
        // Para o statusListener imediatamente para evitar race-condition durante a animação de pop.
        statusListener?.remove()
        statusListener = nil
        // 1. Escreve mensagem de sistema visível para os dois lados
        // senderId deve ser o uid real (regra do Firestore exige auth.uid == senderId)
        let msgId = UUID().uuidString
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        db.collection("emergency_chats")
            .document(emergencyId)
            .collection("messages")
            .document(msgId)
            .setData([
                "id": msgId,
                "emergencyId": emergencyId,
                "senderId": "system",   // mantido para renderização especial na UI
                "senderName": "Sistema",
                "message": "✅ Emergência encerrada. O chat foi bloqueado.",
                "timestamp": nowMs,
                "isFromHelper": false
            ]) { [self] error in
                // Se a regra bloquear (senderId=system), envia com uid real para garantir entrega
                if error != nil {
                    db.collection("emergency_chats")
                        .document(emergencyId)
                        .collection("messages")
                        .document(msgId)
                        .setData([
                            "id": msgId,
                            "emergencyId": emergencyId,
                            "senderId": uid,
                            "senderName": displayName,
                            "message": "✅ \(displayName) encerrou a emergência.",
                            "timestamp": nowMs,
                            "isFromHelper": false
                        ])
                }
            }
        // 2. Atualiza status da emergência
        db.collection("emergency_requests")
            .document(emergencyId)
            .updateData(["status": "resolved", "active": false, "resolvedAt": nowMs])
        container.resolvedEmergencyId = emergencyId
        container.emergency.clearEmergencyStateSwift(cancelledId: emergencyId)
        NotificationCenter.default.post(
            name: .init("AfilaxyEmergencyResolved"),
            object: nil,
            userInfo: ["emergencyId": emergencyId]
        )
        // Mostra avaliação se soubermos quem avaliar
        if reviewedId != nil { showRatingSheet = true }
    }

    /// Limpa estado e navega de volta à home quando foi a outra parte que encerrou.
    private func dismissAndClearState() {
        container.emergency.clearEmergencyStateSwift(cancelledId: emergencyId)
        NotificationCenter.default.post(
            name: .init("AfilaxyEmergencyResolved"),
            object: nil,
            userInfo: ["emergencyId": emergencyId]
        )
    }

    private func startListening() {
        // Listener de mensagens do chat
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
        // Listener do status da emergência — detecta quando A OUTRA PARTE resolve
        statusListener = Firestore.firestore()
            .collection("emergency_requests")
            .document(emergencyId)
            .addSnapshotListener { snapshot, _ in
                guard let data = snapshot?.data(),
                      let status = data["status"] as? String else { return }
                let wasResolvedByOther = status == "resolved" || status == "finished"
                DispatchQueue.main.async {
                    // Ignora o evento se foi este dispositivo que resolveu —
                    // evita o banner "encerrado pela outra parte" aparecer como ghost.
                    guard !didResolveLocally else { return }
                    if wasResolvedByOther && !isResolved {
                        resolvedByOther = true
                        isResolved = true
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

// MARK: - Helper functions (Analytics)

extension ChatView {

    private func fetchParticipants() {
        guard let currentUid = Auth.auth().currentUser?.uid else { return }
        Firestore.firestore().collection("emergency_requests").document(emergencyId).getDocument { snap, _ in
            guard let d = snap?.data() else { return }
            let requesterId = d["requesterId"] as? String
            let helperId = d["helperId"] as? String
            DispatchQueue.main.async {
                reviewedId = (currentUid == requesterId) ? helperId : requesterId
            }
        }
    }

    private func markSamuCalled() {
        guard !samuCalled else { return }
        samuCalled = true
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        Firestore.firestore().collection("emergency_requests").document(emergencyId)
            .updateData(["samuCalled": true, "samuCalledAt": nowMs])
    }

    private func submitReview(reviewedId: String?, rating: Int, comment: String?) {
        guard let reviewedId = reviewedId,
              let reviewerId = Auth.auth().currentUser?.uid else { return }
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
        var data: [String: Any] = [
            "emergencyId": emergencyId,
            "reviewerId": reviewerId,
            "reviewedId": reviewedId,
            "rating": rating,
            "timestamp": nowMs
        ]
        if let comment = comment { data["comment"] = comment }
        Firestore.firestore().collection("reviews").addDocument(data: data)
    }
}

// MARK: - RatingSheetView

struct RatingSheetView: View {
    var onSubmit: (Int, String?) -> Void
    var onSkip: () -> Void
    @State private var selectedRating = 0
    @State private var comment = ""

    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                Text("Como foi o atendimento?")
                    .font(.title3).bold()

                Text("Avalie a pessoa que te ajudou nesta emergência.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                // Estrelas
                HStack(spacing: 8) {
                    ForEach(1...5, id: \.self) { i in
                        Text(i <= selectedRating ? "⭐" : "☆")
                            .font(.largeTitle)
                            .onTapGesture { selectedRating = i }
                    }
                }

                TextField("Comentário opcional", text: $comment, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(3)
                    .padding(.horizontal)

                Spacer()

                HStack(spacing: 16) {
                    Button("Pular") { onSkip() }
                        .foregroundColor(.secondary)

                    Button("Enviar e Encerrar") {
                        onSubmit(max(1, selectedRating), comment.trimmingCharacters(in: .whitespaces).isEmpty ? nil : comment)
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
            }
            .padding(.top, 24)
            .navigationBarTitleDisplayMode(.inline)
        }
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
