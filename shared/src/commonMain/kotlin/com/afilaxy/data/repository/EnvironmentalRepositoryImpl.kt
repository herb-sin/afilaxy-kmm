package com.afilaxy.data.repository

import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.EnvironmentalData
import com.afilaxy.domain.model.AsthmaRiskLevel
import com.afilaxy.domain.model.RiskScore
import com.afilaxy.domain.repository.CheckInRepository
import com.afilaxy.domain.repository.EnvironmentalRepository
import com.afilaxy.domain.model.getCurrentTimeMillis
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Implementação do EnvironmentalRepository usando:
 * - OpenMeteo (gratuito, sem chave) para dados climáticos
 * - WAQI (token demo / chave própria) para qualidade do ar
 */
class EnvironmentalRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val waqiToken: String = WAQI_DEFAULT_TOKEN,
    private val checkInRepository: CheckInRepository? = null
) : EnvironmentalRepository {

    internal companion object {
        /**
         * Token padrão vazio — a chave real é injetada via Koin no módulo DI.
         * Para desenvolvimento local: adicione WAQI_API_TOKEN=<sua_chave> em local.properties
         * Para CI: configure o GitHub Secret WAQI_API_TOKEN
         * Registre em: https://aqicn.org/data-platform/token/
         */
        const val WAQI_DEFAULT_TOKEN = ""

        /** HttpClient singleton — compartilhado entre instâncias para evitar leak de threads OkHttp/URLSession */
        val httpClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }
        }
    }

    // ── OpenMeteo Response DTOs ────────────────────────────────────────────────

    @Serializable
    private data class OpenMeteoResponse(
        val current: OpenMeteoCurrent? = null
    )

    @Serializable
    private data class OpenMeteoCurrent(
        @SerialName("temperature_2m") val temperature: Float = 0f,
        @SerialName("relative_humidity_2m") val humidity: Float = 0f,
        @SerialName("wind_speed_10m") val windSpeed: Float = 0f,
        @SerialName("uv_index") val uvIndex: Float = 0f,
        @SerialName("precipitation") val precipitation: Float = 0f
    )

    // ── WAQI Response DTOs ─────────────────────────────────────────────────────

    @Serializable
    private data class WaqiResponse(
        val status: String = "",
        val data: WaqiData? = null
    )

    @Serializable
    private data class WaqiData(
        val aqi: Int = 0,
        val dominantpol: String? = null,
        val iaqi: WaqiIaqi? = null
    )

    @Serializable
    private data class WaqiIaqi(
        val pm25: WaqiValue? = null,
        val pm10: WaqiValue? = null
    )

    @Serializable
    private data class WaqiValue(val v: Float = 0f)

    // ── Implementação ──────────────────────────────────────────────────────────

    override suspend fun getEnvironmentalData(
        latitude: Double,
        longitude: Double
    ): Result<EnvironmentalData> {
        return try {
            // Busca clima (OpenMeteo — sem chave, gratuito)
            val meteo = try {
                httpClient.get("https://api.open-meteo.com/v1/forecast") {
                    parameter("latitude", latitude)
                    parameter("longitude", longitude)
                    parameter("current", "temperature_2m,relative_humidity_2m,wind_speed_10m,uv_index,precipitation")
                    parameter("timezone", "auto")
                }.body<OpenMeteoResponse>()
            } catch (e: Exception) { null }

            // Busca qualidade do ar (WAQI)
            val waqi = try {
                httpClient.get("https://api.waqi.info/feed/geo:$latitude;$longitude/") {
                    parameter("token", waqiToken)
                }.body<WaqiResponse>()
            } catch (e: Exception) { null }

            val current = meteo?.current
            val waqiData = if (waqi?.status == "ok") waqi.data else null

            Result.success(
                EnvironmentalData(
                    latitude = latitude,
                    longitude = longitude,
                    temperatureCelsius = current?.temperature ?: 0f,
                    humidity = current?.humidity ?: 0f,
                    windSpeedKmh = current?.windSpeed ?: 0f,
                    uvIndex = current?.uvIndex ?: 0f,
                    precipitationMm = current?.precipitation ?: 0f,
                    aqi = waqiData?.aqi,
                    pm25 = waqiData?.iaqi?.pm25?.v,
                    dominantPollutant = waqiData?.dominantpol,
                    fetchedAt = getCurrentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateRiskScore(
        userId: String,
        latitude: Double,
        longitude: Double,
        crises7dOverride: Int,
        crises30dOverride: Int
    ): Result<RiskScore> {
        return try {
            val env = getEnvironmentalData(latitude, longitude).getOrNull()

            // Use caller-provided counts when available (from NavGraph via native SDK — always correct).
            // Only fall back to internal Firestore query if not provided (e.g., iOS, tests).
            val crises7d: Int
            val crises30d: Int
            if (crises7dOverride >= 0 && crises30dOverride >= 0) {
                crises7d = crises7dOverride
                crises30d = crises30dOverride.coerceAtMost(50)
            } else {
                // Fallback: read user_stats via dev.gitlive (may have type serialization issues)
                val userStatsDoc = try { firestore.collection("user_stats").document(userId).get() }
                    catch (e: Exception) { null }
                val total = userStatsDoc?.let {
                    try { (it.get<Double?>("totalEmergencies") ?: it.get<Long?>("totalEmergencies")?.toDouble())?.toInt() }
                    catch (e: Exception) { null }
                } ?: 0
                @Suppress("UNCHECKED_CAST")
                val weeklyMap = try { userStatsDoc?.get<Any?>("weeklyCount") as? Map<String, Any> }
                    catch (e: Exception) { null }
                crises30d = total.coerceAtMost(50)
                crises7d = weeklyMap?.values?.maxOfOrNull { v ->
                    when (v) { is Long -> v.toInt(); is Double -> v.toInt(); is Number -> v.toInt(); else -> 0 }
                } ?: 0
            }

            // samuCalled: query emergency_requests without timestamp filter
            val samuCalledCount = try {
                firestore.collection("emergency_requests")
                    .where { ("requesterId" equalTo userId) and ("samuCalled" equalTo true) }
                    .get().documents.size
            } catch (e: Exception) { 0 }

            // Check-ins da Agenda de Saúde (últimos 7 dias)
            val recentCheckIns = try {
                checkInRepository?.getRecentCheckIns(userId, days = 7)?.getOrNull() ?: emptyList()
            } catch (e: Exception) { emptyList() }

            // Mês atual → sazonalidade
            val month = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber

            val riskScore = RiskScoreEngine.calculate(
                env = env,
                crises30d = crises30d,
                crises7d = crises7d,
                samuCalledCount = samuCalledCount,
                monthOfYear = month,
                recentCheckIns = recentCheckIns
            )

            // Persist risk score snapshot for trend analysis and future ML training
            try {
                val today = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
                firestore.collection("risk_scores")
                    .document(userId)
                    .collection("snapshots")
                    .document(today.toString())
                    .set(mapOf(
                        "userId" to userId,
                        "date" to today.toString(),
                        "score" to riskScore.score,
                        "level" to riskScore.level,
                        "crises7d" to crises7d,
                        "crises30d" to crises30d,
                        "aqi" to (env?.aqi ?: 0),
                        "temperature" to (env?.temperatureCelsius ?: 0f),
                        "humidity" to (env?.humidity ?: 0f),
                        "timestamp" to getCurrentTimeMillis()
                    ))
            } catch (e: Exception) {
                com.afilaxy.util.Logger.e("EnvironmentalRepo", "Falha ao persistir risk score: ${e.message}", e)
            }

            Result.success(riskScore)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Motor Heurístico de Risco ──────────────────────────────────────────────────

internal object RiskScoreEngine {

    fun calculate(
        env: EnvironmentalData?,
        crises30d: Int,
        crises7d: Int,
        samuCalledCount: Int,
        monthOfYear: Int,
        recentCheckIns: List<CheckInResponse> = emptyList()
    ): RiskScore {
        var score = 0
        val factors = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // ── Histórico de crises (emergências registradas) ─────────────────────
        // Clinical reference: ≥2 crises/week = uncontrolled asthma (GINA guidelines).
        // Scoring is non-linear: 1 crisis is moderate, 2+ is severe, 5+ forces VERY_HIGH alone.
        val crises7dScore = when {
            crises7d >= 5 -> 55   // 5+ crises: extreme — VERY_HIGH alone
            crises7d >= 3 -> 40   // 3-4 crises: severe — HIGH alone
            crises7d == 2 -> 28   // 2 crises: uncontrolled per guidelines
            crises7d == 1 -> 14   // 1 crisis: moderate signal
            else -> 0
        }
        score += crises7dScore
        if (crises7d > 0) {
            val label = if (crises7d >= 2) "⚠️ ${crises7d} crise(s) nos últimos 7 dias — asma não controlada"
                        else "${crises7d} crise(s) nos últimos 7 dias"
            factors.add(label)
        }

        val crises30dScore = when {
            crises30d >= 8 -> 25
            crises30d >= 4 -> 15
            crises30d >= 2 -> 8
            crises30d == 1 -> 3
            else -> 0
        }
        score += crises30dScore
        if (crises30d >= 2) factors.add("${crises30d} crises no último mês")

        if (samuCalledCount > 0) {
            score += 15
            factors.add("SAMU acionado em crises anteriores")
            recommendations.add("Tenha o número do SAMU (192) salvo no celular")
        }

        // ── Agenda de Saúde (check-ins dos últimos 7 dias) ────────────────────
        if (recentCheckIns.isNotEmpty()) {
            // Crises reportadas nos check-ins noturnos
            val reportedCrises = recentCheckIns.count { it.hadCrisisToday == true }
            if (reportedCrises > 0) {
                score += (reportedCrises * 12).coerceAtMost(25)
                factors.add("$reportedCrises crise(s) reportada(s) na Agenda de Saúde")
            }

            // Uso de bombinha de resgate (indica sintomas ativos)
            val rescueUsageCount = recentCheckIns.count { it.usedRescueInhaler == true }
            if (rescueUsageCount >= 2) {
                score += 10
                factors.add("Bombinha de resgate usada $rescueUsageCount vezes esta semana")
                recommendations.add("Converse com seu médico sobre seu tratamento de manutenção")
            }

            // Crise grave reportada
            val severeCrises = recentCheckIns.count { it.crisisSeverity == "grave" }
            if (severeCrises > 0) {
                score += 15
                factors.add("Crise grave reportada recentemente")
                recommendations.add("Procure atendimento médico para reavaliar seu tratamento")
            }

            // Sem bombinha de resgate disponível
            val withoutInhaler = recentCheckIns.any {
                it.hasRescueInhaler == false
            }
            if (withoutInhaler) {
                score += 8
                factors.add("Sem bombinha de resgate disponível")
                recommendations.add("Providencie uma bombinha de resgate com seu médico")
            }
        }

        // ── Qualidade do ar ────────────────────────────────────────────────────
        val aqi = env?.aqi
        if (aqi != null) {
            when {
                aqi > 200 -> { score += 25; factors.add("Qualidade do ar muito ruim (AQI $aqi)") }
                aqi > 150 -> { score += 18; factors.add("Qualidade do ar ruim (AQI $aqi)") }
                aqi > 100 -> { score += 10; factors.add("Qualidade do ar moderada (AQI $aqi)") }
                aqi > 50  -> { score += 3 }
            }
        }

        val pm25 = env?.pm25
        if (pm25 != null && pm25 > 25f) {
            score += 10
            factors.add("PM2.5 elevado (${pm25.toInt()} µg/m³)")
            recommendations.add("Evite atividades ao ar livre hoje")
        }

        // ── Clima ──────────────────────────────────────────────────────────────
        val humidity = env?.humidity ?: 50f
        if (humidity < 30f) {
            score += 10
            factors.add("Umidade muito baixa (${humidity.toInt()}%)")
            recommendations.add("Hidrate-se e use umidificador de ar")
        } else if (humidity < 40f) {
            score += 5
            factors.add("Umidade baixa (${humidity.toInt()}%)")
        }

        val temp = env?.temperatureCelsius ?: 25f
        if (temp > 35f) {
            score += 5
            factors.add("Temperatura elevada (${temp.toInt()}°C)")
        }

        val wind = env?.windSpeedKmh ?: 0f
        if (wind > 40f) {
            score += 5
            factors.add("Vento forte (${wind.toInt()} km/h) — dispersa poluentes")
            recommendations.add("Evite ambientes externos com vento forte")
        }

        // ── Sazonalidade ───────────────────────────────────────────────────────
        // No Brasil: temporada de pólen mar-abr e ago-set; inverno = ar seco
        val isPollenSeason = monthOfYear in listOf(3, 4, 8, 9)
        val isWinter = monthOfYear in listOf(6, 7, 8)

        if (isPollenSeason) {
            score += 8
            factors.add("Temporada de pólen (mês ${monthOfYear})")
            recommendations.add("Use máscara ao ar livre se possível")
        }
        if (isWinter) {
            score += 5
            factors.add("Inverno — ar seco e frio")
            recommendations.add("Mantenha ambientes ventilados e aquecidos")
        }

        // ── Score final e nível ────────────────────────────────────────────────
        val finalScore = score.coerceIn(0, 100)

        val level = when {
            finalScore >= 70 -> AsthmaRiskLevel.VERY_HIGH
            finalScore >= 45 -> AsthmaRiskLevel.HIGH
            finalScore >= 20 -> AsthmaRiskLevel.MODERATE
            else             -> AsthmaRiskLevel.LOW
        }

        if (level == AsthmaRiskLevel.LOW && recommendations.isEmpty()) {
            recommendations.add("Continue com sua medicação de manutenção")
        }

        return RiskScore(
            score = finalScore,
            level = level.name,
            factors = factors,
            recommendations = recommendations,
            calculatedAt = getCurrentTimeMillis()
        )
    }
}
