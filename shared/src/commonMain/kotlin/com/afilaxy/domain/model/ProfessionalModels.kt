package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfessionalDashboard(
    val professionalId: String,
    val totalPatients: Int,
    val criticalAlerts: Int,
    val adherenceRate: Float,
    val upcomingConsultations: List<Consultation>,
    val crisisFrequency: List<CrisisData>,
    val priorityPatients: List<PatientSummary>,
    val recentAlerts: List<PatientAlert>
)

@Serializable
data class PatientSummary(
    val id: String,
    val name: String,
    val initials: String,
    val status: PatientStatus,
    val lastCrisis: String,
    val adherenceRate: Float,
    val asmaType: AsmaType,
    val riskLevel: RiskLevel
)

@Serializable
enum class PatientStatus {
    CONTROLADO,
    ALERTA,
    ESTAVEL,
    URGENTE
}

@Serializable
enum class RiskLevel {
    BAIXO,
    MEDIO,
    ALTO,
    CRITICO
}

@Serializable
data class Consultation(
    val id: String,
    val patientId: String,
    val patientName: String,
    val scheduledTime: String,
    val type: ConsultationType,
    val status: ConsultationStatus
)

@Serializable
enum class ConsultationType {
    TELECONSULTA,
    PRESENCIAL,
    EMERGENCIA,
    RETORNO
}

@Serializable
enum class ConsultationStatus {
    AGENDADA,
    EM_ANDAMENTO,
    CONCLUIDA,
    CANCELADA
}

@Serializable
data class CrisisData(
    val date: String,
    val count: Int,
    val severity: CrisisSeverity
)

@Serializable
enum class CrisisSeverity {
    LEVE,
    MODERADA,
    GRAVE,
    MUITO_GRAVE
}

@Serializable
data class PatientAlert(
    val id: String,
    val patientId: String,
    val patientName: String,
    val alertType: AlertType,
    val message: String,
    val timestamp: Long,
    val isUrgent: Boolean
)

@Serializable
enum class AlertType {
    CRISE_ATIVA,
    MEDICACAO_ATRASADA,
    EXAME_VENCIDO,
    BAIXA_ADESAO,
    EMERGENCIA
}
