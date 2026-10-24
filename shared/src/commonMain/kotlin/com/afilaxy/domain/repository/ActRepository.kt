package com.afilaxy.domain.repository

import com.afilaxy.domain.model.AsthmaControlTestResult

interface ActRepository {
    /** Salva o resultado do Teste de Controle da Asma (ACT) no Firestore. */
    suspend fun saveResult(result: AsthmaControlTestResult): Result<Unit>

    /** Retorna o resultado mais recente do usuário, ou null se nunca respondeu. */
    suspend fun getLatestResult(userId: String): Result<AsthmaControlTestResult?>

    /** Retorna o histórico de resultados, mais recente primeiro. */
    suspend fun getHistory(userId: String, limit: Int = 12): Result<List<AsthmaControlTestResult>>
}
