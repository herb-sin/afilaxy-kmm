import Foundation
import shared

class ViewModelProvider {
    static let shared = ViewModelProvider()
    
    var homeViewModel: HomeViewModel {
        return getHomeViewModel()
    }
    
    var medicalProfileViewModel: MedicalProfileViewModel {
        return getMedicalProfileViewModel()
    }
    
    var professionalDashboardViewModel: ProfessionalDashboardViewModel {
        return getProfessionalDashboardViewModel()
    }

    // Usa funções safeGet* marcadas com @Throws em vez de getKoin().get()
    // para que exceções Kotlin não cruzem o boundary e causem abort().
    func getLoginViewModel() -> LoginViewModel {
        return safeGetLoginViewModel()
    }
    func getAuthViewModel() -> AuthViewModel {
        return safeGetAuthViewModel()
    }
    func getEmergencyViewModel() -> EmergencyViewModel {
        return safeGetEmergencyViewModel()
    }
    func getProfileViewModel() -> ProfileViewModel {
        return safeGetProfileViewModel()
    }
    func getHistoryViewModel() -> HistoryViewModel {
        return safeGetHistoryViewModel()
    }
    func getProfessionalListViewModel() -> ProfessionalListViewModel {
        return safeGetProfessionalListViewModel()
    }

    func getProfessionalDetailViewModel() -> ProfessionalDetailViewModel {
        return safeGetProfessionalDetailViewModel()
    }

    func getHomeViewModel() -> HomeViewModel {
        return safeGetHomeViewModel()
    }
    
    func getMedicalProfileViewModel() -> MedicalProfileViewModel {
        return safeGetMedicalProfileViewModel()
    }
    
    func getProfessionalDashboardViewModel() -> ProfessionalDashboardViewModel {
        return safeGetProfessionalDashboardViewModel()
    }

    func getChatViewModel(emergencyId: String) -> ChatViewModel {
        let chatRepository: ChatRepository = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ChatRepository
        let authRepository: AuthRepository = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! AuthRepository
        return ChatViewModel(emergencyId: emergencyId, chatRepository: chatRepository, authRepository: authRepository)
    }
}
