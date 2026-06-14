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
                "hasRescueInhaler" to response.hasRescueInhaler,
                "rescueInhalerName" to response.rescueInhalerName,
                "hadCrisisToday" to response.hadCrisisToday,
                "crisisSeverity" to response.crisisSeverity,
                "usedRescueInhaler" to response.usedRescueInhaler,
                "riskScore" to response.riskScore,
                "aqi" to response.aqi,
                "temperature" to response.temperature,
                "humidity" to response.humidity,
                "hourOfDay" to response.hourOfDay,
                "dayOfWeek" to response.dayOfWeek,
                "monthOfYear" to response.monthOfYear,
                "asmaType" to response.asmaType,
                "asmaTypeSeverity" to response.asmaTypeSeverity
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
                    hasRescueInhaler = doc.get("hasRescueInhaler"),
                    rescueInhalerName = doc.get("rescueInhalerName"),
                    hadCrisisToday = doc.get("hadCrisisToday"),
                    crisisSeverity = doc.get("crisisSeverity"),
                    usedRescueInhaler = doc.get("usedRescueInhaler")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRescueInhalerName(userId: String): Result<String?> {
        return getMedicalContext(userId).map { (inhalerName, _, _) -> inhalerName }
    }

    override suspend fun getAsmaTypeInfo(userId: String): Result<Pair<String?, String?>> {
        return getMedicalContext(userId).map { (_, asmaType, severity) -> Pair(asmaType, severity) }
    }

    override suspend fun getMedicalContext(userId: String): Result<Triple<String?, String?, String?>> {
        return try {
            // Lê de users/{userId}.healthData — fonte que o usuário preenche
            // via tela de edição de perfil (ProfileScreen/ProfileView).
            val doc = firestore.collection("users").document(userId).get()
            if (!doc.exists) return Result.success(Triple(null, null, null))

            @Suppress("UNCHECKED_CAST")
            val healthData = doc.get<Map<String, Any?>?>("healthData")

            @Suppress("UNCHECKED_CAST")
            val medications = healthData?.get("medications") as? List<*>
            val inhalerName = medications?.firstOrNull { it != null && it.toString().isNotBlank() }?.toString()

            @Suppress("UNCHECKED_CAST")
            val conditions = healthData?.get("conditions") as? List<*>
            val asmaType = conditions?.firstOrNull { it != null && it.toString().isNotBlank() }?.toString()

            Result.success(Triple(inhalerName, asmaType, null))
        } catch (e: Exception) {
            Result.success(Triple(null, null, null))
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
                        hasRescueInhaler = doc.get("hasRescueInhaler"),
                        rescueInhalerName = doc.get("rescueInhalerName"),
                        hadCrisisToday = doc.get("hadCrisisToday"),
                        crisisSeverity = doc.get("crisisSeverity"),
                        usedRescueInhaler = doc.get("usedRescueInhaler")
                    )
                } catch (e: Exception) { null }
            }

            Result.success(responses)
        } catch (e: Exception) {
            Result.success(emptyList()) // fallback silencioso
        }
    }

    /** Chave de data no formato YYYY-MM-DD para indexar check-ins por dia. */
    private fun todayKey(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
    }
}

