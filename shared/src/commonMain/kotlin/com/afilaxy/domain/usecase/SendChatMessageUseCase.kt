package com.afilaxy.domain.usecase

import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.domain.repository.ChatRepository

class SendChatMessageUseCase(
    private val chatRepository: ChatRepository
) {
    private companion object {
        const val MAX_MESSAGE_LENGTH = 500
    }

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
        if (message.trim().isEmpty()) {
            return SendMessageResult.MessageEmpty
        }

        if (message.length > MAX_MESSAGE_LENGTH) {
            return SendMessageResult.Error("Message too long (max $MAX_MESSAGE_LENGTH characters)")
        }

        val chatMessage = ChatMessage.create(
            emergencyId = emergencyId,
            senderId = senderId,
            senderName = senderName,
            message = message.trim(),
            isFromHelper = isFromHelper
        )

        return chatRepository.sendMessage(chatMessage).fold(
            onSuccess = { SendMessageResult.Success },
            onFailure = { exception ->
                SendMessageResult.Error(exception.message ?: "Error sending message")
            }
        )
    }
}
