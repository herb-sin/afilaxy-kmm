package com.afilaxy.data.repository

import com.afilaxy.domain.model.*
import com.afilaxy.domain.repository.ProfessionalRepository
import com.afilaxy.util.TimeUtils
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ProfessionalRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ProfessionalRepository {

    override suspend fun getDashboard(professionalId: String): Flow<ProfessionalDashboard> = flow {
        try {
            val dashboard = ProfessionalDashboard(
                professionalId = professionalId,
                totalPatients = 142,
                criticalAlerts = 3,
                adherenceRate = 0.88f,
                upcomingConsultations = listOf(
                    Consultation(
                        id = "1",
                        patientId = "patient1",
                        patientName = "Ana Clara",
                        scheduledTime = "14:05",
                        type = ConsultationType.TELECONSULTA,
                        status = ConsultationStatus.AGENDADA
                    )
                ),
                crisisFrequency = generateCrisisData(),
                priorityPatients = getPriorityPatients(),
                recentAlerts = getRecentAlerts()
            )
            emit(dashboard)
        } catch (e: Exception) {
            emit(ProfessionalDashboard(
                professionalId = professionalId,
                totalPatients = 0,
                criticalAlerts = 0,
                adherenceRate = 0f,
                upcomingConsultations = emptyList(),
                crisisFrequency = emptyList(),
                priorityPatients = emptyList(),
                recentAlerts = emptyList()
            ))
        }
    }

    override suspend fun getPatients(professionalId: String): Flow<List<PatientSummary>> = flow {
        try {
            val snapshot = firestore.collection("patients")
                .where { "professionalId" equalTo professionalId }
                .snapshots
            
            snapshot.collect { querySnapshot ->
                val patients = querySnapshot.documents.map { doc ->
                    doc.data<PatientSummary>()
                }
                emit(patients)
            }
        } catch (e: Exception) {
            emit(getPriorityPatients())
        }
    }

    override suspend fun getPatientDetails(patientId: String): Result<MedicalProfile> {
        return try {
            val document = firestore.collection("medical_profiles").document(patientId).get()
            val profile = document.data<MedicalProfile>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePatientStatus(patientId: String, status: PatientStatus): Result<Unit> {
        return try {
            firestore.collection("patients").document(patientId)
                .update("status" to status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun scheduleConsultation(consultation: Consultation): Result<Unit> {
        return try {
            firestore.collection("consultations").document(consultation.id).set(consultation)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConsultations(professionalId: String): Flow<List<Consultation>> = flow {
        try {
            val snapshot = firestore.collection("consultations")
                .where { "professionalId" equalTo professionalId }
                .orderBy("scheduledTime")
                .snapshots
            
            snapshot.collect { querySnapshot ->
                val consultations = querySnapshot.documents.map { doc ->
                    doc.data<Consultation>()
                }
                emit(consultations)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getCrisisAnalytics(professionalId: String, period: String): Flow<List<CrisisData>> = flow {
        try {
            emit(generateCrisisData())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun getPatientAlerts(professionalId: String): Flow<List<PatientAlert>> = flow {
        try {
            val snapshot = firestore.collection("patient_alerts")
                .where { "professionalId" equalTo professionalId }
                .where { "isRead" equalTo false }
                .orderBy("timestamp", dev.gitlive.firebase.firestore.Direction.DESCENDING)
                .snapshots
            
            snapshot.collect { querySnapshot ->
                val alerts = querySnapshot.documents.map { doc ->
                    doc.data<PatientAlert>()
                }
                emit(alerts)
            }
        } catch (e: Exception) {
            emit(getRecentAlerts())
        }
    }

    override suspend fun markAlertAsRead(alertId: String): Result<Unit> {
        return try {
            firestore.collection("patient_alerts").document(alertId)
                .update("isRead" to true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateCrisisData(): List<CrisisData> {
        return listOf(
            CrisisData("01 OUT", 5, CrisisSeverity.LEVE),
            CrisisData("08 OUT", 8, CrisisSeverity.MODERADA),
            CrisisData("15 OUT", 12, CrisisSeverity.LEVE),
            CrisisData("22 OUT", 6, CrisisSeverity.GRAVE),
            CrisisData("30 OUT", 9, CrisisSeverity.MODERADA)
        )
    }

    private fun getPriorityPatients(): List<PatientSummary> {
        return listOf(
            PatientSummary(
                id = "1",
                name = "Ana Clara Mendes",
                initials = "AC",
                status = PatientStatus.CONTROLADO,
                lastCrisis = "15 dias atrás",
                adherenceRate = 0.92f,
                asmaType = AsmaType.INTERMITENTE,
                riskLevel = RiskLevel.BAIXO
            ),
            PatientSummary(
                id = "2",
                name = "Bruno Pereira",
                initials = "BP",
                status = PatientStatus.ALERTA,
                lastCrisis = "2 horas atrás",
                adherenceRate = 0.65f,
                asmaType = AsmaType.PERSISTENTE_MODERADA,
                riskLevel = RiskLevel.ALTO
            ),
            PatientSummary(
                id = "3",
                name = "Lucia Soares",
                initials = "LS",
                status = PatientStatus.ESTAVEL,
                lastCrisis = "Nunca registrada",
                adherenceRate = 0.98f,
                asmaType = AsmaType.INTERMITENTE,
                riskLevel = RiskLevel.BAIXO
            )
        )
    }

    private fun getRecentAlerts(): List<PatientAlert> {
        return listOf(
            PatientAlert(
                id = "1",
                patientId = "2",
                patientName = "Mariana Costa",
                alertType = AlertType.CRISE_ATIVA,
                message = "Alguém com bombinha de resgate por perto da estação? Esqueci a minha e estou sentindo aperto.",
                timestamp = TimeUtils.currentTimeMillis(),
                isUrgent = true
            ),
            PatientAlert(
                id = "2",
                patientId = "4",
                patientName = "João Oliveira",
                alertType = AlertType.BAIXA_ADESAO,
                message = "Paciente não está seguindo o protocolo de medicação preventiva.",
                timestamp = TimeUtils.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000,
                isUrgent = false
            )
        )
    }
}