package com.afilaxy.presentation.chat

import kotlinx.serialization.Serializable
import com.afilaxy.domain.model.ChatMessage

/**
 * Estado da tela de Chat
 */
@Serializable
data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val currentMessage: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val emergencyId: String = "",
    val currentUserId: String? = null
)
