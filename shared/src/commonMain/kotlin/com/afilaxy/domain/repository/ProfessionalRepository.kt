package com.afilaxy.domain.repository

import com.afilaxy.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProfessionalRepository {
    suspend fun getDashboard(professionalId: String): Flow<ProfessionalDashboard>
    suspend fun getPatients(professionalId: String): Flow<List<PatientSummary>>
    suspend fun getPatientDetails(patientId: String): Result<MedicalProfile>
    suspend fun updatePatientStatus(patientId: String, status: PatientStatus): Result<Unit>
    suspend fun scheduleConsultation(consultation: Consultation): Result<Unit>
    suspend fun getConsultations(professionalId: String): Flow<List<Consultation>>
    suspend fun getCrisisAnalytics(professionalId: String, period: String): Flow<List<CrisisData>>
    suspend fun getPatientAlerts(professionalId: String): Flow<List<PatientAlert>>
    suspend fun markAlertAsRead(alertId: String): Result<Unit>
}