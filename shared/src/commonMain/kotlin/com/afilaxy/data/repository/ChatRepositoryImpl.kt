package com.afilaxy.data.repository

import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.domain.repository.ChatRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ChatRepository {
    
    override suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            val messageData = mapOf(
                "id" to message.id,
                "emergencyId" to message.emergencyId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "message" to message.message,
                "timestamp" to message.timestamp,
                "isFromHelper" to message.isFromHelper
            )
            
            firestore
                .collection("emergency_chats")
                .document(message.emergencyId)
                .collection("messages")
                .document(message.id)
                .set(messageData)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getMessages(emergencyId: String): Flow<List<ChatMessage>> {
        return firestore
            .collection("emergency_chats")
            .document(emergencyId)
            .collection("messages")
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        ChatMessage(
                            id = doc.get("id") as? String ?: "",
                            emergencyId = doc.get("emergencyId") as? String ?: "",
                            senderId = doc.get("senderId") as? String ?: "",
                            senderName = doc.get("senderName") as? String ?: "",
                            message = doc.get("message") as? String ?: "",
                            timestamp = (doc.get("timestamp") as? Long) ?: 0L,
                            isFromHelper = (doc.get("isFromHelper") as? Boolean) ?: false
                        )
                    } catch (e: Exception) {
                        null // Skip invalid documents
                    }
                }.sortedBy { it.timestamp } // Sort manually by timestamp
            }
    }
    
    override suspend fun clearChat(emergencyId: String): Result<Unit> {
        return try {
            // Get all messages
            val messagesRef = firestore
                .collection("emergency_chats")
                .document(emergencyId)
                .collection("messages")
            
            val snapshot = messagesRef.get()
            
            // Delete each message
            snapshot.documents.forEach { doc ->
                doc.reference.delete()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
