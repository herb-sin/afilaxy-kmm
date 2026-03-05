package com.afilaxy.di

import android.content.Context
import com.afilaxy.data.repository.LocationRepositoryImpl
import com.afilaxy.domain.repository.LocationRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.dsl.module

actual fun platformModule() = module {
    single<Settings> {
        SharedPreferencesSettings(
            get<Context>().getSharedPreferences("afilaxy_prefs", Context.MODE_PRIVATE)
        )
    }
    
    single<LocationRepository> {
        LocationRepositoryImpl(get())
    }
}
