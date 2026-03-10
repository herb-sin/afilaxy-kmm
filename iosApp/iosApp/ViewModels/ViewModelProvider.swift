import Foundation
import shared

class ViewModelProvider {
    static let shared = ViewModelProvider()
    
    private func get<T: AnyObject>() -> T {
        return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! T
    }
    
    func getLoginViewModel() -> LoginViewModel { get() }
    func getAuthViewModel() -> AuthViewModel { get() }
    func getEmergencyViewModel() -> EmergencyViewModel { get() }
    func getProfileViewModel() -> ProfileViewModel { get() }
    func getHistoryViewModel() -> HistoryViewModel { get() }
    func getProfessionalListViewModel() -> ProfessionalListViewModel { get() }
    
    func getChatViewModel(emergencyId: String) -> ChatViewModel {
        let chatRepository: ChatRepository = get()
        let authRepository: AuthRepository = get()
        return ChatViewModel(emergencyId: emergencyId, chatRepository: chatRepository, authRepository: authRepository)
    }
}
