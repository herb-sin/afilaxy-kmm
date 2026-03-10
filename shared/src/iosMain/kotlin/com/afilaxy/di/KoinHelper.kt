package com.afilaxy.di

import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.presentation.history.HistoryViewModel
import com.afilaxy.presentation.login.LoginViewModel
import com.afilaxy.presentation.professional.ProfessionalListViewModel
import com.afilaxy.presentation.profile.ProfileViewModel
import org.koin.core.Koin
import org.koin.core.context.startKoin

private var koinInstance: Koin? = null

fun doInitKoin() {
    try {
        koinInstance = startKoin {
            modules(sharedModule(), platformModule())
        }.koin
        println("✅ Koin initialized successfully")
    } catch (e: Exception) {
        println("❌ Koin initialization failed: ${e.message}")
        e.printStackTrace()
        throw e
    }
}

fun getKoin(): Koin = koinInstance ?: error("Koin not initialized. Call doInitKoin() first.")

// Getters marcados com @Throws para que o Swift possa usar try/catch.
// Kotlin/Native converte exceções não marcadas em abort() ao cruzar o boundary Swift/Kotlin.

@Throws(Exception::class)
fun safeGetAuthViewModel(): AuthViewModel {
    println("🔍 safeGetAuthViewModel")
    return getKoin().get<AuthViewModel>().also { println("✅ AuthViewModel OK") }
}

@Throws(Exception::class)
fun safeGetLoginViewModel(): LoginViewModel {
    println("🔍 safeGetLoginViewModel")
    return getKoin().get<LoginViewModel>().also { println("✅ LoginViewModel OK") }
}

@Throws(Exception::class)
fun safeGetEmergencyViewModel(): EmergencyViewModel {
    println("🔍 safeGetEmergencyViewModel")
    return getKoin().get<EmergencyViewModel>().also { println("✅ EmergencyViewModel OK") }
}

@Throws(Exception::class)
fun safeGetProfileViewModel(): ProfileViewModel {
    println("🔍 safeGetProfileViewModel")
    return getKoin().get<ProfileViewModel>().also { println("✅ ProfileViewModel OK") }
}

@Throws(Exception::class)
fun safeGetHistoryViewModel(): HistoryViewModel {
    println("🔍 safeGetHistoryViewModel")
    return getKoin().get<HistoryViewModel>().also { println("✅ HistoryViewModel OK") }
}

@Throws(Exception::class)
fun safeGetProfessionalListViewModel(): ProfessionalListViewModel {
    println("🔍 safeGetProfessionalListViewModel")
    return getKoin().get<ProfessionalListViewModel>().also { println("✅ ProfessionalListViewModel OK") }
}
