import Foundation
import shared

// Observa StateFlow KMM via polling no MainThread.
// Kotlinx_coroutines_coreFlowKt.collect não tem assinatura estável no Swift bridge
// desta versão do KMM — polling é a abordagem mais segura e portável.

private func makeWrapper<T: AnyObject>(
    initial: T,
    stateFlow: Kotlinx_coroutines_coreStateFlow,
    onChange: @escaping (T) -> Void
) -> Timer {
    return Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
        guard let value = stateFlow.value as? T else { return }
        onChange(value)
    }
}

final class EmergencyViewModelWrapper: ObservableObject {
    let vm: EmergencyViewModel
    @Published private(set) var state: EmergencyState
    private var timer: Timer?

    init(_ vm: EmergencyViewModel) {
        self.vm = vm
        self.state = vm.state.value as! EmergencyState
        timer = makeWrapper(initial: state, stateFlow: vm.state) { [weak self] s in
            if self?.state as AnyObject !== s { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}

final class AuthViewModelWrapper: ObservableObject {
    let vm: AuthViewModel
    @Published private(set) var state: AuthState
    private var timer: Timer?

    init(_ vm: AuthViewModel) {
        self.vm = vm
        self.state = vm.state.value as! AuthState
        timer = makeWrapper(initial: state, stateFlow: vm.state) { [weak self] s in
            if self?.state as AnyObject !== s { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}

final class HistoryViewModelWrapper: ObservableObject {
    let vm: HistoryViewModel
    @Published private(set) var state: HistoryState
    private var timer: Timer?

    init(_ vm: HistoryViewModel) {
        self.vm = vm
        self.state = vm.state.value as! HistoryState
        timer = makeWrapper(initial: state, stateFlow: vm.state) { [weak self] s in
            if self?.state as AnyObject !== s { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}

final class ProfileViewModelWrapper: ObservableObject {
    let vm: ProfileViewModel
    @Published private(set) var state: ProfileState
    private var timer: Timer?

    init(_ vm: ProfileViewModel) {
        self.vm = vm
        self.state = vm.state.value as! ProfileState
        timer = makeWrapper(initial: state, stateFlow: vm.state) { [weak self] s in
            if self?.state as AnyObject !== s { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}

final class ProfessionalListViewModelWrapper: ObservableObject {
    let vm: ProfessionalListViewModel
    @Published private(set) var state: ProfessionalListState
    private var timer: Timer?

    init(_ vm: ProfessionalListViewModel) {
        self.vm = vm
        self.state = vm.state.value as! ProfessionalListState
        timer = makeWrapper(initial: state, stateFlow: vm.state) { [weak self] s in
            if self?.state as AnyObject !== s { self?.state = s }
        }
    }
    deinit { timer?.invalidate() }
}
