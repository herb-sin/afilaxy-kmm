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
import com.afilaxy.util.last7DayUtcKeys
import com.afilaxy.util.sumRollingDays
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round
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
            // LGPD/Política de Privacidade: só "localização aproximada" é prometida a
            // processadores terceiros (WAQI, OpenMeteo) — arredonda para 0.001° ≈ 111m,
            // mesma técnica já usada em EmergencyRepositoryImpl para o Modo Ajudante.
            // Precisão de metros não faz diferença nenhuma para clima/qualidade do ar.
            val roundedLat = round(latitude * 1000) / 1000.0
            val roundedLon = round(longitude * 1000) / 1000.0

            // Busca clima (OpenMeteo — sem chave, gratuito)
            val meteo = try {
                httpClient.get("https://api.open-meteo.com/v1/forecast") {
                    parameter("latitude", roundedLat)
                    parameter("longitude", roundedLon)
                    parameter("current", "temperature_2m,relative_humidity_2m,wind_speed_10m,uv_index,precipitation")
                    parameter("timezone", "auto")
                }.body<OpenMeteoResponse>()
            } catch (e: Exception) { null }

            // Busca qualidade do ar (WAQI)
            val waqi = try {
                httpClient.get("https://api.waqi.info/feed/geo:$roundedLat;$roundedLon/") {
                    parameter("token", waqiToken)
                }.body<WaqiResponse>()
            } catch (e: Exception) { null }

            val current = meteo?.current
            val waqiData = if (waqi?.status == "ok") waqi.data else null

            Result.success(
                EnvironmentalData(
                    latitude = roundedLat,
                    longitude = roundedLon,
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
                @Suppress("UNCHECKED_CAST")
                val dailyMap = try { userStatsDoc?.get<Any?>("dailyCount") as? Map<String, Any> }
                    catch (e: Exception) { null }
                val rolling = sumRollingDays(dailyMap, last7DayUtcKeys())
                crises7d = rolling ?: run {
                    // Fallback: ISO week (legacy records without dailyCount)
                    val nowDate = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                    val dayOfYear = nowDate.date.dayOfYear
                    val weekdayNum = nowDate.date.dayOfWeek.ordinal + 1
                    val isoWeek = (dayOfYear - weekdayNum + 10) / 7
                    val currentWeekKey = "${nowDate.year}-W${isoWeek.toString().padStart(2, '0')}"
                    weeklyMap?.get(currentWeekKey)?.let { v ->
                        when (v) { is Long -> v.toInt(); is Double -> v.toInt(); is Number -> v.toInt(); else -> 0 }
                    } ?: weeklyMap?.values?.maxOfOrNull { v ->
                        when (v) { is Long -> v.toInt(); is Double -> v.toInt(); is Number -> v.toInt(); else -> 0 }
                    } ?: 0
                }
            }

            // samuCalled: last 12 months to bound the scan as the dataset grows
            val twelveMonthsAgo = getCurrentTimeMillis() - 365L * 24 * 3600 * 1000
            val samuCalledCount = try {
                firestore.collection("emergency_requests")
                    .where {
                        ("requesterId" equalTo userId) and
                        ("samuCalled" equalTo true) and
                        ("timestamp" greaterThanOrEqualTo twelveMonthsAgo)
                    }
                    .get().documents.size
            } catch (e: Exception) { 0 }

            // Check-ins da Agenda de Saúde (últimos 7 dias)
            val recentCheckIns = try {
                checkInRepository?.getRecentCheckIns(userId, days = 7)?.getOrNull() ?: emptyList()
            } catch (e: Exception) { emptyList() }

            // Comorbidades autodeclaradas no Perfil Médico (ver RiskScoreEngine)
            val userDoc = try { firestore.collection("users").document(userId).get() } catch (e: Exception) { null }
            val hasGerd: Boolean = try { userDoc?.get("healthData.hasGerd") ?: false } catch (e: Exception) { false }
            val hasSleepApnea: Boolean = try { userDoc?.get("healthData.hasSleepApnea") ?: false } catch (e: Exception) { false }
            val hasRhinitis: Boolean = try { userDoc?.get("healthData.hasRhinitis") ?: false } catch (e: Exception) { false }
            val hasObesity: Boolean = try { userDoc?.get("healthData.hasObesity") ?: false } catch (e: Exception) { false }
            val hasFoodAllergy: Boolean = try { userDoc?.get("healthData.hasFoodAllergy") ?: false } catch (e: Exception) { false }
            val hasNsaidAllergy: Boolean = try { userDoc?.get("healthData.hasNsaidAllergy") ?: false } catch (e: Exception) { false }
            val hasInhalantAllergy: Boolean = try { userDoc?.get("healthData.hasInhalantAllergy") ?: false } catch (e: Exception) { false }

            // Mês atual → sazonalidade
            val month = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber

            val riskScore = RiskScoreEngine.calculate(
                env = env,
                crises30d = crises30d,
                crises7d = crises7d,
                samuCalledCount = samuCalledCount,
                monthOfYear = month,
                recentCheckIns = recentCheckIns,
                hasGerd = hasGerd,
                hasSleepApnea = hasSleepApnea,
                hasRhinitis = hasRhinitis,
                hasObesity = hasObesity,
                hasFoodAllergy = hasFoodAllergy,
                hasNsaidAllergy = hasNsaidAllergy,
                hasInhalantAllergy = hasInhalantAllergy
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
        recentCheckIns: List<CheckInResponse> = emptyList(),
        hasGerd: Boolean = false,
        hasSleepApnea: Boolean = false,
        hasRhinitis: Boolean = false,
        hasObesity: Boolean = false,
        hasFoodAllergy: Boolean = false,
        hasNsaidAllergy: Boolean = false,
        hasInhalantAllergy: Boolean = false
    ): RiskScore {
        var score = 0
        val factors = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // ── Histórico de crises (emergências registradas) ─────────────────────
        // ≥2 pedidos de ajuda/semana é sinal forte de descontrole — peso não-linear.
        val crises7dScore = when {
            crises7d >= 5 -> 55
            crises7d >= 3 -> 40
            crises7d == 2 -> 28
            crises7d == 1 -> 14
            else -> 0
        }
        score += crises7dScore
        if (crises7d > 0) {
            val label = if (crises7d >= 2) "⚠️ ${crises7d} pedido(s) de ajuda nos últimos 7 dias — tipifica descontrole"
                        else "${crises7d} pedido(s) de ajuda nos últimos 7 dias"
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

        // ── Bem-estar matinal comprometido (humor + energia, autorrelato) ─────
        // nighttimeAwakening (bucket próprio acima) já cobre o sono — não repetido aqui.
        if (recentCheckIns.isNotEmpty()) {
            val lowMorningDays = recentCheckIns.count { ci ->
                ci.type == "MORNING" &&
                    listOfNotNull(ci.morningMoodGood, ci.morningEnergyGood).count { !it } >= 2
            }
            if (lowMorningDays > 0) {
                score += (lowMorningDays * 8).coerceAtMost(25)
                factors.add("$lowMorningDays manhã(s) com humor/energia comprometidos esta semana")
                if (lowMorningDays >= 3) {
                    recommendations.add("Considere buscar apoio profissional para seu bem-estar")
                }
            }
        }

        // ── Autocuidado noturno comprometido (autorrelato) ────────────────────
        // Sinal genérico de bem-estar, não específico da GINA — peso mais modesto
        // que despertar noturno/dificuldade respiratória/limitação de atividades,
        // que já têm bucket próprio acima.
        if (recentCheckIns.isNotEmpty()) {
            val lowSelfCareDays = recentCheckIns.count { it.type == "EVENING" && it.selfCareGood == false }
            val selfCareScore = when {
                lowSelfCareDays >= 4 -> 15
                lowSelfCareDays >= 2 -> 9
                lowSelfCareDays == 1 -> 4
                else -> 0
            }
            score += selfCareScore
            if (lowSelfCareDays > 0) {
                factors.add("$lowSelfCareDays noite(s) com autocuidado comprometido esta semana")
                if (lowSelfCareDays >= 3) {
                    recommendations.add("Considere buscar apoio profissional para seu bem-estar")
                }
            }
        }

        // ── Uso do inalador de resgate (autorrelato, check-in noturno) ────────
        // GINA: uso de resgate >2x/semana é um dos critérios clássicos de asma não
        // controlada. Soma o autorrelato dos últimos 7 dias — mesma janela de crises7d —
        // para não penalizar um pico isolado num único dia.
        if (recentCheckIns.isNotEmpty()) {
            val rescueUses7d = recentCheckIns.sumOf { it.rescueInhalerUses ?: 0 }
            val rescueScore = when {
                rescueUses7d >= 7 -> 35
                rescueUses7d >= 3 -> 22
                rescueUses7d == 2 -> 10
                else -> 0
            }
            score += rescueScore
            if (rescueUses7d > 2) {
                factors.add("⚠️ Bombinha de resgate usada $rescueUses7d vez(es) nos últimos 7 dias — acima do recomendado pela GINA (até 2x/semana)")
                recommendations.add("Fale com seu médico sobre ajustar seu tratamento de manutenção")
            } else if (rescueUses7d > 0) {
                factors.add("Bombinha de resgate usada $rescueUses7d vez(es) nos últimos 7 dias")
            }
        }

        // ── Despertar noturno (autorrelato, check-in matinal) ─────────────────
        // GINA: despertar noturno é um dos critérios de controle de asma, junto com
        // sintomas diurnos, uso de resgate e limitação de atividades.
        if (recentCheckIns.isNotEmpty()) {
            val awakeningDays = recentCheckIns.count { it.nighttimeAwakening == false }
            val awakeningScore = when {
                awakeningDays >= 4 -> 25
                awakeningDays >= 2 -> 15
                awakeningDays == 1 -> 6
                else -> 0
            }
            score += awakeningScore
            if (awakeningDays > 0) {
                factors.add("Sono interrompido em $awakeningDays dia(s) nos últimos 7 dias")
            }
        }

        // ── Dificuldade respiratória diurna (autorrelato, check-in noturno) ───
        // GINA: sintomas diurnos frequentes (>2x/semana) são critério de asma não
        // controlada.
        if (recentCheckIns.isNotEmpty()) {
            val breathingDifficultyDays = recentCheckIns.count { it.daytimeBreathingEase == false }
            val breathingScore = when {
                breathingDifficultyDays >= 4 -> 25
                breathingDifficultyDays >= 3 -> 18
                breathingDifficultyDays == 2 -> 10
                breathingDifficultyDays == 1 -> 4
                else -> 0
            }
            score += breathingScore
            if (breathingDifficultyDays > 2) {
                factors.add("⚠️ Dificuldade respiratória relatada em $breathingDifficultyDays dia(s) nos últimos 7 dias")
            } else if (breathingDifficultyDays > 0) {
                factors.add("Dificuldade respiratória relatada em $breathingDifficultyDays dia(s) nos últimos 7 dias")
            }
        }

        // ── Limitação de atividades (autorrelato, check-in noturno) ───────────
        // GINA: limitação de atividades diárias é um dos critérios de controle.
        if (recentCheckIns.isNotEmpty()) {
            val limitedDays = recentCheckIns.count { it.activityAsPlanned == false }
            val limitedScore = when {
                limitedDays >= 4 -> 20
                limitedDays >= 2 -> 12
                limitedDays == 1 -> 5
                else -> 0
            }
            score += limitedScore
            if (limitedDays > 0) {
                factors.add("Atividades planejadas não realizadas em $limitedDays dia(s) nos últimos 7 dias")
                if (limitedDays >= 3) {
                    recommendations.add("Considere conversar com seu médico sobre o impacto no seu dia a dia")
                }
            }
        }

        // ── Comorbidades autodeclaradas (Perfil Médico) ───────────────────────
        // GINA: refluxo gastroesofágico (DRGE), apneia do sono, rinite alérgica e
        // obesidade são citados como fatores de risco para exacerbação de asma.
        // Autodeclarado, sem verificação clínica — peso modesto e fixo por item.
        val comorbidityCount = listOf(hasGerd, hasSleepApnea, hasRhinitis, hasObesity).count { it }
        if (comorbidityCount > 0) {
            score += (comorbidityCount * 6).coerceAtMost(20)
            factors.add("$comorbidityCount comorbidade(s) autodeclarada(s) no Perfil Médico")
        }

        // ── Alergias específicas autodeclaradas (Perfil Médico) ──────────────────
        // GINA cita alergia alimentar confirmada e sensibilidade a AINEs/aspirina
        // (AERD) como fatores de risco para crises quase-fatais/fatais; alergia a
        // inalantes (ácaros, pólen, mofo, pelos de animais) como fator de risco de
        // exacerbação por exposição a alérgeno ao qual o paciente é sensibilizado.
        // Autodeclarado, sem verificação clínica — bucket próprio, separado do das
        // comorbidades, pra não diluir o peso já calibrado delas.
        var allergyScore = 0
        if (hasFoodAllergy) allergyScore += 7
        if (hasNsaidAllergy) allergyScore += 7
        if (hasInhalantAllergy) allergyScore += 4
        if (allergyScore > 0) {
            score += allergyScore.coerceAtMost(15)
            factors.add("Alergia(s) específica(s) autodeclarada(s) no Perfil Médico")
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
            calculatedAt = getCurrentTimeMillis(),
            aqi = env?.aqi,
            temperature = env?.temperatureCelsius,
            humidity = env?.humidity
        )
    }
}
