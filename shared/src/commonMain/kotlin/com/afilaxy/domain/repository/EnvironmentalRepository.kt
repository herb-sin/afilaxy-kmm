package com.afilaxy.domain.repository

import com.afilaxy.domain.model.EnvironmentalData
import com.afilaxy.domain.model.RiskScore

interface EnvironmentalRepository {
    /** Busca dados climáticos e de qualidade do ar para as coordenadas fornecidas. */
    suspend fun getEnvironmentalData(latitude: Double, longitude: Double): Result<EnvironmentalData>

    /** Calcula o score de risco de crise para o usuário com base em dados ambientais e histórico.
     *  [crises7dOverride] e [crises30dOverride]: quando >= 0, usam dados já confiáveis do caller
     *  (e.g., do NavGraph que lê user_stats via SDK nativo) em vez de consultar o Firestore novamente. */
    suspend fun calculateRiskScore(
        userId: String,
        latitude: Double,
        longitude: Double,
        crises7dOverride: Int = -1,
        crises30dOverride: Int = -1
    ): Result<RiskScore>
}
