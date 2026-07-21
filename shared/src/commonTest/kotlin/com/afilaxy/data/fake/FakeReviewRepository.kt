package com.afilaxy.data.fake

import com.afilaxy.domain.repository.ReviewRepository

class FakeReviewRepository(
    private var shouldSucceed: Boolean = true
) : ReviewRepository {

    override suspend fun updateSeverity(emergencyId: String, severity: String): Result<Unit> =
        if (shouldSucceed) Result.success(Unit) else Result.failure(Exception("Erro ao atualizar severidade"))

    override suspend fun updateSamuCalled(emergencyId: String): Result<Unit> =
        if (shouldSucceed) Result.success(Unit) else Result.failure(Exception("Erro ao registrar SAMU"))

    override suspend fun submitReview(
        emergencyId: String,
        reviewedId: String,
        rating: Int,
        comment: String?
    ): Result<Unit> =
        if (shouldSucceed) Result.success(Unit) else Result.failure(Exception("Erro ao enviar avaliação"))

    override suspend fun submitNps(score: Int): Result<Unit> =
        if (shouldSucceed) Result.success(Unit) else Result.failure(Exception("Erro ao registrar NPS"))

    override suspend fun getEmergencyParticipants(emergencyId: String): Result<Pair<String?, String?>> =
        if (shouldSucceed) Result.success(Pair("requester-id", "helper-id"))
        else Result.failure(Exception("Erro ao buscar participantes"))

    fun setShouldSucceed(value: Boolean) { shouldSucceed = value }
}
