package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

/** Tipos de check-in disponíveis. */
enum class CheckInType { MORNING, EVENING }

/**
 * Resposta de um check-in — gravada no Firestore para análise de bem-estar.
 * Campos de bem-estar e contexto ambiental capturados no momento da resposta.
 */
@Serializable
data class CheckInResponse(
    val id: String = "",
    val userId: String,
    val type: String,                  // CheckInType.name
    val timestamp: Long,

    // ── Bem-estar (matinal: sono/humor/energia | noturno: dia/atividade/autocuidado) ──
    val wellbeingA: Boolean? = null,
    val wellbeingB: Boolean? = null,
    val wellbeingC: Boolean? = null,

    // Autorrelato — quantas vezes usou a bombinha de resgate no dia (só no check-in noturno,
    // que olha para o dia inteiro já vivido). Usado pelo motor de risco para aproximar o
    // critério da GINA de uso de resgate >2x/semana. Null = pergunta não respondida/não exibida.
    val rescueInhalerUses: Int? = null,

    // ── Contexto ambiental capturado automaticamente ───────────────────────
    val riskScore: Int? = null,
    val aqi: Int? = null,
    val temperature: Float? = null,
    val humidity: Float? = null,

    // ── Contexto temporal ─────────────────────────────────────────────────
    val hourOfDay: Int? = null,
    val dayOfWeek: Int? = null,           // 1=seg, 7=dom
    val monthOfYear: Int? = null
)
