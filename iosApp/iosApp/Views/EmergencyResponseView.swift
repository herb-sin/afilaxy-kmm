import SwiftUI
import shared

struct EmergencyResponseView: View {
    let emergencyId: String
    @EnvironmentObject var container: AppContainer
    @Environment(\.dismiss) private var dismiss
    @State private var isAccepting = false
    @State private var accepted = false
    @State private var errorMessage: String? = nil

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
                } else {
                    Button {
                        guard !isAccepting else { return }
                        isAccepting = true
                        errorMessage = nil
                        FileLogger.shared.write(level: "INFO", tag: "EmergencyResponseView", message: "acceptEmergency tapped emergencyId=\(emergencyId)")
                        // Usa iOS SDK nativo — evita crash Kotlin/Native em thread não-main
                        LocationManagerBridge.shared.acceptEmergency(emergencyId: emergencyId) { success, error in
                            isAccepting = false
                            if success {
                                accepted = true
                                // Atualiza estado KMM para consistência
                                container.emergency.vm.onAcceptEmergency(emergencyId: emergencyId)
                            } else {
                                errorMessage = error ?? "Erro ao aceitar emergência"
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
    }
}
