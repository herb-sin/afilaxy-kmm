package com.afilaxy.data.repository

import com.afilaxy.domain.model.AsthmaControlTestResult
import com.afilaxy.domain.repository.ActRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ActRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ActRepository {

    override suspend fun saveResult(result: AsthmaControlTestResult): Result<Unit> {
        return try {
            val data = mapOf(
                "userId" to result.userId,
                "timestamp" to result.timestamp,
                "activityImpairment" to result.activityImpairment,
                "shortnessOfBreath" to result.shortnessOfBreath,
                "nighttimeSymptoms" to result.nighttimeSymptoms,
                "rescueMedicationUse" to result.rescueMedicationUse,
                "selfRatedControl" to result.selfRatedControl,
                "score" to result.score
            )

            firestore.collection("act")
                .document(result.userId)
                .collection("results")
                .add(data)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLatestResult(userId: String): Result<AsthmaControlTestResult?> {
        return try {
            val results = fetchAll(userId)
            Result.success(results.maxByOrNull { it.timestamp })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistory(userId: String, limit: Int): Result<List<AsthmaControlTestResult>> {
        return try {
            val results = fetchAll(userId)
                .sortedByDescending { it.timestamp }
                .take(limit)
            Result.success(results)
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    private suspend fun fetchAll(userId: String): List<AsthmaControlTestResult> {
        val docs = firestore.collection("act")
            .document(userId)
            .collection("results")
            .get().documents

        return docs.mapNotNull { doc ->
            try {
                AsthmaControlTestResult(
                    id = doc.id,
                    userId = userId,
                    timestamp = doc.get<Long>("timestamp") ?: 0L,
                    activityImpairment = doc.get<Long>("activityImpairment")?.toInt() ?: 0,
                    shortnessOfBreath = doc.get<Long>("shortnessOfBreath")?.toInt() ?: 0,
                    nighttimeSymptoms = doc.get<Long>("nighttimeSymptoms")?.toInt() ?: 0,
                    rescueMedicationUse = doc.get<Long>("rescueMedicationUse")?.toInt() ?: 0,
                    selfRatedControl = doc.get<Long>("selfRatedControl")?.toInt() ?: 0,
                    score = doc.get<Long>("score")?.toInt() ?: 0
                )
            } catch (e: Exception) { null }
        }
    }
}
