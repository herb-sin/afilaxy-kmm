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
    @State private var chatNavigated = false

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
                if accepted {
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
                                    errorMessage = error ?? "Erro ao aceitar emergência"
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
                Section { Text(error).foregroundColor(.red).font(.caption) }
            }
        }
        .navigationTitle("Emergência Próxima")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            FileLogger.shared.write(level: "INFO", tag: "EmergencyResponseView", message: "appeared emergencyId=\(emergencyId)")
        }
        .onDisappear {
            statusListener?.remove()
            statusListener = nil
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
