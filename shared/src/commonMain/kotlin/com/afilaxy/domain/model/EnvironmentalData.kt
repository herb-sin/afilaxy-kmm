package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

/** Dados climáticos e de qualidade do ar para o local atual do usuário. */
@Serializable
data class EnvironmentalData(
    val latitude: Double,
    val longitude: Double,
    // Clima (OpenMeteo)
    val temperatureCelsius: Float,
    val humidity: Float,           // % relativa
    val windSpeedKmh: Float,
    val uvIndex: Float,
    val precipitationMm: Float,
    // Qualidade do ar (WAQI)
    val aqi: Int? = null,          // Air Quality Index 0-500 (null = indisponível)
    val pm25: Float? = null,       // µg/m³
    val dominantPollutant: String? = null,
    val fetchedAt: Long = 0L
)

/** Nível de risco de crise asmática. */
enum class AsthmaRiskLevel(val label: String, val emoji: String) {
    LOW("Baixo", "🟢"),
    MODERATE("Moderado", "🟡"),
    HIGH("Alto", "🟠"),
    VERY_HIGH("Muito Alto", "🔴")
}

/** Score de risco calculado pelo motor heurístico. */
@Serializable
data class RiskScore(
    val score: Int,                        // 0-100
    val level: String,                     // AsthmaRiskLevel.name
    val factors: List<String>,             // Fatores que contribuíram ao score
    val recommendations: List<String>,     // Recomendações para o paciente
    val calculatedAt: Long = 0L
) {
    val riskLevel: AsthmaRiskLevel
        get() = AsthmaRiskLevel.entries.firstOrNull { it.name == level } ?: AsthmaRiskLevel.LOW
}
