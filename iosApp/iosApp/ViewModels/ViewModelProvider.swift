import Foundation
import shared
import FirebaseAuth
import Combine

// MARK: - AuthViewModelWrapper
class AuthViewModelWrapper: ObservableObject {
    private let viewModel: AuthViewModel?
    @Published var state: AuthState?
    private var observer: StateFlowObserver<AuthState>?
    private var cancellable: AnyCancellable?

    init(_ viewModel: AuthViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<AuthState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }
    }

    static func empty() -> AuthViewModelWrapper { AuthViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: AuthViewModel? { viewModel }

    func signOutSwift() {
        try? FirebaseAuth.Auth.auth().signOut()
    }
}

// MARK: - EmergencyViewModelWrapper
class EmergencyViewModelWrapper: ObservableObject {
    private let viewModel: EmergencyViewModel?
    @Published var state: EmergencyState?
    private var observer: StateFlowObserver<EmergencyState>?
    private var cancellable: AnyCancellable?

    init(_ viewModel: EmergencyViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<EmergencyState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }
    }

    static func empty() -> EmergencyViewModelWrapper { EmergencyViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: EmergencyViewModel? { viewModel }

    func freezeSwift() {}
    func setHelperMode(_ enabled: Bool) {}
    func clearEmergencyStateSwift(cancelledId: String? = nil) { viewModel?.onClearEmergencyState() }
}

// MARK: - HistoryViewModelWrapper
class HistoryViewModelWrapper: ObservableObject {
    private let viewModel: HistoryViewModel?
    @Published var state: HistoryState?
    private var observer: StateFlowObserver<HistoryState>?
    private var cancellable: AnyCancellable?

    init(_ viewModel: HistoryViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<HistoryState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }
    }

    static func empty() -> HistoryViewModelWrapper { HistoryViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: HistoryViewModel? { viewModel }
    func freeze() {}
}

// MARK: - ProfileViewModelWrapper
class ProfileViewModelWrapper: ObservableObject {
    private let viewModel: ProfileViewModel?
    @Published var state: ProfileState?
    private var observer: StateFlowObserver<ProfileState>?
    private var cancellable: AnyCancellable?

    init(_ viewModel: ProfileViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<ProfileState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }
    }

    static func empty() -> ProfileViewModelWrapper { ProfileViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: ProfileViewModel? { viewModel }
    func freeze() {}
}

// MARK: - ProfessionalListViewModelWrapper
class ProfessionalListViewModelWrapper: ObservableObject {
    private let viewModel: ProfessionalListViewModel?
    @Published var state: ProfessionalListState?
    private var observer: StateFlowObserver<ProfessionalListState>?
    private var cancellable: AnyCancellable?

    init(_ viewModel: ProfessionalListViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<ProfessionalListState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }
    }

    static func empty() -> ProfessionalListViewModelWrapper { ProfessionalListViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: ProfessionalListViewModel? { viewModel }
    func freeze() {}
}

// MARK: - ProfessionalDetailViewModelWrapper
class ProfessionalDetailViewModelWrapper: ObservableObject {
    private let viewModel: ProfessionalDetailViewModel?
    @Published var state: ProfessionalDetailState?
    private var observer: StateFlowObserver<ProfessionalDetailState>?
    private var cancellable: AnyCancellable?

    init(_ viewModel: ProfessionalDetailViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<ProfessionalDetailState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }
    }

    static func empty() -> ProfessionalDetailViewModelWrapper { ProfessionalDetailViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: ProfessionalDetailViewModel? { viewModel }
    func freeze() {}
}

// MARK: - ViewModelProvider
class ViewModelProvider {
    static let shared = ViewModelProvider()

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
        guard safeGetAuthViewModel() != nil else {
            FileLogger.shared.write(level: "ERROR", tag: "ViewModelProvider", message: "getChatViewModel: Koin não disponível")
            return nil
        }
        do {
            let chatRepo = try KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as? ChatRepository
            let authRepo = try KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as? AuthRepository
            guard let cr = chatRepo, let ar = authRepo else {
                FileLogger.shared.write(level: "ERROR", tag: "ViewModelProvider", message: "getChatViewModel: cast repos falhou")
                return nil
            }
            return ChatViewModel(emergencyId: emergencyId, chatRepository: cr, authRepository: ar)
        } catch {
            FileLogger.shared.write(level: "ERROR", tag: "ViewModelProvider", message: "getChatViewModel: \(error.localizedDescription)")
            return nil
        }
    }
}
