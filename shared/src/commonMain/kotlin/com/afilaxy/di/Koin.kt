package com.afilaxy.di

import com.afilaxy.data.repository.*
import com.afilaxy.domain.repository.*
import com.afilaxy.domain.usecase.*
import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.chat.ChatViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.presentation.history.HistoryViewModel
import com.afilaxy.presentation.login.LoginViewModel
import com.afilaxy.presentation.professional.CrmLookupViewModel
import com.afilaxy.presentation.professional.ProfessionalDetailViewModel
import com.afilaxy.presentation.professional.ProfessionalListViewModel
import com.afilaxy.presentation.profile.ProfileViewModel
import com.afilaxy.presentation.home.HomeViewModel
import com.afilaxy.presentation.medical.MedicalProfileViewModel
import com.russhwolf.settings.Settings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shared Koin module - código comum entre Android e iOS
 */
fun sharedModule(): Module = module {
    
    // Firebase
    single { Firebase.auth }
    single { Firebase.firestore }
    
    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<ChatRepository> { ChatRepositoryImpl(get()) }
    single<EmergencyRepository> { EmergencyRepositoryImpl(get(), get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<HealthProfessionalRepository> { HealthProfessionalRepositoryImpl(get(), get()) }
    
    // New repositories for expanded features
    single<MedicalRepository> { MedicalRepositoryImpl(get()) }
    
    // LocationRepository é injetado no platformModule()
    
    // Use Cases
    factory { CreateEmergencyUseCase(get()) }
    factory { FindHelpersUseCase(get()) }
    factory { SendChatMessageUseCase(get()) }

    // ViewModels
    factory { AuthViewModel(get()) }
    factory { LoginViewModel(get()) }
    factory { (emergencyId: String) -> ChatViewModel(emergencyId, get(), get()) }
    single { EmergencyViewModel(get(), get(), get(), get()) }
    factory { ProfileViewModel(get(), get()) }
    factory { HistoryViewModel(get(), get()) }
    factory { ProfessionalListViewModel(get()) }
    factory { ProfessionalDetailViewModel(get()) }
    factory { CrmLookupViewModel() }
    
    // New ViewModels for expanded features
    factory { HomeViewModel(get(), get(), get()) }
    factory { MedicalProfileViewModel(get(), "default_user") }
}

/**
 * Platform-specific modules (Settings)
 * Android e iOS precisam fornecer Settings
 */
expect fun platformModule(): Module
