import Foundation
import shared

class ViewModelProvider {
    static let shared = ViewModelProvider()
    
    private let koin = KoinHelperKt.getKoin()
    
    func getLoginViewModel() -> LoginViewModel {
        return koin.get(objCClass: LoginViewModel.self) as! LoginViewModel
    }
    
    func getEmergencyViewModel() -> EmergencyViewModel {
        return koin.get(objCClass: EmergencyViewModel.self) as! EmergencyViewModel
    }
    
    func getChatViewModel(emergencyId: String) -> ChatViewModel {
        return koin.get(objCClass: ChatViewModel.self, parameter: emergencyId) as! ChatViewModel
    }
    
    func getAuthViewModel() -> AuthViewModel {
        return koin.get(objCClass: AuthViewModel.self) as! AuthViewModel
    }
    
    func getProfileViewModel() -> ProfileViewModel {
        return koin.get(objCClass: ProfileViewModel.self) as! ProfileViewModel
    }
}
