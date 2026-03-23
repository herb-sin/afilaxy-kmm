import SwiftUI
import CoreLocation
import FirebaseFirestore
import shared

struct EmergencyView: View {
    @EnvironmentObject var container: AppContainer
    @StateObject private var locationManager = LocationManager.shared
    @State private var statusListener: ListenerRegistration? = nil
    @State private var chatNavigated = false
    @State private var secondsLeft: Int = 180
    @State private var countdownTimer: Timer? = nil

    @State private var isCancelling = false
    @State private var lastEmergencyId: String? = nil

    var body: some View {
        guard let state = container.emergency.state else {
            return AnyView(ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity))
        }
        return AnyView(emergencyBody(state: state))
    }

    private func requestLocationAndCreateEmergency() {
        FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "requestLocationAndCreateEmergency hasPermission=\(locationManager.hasPermission) hasLocation=\(locationManager.currentLocation != nil)")
        // Desativa helper mode ao solicitar ajuda — usuário não pode ser helper e requester
        if container.emergency.state?.isHelperMode == true {
            LocationManagerBridge.shared.disableHelperMode()
            container.emergency.setHelperMode(false)
        }
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

    // Cancela emergência via Firestore nativo — evita SIGABRT do KMM no iOS
    private func cancelEmergencyNative(emergencyId: String, status: String = "cancelled") {
        Firestore.firestore()
            .collection("emergency_requests")
            .document(emergencyId)
            .updateData(["active": false, "status": status])
        container.emergency.clearEmergencyStateSwift()
        isCancelling = false
    }

    private func startCountdown(expiresAt: Int64?) {
        // Não reinicia se já está rodando com o mesmo expiry
        guard countdownTimer == nil else { return }
        let expiry: Date
        if let ms = expiresAt, ms > 0 {
            expiry = Date(timeIntervalSince1970: Double(ms) / 1000)
        } else {
            expiry = Date().addingTimeInterval(180)
        }
        let initial = max(0, Int(expiry.timeIntervalSinceNow))
        secondsLeft = initial
        if initial == 0 {
            guard !isCancelling else { return }
            isCancelling = true
            FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "countdown already expired on start — auto cancel")
            if let eid = container.emergency.state?.emergencyId as? String {
                cancelEmergencyNative(emergencyId: eid)
            }
            return
        }
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            let remaining = max(0, Int(expiry.timeIntervalSinceNow))
            DispatchQueue.main.async {
                secondsLeft = remaining
                if remaining == 0 {
                    countdownTimer?.invalidate()
                    countdownTimer = nil
                    guard !chatNavigated, !isCancelling else { return }
                    isCancelling = true
                    FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "countdown expired — auto cancel")
                    if let eid = container.emergency.state?.emergencyId as? String {
                        cancelEmergencyNative(emergencyId: eid)
                    }
                }
            }
        }
    }

    private func startStatusObserver(emergencyId: String) {
        statusListener?.remove()
        statusListener = Firestore.firestore()
            .collection("emergency_requests")
            .document(emergencyId)
            .addSnapshotListener { snapshot, _ in
                guard let data = snapshot?.data(),
                      let status = data["status"] as? String,
                      status == "matched" else { return }
                DispatchQueue.main.async {
                    FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "status=matched — navigating to chat emergencyId=\(emergencyId)")
                    statusListener?.remove()
                    statusListener = nil
                    // Para o countdown — emergência foi aceita, não deve expirar
                    countdownTimer?.invalidate()
                    countdownTimer = nil
                    chatNavigated = true
                    container.navigateToChat(emergencyId: emergencyId)
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
                        let minutes = secondsLeft / 60
                        let seconds = secondsLeft % 60
                        Text(String(format: "Expira em %d:%02d", minutes, seconds))
                            .font(.headline)
                            .foregroundColor(secondsLeft <= 30 ? .red : .orange)
                            .monospacedDigit()
                    }
                    Button("Cancelar Emergência", role: .destructive) {
                        guard !isCancelling else { return }
                        isCancelling = true
                        FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "cancelEmergency tapped")
                        countdownTimer?.invalidate()
                        countdownTimer = nil
                        statusListener?.remove()
                        statusListener = nil
                        if let eid = container.emergency.state?.emergencyId as? String { cancelEmergencyNative(emergencyId: eid) }
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
            guard state.hasActiveEmergency,
                  let eid = state.emergencyId as? String,
                  let ms = state.emergencyExpiresAt?.int64Value, ms > 0,
                  Date(timeIntervalSince1970: Double(ms) / 1000) <= Date() else { return }
            // Só auto-cancela se for a mesma emergência que já estava ativa
            guard eid == lastEmergencyId else { return }
            FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "onAppear: emergency already expired — auto cancel")
            guard !isCancelling else { return }
            isCancelling = true
            cancelEmergencyNative(emergencyId: eid)
        }
        .onDisappear {
            statusListener?.remove()
            statusListener = nil
            countdownTimer?.invalidate()
            countdownTimer = nil
        }
        .onReceive(container.emergency.$state) { newState in
            guard let s = newState else { return }
            if !s.hasActiveEmergency {
                chatNavigated = false
                return
            }
            guard let eid = s.emergencyId else { return }
            // Nova emergência detectada — reseta todo o estado local
            if eid != lastEmergencyId {
                lastEmergencyId = eid
                chatNavigated = false
                isCancelling = false
                countdownTimer?.invalidate()
                countdownTimer = nil
                statusListener?.remove()
                statusListener = nil
            }
            guard !chatNavigated else { return }
            if statusListener == nil {
                FileLogger.shared.write(level: "INFO", tag: "EmergencyView", message: "emergency confirmed by server emergencyId=\(eid)")
                startStatusObserver(emergencyId: eid)
            }
            let expiresAt = s.emergencyExpiresAt?.int64Value ?? 0
            if expiresAt > 0 {
                startCountdown(expiresAt: expiresAt)
            }
        }
    }
}
