import SwiftUI
import CoreLocation
import shared

struct EmergencyView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var locationManager = LocationManager.shared

    var body: some View {
        guard let state = container.emergency.state else {
            return AnyView(ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity))
        }
        return AnyView(emergencyBody(state: state))
    }

    private func requestLocationAndCreateEmergency() {
        if locationManager.hasPermission {
            LocationManagerBridge.shared.start()
            // Aguarda até 3s pela localização antes de chamar o ViewModel
            Task {
                _ = await locationManager.fetchCurrentLocation()
                await MainActor.run {
                    container.emergency.vm.onCreateEmergency()
                    LocationManagerBridge.shared.disableHelperMode()
                }
            }
        } else {
            locationManager.requestWhenInUse()
            // Observa mudança de permissão e tenta novamente
            Task {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                await MainActor.run {
                    if locationManager.hasPermission {
                        LocationManagerBridge.shared.start()
                        container.emergency.vm.onCreateEmergency()
                    } else {
                        // Permissão negada — o ViewModel vai retornar "Erro ao obter localização"
                        container.emergency.vm.onCreateEmergency()
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func emergencyBody(state: EmergencyState) -> some View {
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
                        requestLocationAndCreateEmergency()
                    } label: {
                        Label("🆘 Solicitar Ajuda", systemImage: "exclamationmark.triangle.fill")
                            .frame(maxWidth: .infinity).foregroundColor(.white).padding(.vertical, 8)
                    }
                    .disabled(state.isCreatingEmergency || state.isLoading)
                    .listRowBackground(Color.red)
                }
            }

            let helpers: [Helper] = Array(state.nearbyHelpers)
            if !helpers.isEmpty {
                Section("Helpers Próximos") {
                    ForEach(helpers.indices, id: \.self) { i in
                        let helper = helpers[i]
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
