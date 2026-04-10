package com.afilaxy.domain.repository

import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.CheckInType

interface CheckInRepository {
    /** Salva a resposta do check-in no Firestore. */
    suspend fun saveCheckIn(response: CheckInResponse): Result<Unit>

    /** Retorna o check-in do tipo especificado feito hoje, ou null se não houver. */
    suspend fun getTodayCheckIn(userId: String, type: CheckInType): Result<CheckInResponse?>

    /** Retorna o nome do broncodilatador de resgate do perfil médico do usuário. */
    suspend fun getRescueInhalerName(userId: String): Result<String?>
}
