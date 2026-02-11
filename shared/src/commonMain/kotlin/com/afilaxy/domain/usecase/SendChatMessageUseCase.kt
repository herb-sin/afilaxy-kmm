package com.afilaxy.domain.usecase

import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.domain.repository.ChatRepository

class SendChatMessageUseCase(
    private val chatRepository: ChatRepository
) {
    sealed class SendMessageResult {
        object Success : SendMessageResult()
        object MessageEmpty : SendMessageResult()
        data class Error(val message: String) : SendMessageResult()
    }
    
    suspend fun execute(
        emergencyId: String,
        senderId: String,
        senderName: String,
        message: String,
        isFromHelper: Boolean = false
    ): SendMessageResult {
        // Validate message
        if (message.trim().isEmpty()) {
            return SendMessageResult.MessageEmpty
        }
        
        if (message.length > 500) {
            return SendMessageResult.Error("Message too long (max 500 characters)")
        }
        
        // Create message
        val chatMessage = ChatMessage.create(
            emergencyId = emergencyId,
            senderId = senderId,
            senderName = senderName,
            message = message.trim(),
            isFromHelper = isFromHelper
        )
        
        // Send message
        return chatRepository.sendMessage(chatMessage).fold(
            onSuccess = { SendMessageResult.Success },
            onFailure = { exception ->
                SendMessageResult.Error(exception.message ?: "Error sending message")
            }
        )
    }
}
