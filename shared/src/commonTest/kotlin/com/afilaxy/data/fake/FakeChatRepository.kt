package com.afilaxy.data.fake

import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of ChatRepository for testing
 */
class FakeChatRepository(
    private var shouldSucceed: Boolean = true
) : ChatRepository {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    
    override suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return if (shouldSucceed) {
            _messages.value = _messages.value + message
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to send message"))
        }
    }
    
    override fun getMessages(emergencyId: String): Flow<List<ChatMessage>> {
        return _messages.asStateFlow()
    }
    
    override suspend fun clearChat(emergencyId: String): Result<Unit> {
        return if (shouldSucceed) {
            _messages.value = emptyList()
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to clear chat"))
        }
    }
    
    // Test helpers
    fun setMessages(messages: List<ChatMessage>) {
        _messages.value = messages
    }
    
    fun setShouldSucceed(shouldSucceed: Boolean) {
        this.shouldSucceed = shouldSucceed
    }
    
    fun getMessageCount(): Int {
        return _messages.value.size
    }
}
