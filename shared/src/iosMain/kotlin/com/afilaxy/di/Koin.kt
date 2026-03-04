package com.afilaxy.di

import com.afilaxy.data.repository.LocationRepositoryImpl
import com.afilaxy.domain.repository.LocationRepository
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual fun platformModule() = module {
    single<Settings> {
        val context: Context = get()
        val sharedPreferences = context.getSharedPreferences("afilaxy_prefs", Context.MODE_PRIVATE)
        SharedPreferencesSettings(sharedPreferences)
    }
    
    single<LocationRepository> {
        LocationRepositoryImpl(get())
    }
}
