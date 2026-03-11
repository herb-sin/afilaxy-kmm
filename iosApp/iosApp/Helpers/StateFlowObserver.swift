import Foundation
import shared

final class EmergencyViewModelWrapper: ObservableObject {
    let vm: EmergencyViewModel
    @Published private(set) var state: EmergencyState?
    private var timer: Timer?

    init(_ vm: EmergencyViewModel) {
        self.vm = vm
        self.state = vm.state.value as? EmergencyState
        timer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            guard let s = vm.state.value as? EmergencyState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
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
