import Foundation
import shared

// MARK: - Koin Safe Getters
// Usa `as?` em vez de `as!` para evitar SIGABRT quando o Koin retorna nil
// ou quando exceções Kotlin/Native cruzam o boundary Swift sem serem capturadas
// (comportamento observado no iOS 26+ com KMM-ViewModel ALPHA-16).

private func koinGet<T: AnyObject>(_ type: T.Type, tag: String) -> T? {
    let result = KoinHelperKt.getKoin().get(qualifier: nil, parameters: nil)
    guard let vm = result as? T else {
        let msg = "KoinHelper: falha ao resolver \(tag) — resultado: \(type(of: result))"
        FileLogger.shared.write(level: "ERROR", tag: "KoinHelper", message: msg)
        return nil
    }
    return vm
}

func safeGetLoginViewModel() -> LoginViewModel? {
    return koinGet(LoginViewModel.self, tag: "LoginViewModel")
}

func safeGetAuthViewModel() -> AuthViewModel? {
    return koinGet(AuthViewModel.self, tag: "AuthViewModel")
}

func safeGetEmergencyViewModel() -> EmergencyViewModel? {
    return koinGet(EmergencyViewModel.self, tag: "EmergencyViewModel")
}

func safeGetProfileViewModel() -> ProfileViewModel? {
    return koinGet(ProfileViewModel.self, tag: "ProfileViewModel")
}

func safeGetHistoryViewModel() -> HistoryViewModel? {
    return koinGet(HistoryViewModel.self, tag: "HistoryViewModel")
}

func safeGetProfessionalListViewModel() -> ProfessionalListViewModel? {
    return koinGet(ProfessionalListViewModel.self, tag: "ProfessionalListViewModel")
}

func safeGetProfessionalDetailViewModel() -> ProfessionalDetailViewModel? {
    return koinGet(ProfessionalDetailViewModel.self, tag: "ProfessionalDetailViewModel")
}

func safeGetHomeViewModel() -> HomeViewModel? {
    return koinGet(HomeViewModel.self, tag: "HomeViewModel")
}

func safeGetMedicalProfileViewModel() -> MedicalProfileViewModel? {
    return koinGet(MedicalProfileViewModel.self, tag: "MedicalProfileViewModel")
}

func safeGetProfessionalDashboardViewModel() -> ProfessionalDashboardViewModel? {
    return koinGet(ProfessionalDashboardViewModel.self, tag: "ProfessionalDashboardViewModel")
}