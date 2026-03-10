import Foundation
import shared

// Observa StateFlow KMM coletando via coroutine scope do ViewModel.
// Usa Kotlinx_coroutines_coreFlowKt.collect com callback no MainThread.

private func observe<VM: KMMViewModel, S: AnyObject>(
    vm: VM,
    stateFlow: Kotlinx_coroutines_coreStateFlow,
    onChange: @escaping (S) -> Void
) {
    let scope = vm.viewModelScope.coroutineScope
    Kotlinx_coroutines_coreFlowKt.collect(
        flow: stateFlow,
        collector: { value, completionHandler in
            if let s = value as? S {
                DispatchQueue.main.async { onChange(s) }
            }
            completionHandler(nil)
        },
        completionHandler: { _ in }
    )
}

final class EmergencyViewModelWrapper: ObservableObject {
    let vm: EmergencyViewModel
    @Published private(set) var state: EmergencyState

    init(_ vm: EmergencyViewModel) {
        self.vm = vm
        self.state = vm.state.value as! EmergencyState
        observe(vm: vm, stateFlow: vm.state) { [weak self] (s: EmergencyState) in
            self?.state = s
        }
    }
}

final class AuthViewModelWrapper: ObservableObject {
    let vm: AuthViewModel
    @Published private(set) var state: AuthState

    init(_ vm: AuthViewModel) {
        self.vm = vm
        self.state = vm.state.value as! AuthState
        observe(vm: vm, stateFlow: vm.state) { [weak self] (s: AuthState) in
            self?.state = s
        }
    }
}

final class HistoryViewModelWrapper: ObservableObject {
    let vm: HistoryViewModel
    @Published private(set) var state: HistoryState

    init(_ vm: HistoryViewModel) {
        self.vm = vm
        self.state = vm.state.value as! HistoryState
        observe(vm: vm, stateFlow: vm.state) { [weak self] (s: HistoryState) in
            self?.state = s
        }
    }
}

final class ProfileViewModelWrapper: ObservableObject {
    let vm: ProfileViewModel
    @Published private(set) var state: ProfileState

    init(_ vm: ProfileViewModel) {
        self.vm = vm
        self.state = vm.state.value as! ProfileState
        observe(vm: vm, stateFlow: vm.state) { [weak self] (s: ProfileState) in
            self?.state = s
        }
    }
}

final class ProfessionalListViewModelWrapper: ObservableObject {
    let vm: ProfessionalListViewModel
    @Published private(set) var state: ProfessionalListState

    init(_ vm: ProfessionalListViewModel) {
        self.vm = vm
        self.state = vm.state.value as! ProfessionalListState
        observe(vm: vm, stateFlow: vm.state) { [weak self] (s: ProfessionalListState) in
            self?.state = s
        }
    }
}
