package com.afilaxy.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afilaxy.app.security.RateLimitManager
import com.afilaxy.app.security.RateLimitResult
import com.afilaxy.presentation.login.LoginViewModel as SharedLoginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoginViewModelWrapper : ViewModel(), KoinComponent {
    private val sharedViewModel: SharedLoginViewModel by inject()
    
    val state: StateFlow<com.afilaxy.presentation.login.LoginState> = sharedViewModel.state
    
    private val _rateLimitState = MutableStateFlow<String?>(null)
    val rateLimitState: StateFlow<String?> = _rateLimitState.asStateFlow()
    
    fun onEmailChange(email: String) {
        sharedViewModel.onEmailChange(email)
        _rateLimitState.value = null
    }
    
    fun onPasswordChange(password: String) {
        sharedViewModel.onPasswordChange(password)
        _rateLimitState.value = null
    }
    
    fun onLoginClick() {
        viewModelScope.launch {
            val email = state.value.email
            
            when (val result = RateLimitManager.loginLimiter.checkLimit(email)) {
                is RateLimitResult.Allowed -> {
                    sharedViewModel.onLoginClick()
                }
                is RateLimitResult.Limited -> {
                    val seconds = (result.waitTimeMillis / 1000).toInt()
                    _rateLimitState.value = "Muitas tentativas. Aguarde $seconds segundos."
                }
            }
        }
    }
    
    fun clearError() {
        sharedViewModel.clearError()
        _rateLimitState.value = null
    }
}
