package com.afilaxy.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.russhwolf.settings.Settings

class AnalyticsManager(context: Context, private val settings: Settings) {

    private companion object {
        const val MAX_EVENT_NAME_LENGTH = 40
        const val MAX_VALUE_STRING_LENGTH = 100
        const val KEY_ANALYTICS_CONSENT = "analytics_consent"
    }

    private val analytics = FirebaseAnalytics.getInstance(context)

    init {
        // Desabilita coleta por padrão; só habilita se o usuário consentiu explicitamente.
        val hasConsent = settings.getBoolean(KEY_ANALYTICS_CONSENT, false)
        analytics.setAnalyticsCollectionEnabled(hasConsent)
    }

    fun updateConsent(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    private fun hasConsent(): Boolean = settings.getBoolean(KEY_ANALYTICS_CONSENT, false)

    private fun sanitizeName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_]"), "_").take(MAX_EVENT_NAME_LENGTH)

    private fun sanitizeValue(value: Any): Any = when (value) {
        is String -> value.replace(Regex("[\\x00-\\x1F\\x7F]"), "").take(MAX_VALUE_STRING_LENGTH)
        else -> value
    }

    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        if (!hasConsent()) return
        val safeEvent = sanitizeName(eventName).ifBlank { return }
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            val safeKey = sanitizeName(key).ifBlank { return@forEach }
            when (val safeValue = sanitizeValue(value)) {
                is String -> bundle.putString(safeKey, safeValue)
                is Int -> bundle.putInt(safeKey, safeValue)
                is Long -> bundle.putLong(safeKey, safeValue)
                is Double -> bundle.putDouble(safeKey, safeValue)
                is Boolean -> bundle.putBoolean(safeKey, safeValue)
            }
        }
        analytics.logEvent(safeEvent, bundle)
    }
    
    fun logScreenView(screenName: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName
        ))
    }
    
    fun logEmergencyCreated() {
        logEvent("emergency_created")
    }
    
    fun logEmergencyAccepted() {
        logEvent("emergency_accepted")
    }
    
    fun logHelperModeToggled(enabled: Boolean) {
        logEvent("helper_mode_toggled", mapOf("enabled" to enabled))
    }
    
    fun logLogin(method: String) {
        logEvent(FirebaseAnalytics.Event.LOGIN, mapOf(
            FirebaseAnalytics.Param.METHOD to method
        ))
    }
    
    fun logSignUp(method: String) {
        logEvent(FirebaseAnalytics.Event.SIGN_UP, mapOf(
            FirebaseAnalytics.Param.METHOD to method
        ))
    }
}
