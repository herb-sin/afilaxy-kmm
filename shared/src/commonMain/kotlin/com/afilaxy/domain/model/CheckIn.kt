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

    // ── Bem-estar matinal (humor, energia) ────────────────────────────────
    val morningMoodGood: Boolean? = null,   // "Me sinto bem esta manhã"
    val morningEnergyGood: Boolean? = null, // "Estou com boa energia"

    // ── Bem-estar noturno (dia, atividade, autocuidado) ───────────────────
    val hadGoodDay: Boolean? = null,           // "Tive um bom dia"
    val physicalActivityDone: Boolean? = null, // "Pratiquei atividade física"
    val selfCareGood: Boolean? = null,         // "Me cuidei bem hoje"

    // Autorrelato — quantas vezes usou a bombinha de resgate no dia (só no check-in noturno,
    // que olha para o dia inteiro já vivido). Usado pelo motor de risco para aproximar o
    // critério da GINA de uso de resgate >2x/semana. Null = pergunta não respondida/não exibida.
    val rescueInhalerUses: Int? = null,

    // ── Sinais adicionais de bem-estar, usados pelo motor de risco para aproximar outros
    // critérios do Controle da GINA (despertar noturno, sintomas diurnos, limitação de
    // atividades). Perguntas genéricas de bem-estar na UI — nunca mencionam asma/sintomas.
    val nighttimeAwakening: Boolean? = null,   // matinal: "Meu sono foi tranquilo, sem interrupções?"
    val daytimeBreathingEase: Boolean? = null, // noturno: "Respirei com facilidade ao longo do dia?"
    val activityAsPlanned: Boolean? = null,    // noturno: "Consegui fazer tudo que tinha planejado hoje?"

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
