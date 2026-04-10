package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

/** Tipos de check-in disponíveis. */
enum class CheckInType { MORNING, EVENING }

/**
 * Resposta de um check-in — gravada no Firestore para training do modelo ML.
 * Campos ambientais e clínicos são capturados automaticamente no momento da resposta.
 */
@Serializable
data class CheckInResponse(
    val id: String = "",
    val userId: String,
    val type: String,                  // CheckInType.name
    val timestamp: Long,

    // ── Bombinha de resgate (check-in matinal) ─────────────────────────────
    val hasRescueInhaler: Boolean? = null,
    val rescueInhalerName: String? = null, // ex: "Aerolin", "Salbutamol"

    // ── Crise (check-in noturno) ───────────────────────────────────────────
    val hadCrisisToday: Boolean? = null,
    val crisisSeverity: String? = null,    // "leve", "moderada", "grave"
    val usedRescueInhaler: Boolean? = null,

    // ── Contexto ambiental capturado automaticamente ───────────────────────
    val riskScore: Int? = null,
    val aqi: Int? = null,
    val temperature: Float? = null,
    val humidity: Float? = null,

    // ── Contexto temporal ─────────────────────────────────────────────────
    val hourOfDay: Int? = null,
    val dayOfWeek: Int? = null,           // 1=seg, 7=dom
    val monthOfYear: Int? = null,

    // ── Perfil clínico capturado automaticamente ───────────────────────────
    val asmaType: String? = null,         // AsmaType.name
    val asmaTypeSeverity: String? = null  // "leve", "moderada", "grave"
)
