package com.afilaxy.data.repository

import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.NotificationRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore

class NotificationRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : NotificationRepository {
    
    override suspend fun notifyHelperAccepted(emergencyId: String, userId: String) {
        try {
            val senderId = auth.currentUser?.uid ?: return
            val user = firestore.collection("users").document(userId).get()
            val fcmToken = user.get<String?>("fcmToken") ?: return

            firestore.collection("notifications").add(mapOf(
                "userId"      to senderId,
                "type"        to "helper_accepted",
                "timestamp"   to getCurrentTimeMillis(),
                "to"          to fcmToken,
                "emergencyId" to emergencyId,
                "title"       to "✅ Ajuda a caminho!",
                "body"        to "Mande sua localização no chat."
            ))
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    override suspend fun notifyNewMessage(emergencyId: String, recipientId: String, senderName: String) {
        try {
            val senderId = auth.currentUser?.uid ?: return
            val user = firestore.collection("users").document(recipientId).get()
            val fcmToken = user.get<String?>("fcmToken") ?: return

            firestore.collection("notifications").add(mapOf(
                "userId"      to senderId,
                "type"        to "new_message",
                "timestamp"   to getCurrentTimeMillis(),
                "to"          to fcmToken,
                "emergencyId" to emergencyId,
                "title"       to "💬 $senderName",
                "body"        to "$senderName enviou uma mensagem no chat de emergência."
            ))
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    override suspend fun notifyEmergencyResolved(emergencyId: String, userId: String) {
        try {
            val senderId = auth.currentUser?.uid ?: return
            val user = firestore.collection("users").document(userId).get()
            val fcmToken = user.get<String?>("fcmToken") ?: return

            firestore.collection("notifications").add(mapOf(
                "userId"      to senderId,
                "type"        to "emergency_resolved",
                "timestamp"   to getCurrentTimeMillis(),
                "to"          to fcmToken,
                "emergencyId" to emergencyId,
                "title"       to "✅ Emergência encerrada",
                "body"        to "A situação foi resolvida."
            ))
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
}
