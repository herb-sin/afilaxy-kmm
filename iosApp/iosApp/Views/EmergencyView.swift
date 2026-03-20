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
        FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "requestLocationAndCreateEmergency hasPermission=\(locationManager.hasPermission) hasLocation=\(locationManager.currentLocation != nil)")
        if locationManager.hasPermission {
            LocationManagerBridge.shared.start()
            if locationManager.currentLocation != nil {
                FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "using cached location — calling onCreateEmergency")
                container.emergency.vm.onCreateEmergency()
            } else {
                FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "no cached location — fetching async")
                Task {
                    _ = await locationManager.fetchCurrentLocation()
                    await MainActor.run {
                        FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "async fetch done — calling onCreateEmergency")
                        container.emergency.vm.onCreateEmergency()
                    }
                }
            }
        } else {
            FileLogger.shared.write(level: "WARN", tag: "EmergencyView", message: "no permission — requesting WhenInUse")
            locationManager.requestWhenInUse()
            Task {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                await MainActor.run {
                    FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "calling onCreateEmergency after permission wait")
                    container.emergency.vm.onCreateEmergency()
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
                        FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "cancelEmergency tapped")
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
                                Text(String(format: "%.1f km", helper.distance))
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
        .onAppear {
            FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "appeared hasActiveEmergency=\(state.hasActiveEmergency) isHelperMode=\(state.isHelperMode)")
        }
        .onReceive(container.emergency.$state) { newState in
            guard let s = newState else { return }
            if s.hasActiveEmergency {
                let eid = s.emergencyId ?? "nil"
                FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "emergency confirmed by server emergencyId=\(eid)")
                LocationManagerBridge.shared.disableHelperMode()
            }
            if let error = s.error, !s.hasActiveEmergency {
                FileLogger.shared.write(level: "ERROR", tag: "EmergencyView", message: "createEmergency error: \(error)")
            }
        }
    }
}
