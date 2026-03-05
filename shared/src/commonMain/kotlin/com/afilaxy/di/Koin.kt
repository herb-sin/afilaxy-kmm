package com.afilaxy.di

import com.afilaxy.data.repository.AuthRepositoryImpl
import com.afilaxy.data.repository.ChatRepositoryImpl
import com.afilaxy.data.repository.EmergencyRepositoryImpl
import com.afilaxy.data.repository.HealthProfessionalRepositoryImpl
import com.afilaxy.data.repository.LocationRepositoryImpl
import com.afilaxy.data.repository.NotificationRepositoryImpl
import com.afilaxy.data.repository.PreferencesRepositoryImpl
import com.afilaxy.data.repository.ProfileRepositoryImpl
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.ChatRepository
import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.domain.repository.HealthProfessionalRepository
import com.afilaxy.domain.repository.LocationRepository
import com.afilaxy.domain.repository.NotificationRepository
import com.afilaxy.domain.repository.PreferencesRepository
import com.afilaxy.domain.repository.ProfileRepository
import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.chat.ChatViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.presentation.history.HistoryViewModel
import com.afilaxy.presentation.login.LoginViewModel
import com.afilaxy.presentation.professional.ProfessionalListViewModel
import com.afilaxy.presentation.profile.ProfileViewModel
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
    single<EmergencyRepository> { EmergencyRepositoryImpl(get(), get(), get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get()) }
    single<NotificationRepository> { NotificationRepositoryImpl(get()) }
    single<HealthProfessionalRepository> { HealthProfessionalRepositoryImpl(get()) }
    // LocationRepository é injetado no platformModule()
    
    // ViewModels
    factory { AuthViewModel(get()) }
    factory { LoginViewModel(get()) }
    factory { (emergencyId: String) -> ChatViewModel(emergencyId, get(), get()) }
    factory { EmergencyViewModel(get(), get(), get()) }
    factory { ProfileViewModel(get(), get()) }
    factory { HistoryViewModel(get(), get()) }
    factory { ProfessionalListViewModel(get()) }
}

/**
 * Platform-specific modules (Settings)
 * Android e iOS precisam fornecer Settings
 */
expect fun platformModule(): Module
