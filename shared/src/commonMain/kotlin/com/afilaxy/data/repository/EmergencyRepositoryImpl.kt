package com.afilaxy.data.repository

import com.afilaxy.domain.model.Emergency
import com.afilaxy.domain.model.EmergencyStatus
import com.afilaxy.domain.model.Helper
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.EmergencyRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.*

class EmergencyRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val authRepository: AuthRepository
) : EmergencyRepository {

    override suspend fun createEmergency(emergency: Emergency): Result<String> {
        return createEmergency(emergency.location.latitude, emergency.location.longitude)
    }

    override suspend fun createEmergency(latitude: Double, longitude: Double): Result<String> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            // Buscar nome do usuário
            val userName = try {
                val userDoc = firestore.collection("users").document(userId).get()
                userDoc.get<String?>("name") 
                    ?: auth.currentUser?.displayName 
                    ?: "Usuário"
            } catch (e: Exception) {
                auth.currentUser?.displayName ?: "Usuário"
            }
            
            val currentTime = getCurrentTimeMillis()
            val emergencyData = mapOf(
                "requesterId" to userId,
                "requesterName" to userName,
                "location" to GeoPoint(latitude, longitude),
                "latitude" to latitude,
                "longitude" to longitude,
                "status" to "waiting",
                "active" to true,
                "timestamp" to currentTime,
                "expiresAt" to (currentTime + 600000) // 10 min
            )
            
            val docRef = firestore.collection("emergency_requests")
                .add(emergencyData)
            
            // Notificar helpers próximos
            notifyNearbyHelpers(docRef.id, latitude, longitude, userName)
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun notifyNearbyHelpers(
        emergencyId: String,
        latitude: Double,
        longitude: Double,
        requesterName: String
    ) {
        try {
            // Buscar helpers ativos
            val helpers = firestore.collection("helpers").get()
            
            for (doc in helpers.documents) {
                val isActive = doc.get<Boolean>("isActive") ?: false
                if (!isActive) continue
                
                val helperId = doc.get<String>("id") ?: continue
                
                // Criar notificação push (será processada pela Cloud Function)
                val notification = mapOf(
                    "to" to helperId,
                    "data" to mapOf(
                        "type" to "emergency_request",
                        "emergencyId" to emergencyId,
                        "requesterName" to requesterName,
                        "title" to "🆘 Emergência de Asma",
                        "body" to "$requesterName precisa de ajuda próximo a você"
                    ),
                    "timestamp" to getCurrentTimeMillis(),
                    "processed" to false
                )
                
                firestore.collection("push_notifications").add(notification)
            }
        } catch (e: Exception) {
            // Log silently, não falhar a criação da emergência
        }
    }

    override suspend fun cancelEmergency(emergencyId: String): Result<Boolean> {
        return try {
            firestore.collection("emergency_requests")
                .document(emergencyId)
                .update(
                    "active" to false,
                    "status" to "cancelled"
                )
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun activateHelper(latitude: Double, longitude: Double): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val userEmail = auth.currentUser?.email ?: ""
            
            // Salvar localização no perfil do usuário (para geohash)
            authRepository.updateUserLocation(latitude, longitude)
            
            val helperData = mapOf(
                "id" to userId,
                "email" to userEmail,
                "location" to GeoPoint(latitude, longitude),
                "isActive" to true,
                "lastUpdate" to getCurrentTimeMillis()
            )
            
            firestore.collection("helpers")
                .document(userId)
                .set(helperData)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deactivateHelper(): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            firestore.collection("helpers")
                .document(userId)
                .delete()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptEmergency(emergencyId: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val helperName = try {
                val userDoc = firestore.collection("users").document(userId).get()
                userDoc.get<String?>("name") 
                    ?: auth.currentUser?.displayName 
                    ?: "Helper"
            } catch (e: Exception) {
                auth.currentUser?.displayName ?: "Helper"
            }
            
            // Transação atômica para evitar race condition
            firestore.runTransaction {
                val emergencyRef = firestore.collection("emergency_requests").document(emergencyId)
                val emergencyDoc = get(emergencyRef)
                
                if (!emergencyDoc.exists) {
                    throw Exception("Emergência não encontrada")
                }
                
                val isActive = emergencyDoc.get<Boolean>("active") ?: false
                val currentHelperId = emergencyDoc.get<String?>("helperId")
                val currentStatus = emergencyDoc.get<String>("status") ?: ""
                
                if (!isActive) {
                    throw Exception("Emergência não está ativa")
                }
                
                if (currentHelperId != null || currentStatus != "waiting") {
                    throw Exception("Emergência já foi aceita por outro helper")
                }
                
                val newExpiresAt = getCurrentTimeMillis() + 600000 // 10 min
                
                update(
                    emergencyRef,
                    "status" to "matched",
                    "helperId" to userId,
                    "helperName" to helperName,
                    "matchedAt" to getCurrentTimeMillis(),
                    "expiresAt" to newExpiresAt
                )
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveEmergency(): Result<String?> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val currentTime = getCurrentTimeMillis()

            // Filtros no servidor — reduz leituras do Firestore ao mínimo necessário
            val requesterQuery = firestore.collection("emergency_requests")
                .where { ("requesterId" equalTo userId) and ("active" equalTo true) }
                .get()

            for (doc in requesterQuery.documents) {
                val expiresAt = doc.get<Long>("expiresAt") ?: 0L
                if (expiresAt > currentTime) return Result.success(doc.id)
            }

            val helperQuery = firestore.collection("emergency_requests")
                .where { ("helperId" equalTo userId) and ("active" equalTo true) }
                .get()

            for (doc in helperQuery.documents) {
                val expiresAt = doc.get<Long>("expiresAt") ?: 0L
                if (expiresAt > currentTime) return Result.success(doc.id)
            }

            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearUserEmergencies(): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Filtrar no servidor — apenas emergências ativas do usuário
            val requesterDocs = firestore.collection("emergency_requests")
                .where { ("requesterId" equalTo userId) and ("active" equalTo true) }
                .get()

            for (doc in requesterDocs.documents) {
                doc.reference.update("active" to false, "status" to "cancelled")
            }

            val helperDocs = firestore.collection("emergency_requests")
                .where { ("helperId" equalTo userId) and ("active" equalTo true) }
                .get()

            for (doc in helperDocs.documents) {
                doc.reference.update("active" to false, "status" to "cancelled")
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isHelperActive(): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            
            val doc = firestore.collection("helpers").document(userId).get()
            val isActive = doc.get<Boolean>("isActive") ?: false
            
            Result.success(isActive)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findNearbyHelpers(location: Location, radiusKm: Double): Result<List<Helper>> {
        return try {
            val latitude = location.latitude
            val longitude = location.longitude
            val helpers = mutableListOf<Helper>()
            
            // Query all active helpers (Firebase KMM não tem geospatial query ainda)
            val snapshot = firestore.collection("helpers").get()
            
            for (doc in snapshot.documents) {
                val isActive = doc.get<Boolean>("isActive") ?: false
                if (!isActive) continue
                
                val geoPoint = doc.get<GeoPoint?>("location")
                if (geoPoint != null) {
                    val distance = calculateDistance(
                        latitude, longitude,
                        geoPoint.latitude, geoPoint.longitude
                    )
                    
                    if (distance <= radiusKm) {
                        helpers.add(
                            Helper(
                                id = doc.get("id") ?: "",
                                name = doc.get("name") ?: "Helper",
                                email = doc.get("email") ?: "",
                                latitude = geoPoint.latitude,
                                longitude = geoPoint.longitude,
                                isActive = doc.get("isActive") ?: false,
                                lastUpdate = doc.get("lastUpdate") ?: 0L,
                                distance = distance
                            )
                        )
                    }
                }
            }
            
            Result.success(helpers.sortedBy { it.distance })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEmergencyStatus(emergencyId: String, status: String): Result<Unit> {
        return try {
            val statusStr = status
            
            val updates = if (status == "resolved") {
                mapOf(
                    "status" to statusStr,
                    "active" to false,
                    "resolvedAt" to getCurrentTimeMillis()
                )
            } else {
                mapOf("status" to statusStr)
            }
            
            firestore.collection("emergency_requests")
                .document(emergencyId)
                .update(updates)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun finishEmergency(emergencyId: String): Result<Boolean> {
        return updateEmergencyStatus(emergencyId, "resolved").map { true }
    }
    
    override suspend fun getUserEmergencyHistory(userId: String): Result<List<com.afilaxy.domain.model.EmergencyHistory>> {
        return try {
            val snapshot = firestore.collection("emergency_requests").get()
            
            val history = snapshot.documents.mapNotNull { doc ->
                val requesterId = doc.get<String>("requesterId") ?: ""
                val helperId = doc.get<String?>("helperId")
                val status = doc.get<String>("status") ?: ""
                val active = doc.get<Boolean>("active") ?: false
                
                if ((requesterId == userId || helperId == userId) && !active) {
                    com.afilaxy.domain.model.EmergencyHistory(
                        id = doc.id,
                        requesterId = requesterId,
                        requesterName = doc.get("requesterName") ?: "",
                        helperId = helperId,
                        helperName = doc.get("helperName"),
                        latitude = doc.get("latitude") ?: 0.0,
                        longitude = doc.get("longitude") ?: 0.0,
                        status = status,
                        timestamp = doc.get("timestamp") ?: 0L,
                        resolvedAt = doc.get("resolvedAt"),
                        cancelledAt = if (status == "cancelled") doc.get("timestamp") else null
                    )
                } else null
            }.sortedByDescending { it.timestamp }
            
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeEmergencyStatus(emergencyId: String): Flow<String?> {
        return firestore.collection("emergency_requests")
            .document(emergencyId)
            .snapshots
            .map { snapshot ->
                if (snapshot.exists) {
                    snapshot.get<String>("status")
                } else {
                    null
                }
            }
    }

    /**
     * Calcular distância entre dois pontos (fórmula Haversine)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
