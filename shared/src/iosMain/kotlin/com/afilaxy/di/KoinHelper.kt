package com.afilaxy.di

import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.checkin.CheckInViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.presentation.history.HistoryViewModel
import com.afilaxy.presentation.login.LoginViewModel
import com.afilaxy.presentation.professional.ProfessionalListViewModel
import com.afilaxy.presentation.professional.ProfessionalDetailViewModel
import com.afilaxy.presentation.profile.ProfileViewModel
import com.afilaxy.presentation.home.HomeViewModel
import com.afilaxy.presentation.medical.MedicalProfileViewModel
import com.afilaxy.presentation.risk.RiskViewModel

import com.afilaxy.util.Logger
import org.koin.core.Koin
import org.koin.core.context.startKoin

private const val TAG = "KoinHelper"
private var koinInstance: Koin? = null

@Throws(Exception::class)
fun doInitKoin() {
    try {
        koinInstance = startKoin {
            modules(sharedModule(), platformModule())
        }.koin
        Logger.i(TAG, "Koin initialized successfully")
    } catch (e: Exception) {
        Logger.e(TAG, "Koin initialization failed: ${e.message}")
        // Não re-throw: permite que Swift capture via @Throws e exiba a tela de erro
        // em vez de abort(). koinInstance permanece null — os @Throws getters
        // retornarão erro via Swift try/catch normalmente.
        throw e
    }
}

fun getKoin(): Koin = koinInstance ?: error("Koin not initialized. Call doInitKoin() first.")

// Getters marcados com @Throws para que o Swift possa usar try/catch.
// Kotlin/Native converte exceções não marcadas em abort() ao cruzar o boundary Swift/Kotlin.

@Throws(Exception::class)
fun safeGetAuthViewModel(): AuthViewModel {
    Logger.d(TAG, "safeGetAuthViewModel")
    return getKoin().get<AuthViewModel>().also { Logger.d(TAG, "AuthViewModel OK") }
}

@Throws(Exception::class)
fun safeGetLoginViewModel(): LoginViewModel {
    Logger.d(TAG, "safeGetLoginViewModel")
    return getKoin().get<LoginViewModel>().also { Logger.d(TAG, "LoginViewModel OK") }
}

@Throws(Exception::class)
fun safeGetEmergencyViewModel(): EmergencyViewModel {
    Logger.d(TAG, "safeGetEmergencyViewModel")
    return getKoin().get<EmergencyViewModel>().also { Logger.d(TAG, "EmergencyViewModel OK") }
}

@Throws(Exception::class)
fun safeGetProfileViewModel(): ProfileViewModel {
    Logger.d(TAG, "safeGetProfileViewModel")
    return getKoin().get<ProfileViewModel>().also { Logger.d(TAG, "ProfileViewModel OK") }
}

@Throws(Exception::class)
fun safeGetHistoryViewModel(): HistoryViewModel {
    Logger.d(TAG, "safeGetHistoryViewModel")
    return getKoin().get<HistoryViewModel>().also { Logger.d(TAG, "HistoryViewModel OK") }
}

@Throws(Exception::class)
fun safeGetProfessionalListViewModel(): ProfessionalListViewModel {
    Logger.d(TAG, "safeGetProfessionalListViewModel")
    return getKoin().get<ProfessionalListViewModel>().also { Logger.d(TAG, "ProfessionalListViewModel OK") }
}

@Throws(Exception::class)
fun safeGetProfessionalDetailViewModel(): ProfessionalDetailViewModel {
    Logger.d(TAG, "safeGetProfessionalDetailViewModel")
    return getKoin().get<ProfessionalDetailViewModel>().also { Logger.d(TAG, "ProfessionalDetailViewModel OK") }
}

@Throws(Exception::class)
fun safeGetHomeViewModel(): HomeViewModel {
    Logger.d(TAG, "safeGetHomeViewModel")
    return getKoin().get<HomeViewModel>().also { Logger.d(TAG, "HomeViewModel OK") }
}

@Throws(Exception::class)
fun safeGetMedicalProfileViewModel(): MedicalProfileViewModel {
    Logger.d(TAG, "safeGetMedicalProfileViewModel")
    return getKoin().get<MedicalProfileViewModel>().also { Logger.d(TAG, "MedicalProfileViewModel OK") }
}

@Throws(Exception::class)
fun safeGetRiskViewModel(): RiskViewModel {
    Logger.d(TAG, "safeGetRiskViewModel")
    return getKoin().get<RiskViewModel>().also { Logger.d(TAG, "RiskViewModel OK") }
}

@Throws(Exception::class)
fun safeGetCheckInViewModel(): CheckInViewModel {
    Logger.d(TAG, "safeGetCheckInViewModel")
    return getKoin().get<CheckInViewModel>().also { Logger.d(TAG, "CheckInViewModel OK") }
}
