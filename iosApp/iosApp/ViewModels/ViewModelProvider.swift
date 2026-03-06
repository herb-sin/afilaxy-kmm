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
        return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ChatViewModel
    }
    
    func getAuthViewModel() -> AuthViewModel {
        return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! AuthViewModel
    }
    
    func getProfileViewModel() -> ProfileViewModel {
        return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfileViewModel
    }
}
