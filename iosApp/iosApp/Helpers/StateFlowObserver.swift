import Foundation
import shared

// Cada wrapper observa o StateFlow do ViewModel KMM via .watch{}
// e republica como @Published para o SwiftUI

final class EmergencyViewModelWrapper: ObservableObject {
    let vm: EmergencyViewModel
    @Published var state: EmergencyState

    init(_ vm: EmergencyViewModel) {
        self.vm = vm
        self.state = vm.state.value as! EmergencyState
        vm.state.watch { [weak self] s in
            guard let s = s as? EmergencyState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
}

final class AuthViewModelWrapper: ObservableObject {
    let vm: AuthViewModel
    @Published var state: AuthState

    init(_ vm: AuthViewModel) {
        self.vm = vm
        self.state = vm.state.value as! AuthState
        vm.state.watch { [weak self] s in
            guard let s = s as? AuthState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
}

final class HistoryViewModelWrapper: ObservableObject {
    let vm: HistoryViewModel
    @Published var state: HistoryState

    init(_ vm: HistoryViewModel) {
        self.vm = vm
        self.state = vm.state.value as! HistoryState
        vm.state.watch { [weak self] s in
            guard let s = s as? HistoryState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
}

final class ProfileViewModelWrapper: ObservableObject {
    let vm: ProfileViewModel
    @Published var state: ProfileState

    init(_ vm: ProfileViewModel) {
        self.vm = vm
        self.state = vm.state.value as! ProfileState
        vm.state.watch { [weak self] s in
            guard let s = s as? ProfileState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
}

final class ProfessionalListViewModelWrapper: ObservableObject {
    let vm: ProfessionalListViewModel
    @Published var state: ProfessionalListState

    init(_ vm: ProfessionalListViewModel) {
        self.vm = vm
        self.state = vm.state.value as! ProfessionalListState
        vm.state.watch { [weak self] s in
            guard let s = s as? ProfessionalListState else { return }
            DispatchQueue.main.async { self?.state = s }
        }
    }
}
