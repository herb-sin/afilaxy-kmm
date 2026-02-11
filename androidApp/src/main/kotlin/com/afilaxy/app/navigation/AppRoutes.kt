package com.afilaxy.app.navigation

object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val EMERGENCY = "emergency"
    const val EMERGENCY_REQUEST = "emergency_request"
    const val EMERGENCY_RESPONSE = "emergency_response/{emergencyId}"
    const val CHAT = "chat/{emergencyId}"
    const val PROFILE = "profile"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val TERMS = "terms"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
    const val COMMUNITY = "community"
    const val AUTOCUIDADO = "autocuidado"
    const val MAP = "map"
    const val NOTIFICATIONS = "notifications"
    const val HELP = "help"
    
    fun emergencyResponse(emergencyId: String) = "emergency_response/$emergencyId"
    fun chat(emergencyId: String) = "chat/$emergencyId"
}
