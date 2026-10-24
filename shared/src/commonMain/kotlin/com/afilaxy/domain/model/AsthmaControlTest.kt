package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

/**
 * Classificação do controle da asma pelo ACT (Asthma Control Test — 5 perguntas,
 * escore de 5 a 25). Cortes confirmados empiricamente contra a ferramenta pública
 * de um pneumologista parceiro (padrão internacional do instrumento, não o corte
 * alternativo de 18 usado na validação brasileira do JBP).
 */
enum class AsthmaControlLevel(val range: IntRange, val label: String) {
    WELL_CONTROLLED(20..25, "Bem controlada"),
    PARTIALLY_CONTROLLED(15..19, "Parcialmente controlada"),
    NOT_CONTROLLED(5..14, "Não controlada");

    companion object {
        fun fromScore(score: Int): AsthmaControlLevel =
            entries.firstOrNull { score in it.range } ?: NOT_CONTROLLED
    }
}

/**
 * Resposta de um Teste de Controle da Asma (ACT) — cada pergunta vale de 1 a 5,
 * escore total de 5 a 25. As 5 perguntas seguem o instrumento ACT padrão
 * (janela retrospectiva de 4 semanas), não critérios próprios do Afilaxy.
 */
@Serializable
data class AsthmaControlTestResult(
    val id: String = "",
    val userId: String,
    val timestamp: Long,
    val activityImpairment: Int,   // P1: limitação de atividades
    val shortnessOfBreath: Int,    // P2: falta de ar
    val nighttimeSymptoms: Int,    // P3: despertares noturnos
    val rescueMedicationUse: Int,  // P4: uso de medicação de alívio
    val selfRatedControl: Int,     // P5: autoavaliação do controle
    val score: Int                 // soma das 5 respostas, 5-25
) {
    val level: AsthmaControlLevel get() = AsthmaControlLevel.fromScore(score)
}
