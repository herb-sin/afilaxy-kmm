package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = "",
    val emergencyId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isFromHelper: Boolean = false
) {
    companion object {
        fun create(
            emergencyId: String,
            senderId: String,
            senderName: String,
            message: String,
            isFromHelper: Boolean = false
        ): ChatMessage {
            val currentTime = getCurrentTimeMillis()
            return ChatMessage(
                id = "${currentTime}_${senderId.take(8)}",
                emergencyId = emergencyId,
                senderId = senderId,
                senderName = senderName,
                message = message,
                timestamp = currentTime,
                isFromHelper = isFromHelper
            )
        }
    }
}
