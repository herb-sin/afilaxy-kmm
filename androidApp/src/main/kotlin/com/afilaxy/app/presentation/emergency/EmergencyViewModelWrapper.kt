package com.afilaxy.app.presentation.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afilaxy.app.security.RateLimitManager
import com.afilaxy.app.security.RateLimitResult
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.presentation.emergency.EmergencyViewModel as SharedEmergencyViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EmergencyViewModelWrapper : ViewModel(), KoinComponent {
    private val sharedViewModel: SharedEmergencyViewModel by inject()
    private val authRepository: AuthRepository by inject()
    
    val state: StateFlow<com.afilaxy.presentation.emergency.EmergencyState> = sharedViewModel.state
    
    private val _rateLimitState = MutableStateFlow<String?>(null)
    val rateLimitState: StateFlow<String?> = _rateLimitState.asStateFlow()
    
    fun onCreateEmergency() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            
            when (val result = RateLimitManager.emergencyLimiter.checkLimit(userId)) {
                is RateLimitResult.Allowed -> {
                    sharedViewModel.onCreateEmergency()
                }
                is RateLimitResult.Limited -> {
                    val minutes = (result.waitTimeMillis / 60_000).toInt()
                    _rateLimitState.value = "Limite atingido. Aguarde $minutes minutos."
                }
            }
        }
    }
    
    fun onAcceptEmergency(emergencyId: String) {
        sharedViewModel.onAcceptEmergency(emergencyId)
    }
    
    fun onResolveEmergency() {
        sharedViewModel.onResolveEmergency()
    }
    
    fun onCancelEmergency() {
        sharedViewModel.onCancelEmergency()
    }
    
    fun onToggleHelperMode(enable: Boolean) {
        sharedViewModel.onToggleHelperMode(enable)
    }
    
    fun clearError() {
        sharedViewModel.clearError()
        _rateLimitState.value = null
    }
}
