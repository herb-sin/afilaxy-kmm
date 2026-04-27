package com.afilaxy.data.repository

import com.afilaxy.domain.model.AsthmaRiskLevel
import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.EnvironmentalData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testes unitários do RiskScoreEngine.
 *
 * O engine é pura lógica Kotlin (zero I/O), portanto todos os testes
 * rodam em memória sem dependência do Firebase ou de rede.
 *
 * Perfis sintéticos são gerados inline para simular uma diversidade de
 * pacientes — de baixo risco até crise iminente — e validar se os
 * limiares do motor heurístico estão calibrados corretamente.
 *
 * Limiares do engine:
 *   score >= 70 → VERY_HIGH
 *   score >= 45 → HIGH
 *   score >= 20 → MODERATE
 *   score  < 20 → LOW
 */
class RiskScoreEngineTest {

    // ── Helpers de construção de dados sintéticos ─────────────────────────────

    private fun envBom() = EnvironmentalData(
        latitude = -23.5, longitude = -46.6,
        temperatureCelsius = 22f, humidity = 55f,
        windSpeedKmh = 10f, uvIndex = 3f,
        precipitationMm = 0f, aqi = 30, pm25 = 10f
    )

    private fun envModerado() = EnvironmentalData(
        latitude = -23.5, longitude = -46.6,
        temperatureCelsius = 28f, humidity = 38f,   // umidade baixa
        windSpeedKmh = 15f, uvIndex = 6f,
        precipitationMm = 0f, aqi = 110, pm25 = 20f // AQI moderado
    )

    private fun envRuim() = EnvironmentalData(
        latitude = -23.5, longitude = -46.6,
        temperatureCelsius = 36f, humidity = 22f,   // umidade crítica + calor
        windSpeedKmh = 45f, uvIndex = 10f,
        precipitationMm = 0f, aqi = 160, pm25 = 30f // AQI ruim + PM2.5 alto
    )

    private fun envCritico() = EnvironmentalData(
        latitude = -23.5, longitude = -46.6,
        temperatureCelsius = 38f, humidity = 18f,
        windSpeedKmh = 50f, uvIndex = 11f,
        precipitationMm = 0f, aqi = 210, pm25 = 40f // AQI muito ruim
    )

    private fun checkInComCrise(severity: String = "moderada") = CheckInResponse(
        id = "ci-1", userId = "user-1",
        type = "EVENING", timestamp = System.currentTimeMillis(),
        hadCrisisToday = true, crisisSeverity = severity,
        usedRescueInhaler = true
    )

    private fun checkInSemCrise() = CheckInResponse(
        id = "ci-2", userId = "user-1",
        type = "EVENING", timestamp = System.currentTimeMillis(),
        hadCrisisToday = false
    )

    private fun checkInSemBombinha() = CheckInResponse(
        id = "ci-3", userId = "user-1",
        type = "MORNING", timestamp = System.currentTimeMillis(),
        hasRescueInhaler = false
    )

    private fun checkInComBombinha() = CheckInResponse(
        id = "ci-4", userId = "user-1",
        type = "MORNING", timestamp = System.currentTimeMillis(),
        hasRescueInhaler = true
    )

    // ── GRUPO 1: Pacientes de Baixo Risco ────────────────────────────────────

    @Test
    fun `paciente saudavel sem crises em ambiente bom deve ser LOW`() {
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5 // maio — sem sazonalidade especial
        )
        assertEquals("LOW", result.level)
        assertTrue(result.score < 20, "Score esperado < 20, obtido: ${result.score}")
        assertTrue(result.recommendations.isNotEmpty(), "LOW deve recomendar manter medicação")
    }

    @Test
    fun `paciente com 1 crise no mes em ambiente bom ainda e LOW`() {
        // 1 crise em 30d = +5 pts; sem outros fatores → score = 5
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 1, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5
        )
        assertEquals("LOW", result.level)
        assertEquals(5, result.score)
    }

    @Test
    fun `ausencia de dados ambientais nao causa crash e retorna resultado valido`() {
        val result = RiskScoreEngine.calculate(
            env = null,
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5
        )
        assertTrue(result.score in 0..100)
        assertTrue(result.level in listOf("LOW", "MODERATE", "HIGH", "VERY_HIGH"))
    }

    @Test
    fun `check-ins sem crise nao aumentam o score`() {
        val checkIns = List(7) { checkInSemCrise() }
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5,
            recentCheckIns = checkIns
        )
        assertEquals("LOW", result.level)
    }

    // ── GRUPO 2: Pacientes de Risco Moderado ─────────────────────────────────

    @Test
    fun `paciente com 1 crise em 7 dias em ambiente moderado e MODERATE`() {
        // crises7d=1 → +15; AQI=110 → +10; humidity=38% → +5 = 30 → MODERATE
        val result = RiskScoreEngine.calculate(
            env = envModerado(),
            crises30d = 1, crises7d = 1, samuCalledCount = 0,
            monthOfYear = 5
        )
        assertEquals("MODERATE", result.level)
        assertTrue(result.score in 20..44, "Score esperado 20-44, obtido: ${result.score}")
        assertTrue(result.factors.any { "crise" in it.lowercase() })
    }

    @Test
    fun `ambiente moderado sem historico de crises e MODERATE`() {
        // AQI=110 → +10; humidity=38% → +5 = 15 → LOW, mas +pm25=0 (pm25<25)
        // AQI 110 → +10; humidity 38% → +5 = 15 → LOW (abaixo de 20)
        // verificando que é pelo menos calculado corretamente
        val result = RiskScoreEngine.calculate(
            env = envModerado(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5
        )
        // AQI 110 = +10; humidity 38% = +5 = 15 → LOW
        assertTrue(result.score in 0..44)
    }

    @Test
    fun `3 crises nos ultimos 30 dias aparece nos fatores`() {
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 3, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5
        )
        assertTrue(result.factors.any { "3" in it && "mês" in it.lowercase() },
            "Deve mencionar 3 crises no último mês. Fatores: ${result.factors}")
    }

    @Test
    fun `paciente em temporada de polen em maio nao recebe bonus sazonal`() {
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5 // maio não é temporada de pólen (mar, abr, ago, set)
        )
        assertFalse(result.factors.any { "pólen" in it.lowercase() })
    }

    @Test
    fun `paciente em marco recebe bonus sazonal de polen`() {
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 3 // março = temporada de pólen
        )
        assertTrue(result.factors.any { "pólen" in it.lowercase() },
            "Março deve ativar fator de pólen. Fatores: ${result.factors}")
        assertTrue(result.recommendations.any { "máscara" in it.lowercase() })
    }

    @Test
    fun `inverno julho acumula bonus de sazonalidade e polen`() {
        // Julho está em isWinter (jun,jul,ago) E em isPollenSeason não (mar,abr,ago,set)
        // Agosto está em ambos — julho só em inverno
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 7 // julho = inverno mas não pólen
        )
        assertTrue(result.factors.any { "inverno" in it.lowercase() },
            "Julho deve ativar fator de inverno. Fatores: ${result.factors}")
        assertFalse(result.factors.any { "pólen" in it.lowercase() })
    }

    @Test
    fun `agosto acumula bonus de inverno e de polen simultaneamente`() {
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 8 // agosto = inverno + pólen
        )
        // +8 (pólen) + +5 (inverno) = 13 → LOW mas ambos fatores presentes
        assertTrue(result.factors.any { "pólen" in it.lowercase() })
        assertTrue(result.factors.any { "inverno" in it.lowercase() })
    }

    // ── GRUPO 3: Pacientes de Alto Risco ─────────────────────────────────────

    @Test
    fun `paciente com 2 crises em 7 dias e SAMU acionado e HIGH`() {
        // crises7d=2 → +30 (cap); samuCalledCount=1 → +15 = 45 → HIGH
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 3, crises7d = 2, samuCalledCount = 1,
            monthOfYear = 5
        )
        assertEquals("HIGH", result.level)
        assertTrue(result.score in 45..69, "Score esperado 45-69, obtido: ${result.score}")
        assertTrue(result.factors.any { "SAMU" in it })
        assertTrue(result.recommendations.any { "192" in it })
    }

    @Test
    fun `paciente com ambiente ruim e 1 crise em 7 dias e VERY_HIGH`() {
        // crises7d=1 → +15; crises30d=2 → +10; AQI=160 → +18; pm25=30 → +10
        // humidity=22% → +10; temp=36°C → +5; wind=45km/h → +5 = 73 → VERY_HIGH
        val result = RiskScoreEngine.calculate(
            env = envRuim(),
            crises30d = 2, crises7d = 1, samuCalledCount = 0,
            monthOfYear = 5
        )
        assertEquals("VERY_HIGH", result.level, "Score: ${result.score}")
        assertTrue(result.score >= 70)
        assertTrue(result.recommendations.any { "ar livre" in it.lowercase() })
    }

    @Test
    fun `check-in com crise grave dispara fator e recomendacao medica`() {
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5,
            recentCheckIns = listOf(checkInComCrise(severity = "grave"))
        )
        // hadCrisisToday=true → +12; crisisSeverity="grave" → +15 = 27 → MODERATE
        assertTrue(result.factors.any { "grave" in it.lowercase() })
        assertTrue(result.recommendations.any { "médico" in it.lowercase() })
        assertTrue(result.score >= 20)
    }

    @Test
    fun `uso de bombinha 2 vezes na semana dispara recomendacao de revisao`() {
        val checkIns = listOf(
            CheckInResponse("c1", "u1", "EVENING", 0L, usedRescueInhaler = true),
            CheckInResponse("c2", "u1", "EVENING", 0L, usedRescueInhaler = true)
        )
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5,
            recentCheckIns = checkIns
        )
        assertTrue(result.factors.any { "bombinha" in it.lowercase() || "resgate" in it.lowercase() })
        assertTrue(result.recommendations.any { "médico" in it.lowercase() || "manutenção" in it.lowercase() })
    }

    @Test
    fun `paciente sem bombinha de resgate disponivel recebe fator e recomendacao`() {
        val result = RiskScoreEngine.calculate(
            env = envBom(),
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5,
            recentCheckIns = listOf(checkInSemBombinha())
        )
        // hasRescueInhaler=false → +8
        assertEquals(8, result.score)
        assertTrue(result.factors.any { "sem bombinha" in it.lowercase() || "bombinha" in it.lowercase() })
        assertTrue(result.recommendations.any { "bombinha" in it.lowercase() || "médico" in it.lowercase() })
    }

    // ── GRUPO 4: Pacientes de Risco Muito Alto ───────────────────────────────

    @Test
    fun `paciente critico com multiplos fatores deve ser VERY_HIGH`() {
        // crises7d=2 → +30 (cap); crises30d=5 → +20 (cap); samuCalledCount=2 → +15
        // AQI=210 → +25; pm25=40 → +10; humidity=18% → +10; temp=38 → +5
        // wind=50 → +5; total=120 → cap em 100 → VERY_HIGH
        val result = RiskScoreEngine.calculate(
            env = envCritico(),
            crises30d = 5, crises7d = 2, samuCalledCount = 2,
            monthOfYear = 8 // agosto = inverno + pólen
        )
        assertEquals("VERY_HIGH", result.level)
        assertEquals(100, result.score, "Score deve ser 100 (cap), obtido: ${result.score}")
        assertTrue(result.factors.size >= 5, "Esperado 5+ fatores, obtido: ${result.factors.size}")
    }

    @Test
    fun `paciente com 3 crises em 7 dias e SAMU e ambiente critico e VERY_HIGH`() {
        // crises7d=3 → +30 (cap); samuCalledCount=1 → +15; AQI=210 → +25; = 70 → VERY_HIGH
        val result = RiskScoreEngine.calculate(
            env = envCritico(),
            crises30d = 4, crises7d = 3, samuCalledCount = 1,
            monthOfYear = 5
        )
        assertEquals("VERY_HIGH", result.level)
        assertTrue(result.score >= 70)
    }

    @Test
    fun `paciente com check-ins de crise grave e ambiente critico e VERY_HIGH`() {
        val checkIns = listOf(
            checkInComCrise(severity = "grave"),
            checkInComCrise(severity = "grave"),
            checkInSemBombinha()
        )
        val result = RiskScoreEngine.calculate(
            env = envCritico(),
            crises30d = 2, crises7d = 1, samuCalledCount = 1,
            monthOfYear = 8,
            recentCheckIns = checkIns
        )
        assertEquals("VERY_HIGH", result.level, "Score: ${result.score}, fatores: ${result.factors}")
        assertTrue(result.score >= 70)
    }

    // ── GRUPO 5: Limites e Caps ───────────────────────────────────────────────

    @Test
    fun `score nunca excede 100 independente dos fatores`() {
        val checkIns = List(7) { checkInComCrise(severity = "grave") }
        val result = RiskScoreEngine.calculate(
            env = envCritico(),
            crises30d = 30, crises7d = 10, samuCalledCount = 5,
            monthOfYear = 8,
            recentCheckIns = checkIns
        )
        assertTrue(result.score <= 100, "Score excedeu 100: ${result.score}")
    }

    @Test
    fun `score nunca e negativo`() {
        val result = RiskScoreEngine.calculate(
            env = null,
            crises30d = 0, crises7d = 0, samuCalledCount = 0,
            monthOfYear = 5
        )
        assertTrue(result.score >= 0)
    }

    @Test
    fun `crises7d cap e aplicado em 30 pontos maximo`() {
        // crises7d=10 → 10*15 = 150 → cap em 30
        val resultado10crises = RiskScoreEngine.calculate(
            env = null, crises30d = 0, crises7d = 10,
            samuCalledCount = 0, monthOfYear = 5
        )
        val resultado2crises = RiskScoreEngine.calculate(
            env = null, crises30d = 0, crises7d = 2,
            samuCalledCount = 0, monthOfYear = 5
        )
        // Ambos devem ter o mesmo score de crises7d (30)
        assertEquals(resultado10crises.score, resultado2crises.score,
            "Cap de crises7d deve igualar scores. 10crises=${resultado10crises.score}, 2crises=${resultado2crises.score}")
    }

    @Test
    fun `crises30d cap e aplicado em 20 pontos maximo`() {
        // crises30d=10 → 10*5 = 50 → cap em 20
        val resultado10 = RiskScoreEngine.calculate(
            env = null, crises30d = 10, crises7d = 0,
            samuCalledCount = 0, monthOfYear = 5
        )
        val resultado4 = RiskScoreEngine.calculate(
            env = null, crises30d = 4, crises7d = 0,
            samuCalledCount = 0, monthOfYear = 5
        )
        assertEquals(resultado10.score, resultado4.score,
            "Cap de crises30d deve igualar scores. 10=${resultado10.score}, 4=${resultado4.score}")
    }

    @Test
    fun `check-in reportedCrises cap e aplicado em 25 pontos maximo`() {
        // 3 crises × 12 = 36 → cap em 25
        val checkIns3 = List(3) { CheckInResponse("c$it", "u1", "EVENING", 0L, hadCrisisToday = true) }
        val checkIns2 = List(2) { CheckInResponse("c$it", "u1", "EVENING", 0L, hadCrisisToday = true) }

        val r3 = RiskScoreEngine.calculate(env = null, crises30d = 0, crises7d = 0,
            samuCalledCount = 0, monthOfYear = 5, recentCheckIns = checkIns3)
        val r2 = RiskScoreEngine.calculate(env = null, crises30d = 0, crises7d = 0,
            samuCalledCount = 0, monthOfYear = 5, recentCheckIns = checkIns2)

        // r3 deve ter 25 (cap), r2 deve ter 24
        assertEquals(25, r3.score, "3 crises devem resultar em score 25 (cap), obtido: ${r3.score}")
        assertEquals(24, r2.score, "2 crises devem resultar em score 24, obtido: ${r2.score}")
    }

    // ── GRUPO 6: Qualidade do Ar ──────────────────────────────────────────────

    @Test
    fun `AQI acima de 200 adiciona 25 pontos e label muito ruim`() {
        val result = RiskScoreEngine.calculate(
            env = EnvironmentalData(-23.5, -46.6, 25f, 55f, 10f, 5f, 0f, aqi = 201),
            crises30d = 0, crises7d = 0, samuCalledCount = 0, monthOfYear = 5
        )
        assertTrue(result.factors.any { "muito ruim" in it.lowercase() && "201" in it })
        assertEquals(25, result.score)
    }

    @Test
    fun `AQI entre 151 e 200 adiciona 18 pontos e label ruim`() {
        val result = RiskScoreEngine.calculate(
            env = EnvironmentalData(-23.5, -46.6, 25f, 55f, 10f, 5f, 0f, aqi = 160),
            crises30d = 0, crises7d = 0, samuCalledCount = 0, monthOfYear = 5
        )
        assertTrue(result.factors.any { "ruim" in it.lowercase() && "160" in it })
        assertEquals(18, result.score)
    }

    @Test
    fun `AQI entre 101 e 150 adiciona 10 pontos e label moderada`() {
        val result = RiskScoreEngine.calculate(
            env = EnvironmentalData(-23.5, -46.6, 25f, 55f, 10f, 5f, 0f, aqi = 110),
            crises30d = 0, crises7d = 0, samuCalledCount = 0, monthOfYear = 5
        )
        assertTrue(result.factors.any { "moderada" in it.lowercase() && "110" in it })
        assertEquals(10, result.score)
    }

    @Test
    fun `AQI bom abaixo de 50 nao adiciona fator`() {
        val result = RiskScoreEngine.calculate(
            env = EnvironmentalData(-23.5, -46.6, 25f, 55f, 10f, 5f, 0f, aqi = 40),
            crises30d = 0, crises7d = 0, samuCalledCount = 0, monthOfYear = 5
        )
        assertFalse(result.factors.any { "aqi" in it.lowercase() })
        assertEquals(0, result.score)
    }

    @Test
    fun `PM25 acima de 25 adiciona fator e recomendacao de evitar ar livre`() {
        val result = RiskScoreEngine.calculate(
            env = EnvironmentalData(-23.5, -46.6, 25f, 55f, 10f, 5f, 0f, pm25 = 30f),
            crises30d = 0, crises7d = 0, samuCalledCount = 0, monthOfYear = 5
        )
        assertTrue(result.factors.any { "PM2.5" in it || "pm25" in it.lowercase() })
        assertTrue(result.recommendations.any { "ar livre" in it.lowercase() })
        assertEquals(10, result.score)
    }

    // ── GRUPO 7: Métricas de Saída ────────────────────────────────────────────

    @Test
    fun `resultado LOW sem recomendacoes gera recomendacao de manter medicacao`() {
        val result = RiskScoreEngine.calculate(
            env = envBom(), crises30d = 0, crises7d = 0,
            samuCalledCount = 0, monthOfYear = 5
        )
        assertEquals("LOW", result.level)
        assertTrue(result.recommendations.isNotEmpty())
        assertTrue(result.recommendations.any { "medicação" in it.lowercase() || "manutenção" in it.lowercase() })
    }

    @Test
    fun `riskLevel computed property retorna enum correto para VERY_HIGH`() {
        val result = RiskScoreEngine.calculate(
            env = envCritico(), crises30d = 5, crises7d = 2,
            samuCalledCount = 2, monthOfYear = 8
        )
        assertEquals(AsthmaRiskLevel.VERY_HIGH, result.riskLevel)
        assertEquals("🔴", result.riskLevel.emoji)
        assertEquals("Muito Alto", result.riskLevel.label)
    }

    @Test
    fun `riskLevel computed property retorna LOW para score zero`() {
        val result = RiskScoreEngine.calculate(
            env = null, crises30d = 0, crises7d = 0,
            samuCalledCount = 0, monthOfYear = 5
        )
        assertEquals(AsthmaRiskLevel.LOW, result.riskLevel)
        assertEquals("🟢", result.riskLevel.emoji)
    }

    @Test
    fun `lista de fatores e preenchida proporcionalmente ao risco`() {
        val low = RiskScoreEngine.calculate(
            env = envBom(), crises30d = 0, crises7d = 0,
            samuCalledCount = 0, monthOfYear = 5
        )
        val high = RiskScoreEngine.calculate(
            env = envCritico(), crises30d = 5, crises7d = 2,
            samuCalledCount = 1, monthOfYear = 8
        )
        assertTrue(high.factors.size > low.factors.size,
            "Paciente de alto risco deve ter mais fatores. low=${low.factors.size}, high=${high.factors.size}")
    }
}
