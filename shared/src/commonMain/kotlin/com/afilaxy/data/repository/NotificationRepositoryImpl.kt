package com.afilaxy.data.repository

import com.afilaxy.domain.repository.NotificationRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore

class NotificationRepositoryImpl(
    private val firestore: FirebaseFirestore
) : NotificationRepository {
    
    override suspend fun notifyNearbyHelpers(emergencyId: String, latitude: Double, longitude: Double) {
        try {
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
                
                if (distance <= 5.0) {
                    val notification = mapOf(
                        "to" to helper.get<String>("fcmToken"),
                        "data" to mapOf(
                            "type" to "new_emergency",
                            "title" to "Nova Emergência Próxima!",
                            "body" to "Há uma emergência a ${distance.toString().take(4)}km de você",
                            "emergencyId" to emergencyId
                        )
                    )
                    
                    firestore.collection("notifications").add(notification)
                }
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    override suspend fun notifyHelperAccepted(emergencyId: String, userId: String) {
        try {
            val user = firestore.collection("users").document(userId).get()
            val fcmToken = user.get<String?>("fcmToken") ?: return
            
            val notification = mapOf(
                "to" to fcmToken,
                "data" to mapOf(
                    "type" to "helper_accepted",
                    "title" to "Helper Aceitou!",
                    "body" to "Um helper aceitou sua emergência",
                    "emergencyId" to emergencyId
                )
            )
            
            firestore.collection("notifications").add(notification)
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    override suspend fun notifyNewMessage(emergencyId: String, recipientId: String, senderName: String) {
        try {
            val user = firestore.collection("users").document(recipientId).get()
            val fcmToken = user.get<String?>("fcmToken") ?: return
            
            val notification = mapOf(
                "to" to fcmToken,
                "data" to mapOf(
                    "type" to "new_message",
                    "title" to "Nova Mensagem",
                    "body" to "$senderName enviou uma mensagem",
                    "emergencyId" to emergencyId
                )
            )
            
            firestore.collection("notifications").add(notification)
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    override suspend fun notifyEmergencyResolved(emergencyId: String, userId: String) {
        try {
            val user = firestore.collection("users").document(userId).get()
            val fcmToken = user.get<String?>("fcmToken") ?: return
            
            val notification = mapOf(
                "to" to fcmToken,
                "data" to mapOf(
                    "type" to "emergency_resolved",
                    "title" to "Emergência Resolvida",
                    "body" to "A emergência foi resolvida com sucesso",
                    "emergencyId" to emergencyId
                )
            )
            
            firestore.collection("notifications").add(notification)
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
