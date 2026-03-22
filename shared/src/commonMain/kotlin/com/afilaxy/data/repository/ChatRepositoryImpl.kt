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
        com.afilaxy.util.Logger.d("ChatRepository", "getMessages emergencyId=$emergencyId")
        return firestore
            .collection("emergency_chats")
            .document(emergencyId)
            .collection("messages")
            .orderBy("timestamp")
            .snapshots
            .map { snapshot ->
                com.afilaxy.util.Logger.d("ChatRepository", "snapshot docs=${snapshot.documents.size}")
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val ts = doc.id.let {
                            try { doc.get<Long>("timestamp") }
                            catch (e1: Exception) {
                                try { doc.get<Double>("timestamp").toLong() }
                                catch (e2: Exception) { 0L }
                            }
                        }
                        com.afilaxy.util.Logger.d("ChatRepository", "doc=${doc.id} ts=$ts")
                        ChatMessage(
                            id = doc.get("id") as? String ?: doc.id,
                            emergencyId = doc.get("emergencyId") as? String ?: "",
                            senderId = doc.get("senderId") as? String ?: "",
                            senderName = doc.get("senderName") as? String ?: "",
                            message = doc.get("message") as? String ?: "",
                            timestamp = ts,
                            isFromHelper = (doc.get("isFromHelper") as? Boolean) ?: false
                        )
                    } catch (e: Exception) {
                        com.afilaxy.util.Logger.e("ChatRepository", "failed to parse doc=${doc.id}: ${e.message}")
                        null
                    }
                }
            }
    }
    
    override suspend fun clearChat(emergencyId: String): Result<Unit> {
        return try {
            val messagesRef = firestore
                .collection("emergency_chats")
                .document(emergencyId)
                .collection("messages")

            val snapshot = messagesRef.get()

            // Deletar em lote — operação atômica mais eficiente que deletes individuais
            if (snapshot.documents.isNotEmpty()) {
                firestore.batch().apply {
                    snapshot.documents.forEach { doc -> delete(doc.reference) }
                }.commit()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
