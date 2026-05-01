package com.afilaxy.presentation.chat

import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.ChatRepository
import com.afilaxy.domain.repository.EmergencyRepository
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
    private val authRepository: AuthRepository,
    private val emergencyRepository: EmergencyRepository
) : KMMViewModel() {

    private val _state = MutableStateFlow(ChatState(
        emergencyId = emergencyId,
        currentUserId = authRepository.getCurrentUserId()
    ))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        observeMessages()
        loadParticipants()
        observeEmergencyStatus()
    }

    private fun observeMessages() {
        viewModelScope.coroutineScope.launch {
            chatRepository.getMessages(emergencyId).collect { messages ->
                // Sort explicitly on the client: Firestore orderBy can behave differently
                // when mixing stored types (Double from Android, Timestamp from iOS native).
                _state.update { it.copy(messages = messages.sortedBy { it.timestamp }, isLoading = false) }
            }
        }
    }

    private fun loadParticipants() {
        viewModelScope.coroutineScope.launch {
            emergencyRepository.getEmergencyParticipants(emergencyId).onSuccess { (requesterId, helperId) ->
                val currentUserId = _state.value.currentUserId
                val isRequester = currentUserId == requesterId
                // Determine who to review: the other party
                val reviewedId = if (isRequester) helperId else requesterId
                _state.update { it.copy(reviewedId = reviewedId, isRequester = isRequester) }
            }
        }
    }

    private fun observeEmergencyStatus() {
        viewModelScope.coroutineScope.launch {
            emergencyRepository.observeEmergencyStatus(emergencyId).collect { status ->
                if (status == "resolved" || status == "finished") {
                    _state.update { it.copy(isResolvedByOther = true) }
                }
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
                _state.update { it.copy(isLoading = false, error = "Usuário não autenticado") }
                return@launch
            }

            val currentUserId = currentUser.uid
            val isRequester = _state.value.isRequester
            // Determine sender role: helpers have isFromHelper=true
            val chatMessage = ChatMessage(
                id = Uuid.random().toString(),
                emergencyId = emergencyId,
                senderId = currentUserId,
                senderName = currentUser.name ?: "Usuário",
                message = messageText,
                timestamp = getCurrentTimeMillis(),
                isFromHelper = !isRequester
            )

            chatRepository.sendMessage(chatMessage)
                .onSuccess { _state.update { it.copy(isLoading = false) } }
                .onFailure { exception ->
                    _state.update {
                        it.copy(isLoading = false, error = exception.message ?: "Erro ao enviar mensagem")
                    }
                }
        }
    }

    fun onSamuCalled() {
        viewModelScope.coroutineScope.launch {
            emergencyRepository.updateSamuCalled(emergencyId)
        }
    }

    fun submitReview(rating: Int, comment: String?) {
        val reviewedId = _state.value.reviewedId ?: return
        viewModelScope.coroutineScope.launch {
            emergencyRepository.submitReview(emergencyId, reviewedId, rating, comment)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
