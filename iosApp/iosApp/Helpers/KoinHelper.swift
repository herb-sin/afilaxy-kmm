import Foundation
import shared

// Safe getters para ViewModels - evita crashes no boundary Kotlin/Swift
@_cdecl("safeGetLoginViewModel")
public func safeGetLoginViewModel() throws -> LoginViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! LoginViewModel
}

@_cdecl("safeGetAuthViewModel") 
public func safeGetAuthViewModel() throws -> AuthViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! AuthViewModel
}

@_cdecl("safeGetEmergencyViewModel")
public func safeGetEmergencyViewModel() throws -> EmergencyViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! EmergencyViewModel
}

@_cdecl("safeGetProfileViewModel")
public func safeGetProfileViewModel() throws -> ProfileViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfileViewModel
}

@_cdecl("safeGetHistoryViewModel")
public func safeGetHistoryViewModel() throws -> HistoryViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! HistoryViewModel
}

@_cdecl("safeGetProfessionalListViewModel")
public func safeGetProfessionalListViewModel() throws -> ProfessionalListViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfessionalListViewModel
}

@_cdecl("safeGetProfessionalDetailViewModel")
public func safeGetProfessionalDetailViewModel() throws -> ProfessionalDetailViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfessionalDetailViewModel
}

@_cdecl("safeGetHomeViewModel")
public func safeGetHomeViewModel() throws -> HomeViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! HomeViewModel
}

@_cdecl("safeGetMedicalProfileViewModel")
public func safeGetMedicalProfileViewModel() throws -> MedicalProfileViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! MedicalProfileViewModel
}

@_cdecl("safeGetProfessionalDashboardViewModel")
public func safeGetProfessionalDashboardViewModel() throws -> ProfessionalDashboardViewModel {
    return KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil) as! ProfessionalDashboardViewModel
}