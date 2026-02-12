package com.afilaxy.presentation.chat

import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.ChatRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel compartilhado para Chat
 */
class ChatViewModel(
    private val emergencyId: String,
    private val chatRepository: ChatRepository,
    private val authRepository: com.afilaxy.domain.repository.AuthRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(ChatState(
        emergencyId = emergencyId,
        currentUserId = authRepository.getCurrentUserId()
    ))
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    init {
        observeMessages()
    }
    
    private fun observeMessages() {
        viewModelScope.coroutineScope.launch {
            chatRepository.getMessages(emergencyId).collect { messages ->
                _state.update { it.copy(messages = messages, isLoading = false) }
            }
        }
    }
    
    fun onMessageChange(message: String) {
        _state.update { it.copy(currentMessage = message) }
    }
    
    @OptIn(ExperimentalUuidApi::class)
    fun sendMessage(message: String) {
        val messageText = message.trim()
        
        if (messageText.isBlank()) return
        
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                }
                return@launch
            }
            
            val chatMessage = ChatMessage(
                id = Uuid.random().toString(),
                emergencyId = emergencyId,
                senderId = currentUser.uid,
                senderName = currentUser.name ?: "Usuário",
                message = messageText,
                timestamp = getCurrentTimeMillis(),
                isFromHelper = false
            )
            
            chatRepository.sendMessage(chatMessage)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Erro ao enviar mensagem"
                        )
                    }
                }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
