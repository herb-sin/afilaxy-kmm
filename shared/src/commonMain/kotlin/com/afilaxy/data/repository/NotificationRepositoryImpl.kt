package com.afilaxy.data.repository

import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.NotificationRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore

class NotificationRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : NotificationRepository {
    
    override suspend fun notifyNearbyHelpers(emergencyId: String, latitude: Double, longitude: Double) {
        try {
            val senderId = auth.currentUser?.uid ?: return
            val helpers = firestore.collection("users")
                .get()
                .documents
                .filter { doc ->
                    doc.get<Boolean?>("isHelper") == true &&
                    doc.get<String?>("fcmToken") != null
                }

            helpers.forEach { helper ->
                val helperLat = helper.get<Double?>("latitude") ?: return@forEach
                val helperLng = helper.get<Double?>("longitude") ?: return@forEach
                val distance = calculateDistance(latitude, longitude, helperLat, helperLng)

                if (distance <= 0.25) {
                    firestore.collection("notifications").add(mapOf(
                        "userId"      to senderId,
                        "type"        to "new_emergency",
                        "timestamp"   to getCurrentTimeMillis(),
                        "to"          to helper.get<String>("fcmToken"),
                        "emergencyId" to emergencyId,
                        // Triagem passiva: a pergunta do Sulfato de Salbutamol fica
                        // na tela de aceitar/rejeitar dentro do app, não no push —
                        // o push é o gatilho; o contexto é dado pela UI.
                        "title"       to "🚨 Alguém precisa de ajuda a ${distance.toString().take(4)}km de você",
                        "body"        to "Abra o app para ver detalhes e aceitar o pedido de socorro."
                    ))
                }
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
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
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(toRadians(lat1)) * cos(toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
    
    private fun toRadians(degrees: Double) = degrees * kotlin.math.PI / 180.0
    private fun sin(x: Double) = kotlin.math.sin(x)
    private fun cos(x: Double) = kotlin.math.cos(x)
    private fun sqrt(x: Double) = kotlin.math.sqrt(x)
    private fun atan2(y: Double, x: Double) = kotlin.math.atan2(y, x)
}
