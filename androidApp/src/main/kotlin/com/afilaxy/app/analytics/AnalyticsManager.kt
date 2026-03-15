package com.afilaxy.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsManager(context: Context) {
    
    private val analytics = FirebaseAnalytics.getInstance(context)

    private fun sanitizeName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_]"), "_").take(40)

    private fun sanitizeValue(value: Any): Any = when (value) {
        is String -> value.replace(Regex("[\\x00-\\x1F\\x7F]"), "").take(100)
        else -> value
    }

    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
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
