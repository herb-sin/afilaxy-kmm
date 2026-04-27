package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val timing: String? = null, // "Noite", "Se necessário"
    val type: MedicationType,
    val isActive: Boolean = true
)

@Serializable
enum class MedicationType {
    CONTROLE,
    MANUTENCAO,
    RESGATE
}

@Serializable
data class MedicalProfile(
    val userId: String,
    val asmaType: AsmaType,
    val severity: String,
    val status: HealthStatus,
    val lastExam: MedicalExam? = null,
    val medications: List<Medication> = emptyList(),
    val emergencyProtocol: List<EmergencyStep> = emptyList(),
    val emergencyContacts: List<MedicalEmergencyContact> = emptyList()
)

@Serializable
enum class AsmaType {
    INTERMITENTE,
    PERSISTENTE_LEVE,
    PERSISTENTE_MODERADA,
    PERSISTENTE_GRAVE
}

@Serializable
enum class HealthStatus {
    ESTAVEL,
    CONTROLADO,
    ALERTA,
    URGENTE
}

@Serializable
data class MedicalExam(
    val id: String,
    val type: ExamType,
    val date: String,
    val results: String,
    val parameters: Map<String, String> = emptyMap()
)

@Serializable
enum class ExamType {
    ESPIROMETRIA,
    RAIO_X,
    TESTE_ALERGIA,
    CONSULTA_ROTINA
}

@Serializable
data class EmergencyStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
)

@Serializable
data class MedicalEmergencyContact(
    val id: String,
    val name: String,
    val relationship: String,
    val phone: String,
    val isProfessional: Boolean = false,
    val specialty: String? = null
)
