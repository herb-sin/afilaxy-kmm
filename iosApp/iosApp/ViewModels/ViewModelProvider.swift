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

    func freezeSwift() {
        // 1. Cancela os coroutines KMM PRIMEIRO — sem isso, um evento Firestore em voo
        //    ainda tenta executar _state.update após o logout → SIGABRT (build 299 e 300)
        viewModel?.cancelAllObservations()
        // 2. Para a pipeline Combine — não mais atualizações no @Published state
        cancellable?.cancel()
        cancellable = nil
        // 3. Remove a referência ao StateFlowObserver — libera a coleta do KMM StateFlow
        observer = nil
        // 4. Reseta o estado local do ViewModel
        viewModel?.onClearEmergencyState()
    }

    /// Atualiza isHelperMode no shared ViewModel.
    /// Antes estava vazio — o toggle nunca refletia o estado correto no iOS.
    func setHelperMode(_ enabled: Bool) {
        viewModel?.onToggleHelperMode(enable: enabled)
    }

    /// Inicia o observer em tempo real de helpers próximos via KMM ViewModel.
    /// Chamado pelo MapView ao abrir o mapa se a localização já estiver disponível.
    func startObservingNearbyHelpers(latitude: Double, longitude: Double) {
        viewModel?.startObservingNearbyHelpers(latitude: latitude, longitude: longitude)
    }

    /// Limpa o estado de emergência local. Garante que hasActiveEmergency e
    /// isHelperMode fiquem false para evitar botão "Ativa" preso.
    func clearEmergencyStateSwift(cancelledId: String? = nil) {
        viewModel?.onClearEmergencyState()
        viewModel?.onToggleHelperMode(enable: false) // garante helper mode limpo
    }
}

// MARK: - HistoryViewModelWrapper
class HistoryViewModelWrapper: ObservableObject {
    private let viewModel: HistoryViewModel?
    @Published var state: HistoryState?
    private var observer: StateFlowObserver<HistoryState>?
    private var cancellable: AnyCancellable?
    private var authHandle: AuthStateDidChangeListenerHandle?

    init(_ viewModel: HistoryViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<HistoryState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }

        // HistoryViewModel.init { loadHistory() } executa antes do Firebase Auth
        // restaurar currentUser → "Usuário não autenticado". Listener faz retry
        // no momento correto, sem depender do timing do TabBar ou onAppear.
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard user != nil, self?.state?.history.isEmpty == true else { return }
            FileLogger.shared.write(level: "INFO", tag: "HistoryViewModelWrapper",
                message: "authStateDidChange: user disponível, recarregando histórico")
            self?.viewModel?.loadHistory()
        }
    }

    deinit {
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    static func empty() -> HistoryViewModelWrapper { HistoryViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: HistoryViewModel? { viewModel }

    func freeze() {
        cancellable?.cancel()
        cancellable = nil
        observer = nil
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
            authHandle = nil
        }
    }
}

// MARK: - ProfileViewModelWrapper
class ProfileViewModelWrapper: ObservableObject {
    private let viewModel: ProfileViewModel?
    @Published var state: ProfileState?
    private var observer: StateFlowObserver<ProfileState>?
    private var cancellable: AnyCancellable?
    // Handle do listener de Auth — removido em deinit para evitar leak
    private var authHandle: AuthStateDidChangeListenerHandle?

    init(_ viewModel: ProfileViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<ProfileState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }

        // Fix: ProfileViewModel.init { loadProfile() } executa no boot, antes do
        // Firebase Auth iOS restaurar o currentUser persistido em disco (~15s de gap).
        // O ProfileView é uma tab do TabBar → onAppear também dispara antes do Auth.
        // Solução: listener de Auth que recarrega o perfil quando o user fica disponível.
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard user != nil, self?.state?.profile == nil else { return }
            FileLogger.shared.write(level: "INFO", tag: "ProfileViewModelWrapper",
                message: "authStateDidChange: user disponível, recarregando perfil")
            self?.viewModel?.loadProfile()
        }
    }

    deinit {
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    static func empty() -> ProfileViewModelWrapper { ProfileViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: ProfileViewModel? { viewModel }
    func freeze() {
        cancellable?.cancel()
        cancellable = nil
        observer = nil
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
            authHandle = nil
        }
    }
}

// MARK: - ProfessionalListViewModelWrapper
class ProfessionalListViewModelWrapper: ObservableObject {
    private let viewModel: ProfessionalListViewModel?
    @Published var state: ProfessionalListState?
    private var observer: StateFlowObserver<ProfessionalListState>?
    private var cancellable: AnyCancellable?
    private var authHandle: AuthStateDidChangeListenerHandle?

    init(_ viewModel: ProfessionalListViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<ProfessionalListState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }

        // Mesmo padrão do ProfileViewModelWrapper: ProfessionalListViewModel.init
        // dispara loadProfessionals() antes do Firebase Auth restaurar currentUser
        // → "Missing or insufficient permissions". Listener garante retry no momento certo.
        authHandle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            guard user != nil, self?.state?.professionals.isEmpty == true else { return }
            FileLogger.shared.write(level: "INFO", tag: "ProfessionalListViewModelWrapper",
                message: "authStateDidChange: user disponível, recarregando profissionais")
            self?.viewModel?.loadProfessionals(specialty: nil)
        }
    }

    deinit {
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
        }
    }

    static func empty() -> ProfessionalListViewModelWrapper { ProfessionalListViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: ProfessionalListViewModel? { viewModel }

    /// Força um recarregamento da lista — chamado pelo .onAppear da ProfessionalListView
    /// para garantir que o Firebase Auth já está pronto (o init() do ViewModel dispara
    /// antes do auth restaurar a sessão, resultando em lista vazia).
    func loadProfessionals() {
        viewModel?.loadProfessionals(specialty: nil)
    }

    func freeze() {
        cancellable?.cancel()
        cancellable = nil
        observer = nil
        if let handle = authHandle {
            Auth.auth().removeStateDidChangeListener(handle)
            authHandle = nil
        }
    }
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
    func freeze() {
        cancellable?.cancel()
        cancellable = nil
        observer = nil
    }
}

// MARK: - RiskViewModelWrapper
class RiskViewModelWrapper: ObservableObject {
    private let viewModel: RiskViewModel?
    @Published var state: RiskState?
    private var observer: StateFlowObserver<RiskState>?
    private var cancellable: AnyCancellable?

    init(_ viewModel: RiskViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<RiskState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }
    }

    static func empty() -> RiskViewModelWrapper { RiskViewModelWrapper() }
    private init() { self.viewModel = nil }

    func loadRiskScore(latitude: Double, longitude: Double) {
        viewModel?.loadRiskScore(latitude: latitude, longitude: longitude)
    }

    func freeze() {
        cancellable?.cancel()
        cancellable = nil
        observer = nil
    }
}

// MARK: - CheckInViewModelWrapper
class CheckInViewModelWrapper: ObservableObject {
    private let viewModel: CheckInViewModel?
    @Published var state: CheckInState?
    private var observer: StateFlowObserver<CheckInState>?
    private var cancellable: AnyCancellable?

    init(_ viewModel: CheckInViewModel) {
        self.viewModel = viewModel
        let obs = StateFlowObserver<CheckInState>(viewModel.state)
        self.observer = obs
        self.state = obs.value
        self.cancellable = obs.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] newState in self?.state = newState }
    }

    static func empty() -> CheckInViewModelWrapper { CheckInViewModelWrapper() }
    private init() { self.viewModel = nil }

    var vm: CheckInViewModel? { viewModel }

    func initialize(type: CheckInType, riskScore: KotlinInt? = nil, aqi: KotlinInt? = nil) {
        viewModel?.initialize(type: type, riskScore: riskScore, aqi: aqi, temperature: nil, humidity: nil)
    }

    func submitMorning(hasInhaler: Bool) {
        viewModel?.submitMorningCheckIn(hasInhaler: hasInhaler)
    }

    func submitEvening(hadCrisis: Bool, severity: String? = nil, usedRescueInhaler: KotlinBoolean? = nil) {
        viewModel?.submitEveningCheckIn(hadCrisis: hadCrisis, severity: severity, usedRescueInhaler: usedRescueInhaler)
    }

    func freeze() {
        cancellable?.cancel()
        cancellable = nil
        observer = nil
    }
}

// MARK: - ViewModelProvider
class ViewModelProvider {
    static let shared = ViewModelProvider()

    func getRiskViewModel() -> RiskViewModel? { safeGetRiskViewModel() }
    func getLoginViewModel() -> LoginViewModel? { safeGetLoginViewModel() }
    func getAuthViewModel() -> AuthViewModel? { safeGetAuthViewModel() }
    func getEmergencyViewModel() -> EmergencyViewModel? { safeGetEmergencyViewModel() }
    func getProfileViewModel() -> ProfileViewModel? { safeGetProfileViewModel() }
    func getHistoryViewModel() -> HistoryViewModel? { safeGetHistoryViewModel() }
    func getProfessionalListViewModel() -> ProfessionalListViewModel? { safeGetProfessionalListViewModel() }
    func getProfessionalDetailViewModel() -> ProfessionalDetailViewModel? { safeGetProfessionalDetailViewModel() }
    func getHomeViewModel() -> HomeViewModel? { safeGetHomeViewModel() }
    func getMedicalProfileViewModel() -> MedicalProfileViewModel? { safeGetMedicalProfileViewModel() }
    func getCheckInViewModel() -> CheckInViewModel? { safeGetCheckInViewModel() }


    func getChatViewModel(emergencyId: String) -> ChatViewModel? {
        guard safeGetAuthViewModel() != nil else {
            FileLogger.shared.write(level: "ERROR", tag: "ViewModelProvider", message: "getChatViewModel: Koin não disponível")
            return nil
        }
        do {
            let chatRepo = try KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as? ChatRepository
            let authRepo = try KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as? AuthRepository
            let emergencyRepo = try KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as? EmergencyRepository
            guard let cr = chatRepo, let ar = authRepo, let er = emergencyRepo else {
                FileLogger.shared.write(level: "ERROR", tag: "ViewModelProvider", message: "getChatViewModel: cast repos falhou")
                return nil
            }
            return ChatViewModel(emergencyId: emergencyId, chatRepository: cr, authRepository: ar, emergencyRepository: er)
        } catch {
            FileLogger.shared.write(level: "ERROR", tag: "ViewModelProvider", message: "getChatViewModel: \(error.localizedDescription)")
            return nil
        }
    }
}

