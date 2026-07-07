package com.afilaxy.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.afilaxy.data.repository.AndroidHealthRepository
import com.afilaxy.data.repository.LocationRepositoryImpl
import com.afilaxy.domain.repository.HealthRepository
import com.afilaxy.domain.repository.LocationRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.dsl.module
import android.content.Context

actual fun platformModule() = module {
    single<Settings> {
        val context: Context = get()
        val prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "afilaxy_prefs_enc",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback para dispositivos com keystore corrompido ou indisponível
            context.getSharedPreferences("afilaxy_prefs", Context.MODE_PRIVATE)
        }
        SharedPreferencesSettings(prefs)
    }
    
    single<LocationRepository> {
        LocationRepositoryImpl(get())
    }

    single<HealthRepository> {
        AndroidHealthRepository(get())
    }
}
