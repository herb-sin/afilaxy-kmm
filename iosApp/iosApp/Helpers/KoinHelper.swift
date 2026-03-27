import Foundation
import shared

// Safe getters para ViewModels - evita crashes no boundary Kotlin/Swift
func safeGetLoginViewModel() -> LoginViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! LoginViewModel
}

func safeGetAuthViewModel() -> AuthViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! AuthViewModel
}

func safeGetEmergencyViewModel() -> EmergencyViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! EmergencyViewModel
}

func safeGetProfileViewModel() -> ProfileViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfileViewModel
}

func safeGetHistoryViewModel() -> HistoryViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! HistoryViewModel
}

func safeGetProfessionalListViewModel() -> ProfessionalListViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfessionalListViewModel
}

func safeGetProfessionalDetailViewModel() -> ProfessionalDetailViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfessionalDetailViewModel
}

func safeGetHomeViewModel() -> HomeViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! HomeViewModel
}

func safeGetMedicalProfileViewModel() -> MedicalProfileViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! MedicalProfileViewModel
}

func safeGetProfessionalDashboardViewModel() -> ProfessionalDashboardViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfessionalDashboardViewModel
}