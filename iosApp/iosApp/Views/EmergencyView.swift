import SwiftUI
import shared

struct EmergencyView: View {
    @EnvironmentObject var container: AppContainer
    @State private var vmState: EmergencyState? = nil

    var body: some View {
        List {
            if vmState?.hasActiveEmergency == true {
                Section {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundColor(.red)
                        Text("Emergência Ativa")
                            .font(.headline)
                    }
                    if vmState?.currentEmergency?.assignedHelperId == nil {
                        HStack {
                            ProgressView()
                            Text("Aguardando helper...")
                                .foregroundColor(.secondary)
                        }
                    }
                    Button("Cancelar Emergência", role: .destructive) {
                        container.emergencyViewModel.onCancelEmergency()
                    }
                }
            } else {
                Section {
                    Button {
                        container.emergencyViewModel.onCreateEmergency()
                    } label: {
                        Label("🆘 Solicitar Ajuda", systemImage: "exclamationmark.triangle.fill")
                            .frame(maxWidth: .infinity)
                            .foregroundColor(.white)
                            .padding(.vertical, 8)
                    }
                    .disabled(vmState?.isCreatingEmergency == true || vmState?.isLoading == true)
                    .listRowBackground(Color.red)
                }
            }

            let helpers = vmState?.nearbyHelpers ?? []
            if !helpers.isEmpty {
                Section("Helpers Próximos") {
                    ForEach(helpers, id: \.uid) { helper in
                        HStack {
                            Image(systemName: "person.fill")
                                .foregroundColor(.accentColor)
                            VStack(alignment: .leading) {
                                Text(helper.name).font(.headline)
                                Text(String(format: "%.1f km", Double(helper.distance) / 1000.0))
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }

            if let error = vmState?.error {
                Section {
                    Text(error).foregroundColor(.red).font(.caption)
                }
            }
        }
        .navigationTitle("Emergência")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            for await state in container.emergencyViewModel.state {
                vmState = state
            }
        }
    }
}
