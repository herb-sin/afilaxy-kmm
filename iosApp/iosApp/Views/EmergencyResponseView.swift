import SwiftUI
import shared

/// Tela exibida ao helper quando clica na notificação de emergência próxima.
/// Permite aceitar a emergência e iniciar o chat.
struct EmergencyResponseView: View {
    let emergencyId: String
    @EnvironmentObject var container: AppContainer
    @Environment(\.dismiss) private var dismiss
    @State private var isAccepting = false

    var body: some View {
        guard let state = container.emergency.state else {
            return AnyView(ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity))
        }
        return AnyView(content(state: state))
    }

    @ViewBuilder
    private func content(state: EmergencyState) -> some View {
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
                if state.hasActiveEmergency && state.emergencyId == emergencyId {
                    // Já aceitou — mostra status
                    Label("Você aceitou esta emergência", systemImage: "checkmark.circle.fill")
                        .foregroundColor(.green)
                    NavigationLink("Abrir Chat", value: AppRoute.emergency) // placeholder — chat não implementado no iOS ainda
                } else {
                    Button {
                        guard !isAccepting else { return }
                        isAccepting = true
                        FileLogger.shared.write(level: "INFO", tag: "EmergencyResponseView", message: "acceptEmergency tapped emergencyId=\(emergencyId)")
                        container.emergency.vm.onAcceptEmergency(emergencyId: emergencyId)
                    } label: {
                        if isAccepting {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                        } else {
                            Label("Aceitar e Ajudar", systemImage: "heart.fill")
                                .frame(maxWidth: .infinity)
                                .foregroundColor(.white)
                                .padding(.vertical, 8)
                        }
                    }
                    .disabled(state.isLoading || isAccepting)
                    .listRowBackground(Color.green)
                }
            }

            if let error = state.error {
                Section { Text(error).foregroundColor(.red).font(.caption) }
            }
        }
        .navigationTitle("Emergência Próxima")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            FileLogger.shared.write(level: "INFO", tag: "EmergencyResponseView", message: "appeared emergencyId=\(emergencyId)")
        }
        // Dismiss automático quando a emergência foi atribuída a este helper
        .onReceive(container.emergency.$state) { newState in
            guard let s = newState else { return }
            if (s.emergencyId == emergencyId && s.hasActiveEmergency) ||
               (s.emergencyStatus == "matched" && isAccepting) {
                FileLogger.shared.write(level: "INFO", tag: "EmergencyResponseView", message: "emergency matched — dismissing")
                dismiss()
            }
        }
    }
}
