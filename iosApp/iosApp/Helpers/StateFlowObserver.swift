import Foundation
import shared

final class EmergencyViewModelWrapper: ObservableObject {
    let vm: EmergencyViewModel
    @Published private(set) var state: EmergencyState?
    private var timer: Timer?
    /// Enquanto frozenUntil > now, o timer não sobrescreve o estado Swift local.
    private var frozenUntil: Date = .distantPast

    init(_ vm: EmergencyViewModel) {
        self.vm = vm
        self.state = vm.state.value as? EmergencyState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let self, Date() > self.frozenUntil else { return }
            guard let s = vm.state.value as? EmergencyState else { return }
            DispatchQueue.main.async { self.state = s }
        }
    }
    deinit { timer?.invalidate() }

    /// Congela o polling por `seconds` segundos e aplica `newState` imediatamente.
    private func applyLocalState(_ newState: EmergencyState, freezeFor seconds: TimeInterval = 3.0) {
        frozenUntil = Date().addingTimeInterval(seconds)
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
        ))
    }

    /// Altera apenas isHelperMode — sem chamar nenhum método KMM.
    func setHelperMode(_ enabled: Bool) {
        guard let current = state else { return }
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
        ))
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
            guard let s = vm.state.value as? AuthState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
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
