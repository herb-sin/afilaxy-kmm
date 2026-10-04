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
