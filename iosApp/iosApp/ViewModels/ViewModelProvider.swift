import Foundation
import shared
import FirebaseAuth

// MARK: - StateFlow Wrapper Classes
class AuthViewModelWrapper: ObservableObject {
    private let viewModel: AuthViewModel
    @Published var state: AuthState?
    private var observer: StateFlowObserver<AuthState>?
    
    init(_ viewModel: AuthViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(stateFlow: viewModel.state, callback: { [weak self] state in
            DispatchQueue.main.async {
                self?.state = state
            }
        })
    }
    
    var vm: AuthViewModel { viewModel }
    
    func signOutSwift() {
        // Use Firebase Auth directly since KMM ViewModel doesn't have signOut
        try? FirebaseAuth.Auth.auth().signOut()
    }
}

class EmergencyViewModelWrapper: ObservableObject {
    private let viewModel: EmergencyViewModel
    @Published var state: EmergencyState?
    private var observer: StateFlowObserver<EmergencyState>?
    
    init(_ viewModel: EmergencyViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(stateFlow: viewModel.state, callback: { [weak self] state in
            DispatchQueue.main.async {
                self?.state = state
            }
        })
    }
    
    var vm: EmergencyViewModel { viewModel }
    
    func freezeSwift() {
        // Freeze functionality handled at app level
    }
    
    func setHelperMode(_ enabled: Bool) {
        // Helper mode handled through LocationManagerBridge
    }
    
    func clearEmergencyStateSwift(cancelledId: String? = nil) {
        // Emergency state clearing handled through Firebase directly
    }
}

class HistoryViewModelWrapper: ObservableObject {
    private let viewModel: HistoryViewModel
    @Published var state: HistoryState?
    private var observer: StateFlowObserver<HistoryState>?
    
    init(_ viewModel: HistoryViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(stateFlow: viewModel.state, callback: { [weak self] state in
            DispatchQueue.main.async {
                self?.state = state
            }
        })
    }
    
    var vm: HistoryViewModel { viewModel }
    
    func freeze() {
        // Freeze functionality handled at app level
    }
}

class ProfileViewModelWrapper: ObservableObject {
    private let viewModel: ProfileViewModel
    @Published var state: ProfileState?
    private var observer: StateFlowObserver<ProfileState>?
    
    init(_ viewModel: ProfileViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(stateFlow: viewModel.state, callback: { [weak self] state in
            DispatchQueue.main.async {
                self?.state = state
            }
        })
    }
    
    var vm: ProfileViewModel { viewModel }
    
    func freeze() {
        // Freeze functionality handled at app level
    }
}

class ProfessionalListViewModelWrapper: ObservableObject {
    private let viewModel: ProfessionalListViewModel
    @Published var state: ProfessionalListState?
    private var observer: StateFlowObserver<ProfessionalListState>?
    
    init(_ viewModel: ProfessionalListViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(stateFlow: viewModel.state, callback: { [weak self] state in
            DispatchQueue.main.async {
                self?.state = state
            }
        })
    }
    
    var vm: ProfessionalListViewModel { viewModel }
    
    func freeze() {
        // Freeze functionality handled at app level
    }
}

class ProfessionalDetailViewModelWrapper: ObservableObject {
    private let viewModel: ProfessionalDetailViewModel
    @Published var state: ProfessionalDetailState?
    private var observer: StateFlowObserver<ProfessionalDetailState>?
    
    init(_ viewModel: ProfessionalDetailViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(stateFlow: viewModel.state, callback: { [weak self] state in
            DispatchQueue.main.async {
                self?.state = state
            }
        })
    }
    
    var vm: ProfessionalDetailViewModel { viewModel }
    
    func freeze() {
        // Freeze functionality handled at app level
    }
}

class ViewModelProvider {
    static let shared = ViewModelProvider()
    
    var homeViewModel: HomeViewModel {
        return getHomeViewModel()
    }
    
    var medicalProfileViewModel: MedicalProfileViewModel {
        return getMedicalProfileViewModel()
    }
    
    var professionalDashboardViewModel: ProfessionalDashboardViewModel {
        return getProfessionalDashboardViewModel()
    }

    // Usa funções safeGet* marcadas com @Throws em vez de getKoin().get()
    // para que exceções Kotlin não cruzem o boundary e causem abort().
    func getLoginViewModel() -> LoginViewModel {
        return safeGetLoginViewModel()
    }
    func getAuthViewModel() -> AuthViewModel {
        return safeGetAuthViewModel()
    }
    func getEmergencyViewModel() -> EmergencyViewModel {
        return safeGetEmergencyViewModel()
    }
    func getProfileViewModel() -> ProfileViewModel {
        return safeGetProfileViewModel()
    }
    func getHistoryViewModel() -> HistoryViewModel {
        return safeGetHistoryViewModel()
    }
    func getProfessionalListViewModel() -> ProfessionalListViewModel {
        return safeGetProfessionalListViewModel()
    }

    func getProfessionalDetailViewModel() -> ProfessionalDetailViewModel {
        return safeGetProfessionalDetailViewModel()
    }

    func getHomeViewModel() -> HomeViewModel {
        return safeGetHomeViewModel()
    }
    
    func getMedicalProfileViewModel() -> MedicalProfileViewModel {
        return safeGetMedicalProfileViewModel()
    }
    
    func getProfessionalDashboardViewModel() -> ProfessionalDashboardViewModel {
        return safeGetProfessionalDashboardViewModel()
    }

    func getChatViewModel(emergencyId: String) -> ChatViewModel {
        let chatRepository: ChatRepository = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ChatRepository
        let authRepository: AuthRepository = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! AuthRepository
        return ChatViewModel(emergencyId: emergencyId, chatRepository: chatRepository, authRepository: authRepository)
    }
}
