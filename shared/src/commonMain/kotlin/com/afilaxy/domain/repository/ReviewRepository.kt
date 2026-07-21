package com.afilaxy.domain.repository

interface ReviewRepository {
    /** Grava a gravidade da emergência (leve / moderada / grave). */
    suspend fun updateSeverity(emergencyId: String, severity: String): Result<Unit>

    /** Registra que o usuário acionou o SAMU durante o chat. */
    suspend fun updateSamuCalled(emergencyId: String): Result<Unit>

    /** Salva avaliação (1–5 ⭐) + comentário opcional em `reviews`. */
    suspend fun submitReview(
        emergencyId: String,
        reviewedId: String,
        rating: Int,
        comment: String?
    ): Result<Unit>

    /** Grava resposta NPS (0–10) em `nps_responses`. */
    suspend fun submitNps(score: Int): Result<Unit>

    /** Retorna (requesterId, helperId) para montar a avaliação correta. */
    suspend fun getEmergencyParticipants(emergencyId: String): Result<Pair<String?, String?>>
}
