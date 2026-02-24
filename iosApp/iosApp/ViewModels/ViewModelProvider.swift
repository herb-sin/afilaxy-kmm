import Foundation
import shared

class ViewModelProvider {
    static let shared = ViewModelProvider()
    
    private let koin = KoinHelperKt.getKoin()
    
    func getLoginViewModel() -> LoginViewModel {
        return koin.get() as! LoginViewModel
    }
    
    func getEmergencyViewModel() -> EmergencyViewModel {
        return koin.get() as! EmergencyViewModel
    }
    
    func getChatViewModel(emergencyId: String) -> ChatViewModel {
        return koin.get(parameters: { _ in
            return Koin_coreParametersHolder(values: [emergencyId])
        }) as! ChatViewModel
    }
    
    func getAuthViewModel() -> AuthViewModel {
        return koin.get() as! AuthViewModel
    }
    
    func getProfileViewModel() -> ProfileViewModel {
        return koin.get() as! ProfileViewModel
    }
}
