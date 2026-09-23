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
    // Countdown — faltava por completo nesta tela (só existia na tela do solicitante,
    // EmergencyView.swift); o helper aceitando não tinha nenhuma noção de quanto tempo
    // restava até a oferta expirar.
    @State private var secondsLeft: Int = 180
    @State private var countdownTimer: Timer? = nil

    var body: some View {
        List {
            Section {
                Label("Pedido de Socorro", systemImage: "exclamationmark.triangle.fill")
                    .foregroundColor(.red)
                    .font(.headline)
                Text("Alguém próximo está pedindo ajuda. Você pode aceitar e ir ao encontro desta pessoa.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                if !isUnavailable && !accepted {
                    Text(String(format: "Expira em %d:%02d", secondsLeft / 60, secondsLeft % 60))
                        .font(.headline)
                        .foregroundColor(secondsLeft <= 30 ? .red : .orange)
                        .monospacedDigit()
                }
            }

            // Triagem passiva: a pergunta aparece antes dos botões para que o helper
            // confirme que tem o medicamento antes de decidir aceitar ou recusar.
            Section {
                HStack(spacing: 12) {
                    Text("💊")
                        .font(.title2)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Você tem a bombinha com")
                            .font(.subheadline)
                            .foregroundColor(Color(red: 0.52, green: 0.39, blue: 0.02))
                        Text("Sulfato de Salbutamol com você?")
                            .font(.subheadline).bold()
                            .foregroundColor(Color(red: 0.52, green: 0.39, blue: 0.02))
                    }
                }
                .padding(.vertical, 4)
                .listRowBackground(Color(red: 1.0, green: 0.95, blue: 0.80))
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
                            Label("Sim, aceitar!", systemImage: "heart.fill")
                                .frame(maxWidth: .infinity).foregroundColor(.white).padding(.vertical, 8)
                        }
                    }
                    .disabled(isAccepting)
                    .listRowBackground(Color.green)

                    Button(role: .destructive) {
                        dismiss()
                    } label: {
                        Label("Não, recusar", systemImage: "xmark.circle")
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
            countdownTimer?.invalidate()
            countdownTimer = nil
        }
    }

    /// Inicia o countdown a partir do expiresAt real (espelhado em emergency_pings pela Cloud
    /// Function onEmergencyRequestWrite). Sem esse campo ainda mirrorado, cai no fallback de
    /// 3 minutos a partir de agora — mesmo padrão do EmergencyResponseScreen.kt (Android) e do
    /// EmergencyView.swift (tela do solicitante).
    private func startCountdown(expiresAtMs: Int64?) {
        guard countdownTimer == nil else { return }
        let expiry: Date
        if let ms = expiresAtMs, ms > 0 {
            expiry = Date(timeIntervalSince1970: Double(ms) / 1000)
        } else {
            expiry = Date().addingTimeInterval(180)
        }
        secondsLeft = max(0, Int(expiry.timeIntervalSinceNow))
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            let remaining = max(0, Int(expiry.timeIntervalSinceNow))
            DispatchQueue.main.async {
                secondsLeft = remaining
                if remaining == 0 {
                    countdownTimer?.invalidate()
                    countdownTimer = nil
                }
            }
        }
    }

    /// Observa o documento da emergência. Se ficar inativa (cancelada, expirada ou já aceita
    /// por outro helper), marca como indisponível e dispensa a view automaticamente.
    ///
    /// Lê emergency_pings (projeção sem PII) em vez de emergency_requests — nesta tela o
    /// usuário ainda não é participante, e só precisa de active/status/helperId, que a
    /// projeção já tem (ver firestore.rules e a Cloud Function onEmergencyRequestWrite).
    private func startAvailabilityObserver() {
        availabilityListener = Firestore.firestore()
            .collection("emergency_pings")
            .document(emergencyId)
            .addSnapshotListener { snapshot, _ in
                guard let data = snapshot?.data() else { return }
                let active = data["active"] as? Bool ?? true
                let status = data["status"] as? String ?? "waiting"
                let alreadyMatched = (data["helperId"] as? String) != nil && !accepted
                let unavailable = !active || status == "cancelled" || (alreadyMatched && !accepted)
                let expiresAtMs = data["expiresAt"] as? Int64 ?? (data["expiresAt"] as? NSNumber)?.int64Value
                DispatchQueue.main.async {
                    startCountdown(expiresAtMs: expiresAtMs)
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

    // Também lê emergency_pings — mesmo motivo de startAvailabilityObserver acima.
    private func startStatusObserver() {
        statusListener = Firestore.firestore()
            .collection("emergency_pings")
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
