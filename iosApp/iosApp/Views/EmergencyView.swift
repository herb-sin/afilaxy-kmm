import SwiftUI
import shared

struct EmergencyView: View {
    @EnvironmentObject var container: AppContainer

    var body: some View {
        let state = container.emergency.state
        List {
            if state.hasActiveEmergency {
                Section {
                    Label("Emergência Ativa", systemImage: "exclamationmark.triangle.fill")
                        .foregroundColor(.red)
                    if state.currentEmergency?.assignedHelperId == nil {
                        HStack { ProgressView(); Text("Aguardando helper...").foregroundColor(.secondary) }
                    }
                    Button("Cancelar Emergência", role: .destructive) {
                        container.emergency.vm.onCancelEmergency()
                    }
                }
            } else {
                Section {
                    Button {
                        container.emergency.vm.onCreateEmergency()
                    } label: {
                        Label("🆘 Solicitar Ajuda", systemImage: "exclamationmark.triangle.fill")
                            .frame(maxWidth: .infinity).foregroundColor(.white).padding(.vertical, 8)
                    }
                    .disabled(state.isCreatingEmergency || state.isLoading)
                    .listRowBackground(Color.red)
                }
            }

            let helpers = state.nearbyHelpers
            if !helpers.isEmpty {
                Section("Helpers Próximos") {
                    ForEach(helpers, id: \.uid) { helper in
                        HStack {
                            Image(systemName: "person.fill").foregroundColor(.accentColor)
                            VStack(alignment: .leading) {
                                Text(helper.name).font(.headline)
                                Text(String(format: "%.1f km", Double(helper.distance) / 1000.0))
                                    .font(.caption).foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }

            if let error = state.error {
                Section { Text(error).foregroundColor(.red).font(.caption) }
            }
        }
        .navigationTitle("Emergência")
        .navigationBarTitleDisplayMode(.inline)
    }
}
