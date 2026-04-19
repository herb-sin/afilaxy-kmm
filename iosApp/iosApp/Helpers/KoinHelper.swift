import Foundation
import shared

// MARK: - Koin Safe Getters
//
// REGRA DE OURO: NUNCA chame KoinHelperKt.getKoin().get() diretamente do Swift.
// O Koin.get() é Kotlin puro sem @Throws — qualquer exceção (NoBeanDefFoundException,
// IllegalStateException, etc.) se torna abort() ao cruzar a boundary Kotlin/Native → Swift.
//
// As funções abaixo delegam para os wrappers Kotlin @Throws(..) que convertem
// Kotlin exceptions em NSError catchable pelo Swift try/catch.

private func koinGet<T>(_ kotlinGetter: () throws -> T, tag: String) -> T? {
    do {
        return try kotlinGetter()
    } catch {
        FileLogger.shared.write(
            level: "ERROR",
            tag: "KoinHelper",
            message: "\(tag) falhou: \(error.localizedDescription)"
        )
        return nil
    }
}

func safeGetLoginViewModel() -> LoginViewModel? {
    koinGet({ try KoinHelperKt.safeGetLoginViewModel() }, tag: "LoginViewModel")
}

func safeGetAuthViewModel() -> AuthViewModel? {
    koinGet({ try KoinHelperKt.safeGetAuthViewModel() }, tag: "AuthViewModel")
}

func safeGetEmergencyViewModel() -> EmergencyViewModel? {
    koinGet({ try KoinHelperKt.safeGetEmergencyViewModel() }, tag: "EmergencyViewModel")
}

func safeGetProfileViewModel() -> ProfileViewModel? {
    koinGet({ try KoinHelperKt.safeGetProfileViewModel() }, tag: "ProfileViewModel")
}

func safeGetHistoryViewModel() -> HistoryViewModel? {
    koinGet({ try KoinHelperKt.safeGetHistoryViewModel() }, tag: "HistoryViewModel")
}

func safeGetProfessionalListViewModel() -> ProfessionalListViewModel? {
    koinGet({ try KoinHelperKt.safeGetProfessionalListViewModel() }, tag: "ProfessionalListViewModel")
}

func safeGetProfessionalDetailViewModel() -> ProfessionalDetailViewModel? {
    koinGet({ try KoinHelperKt.safeGetProfessionalDetailViewModel() }, tag: "ProfessionalDetailViewModel")
}

func safeGetHomeViewModel() -> HomeViewModel? {
    koinGet({ try KoinHelperKt.safeGetHomeViewModel() }, tag: "HomeViewModel")
}

func safeGetMedicalProfileViewModel() -> MedicalProfileViewModel? {
    koinGet({ try KoinHelperKt.safeGetMedicalProfileViewModel() }, tag: "MedicalProfileViewModel")
}

func safeGetRiskViewModel() -> RiskViewModel? {
    koinGet({ try KoinHelperKt.safeGetRiskViewModel() }, tag: "RiskViewModel")
}
