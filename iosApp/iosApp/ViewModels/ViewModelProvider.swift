import Foundation
import shared

class ViewModelProvider {
    static let shared = ViewModelProvider()
    
    private let koin = KoinHelperKt.getKoin()
    
    func getLoginViewModel() -> LoginViewModel {
        return koin.get(qualifier: nil, parameters: nil) as! LoginViewModel
    }
    
    func getEmergencyViewModel() -> EmergencyViewModel {
        return koin.get(qualifier: nil, parameters: nil) as! EmergencyViewModel
    }
    
    func getChatViewModel(emergencyId: String) -> ChatViewModel {
        return koin.get(qualifier: nil, parameters: {
            return Koin_coreParametersHolder()
        }) as! ChatViewModel
    }
    
    func getAuthViewModel() -> AuthViewModel {
        return koin.get(qualifier: nil, parameters: nil) as! AuthViewModel
    }
    
    func getProfileViewModel() -> ProfileViewModel {
        return koin.get(qualifier: nil, parameters: nil) as! ProfileViewModel
    }
}
