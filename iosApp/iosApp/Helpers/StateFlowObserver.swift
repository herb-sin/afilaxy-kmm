import Foundation
import Combine
import shared

// Padrão correto para kmm-viewmodel-core 1.0.0-ALPHA-16:
// O KMMViewModel expõe setSendObjectWillChange via ObjC protocol.
// Cada wrapper conecta esse callback ao objectWillChange do ObservableObject.

final class EmergencyViewModelWrapper: ObservableObject {
    let vm: EmergencyViewModel
    init(_ vm: EmergencyViewModel) {
        self.vm = vm
        vm.viewModelScope.setSendObjectWillChange { [weak self] in
            self?.objectWillChange.send()
        }
    }
    var state: EmergencyState { vm.state.value as! EmergencyState }
}

final class AuthViewModelWrapper: ObservableObject {
    let vm: AuthViewModel
    init(_ vm: AuthViewModel) {
        self.vm = vm
        vm.viewModelScope.setSendObjectWillChange { [weak self] in
            self?.objectWillChange.send()
        }
    }
    var state: AuthState { vm.state.value as! AuthState }
}

final class HistoryViewModelWrapper: ObservableObject {
    let vm: HistoryViewModel
    init(_ vm: HistoryViewModel) {
        self.vm = vm
        vm.viewModelScope.setSendObjectWillChange { [weak self] in
            self?.objectWillChange.send()
        }
    }
    var state: HistoryState { vm.state.value as! HistoryState }
}

final class ProfileViewModelWrapper: ObservableObject {
    let vm: ProfileViewModel
    init(_ vm: ProfileViewModel) {
        self.vm = vm
        vm.viewModelScope.setSendObjectWillChange { [weak self] in
            self?.objectWillChange.send()
        }
    }
    var state: ProfileState { vm.state.value as! ProfileState }
}

final class ProfessionalListViewModelWrapper: ObservableObject {
    let vm: ProfessionalListViewModel
    init(_ vm: ProfessionalListViewModel) {
        self.vm = vm
        vm.viewModelScope.setSendObjectWillChange { [weak self] in
            self?.objectWillChange.send()
        }
    }
    var state: ProfessionalListState { vm.state.value as! ProfessionalListState }
}
