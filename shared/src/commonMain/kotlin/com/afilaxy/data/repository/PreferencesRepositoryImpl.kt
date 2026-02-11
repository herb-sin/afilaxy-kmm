package com.afilaxy.data.repository

import com.afilaxy.domain.repository.PreferencesRepository
import com.russhwolf.settings.Settings

class PreferencesRepositoryImpl(
    private val settings: Settings
) : PreferencesRepository {
    
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return settings.getBoolean(key, defaultValue)
    }
    
    override fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
    }
    
    override fun getString(key: String, defaultValue: String?): String? {
        return settings.getStringOrNull(key) ?: defaultValue
    }
    
    override fun putString(key: String, value: String) {
        settings.putString(key, value)
    }
}
