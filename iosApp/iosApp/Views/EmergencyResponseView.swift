import SwiftUI
import FirebaseFirestore
import shared

struct EmergencyResponseView: View {
    let emergencyId: String
    @EnvironmentObject var container: AppContainer
    @Environment(\.dismiss) private var dismiss
    @State private var isAccepting = false
    @State private var accepted = false
    @State private var errorMessage: String? = nil
    @State private var statusListener: ListenerRegistration? = nil
    @State private var availabilityListener: ListenerRegistration? = nil
    @State private var chatNavigated = false
    @State private var isUnavailable = false  // emergência cancelada/expirada/já aceita

    var body: some View {
        List {
            Section {
                Label("Pedido de Socorro", systemImage: "exclamationmark.triangle.fill")
                    .foregroundColor(.red)
                    .font(.headline)
                Text("Alguém próximo precisa de ajuda com asma. Você pode aceitar e ir ao encontro desta pessoa.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Section {
                if isUnavailable {
                Section {
                    Label("Emergência não disponível", systemImage: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                    Text("Esta emergência foi cancelada ou já foi atendida por outra pessoa.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            } else if accepted {
                    Label("Você aceitou esta emergência", systemImage: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    Text("Abrindo chat...")
                        .font(.caption)
                        .foregroundColor(.secondary)
                } else {
                    Button {
                        guard !isAccepting else { return }
                        isAccepting = true
                        errorMessage = nil
                        FileLogger.shared.write(level: "INFO", tag: "EmergencyResponseView", message: "acceptEmergency tapped emergencyId=\(emergencyId)")
                        LocationManagerBridge.shared.acceptEmergency(emergencyId: emergencyId) { success, error in
                            DispatchQueue.main.async {
                                isAccepting = false
                                if success {
                                    accepted = true
                                    container.emergency.setHelperMode(false)
                                    startStatusObserver()
                                } else {
                                    // "Missing or insufficient permissions" = emergência já cancelada/aceita
                                    let friendlyError = (error?.contains("permissions") == true || error?.contains("permission") == true)
                                        ? "Esta emergência não está mais disponível."
                                        : (error ?? "Erro ao aceitar emergência")
                                    errorMessage = friendlyError
                                }
                            }
                        }
                    } label: {
                        if isAccepting {
                            ProgressView().frame(maxWidth: .infinity).padding(.vertical, 8)
                        } else {
                            Label("Aceitar e Ajudar", systemImage: "heart.fill")
                                .frame(maxWidth: .infinity).foregroundColor(.white).padding(.vertical, 8)
                        }
                    }
                    .disabled(isAccepting)
                    .listRowBackground(Color.green)

                    Button(role: .destructive) {
                        dismiss()
                    } label: {
                        Label("Recusar", systemImage: "xmark.circle")
                            .frame(maxWidth: .infinity).padding(.vertical, 8)
                    }
                    .disabled(isAccepting)
                }
            }

            if let error = errorMessage {
                Section {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }
            }
        }
        .navigationTitle("Emergência Próxima")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            FileLogger.shared.write(level: "INFO", tag: "EmergencyResponseView", message: "appeared emergencyId=\(emergencyId)")
            // Guarda contra auto-match: o device pode receber notificação da própria
            // emergência se ainda estava registrado na coleção 'helpers'.
            let ownId = container.emergency.state?.emergencyId
            if ownId == emergencyId {
                FileLogger.shared.write(level: "WARN", tag: "EmergencyResponseView", message: "self-match detected — dismissing emergencyId=\(emergencyId)")
                container.dismissIncomingEmergency(id: emergencyId)
                dismiss()
                return
            }
            startAvailabilityObserver()
        }
        .onDisappear {
            statusListener?.remove()
            statusListener = nil
            availabilityListener?.remove()
            availabilityListener = nil
        }
    }

    /// Observa o documento da emergência. Se ficar inativa (cancelada, expirada ou já aceita
    /// por outro helper), marca como indisponível e dispensa a view automaticamente.
    private func startAvailabilityObserver() {
        availabilityListener = Firestore.firestore()
            .collection("emergency_requests")
            .document(emergencyId)
            .addSnapshotListener { snapshot, _ in
                guard let data = snapshot?.data() else { return }
                let active = data["active"] as? Bool ?? true
                let status = data["status"] as? String ?? "waiting"
                let alreadyMatched = (data["helperId"] as? String) != nil && !accepted
                let unavailable = !active || status == "cancelled" || (alreadyMatched && !accepted)
                DispatchQueue.main.async {
                    if unavailable && !chatNavigated {
                        isUnavailable = true
                        errorMessage = nil
                        // Remove da lista pendente e dispensa após breve delay
                        container.dismissIncomingEmergency(id: emergencyId)
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { dismiss() }
                    }
                }
            }
    }

    private func startStatusObserver() {
        statusListener = Firestore.firestore()
            .collection("emergency_requests")
            .document(emergencyId)
            .addSnapshotListener { snapshot, _ in
                guard let data = snapshot?.data(),
                      let status = data["status"] as? String,
                      status == "matched",
                      !chatNavigated else { return }
                DispatchQueue.main.async {
                    guard !chatNavigated else { return }
                    chatNavigated = true
                    FileLogger.shared.write(level: "INFO", tag: "EmergencyResponseView", message: "status=matched — navigating to chat emergencyId=\(emergencyId)")
                    statusListener?.remove()
                    statusListener = nil
                    container.navigateToChat(emergencyId: emergencyId)
                }
            }
    }
}
