import Foundation
import shared

final class EmergencyViewModelWrapper: ObservableObject {
    let vm: EmergencyViewModel
    @Published private(set) var state: EmergencyState?
    private var timer: Timer?
    /// Quando true, o timer só retoma após o KMM confirmar hasActiveEmergency=false.
    /// Evita que o polling sobrescreva estado Swift local com KMM desatualizado.
    private var localOverride = false
    private var helperModeOverride = false
    private var cancelledEmergencyIds = Set<String>()
    private var frozen = false

    init(_ vm: EmergencyViewModel) {
        self.vm = vm
        self.state = vm.state.value as? EmergencyState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, !self.frozen else { return }
            guard var s = vm.state.value as? EmergencyState else { return }
            if self.localOverride {
                guard !s.hasActiveEmergency else { return }
                self.localOverride = false
            }
            // Bloqueia restauração de emergência já cancelada pelo usuário
            if let eid = s.emergencyId as? String, self.cancelledEmergencyIds.contains(eid) {
                s = EmergencyState(
                    currentEmergency: nil,
                    emergencyId: nil,
                    emergencyStatus: nil,
                    emergencyExpiresAt: nil,
                    nearbyHelpers: s.nearbyHelpers,
                    incomingEmergencies: s.incomingEmergencies,
                    isLoading: false,
                    isCreatingEmergency: false,
                    error: nil,
                    isHelperMode: s.isHelperMode,
                    hasActiveEmergency: false,
                    isRequester: false,
                    userLatitude: s.userLatitude,
                    userLongitude: s.userLongitude
                )
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
                self.helperModeOverride = false
            }
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.frozen else { return }
                self.state = s
            }
        }
    }
    deinit { timer?.invalidate() }

    /// Aplica estado local e bloqueia polling até KMM confirmar hasActiveEmergency=false.
    private func applyLocalState(_ newState: EmergencyState, holdUntilKmmClears: Bool = false) {
        localOverride = holdUntilKmmClears
        guard !frozen else { return }
        state = newState
    }

    /// Para o timer permanentemente — deve ser chamado no logout antes de qualquer signOut.
    func freezeSwift() {
        frozen = true
        timer?.invalidate()
        timer = nil
        localOverride = false
        helperModeOverride = false
        cancelledEmergencyIds.removeAll()
    }

    /// Limpa o estado de emergência localmente — sem chamar nenhum método KMM.
    /// emergencyId: ID a bloquear permanentemente no polling (evita restauração pelo KMM).
    func clearEmergencyStateSwift(cancelledId: String? = nil) {
        guard let current = state else { return }
        if let eid = cancelledId { cancelledEmergencyIds.insert(eid) }
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
        ), holdUntilKmmClears: false)
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
    private var frozen = false

    init(_ vm: AuthViewModel) {
        self.vm = vm
        self.state = vm.state.value as? AuthState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, !self.frozen else { return }
            guard let s = vm.state.value as? AuthState else { return }
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.frozen else { return }
                self.state = s
            }
        }
    }
    deinit { timer?.invalidate() }

    /// Para o polling e publica isAuthenticated=false sincronamente.
    /// Deve ser chamado ANTES de Auth.auth().signOut().
    func signOutSwift() {
        frozen = true
        timer?.invalidate()
        timer = nil
        state = AuthState(user: nil, isLoading: false, error: nil, isAuthenticated: false)
    }
}

final class HistoryViewModelWrapper: ObservableObject {
    let vm: HistoryViewModel
    @Published private(set) var state: HistoryState?
    private var timer: Timer?
    private var frozen = false

    init(_ vm: HistoryViewModel) {
        self.vm = vm
        self.state = vm.state.value as? HistoryState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, !self.frozen else { return }
            guard let s = vm.state.value as? HistoryState else { return }
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.frozen else { return }
                self.state = s
            }
        }
    }
    deinit { timer?.invalidate() }
    func freeze() { frozen = true; timer?.invalidate(); timer = nil }
}

final class ProfileViewModelWrapper: ObservableObject {
    let vm: ProfileViewModel
    @Published private(set) var state: ProfileState?
    private var timer: Timer?
    private var frozen = false

    init(_ vm: ProfileViewModel) {
        self.vm = vm
        self.state = vm.state.value as? ProfileState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, !self.frozen else { return }
            guard let s = vm.state.value as? ProfileState else { return }
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.frozen else { return }
                self.state = s
            }
        }
    }
    deinit { timer?.invalidate() }
    func freeze() { frozen = true; timer?.invalidate(); timer = nil }
}

final class ProfessionalListViewModelWrapper: ObservableObject {
    let vm: ProfessionalListViewModel
    @Published private(set) var state: ProfessionalListState?
    private var timer: Timer?
    private var frozen = false

    init(_ vm: ProfessionalListViewModel) {
        self.vm = vm
        self.state = vm.state.value as? ProfessionalListState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, !self.frozen else { return }
            guard let s = vm.state.value as? ProfessionalListState else { return }
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.frozen else { return }
                self.state = s
            }
        }
    }
    deinit { timer?.invalidate() }
    func freeze() { frozen = true; timer?.invalidate(); timer = nil }
}

final class ProfessionalDetailViewModelWrapper: ObservableObject {
    let vm: ProfessionalDetailViewModel
    @Published private(set) var state: ProfessionalDetailState?
    private var timer: Timer?
    private var frozen = false

    init(_ vm: ProfessionalDetailViewModel) {
        self.vm = vm
        self.state = vm.state.value as? ProfessionalDetailState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, !self.frozen else { return }
            guard let s = vm.state.value as? ProfessionalDetailState else { return }
            DispatchQueue.main.async { [weak self] in
                guard let self, !self.frozen else { return }
                self.state = s
            }
        }
    }
    deinit { timer?.invalidate() }
    func freeze() { frozen = true; timer?.invalidate(); timer = nil }
}
