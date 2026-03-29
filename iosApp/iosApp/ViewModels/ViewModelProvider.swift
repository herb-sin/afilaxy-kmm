import Foundation
import shared
import FirebaseAuth

// MARK: - StateFlow Wrapper Classes
class AuthViewModelWrapper: ObservableObject {
    private let viewModel: AuthViewModel?
    @Published var state: AuthState?
    private var observer: StateFlowObserver<AuthState>?

    init(_ viewModel: AuthViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(viewModel.state)
        self.state = viewModel.state.value as? AuthState
    }

    // Wrapper vazio — usado quando o Koin falha (não crasha a UI)
    static func empty() -> AuthViewModelWrapper { AuthViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: AuthViewModel? { viewModel }

    func signOutSwift() {
        try? FirebaseAuth.Auth.auth().signOut()
    }
}

class EmergencyViewModelWrapper: ObservableObject {
    private let viewModel: EmergencyViewModel?
    @Published var state: EmergencyState?
    private var observer: StateFlowObserver<EmergencyState>?

    init(_ viewModel: EmergencyViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(viewModel.state)
        self.state = viewModel.state.value as? EmergencyState
    }

    static func empty() -> EmergencyViewModelWrapper { EmergencyViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: EmergencyViewModel? { viewModel }

    func freezeSwift() {}
    func setHelperMode(_ enabled: Bool) {}
    func clearEmergencyStateSwift(cancelledId: String? = nil) { viewModel?.onClearEmergencyState() }
}

class HistoryViewModelWrapper: ObservableObject {
    private let viewModel: HistoryViewModel?
    @Published var state: HistoryState?
    private var observer: StateFlowObserver<HistoryState>?

    init(_ viewModel: HistoryViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(viewModel.state)
        self.state = viewModel.state.value as? HistoryState
    }

    static func empty() -> HistoryViewModelWrapper { HistoryViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: HistoryViewModel? { viewModel }
    func freeze() {}
}

class ProfileViewModelWrapper: ObservableObject {
    private let viewModel: ProfileViewModel?
    @Published var state: ProfileState?
    private var observer: StateFlowObserver<ProfileState>?

    init(_ viewModel: ProfileViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(viewModel.state)
        self.state = viewModel.state.value as? ProfileState
    }

    static func empty() -> ProfileViewModelWrapper { ProfileViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: ProfileViewModel? { viewModel }
    func freeze() {}
}

class ProfessionalListViewModelWrapper: ObservableObject {
    private let viewModel: ProfessionalListViewModel?
    @Published var state: ProfessionalListState?
    private var observer: StateFlowObserver<ProfessionalListState>?

    init(_ viewModel: ProfessionalListViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(viewModel.state)
        self.state = viewModel.state.value as? ProfessionalListState
    }

    static func empty() -> ProfessionalListViewModelWrapper { ProfessionalListViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: ProfessionalListViewModel? { viewModel }
    func freeze() {}
}

class ProfessionalDetailViewModelWrapper: ObservableObject {
    private let viewModel: ProfessionalDetailViewModel?
    @Published var state: ProfessionalDetailState?
    private var observer: StateFlowObserver<ProfessionalDetailState>?

    init(_ viewModel: ProfessionalDetailViewModel) {
        self.viewModel = viewModel
        self.observer = StateFlowObserver(viewModel.state)
        self.state = viewModel.state.value as? ProfessionalDetailState
    }

    static func empty() -> ProfessionalDetailViewModelWrapper { ProfessionalDetailViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: ProfessionalDetailViewModel? { viewModel }
    func freeze() {}
}

class ViewModelProvider {
    static let shared = ViewModelProvider()

    // MARK: - Optional getters (seguros para iOS 26 + KMM-ViewModel ALPHA)
    func getLoginViewModel() -> LoginViewModel? { safeGetLoginViewModel() }
    func getAuthViewModel() -> AuthViewModel? { safeGetAuthViewModel() }
    func getEmergencyViewModel() -> EmergencyViewModel? { safeGetEmergencyViewModel() }
    func getProfileViewModel() -> ProfileViewModel? { safeGetProfileViewModel() }
    func getHistoryViewModel() -> HistoryViewModel? { safeGetHistoryViewModel() }
    func getProfessionalListViewModel() -> ProfessionalListViewModel? { safeGetProfessionalListViewModel() }
    func getProfessionalDetailViewModel() -> ProfessionalDetailViewModel? { safeGetProfessionalDetailViewModel() }
    func getHomeViewModel() -> HomeViewModel? { safeGetHomeViewModel() }
    func getMedicalProfileViewModel() -> MedicalProfileViewModel? { safeGetMedicalProfileViewModel() }
    func getProfessionalDashboardViewModel() -> ProfessionalDashboardViewModel? { safeGetProfessionalDashboardViewModel() }

    func getChatViewModel(emergencyId: String) -> ChatViewModel? {
        guard let chatRepo = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as? ChatRepository,
              let authRepo = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as? AuthRepository else {
            FileLogger.shared.write(level: "ERROR", tag: "ViewModelProvider", message: "getChatViewModel: falha ao resolver dependências")
            return nil
        }
        return ChatViewModel(emergencyId: emergencyId, chatRepository: chatRepo, authRepository: authRepo)
    }
}
