package com.afilaxy.data.repository

import com.afilaxy.domain.model.EnvironmentalData
import com.afilaxy.domain.model.AsthmaRiskLevel
import com.afilaxy.domain.model.RiskScore
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
    private val waqiToken: String = WAQI_DEFAULT_TOKEN
) : EnvironmentalRepository {

    internal companion object {
        /**
         * Token padrão vazio — a chave real é injetada via Koin no módulo DI.
         * Para desenvolvimento local: adicione WAQI_API_TOKEN=<sua_chave> em local.properties
         * Para CI: configure o GitHub Secret WAQI_API_TOKEN
         * Registre em: https://aqicn.org/data-platform/token/
         */
        const val WAQI_DEFAULT_TOKEN = ""
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
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
        longitude: Double
    ): Result<RiskScore> {
        return try {
            val env = getEnvironmentalData(latitude, longitude).getOrNull()

            // Histórico de crises dos últimos 30 dias
            val thirtyDaysAgo = getCurrentTimeMillis() - 30L * 24 * 3600 * 1000
            val sevenDaysAgo = getCurrentTimeMillis() - 7L * 24 * 3600 * 1000

            val history = try {
                firestore.collection("emergency_requests")
                    .where { ("requesterId" equalTo userId) and ("timestamp" greaterThanOrEqualTo thirtyDaysAgo) }
                    .get().documents
            } catch (e: Exception) { emptyList() }

            val crises30d = history.size
            val crises7d = history.count { doc ->
                (doc.get<Long?>("timestamp") ?: 0L) >= sevenDaysAgo
            }
            val samuCalledCount = history.count { doc ->
                doc.get<Boolean?>("samuCalled") == true
            }

            // Mês atual → sazonalidade
            val month = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber

            Result.success(
                RiskScoreEngine.calculate(
                    env = env,
                    crises30d = crises30d,
                    crises7d = crises7d,
                    samuCalledCount = samuCalledCount,
                    monthOfYear = month
                )
            )
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
        monthOfYear: Int
    ): RiskScore {
        var score = 0
        val factors = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // ── Histórico de crises ────────────────────────────────────────────────
        score += (crises7d * 15).coerceAtMost(30)
        if (crises7d > 0) factors.add("${crises7d} crise(s) nos últimos 7 dias")

        score += (crises30d * 5).coerceAtMost(20)
        if (crises30d >= 3) factors.add("${crises30d} crises no último mês")

        if (samuCalledCount > 0) {
            score += 15
            factors.add("SAMU acionado em crises anteriores")
            recommendations.add("Tenha o número do SAMU (192) salvo no celular")
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
