import Foundation
import shared

class ViewModelProvider {
    static let shared = ViewModelProvider()
    
    func getLoginViewModel() -> LoginViewModel {
        return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! LoginViewModel
    }
    
    func getEmergencyViewModel() -> EmergencyViewModel {
        return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! EmergencyViewModel
    }
    
    func getChatViewModel(emergencyId: String) -> ChatViewModel {
        let chatRepository = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ChatRepository
        let authRepository = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! AuthRepository
        return ChatViewModel(emergencyId: emergencyId, chatRepository: chatRepository, authRepository: authRepository)
    }
    
    func getAuthViewModel() -> AuthViewModel {
        return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! AuthViewModel
    }
    
    func getProfileViewModel() -> ProfileViewModel {
        return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfileViewModel
    }
}
