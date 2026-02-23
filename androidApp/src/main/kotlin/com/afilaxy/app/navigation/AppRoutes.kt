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
    const val EVENTO_DETAIL = "evento_detail/{eventoId}"
    const val PRODUTO_DETAIL = "produto_detail/{produtoId}"
    const val NAVIGATION = "navigation/{lat}/{lng}/{name}"

    fun emergencyResponse(emergencyId: String) = "emergency_response/$emergencyId"
    fun chat(emergencyId: String) = "chat/$emergencyId"
    fun eventoDetail(eventoId: String) = "evento_detail/$eventoId"
    fun produtoDetail(produtoId: String) = "produto_detail/$produtoId"
    fun navigation(lat: Double, lng: Double, name: String) =
        "navigation/$lat/$lng/${android.net.Uri.encode(name)}"
}
