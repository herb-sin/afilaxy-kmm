package com.afilaxy.data.repository

import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.ReviewRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ReviewRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ReviewRepository {

    override suspend fun updateSeverity(emergencyId: String, severity: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val doc = firestore.collection("emergency_requests").document(emergencyId).get()
            if (!doc.exists) return Result.failure(IllegalStateException("Emergência não encontrada"))
            val requesterId = doc.get<String?>("requesterId")
            val helperId = doc.get<String?>("helperId")
            if (requesterId != userId && helperId != userId) {
                return Result.failure(IllegalStateException("Não autorizado: apenas participantes podem atualizar a severidade"))
            }
            firestore.collection("emergency_requests").document(emergencyId)
                .update(mapOf("severity" to severity))
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateSamuCalled(emergencyId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not authenticated"))
            val doc = firestore.collection("emergency_requests").document(emergencyId).get()
            if (!doc.exists) return Result.failure(IllegalStateException("Emergência não encontrada"))
            val requesterId = doc.get<String?>("requesterId")
            val helperId = doc.get<String?>("helperId")
            if (requesterId != userId && helperId != userId) {
                return Result.failure(IllegalStateException("Não autorizado: apenas participantes podem registrar chamada do SAMU"))
            }
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
                "platform" to com.afilaxy.util.platformName(),
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
