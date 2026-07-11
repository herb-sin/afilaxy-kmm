package com.afilaxy.data.repository

import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.CheckInType
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.CheckInRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class CheckInRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CheckInRepository {

    override suspend fun saveCheckIn(response: CheckInResponse): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuário não autenticado"))

            val today = todayKey()
            val docId = "${response.type}_$today"

            val data = mutableMapOf<String, Any?>(
                "userId" to userId,
                "type" to response.type,
                "timestamp" to response.timestamp,
                "wellbeingA" to response.wellbeingA,
                "wellbeingB" to response.wellbeingB,
                "wellbeingC" to response.wellbeingC,
                "riskScore" to response.riskScore,
                "aqi" to response.aqi,
                "temperature" to response.temperature,
                "humidity" to response.humidity,
                "hourOfDay" to response.hourOfDay,
                "dayOfWeek" to response.dayOfWeek,
                "monthOfYear" to response.monthOfYear
            )

            firestore.collection("checkins")
                .document(userId)
                .collection("responses")
                .document(docId)
                .set(data)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTodayCheckIn(
        userId: String,
        type: CheckInType
    ): Result<CheckInResponse?> {
        return try {
            val today = todayKey()
            val docId = "${type.name}_$today"
            val doc = firestore.collection("checkins")
                .document(userId)
                .collection("responses")
                .document(docId)
                .get()

            if (!doc.exists) return Result.success(null)

            Result.success(
                CheckInResponse(
                    id = docId,
                    userId = userId,
                    type = doc.get("type") ?: type.name,
                    timestamp = doc.get<Long>("timestamp") ?: 0L,
                    wellbeingA = doc.get("wellbeingA"),
                    wellbeingB = doc.get("wellbeingB"),
                    wellbeingC = doc.get("wellbeingC")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentCheckIns(
        userId: String,
        days: Int
    ): Result<List<CheckInResponse>> {
        return try {
            val cutoff = getCurrentTimeMillis() - days.toLong() * 24 * 3600 * 1000

            val docs = firestore.collection("checkins")
                .document(userId)
                .collection("responses")
                .where { "timestamp" greaterThanOrEqualTo cutoff }
                .get().documents

            val responses = docs.mapNotNull { doc ->
                try {
                    CheckInResponse(
                        id = doc.id,
                        userId = userId,
                        type = doc.get("type") ?: "",
                        timestamp = doc.get<Long>("timestamp") ?: 0L,
                        wellbeingA = doc.get("wellbeingA"),
                        wellbeingB = doc.get("wellbeingB"),
                        wellbeingC = doc.get("wellbeingC")
                    )
                } catch (e: Exception) { null }
            }

            Result.success(responses)
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    private fun todayKey(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
    }
}
