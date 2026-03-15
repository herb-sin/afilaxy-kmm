package com.afilaxy.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afilaxy.app.security.RateLimitManager
import com.afilaxy.app.security.RateLimitResult
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.presentation.chat.ChatViewModel as SharedChatViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class ChatViewModelWrapper(private val emergencyId: String) : ViewModel(), KoinComponent {
    private val sharedViewModel: SharedChatViewModel by inject { parametersOf(emergencyId) }
    private val authRepository: AuthRepository by inject()
    
    val state: StateFlow<com.afilaxy.presentation.chat.ChatState> = sharedViewModel.state
    
    private val _rateLimitState = MutableStateFlow<String?>(null)
    val rateLimitState: StateFlow<String?> = _rateLimitState.asStateFlow()
    
    fun onMessageChange(message: String) {
        sharedViewModel.onMessageChange(message)
    }
    
    fun sendMessage(message: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            
            when (val result = RateLimitManager.chatLimiter.checkLimit(userId)) {
                is RateLimitResult.Allowed -> {
                    sharedViewModel.sendMessage(message)
                }
                is RateLimitResult.Limited -> {
                    val seconds = (result.waitTimeMillis / 1000).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                    _rateLimitState.value = "Muitas mensagens. Aguarde $seconds segundos."
                }
            }
        }
    }
    
    fun clearError() {
        sharedViewModel.clearError()
        _rateLimitState.value = null
    }
}
