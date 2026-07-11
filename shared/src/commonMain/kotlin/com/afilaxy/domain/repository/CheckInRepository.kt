package com.afilaxy.domain.repository

import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.CheckInType

interface CheckInRepository {
    /** Salva a resposta do check-in no Firestore. */
    suspend fun saveCheckIn(response: CheckInResponse): Result<Unit>

    /** Retorna o check-in do tipo especificado feito hoje, ou null se não houver. */
    suspend fun getTodayCheckIn(userId: String, type: CheckInType): Result<CheckInResponse?>

    /** Retorna check-ins dos últimos [days] dias para alimentar o motor de risco. */
    suspend fun getRecentCheckIns(userId: String, days: Int = 7): Result<List<CheckInResponse>>
}
