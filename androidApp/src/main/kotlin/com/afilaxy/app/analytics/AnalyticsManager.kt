package com.afilaxy.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsManager(context: Context) {
    
    private val analytics = FirebaseAnalytics.getInstance(context)
    
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
            }
        }
        analytics.logEvent(eventName, bundle)
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
