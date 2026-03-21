import Foundation
import shared

class ViewModelProvider {
    static let shared = ViewModelProvider()

    // Usa funções safeGet* marcadas com @Throws em vez de getKoin().get()
    // para que exceções Kotlin não cruzem o boundary e causem abort().
    func getLoginViewModel() -> LoginViewModel {
        do { return try KoinHelperKt.safeGetLoginViewModel() }
        catch { fatalError("❌ Koin: LoginViewModel failed — \(error)") }
    }
    func getAuthViewModel() -> AuthViewModel {
        do { return try KoinHelperKt.safeGetAuthViewModel() }
        catch { fatalError("❌ Koin: AuthViewModel failed — \(error)") }
    }
    func getEmergencyViewModel() -> EmergencyViewModel {
        do { return try KoinHelperKt.safeGetEmergencyViewModel() }
        catch { fatalError("❌ Koin: EmergencyViewModel failed — \(error)") }
    }
    func getProfileViewModel() -> ProfileViewModel {
        do { return try KoinHelperKt.safeGetProfileViewModel() }
        catch { fatalError("❌ Koin: ProfileViewModel failed — \(error)") }
    }
    func getHistoryViewModel() -> HistoryViewModel {
        do { return try KoinHelperKt.safeGetHistoryViewModel() }
        catch { fatalError("❌ Koin: HistoryViewModel failed — \(error)") }
    }
    func getProfessionalListViewModel() -> ProfessionalListViewModel {
        do { return try KoinHelperKt.safeGetProfessionalListViewModel() }
        catch { fatalError("❌ Koin: ProfessionalListViewModel failed — \(error)") }
    }

    func getProfessionalDetailViewModel() -> ProfessionalDetailViewModel {
        do { return try KoinHelperKt.safeGetProfessionalDetailViewModel() }
        catch { fatalError("❌ Koin: ProfessionalDetailViewModel failed — \(error)") }
    }

    func getChatViewModel(emergencyId: String) -> ChatViewModel {
        let chatRepository: ChatRepository = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ChatRepository
        let authRepository: AuthRepository = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! AuthRepository
        return ChatViewModel(emergencyId: emergencyId, chatRepository: chatRepository, authRepository: authRepository)
    }
}
