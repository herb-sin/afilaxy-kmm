import Foundation
import shared

final class EmergencyViewModelWrapper: ObservableObject {
    let vm: EmergencyViewModel
    @Published private(set) var state: EmergencyState?
    private var timer: Timer?
    /// Quando true, o timer só retoma após o KMM confirmar hasActiveEmergency=false.
    /// Evita que o polling sobrescreva estado Swift local com KMM desatualizado.
    private var localOverride = false
    /// Quando true, o polling preserva isHelperMode=true mesmo que o KMM diga false.
    private var helperModeOverride = false

    init(_ vm: EmergencyViewModel) {
        self.vm = vm
        self.state = vm.state.value as? EmergencyState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self else { return }
            guard var s = vm.state.value as? EmergencyState else { return }
            if self.localOverride {
                guard !s.hasActiveEmergency else { return }
                self.localOverride = false
            }
            // Preserva helper mode local se o KMM ainda não confirmou
            if self.helperModeOverride && !s.isHelperMode {
                s = EmergencyState(
                    currentEmergency: s.currentEmergency,
                    emergencyId: s.emergencyId,
                    emergencyStatus: s.emergencyStatus,
                    emergencyExpiresAt: s.emergencyExpiresAt,
                    nearbyHelpers: s.nearbyHelpers,
                    incomingEmergencies: s.incomingEmergencies,
                    isLoading: s.isLoading,
                    isCreatingEmergency: s.isCreatingEmergency,
                    error: s.error,
                    isHelperMode: true,
                    hasActiveEmergency: s.hasActiveEmergency,
                    isRequester: s.isRequester,
                    userLatitude: s.userLatitude,
                    userLongitude: s.userLongitude
                )
            } else if self.helperModeOverride && s.isHelperMode {
                self.helperModeOverride = false  // KMM confirmou
            }
            DispatchQueue.main.async { self.state = s }
        }
    }
    deinit { timer?.invalidate() }

    /// Aplica estado local e bloqueia polling até KMM confirmar hasActiveEmergency=false.
    private func applyLocalState(_ newState: EmergencyState, holdUntilKmmClears: Bool = false) {
        localOverride = holdUntilKmmClears
        DispatchQueue.main.async { self.state = newState }
    }

    /// Limpa o estado de emergência localmente — sem chamar nenhum método KMM.
    func clearEmergencyStateSwift() {
        guard let current = state else { return }
        applyLocalState(EmergencyState(
            currentEmergency: nil,
            emergencyId: nil,
            emergencyStatus: nil,
            emergencyExpiresAt: nil,
            nearbyHelpers: current.nearbyHelpers,
            incomingEmergencies: current.incomingEmergencies,
            isLoading: false,
            isCreatingEmergency: false,
            error: nil,
            isHelperMode: false,
            hasActiveEmergency: false,
            isRequester: false,
            userLatitude: current.userLatitude,
            userLongitude: current.userLongitude
        ), holdUntilKmmClears: true)
    }

    /// Altera apenas isHelperMode — sem chamar nenhum método KMM.
    func setHelperMode(_ enabled: Bool) {
        guard let current = state else { return }
        if enabled { helperModeOverride = true }
        else { helperModeOverride = false }
        applyLocalState(EmergencyState(
            currentEmergency: current.currentEmergency,
            emergencyId: current.emergencyId,
            emergencyStatus: current.emergencyStatus,
            emergencyExpiresAt: current.emergencyExpiresAt,
            nearbyHelpers: current.nearbyHelpers,
            incomingEmergencies: current.incomingEmergencies,
            isLoading: current.isLoading,
            isCreatingEmergency: current.isCreatingEmergency,
            error: current.error,
            isHelperMode: enabled,
            hasActiveEmergency: current.hasActiveEmergency,
            isRequester: current.isRequester,
            userLatitude: current.userLatitude,
            userLongitude: current.userLongitude
        ), holdUntilKmmClears: false)
    }
}

final class AuthViewModelWrapper: ObservableObject {
    let vm: AuthViewModel
    @Published private(set) var state: AuthState?
    private var timer: Timer?

    init(_ vm: AuthViewModel) {
        self.vm = vm
        self.state = vm.state.value as? AuthState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, !self.frozen else { return }
            guard let s = vm.state.value as? AuthState else { return }
            DispatchQueue.main.async { self.state = s }
        }
    }
    deinit { timer?.invalidate() }

    private var frozen = false

    /// Para o polling e publica isAuthenticated=false imediatamente.
    /// Deve ser chamado ANTES de Auth.auth().signOut() para evitar que
    /// o KMM observeAuthState() processe a mudança e cause SIGABRT.
    func signOutSwift() {
        frozen = true
        timer?.invalidate()
        timer = nil
        DispatchQueue.main.async {
            self.state = AuthState(
                user: nil,
                isLoading: false,
                error: nil,
                isAuthenticated: false
            )
        }
    }
}

final class HistoryViewModelWrapper: ObservableObject {
    let vm: HistoryViewModel
    @Published private(set) var state: HistoryState?
    private var timer: Timer?

    init(_ vm: HistoryViewModel) {
        self.vm = vm
        self.state = vm.state.value as? HistoryState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let s = vm.state.value as? HistoryState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}

final class ProfileViewModelWrapper: ObservableObject {
    let vm: ProfileViewModel
    @Published private(set) var state: ProfileState?
    private var timer: Timer?

    init(_ vm: ProfileViewModel) {
        self.vm = vm
        self.state = vm.state.value as? ProfileState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let s = vm.state.value as? ProfileState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}

final class ProfessionalListViewModelWrapper: ObservableObject {
    let vm: ProfessionalListViewModel
    @Published private(set) var state: ProfessionalListState?
    private var timer: Timer?

    init(_ vm: ProfessionalListViewModel) {
        self.vm = vm
        self.state = vm.state.value as? ProfessionalListState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let s = vm.state.value as? ProfessionalListState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}

final class ProfessionalDetailViewModelWrapper: ObservableObject {
    let vm: ProfessionalDetailViewModel
    @Published private(set) var state: ProfessionalDetailState?
    private var timer: Timer?

    init(_ vm: ProfessionalDetailViewModel) {
        self.vm = vm
        self.state = vm.state.value as? ProfessionalDetailState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let s = vm.state.value as? ProfessionalDetailState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}
