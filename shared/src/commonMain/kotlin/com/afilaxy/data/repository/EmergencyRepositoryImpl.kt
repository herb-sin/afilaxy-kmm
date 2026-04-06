package com.afilaxy.data.repository

import com.afilaxy.domain.model.Emergency
import com.afilaxy.domain.model.EmergencyStatus
import com.afilaxy.domain.model.Helper
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.EmergencyRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.*

class EmergencyRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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
                "expiresAt" to (currentTime + 180000) // 3 min
            )
            
            // Criar emergência
            val docRef = firestore.collection("emergency_requests")
                .add(emergencyData)
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelEmergency(emergencyId: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Verificar ownership antes de cancelar
            val doc = firestore.collection("emergency_requests").document(emergencyId).get()
            if (!doc.exists) {
                return Result.failure(IllegalStateException("Emergência não encontrada"))
            }
            val requesterId = doc.get<String?>("requesterId")
            if (requesterId != userId) {
                return Result.failure(IllegalStateException("Não autorizado: apenas o solicitante pode cancelar a emergência"))
            }

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
            
            // LGPD: arredonda coordenadas para 0.001° ≈ 111m (latitude) / 78m (longitude em -23°)
            // Impede exposição de endereço exato. Precisão suficiente para mapa de proximidade.
            val obfuscatedLat = round(latitude * 1000) / 1000.0
            val obfuscatedLon = round(longitude * 1000) / 1000.0
            
            try {
                firestore.collection("users").document(userId)
                    .update(mapOf("latitude" to latitude, "longitude" to longitude))
            } catch (e: Exception) {
                com.afilaxy.util.Logger.w("EmergencyRepo", "Falha ao atualizar localização em users/${userId}: ${e.message}")
            }

            // Grava coordenadas obfuscadas no Firestore — sem email (PII desnecessária para o mapa)
            val helperData = mapOf(
                "id" to userId,
                "location" to GeoPoint(obfuscatedLat, obfuscatedLon),
                "latitude" to obfuscatedLat,
                "longitude" to obfuscatedLon,
                "geohash" to encodeGeohash(obfuscatedLat, obfuscatedLon),
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
            // Busca o nome do helper com timeout — evita que um get pausado trave toda a operação
            val helperName = try {
                kotlinx.coroutines.withTimeout(5_000) {
                    val userDoc = firestore.collection("users").document(userId).get()
                    userDoc.get<String?>("name")
                        ?: auth.currentUser?.displayName
                        ?: "Helper"
                }
            } catch (e: Exception) {
                auth.currentUser?.displayName ?: "Helper"
            }
            
            // Timeout de 10s na transação — sem ele, um runTransaction com regra rejeitada
            // ou problema de rede pode travara indefinidamente sem retornar sucesso nem falha.
            kotlinx.coroutines.withTimeout(10_000) {
                firestore.runTransaction {
                    val emergencyRef = firestore.collection("emergency_requests").document(emergencyId)
                    val emergencyDoc = get(emergencyRef)

                    if (!emergencyDoc.exists) throw Exception("Emergência não encontrada")

                    val isActive = emergencyDoc.get<Boolean>("active") ?: false
                    val currentHelperId = emergencyDoc.get<String?>("helperId")
                    val currentStatus = emergencyDoc.get<String>("status") ?: ""

                    if (!isActive) throw Exception("Emergência não está ativa")
                    if (currentHelperId != null || currentStatus != "waiting") throw Exception("Emergência já foi aceita")

                    update(
                        emergencyRef,
                        "status" to "matched",
                        "helperId" to userId,
                        "helperName" to helperName,
                        "matchedAt" to getCurrentTimeMillis(),
                        "expiresAt" to (getCurrentTimeMillis() + 180000)
                    )
                }
            }
            
            Result.success(true)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // Converte para Exception regular — evita que seja tratada como cancelamento
            // do coroutine e deixe isLoading=true indefinidamente sem tocar o onFailure.
            com.afilaxy.util.Logger.e("EmergencyRepo", "acceptEmergency timeout emergencyId=$emergencyId")
            Result.failure(Exception("Tempo esgotado ao aceitar emergência. Tente novamente."))
        } catch (e: Exception) {
            com.afilaxy.util.Logger.e("EmergencyRepo", "acceptEmergency failed emergencyId=$emergencyId: ${e.message}")
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
            val currentUserId = auth.currentUser?.uid

            // Pré-filtro por bounding box de latitude para reduzir leituras do Firestore
            val deltaLat = radiusKm / 111.0
            val minLat = latitude - deltaLat
            val maxLat = latitude + deltaLat

            val snapshot = firestore.collection("helpers")
                .where { ("isActive" equalTo true) and ("latitude" greaterThanOrEqualTo minLat) and ("latitude" lessThanOrEqualTo maxLat) }
                .get()

            val helpers = mutableListOf<Helper>()

            for (doc in snapshot.documents) {
                val id = doc.get<String?>("id") ?: continue
                // Exclui o próprio usuário dos helpers visíveis
                if (id == currentUserId) continue
                val geoPoint = doc.get<GeoPoint?>("location")
                if (geoPoint != null) {
                    val distance = calculateDistance(
                        latitude, longitude,
                        geoPoint.latitude, geoPoint.longitude
                    )

                    if (distance <= radiusKm) {
                        helpers.add(
                            Helper(
                                id = id,
                                name = doc.get("name") ?: "Helper",
                                email = "", // email não gravado — campo vazio por design (LGPD)
                                latitude = geoPoint.latitude,
                                longitude = geoPoint.longitude,
                                isActive = doc.get("isActive") ?: false,
                                lastUpdate = 0L, // não usado no mapa; evita crash Timestamp vs Long
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

    override suspend fun updateEmergencyStatus(emergencyId: String, status: EmergencyStatus): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val doc = firestore.collection("emergency_requests").document(emergencyId).get()
            if (!doc.exists) return Result.failure(IllegalStateException("Emergência não encontrada"))

            val requesterId = doc.get<String?>("requesterId")
            val helperId = doc.get<String?>("helperId")
            if (requesterId != userId && helperId != userId) {
                return Result.failure(IllegalStateException("Não autorizado"))
            }

            val updates = if (status == EmergencyStatus.RESOLVED) {
                mapOf("status" to status.dbValue, "active" to false, "resolvedAt" to getCurrentTimeMillis())
            } else {
                mapOf("status" to status.dbValue)
            }

            firestore.collection("emergency_requests").document(emergencyId).update(updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun finishEmergency(emergencyId: String): Result<Boolean> {
        return updateEmergencyStatus(emergencyId, EmergencyStatus.RESOLVED).map { true }
    }
    
    override suspend fun getUserEmergencyHistory(userId: String): Result<List<com.afilaxy.domain.model.EmergencyHistory>> {
        return try {
            // Filtrar requester no servidor — evita leitura de dados de outros usuários
            val requesterSnapshot = firestore.collection("emergency_requests")
                .where { ("requesterId" equalTo userId) and ("active" equalTo false) }
                .get()

            // Filtrar helper no servidor separadamente (Firestore não suporta OR em campos distintos num único where)
            val helperSnapshot = firestore.collection("emergency_requests")
                .where { ("helperId" equalTo userId) and ("active" equalTo false) }
                .get()

            val allDocs = (requesterSnapshot.documents + helperSnapshot.documents)
                .distinctBy { it.id } // evitar duplicatas se o usuário for requester e helper

            val history = allDocs.mapNotNull { doc ->
                val requesterId = doc.get<String>("requesterId") ?: ""
                val helperId = doc.get<String?>("helperId")
                val status = doc.get<String>("status") ?: ""

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
            }.sortedByDescending { it.timestamp }

            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeNearbyEmergencies(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<Emergency>> {
        val deltaLat = radiusKm / 111.0
        val currentUserId = auth.currentUser?.uid
        val sessionStartMs = getCurrentTimeMillis()
        return firestore.collection("emergency_requests")
            .where {
                ("active" equalTo true) and
                ("latitude" greaterThanOrEqualTo latitude - deltaLat) and
                ("latitude" lessThanOrEqualTo latitude + deltaLat) and
                ("timestamp" greaterThanOrEqualTo sessionStartMs)
            }
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val requesterId = doc.get<String?>("requesterId") ?: return@mapNotNull null
                    if (requesterId == currentUserId) return@mapNotNull null
                    val lat = doc.get<Double?>("latitude") ?: return@mapNotNull null
                    val lon = doc.get<Double?>("longitude") ?: return@mapNotNull null
                    val distance = calculateDistance(latitude, longitude, lat, lon)
                    if (distance > radiusKm) return@mapNotNull null
                    Emergency(
                        id = doc.id,
                        userId = requesterId,
                        userName = doc.get("requesterName") ?: "",
                        location = Location(lat, lon, "", doc.get("timestamp") ?: 0L),
                        status = EmergencyStatus.fromDb(doc.get("status") ?: "waiting"),
                        assignedHelperId = doc.get("helperId"),
                        timestamp = doc.get("timestamp") ?: 0L
                    )
                }
            }
    }

    override suspend fun getEmergencyExpiresAt(emergencyId: String): Long? {
        return try {
            val doc = firestore.collection("emergency_requests").document(emergencyId).get()
            // O Firestore KMM às vezes serializa campos numéricos como Double em vez de Long.
            // Tenta Long primeiro; fallback para Double.toLong() para garantir que o countdown
            // da EmergencyResponseScreen (Android) receba um valor não-nulo e inicie corretamente.
            doc.get<Long?>("expiresAt")
                ?: doc.get<Double?>("expiresAt")?.toLong()
        } catch (e: Exception) { null }
    }

    override fun observeNearbyHelpers(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Flow<List<Helper>> {
        val currentUserId = auth.currentUser?.uid
        val deltaLat = radiusKm / 111.0
        val minLat = latitude - deltaLat
        val maxLat = latitude + deltaLat
        return firestore.collection("helpers")
            .where {
                ("isActive" equalTo true) and
                ("latitude" greaterThanOrEqualTo minLat) and
                ("latitude" lessThanOrEqualTo maxLat)
            }
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val id = doc.get<String?>("id") ?: return@mapNotNull null
                    // Exclui o próprio usuário do resultado
                    if (id == currentUserId) return@mapNotNull null
                    val geoPoint = doc.get<GeoPoint?>("location") ?: return@mapNotNull null
                    val distance = calculateDistance(
                        latitude, longitude,
                        geoPoint.latitude, geoPoint.longitude
                    )
                    if (distance > radiusKm) return@mapNotNull null
                    Helper(
                        id = id,
                        name = doc.get("name") ?: "Helper",
                        email = "", // email não gravado por design (LGPD)
                        latitude = geoPoint.latitude,
                        longitude = geoPoint.longitude,
                        isActive = true,
                        lastUpdate = 0L, // não usado no mapa; evita crash Timestamp vs Long
                        distance = distance
                    )
                }.sortedBy { it.distance }
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
        val r = 6371.0
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Geohash precision=9 — compatível com geofire-common usado na Cloud Function
    private fun encodeGeohash(latitude: Double, longitude: Double, precision: Int = 9): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var minLat = -90.0; var maxLat = 90.0
        var minLon = -180.0; var maxLon = 180.0
        val hash = StringBuilder()
        var bits = 0; var bitsTotal = 0; var hashValue = 0
        while (hash.length < precision) {
            if (bitsTotal % 2 == 0) {
                val mid = (minLon + maxLon) / 2
                if (longitude >= mid) { hashValue = (hashValue shl 1) or 1; minLon = mid }
                else { hashValue = hashValue shl 1; maxLon = mid }
            } else {
                val mid = (minLat + maxLat) / 2
                if (latitude >= mid) { hashValue = (hashValue shl 1) or 1; minLat = mid }
                else { hashValue = hashValue shl 1; maxLat = mid }
            }
            bits++; bitsTotal++
            if (bits == 5) { hash.append(base32[hashValue]); bits = 0; hashValue = 0 }
        }
        return hash.toString()
    }

    // ── Analytics & Data Enrichment ─────────────────────────────────────────

    override suspend fun updateSeverity(emergencyId: String, severity: String): Result<Unit> {
        return try {
            firestore.collection("emergency_requests").document(emergencyId)
                .update(mapOf("severity" to severity))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateSamuCalled(emergencyId: String): Result<Unit> {
        return try {
            firestore.collection("emergency_requests").document(emergencyId)
                .update(mapOf(
                    "samuCalled" to true,
                    "samuCalledAt" to getCurrentTimeMillis()
                ))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun submitReview(
        emergencyId: String,
        reviewedId: String,
        rating: Int,
        comment: String?
    ): Result<Unit> {
        return try {
            val reviewerId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val data = mutableMapOf<String, Any>(
                "emergencyId" to emergencyId,
                "reviewerId" to reviewerId,
                "reviewedId" to reviewedId,
                "rating" to rating,
                "timestamp" to getCurrentTimeMillis()
            )
            if (comment != null) data["comment"] = comment
            firestore.collection("reviews").add(data)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun submitNps(score: Int): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            firestore.collection("nps_responses").add(mapOf(
                "userId" to userId,
                "score" to score,
                "timestamp" to getCurrentTimeMillis()
            ))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getEmergencyParticipants(emergencyId: String): Result<Pair<String?, String?>> {
        return try {
            val doc = firestore.collection("emergency_requests").document(emergencyId).get()
            val requesterId = doc.get<String?>("requesterId")
            val helperId = doc.get<String?>("helperId")
            Result.success(Pair(requesterId, helperId))
        } catch (e: Exception) { Result.failure(e) }
    }
}
