package com.afilaxy.data.repository

import com.afilaxy.domain.model.ACT_INTERVAL_MS
import com.afilaxy.domain.model.AsthmaControlTestResult
import com.afilaxy.domain.model.SEVERE_EXACERBATION_WINDOW_MS
import com.afilaxy.domain.model.daysSince
import com.afilaxy.domain.model.declaredSevereExacerbationIsCurrent
import com.afilaxy.domain.model.isRecent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Regras de validade do histórico de crises graves (Perfil) e do intervalo do ACT. */
class SevereExacerbationHistoryTest {

    private val now = 1_800_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `contagem informada vale por 12 meses a partir de quando foi informada`() {
        assertTrue(declaredSevereExacerbationIsCurrent(count = 2, updatedAtMs = now - 30 * day, nowMs = now))
        assertFalse(declaredSevereExacerbationIsCurrent(count = 2, updatedAtMs = now - SEVERE_EXACERBATION_WINDOW_MS, nowMs = now))
    }

    @Test
    fun `contagem zero ou nunca informada nao conta como crise`() {
        assertFalse(declaredSevereExacerbationIsCurrent(count = 0, updatedAtMs = now - day, nowMs = now))
        assertFalse(declaredSevereExacerbationIsCurrent(count = 1, updatedAtMs = 0L, nowMs = now))
    }

    private fun act(timestamp: Long, steroid: Boolean? = null, er: Boolean? = null) = AsthmaControlTestResult(
        userId = "u", timestamp = timestamp,
        activityImpairment = 5, shortnessOfBreath = 5, nighttimeSymptoms = 5,
        rescueMedicationUse = 5, selfRatedControl = 5, score = 25,
        oralSteroidCourse4w = steroid, emergencyVisit4w = er
    )

    @Test
    fun `ACT marca crise grave por corticoide oral ou pronto-socorro`() {
        assertTrue(act(now, steroid = true, er = false).hadSevereExacerbation)
        assertTrue(act(now, steroid = false, er = true).hadSevereExacerbation)
        assertFalse(act(now, steroid = false, er = false).hadSevereExacerbation)
        // Resultados antigos, sem as perguntas complementares
        assertFalse(act(now).hadSevereExacerbation)
    }

    @Test
    fun `ACT com menos de 28 dias e recente`() {
        assertTrue(act(now - 12 * day).isRecent(now))
        assertFalse(act(now - ACT_INTERVAL_MS).isRecent(now))
        assertEquals(12, act(now - 12 * day).daysSince(now))
    }
}
