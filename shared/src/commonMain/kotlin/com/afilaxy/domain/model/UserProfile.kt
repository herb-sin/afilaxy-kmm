package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val healthData: UserHealthData? = null,
    val emergencyContact: EmergencyContact? = null,
    val isHealthProfessional: Boolean = false
)

@Serializable
data class UserHealthData(
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val notes: String = "",
    // Comorbidades autodeclaradas — fatores de risco citados pela GINA, sem verificação clínica.
    val hasGerd: Boolean = false,
    val hasSleepApnea: Boolean = false,
    val hasRhinitis: Boolean = false,
    val hasObesity: Boolean = false,
    // Alergias específicas autodeclaradas — fatores de risco citados pela GINA para
    // crise grave/quase-fatal (alimentar, AINEs/aspirina) ou exacerbação por exposição
    // a alérgeno inalante. Separado do campo de texto livre "allergies" acima.
    val hasFoodAllergy: Boolean = false,
    val hasNsaidAllergy: Boolean = false,
    val hasInhalantAllergy: Boolean = false,
    // Histórico de crises graves autodeclarado — fator de risco da GINA. Crise grave =
    // precisou de corticoide oral por ≥3 dias, pronto-socorro/UPA ou internação.
    // A contagem cobre os 12 meses anteriores à data em que foi informada
    // (severeExacerbationsUpdatedAt, epoch ms; 0 = nunca informada), então perde
    // validade depois de um ano. UTI/intubação é "alguma vez na vida" — fator de
    // risco da GINA para crise fatal.
    val severeExacerbations12m: Int = 0,
    val severeExacerbationsUpdatedAt: Long = 0L,
    val everIcuOrIntubated: Boolean = false,
    // Informações para quem for te ajudar — mostradas só ao Helper que aceitar o
    // pedido numa emergência ativa. NÃO alimentam o motor de risco (propósito
    // diferente das comorbidades acima: suporte/acessibilidade, não controle de asma).
    val hasWheelchair: Boolean = false,
    val hasLowVision: Boolean = false,
    val hasSpecialCondition: Boolean = false
)

@Serializable
data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relationship: String = ""
)

/** Janela da GINA para o histórico de crises graves: 12 meses. */
const val SEVERE_EXACERBATION_WINDOW_MS = 365L * 24 * 60 * 60 * 1000

/**
 * true se a contagem de crises graves informada no Perfil ainda cobre o último ano:
 * há pelo menos 1 crise e ela foi informada há menos de 12 meses. Depois disso a
 * contagem fica desatualizada e precisa ser informada de novo.
 */
fun declaredSevereExacerbationIsCurrent(count: Int, updatedAtMs: Long, nowMs: Long): Boolean =
    count > 0 && updatedAtMs > 0L && nowMs - updatedAtMs < SEVERE_EXACERBATION_WINDOW_MS

fun UserHealthData.declaredSevereExacerbationIsCurrent(nowMs: Long): Boolean =
    declaredSevereExacerbationIsCurrent(severeExacerbations12m, severeExacerbationsUpdatedAt, nowMs)
