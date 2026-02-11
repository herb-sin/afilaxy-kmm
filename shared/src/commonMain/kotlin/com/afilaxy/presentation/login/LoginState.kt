package com.afilaxy.presentation.login

import kotlinx.serialization.Serializable

/**
 * Estado da tela de Login
 * Compartilhado entre Android e iOS
 */
@Serializable
data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)
