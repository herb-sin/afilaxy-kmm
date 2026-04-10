package com.afilaxy.domain.repository

import com.afilaxy.domain.model.EnvironmentalData
import com.afilaxy.domain.model.RiskScore

interface EnvironmentalRepository {
    /** Busca dados climáticos e de qualidade do ar para as coordenadas fornecidas. */
    suspend fun getEnvironmentalData(latitude: Double, longitude: Double): Result<EnvironmentalData>

    /** Calcula o score de risco de crise para o usuário com base em dados ambientais e histórico. */
    suspend fun calculateRiskScore(
        userId: String,
        latitude: Double,
        longitude: Double
    ): Result<RiskScore>
}
