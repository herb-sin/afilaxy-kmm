package com.afilaxy.data.repository

import com.afilaxy.domain.model.*
import com.afilaxy.domain.repository.MedicalRepository
import com.afilaxy.util.TimeUtils
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MedicalRepositoryImpl(
    private val firestore: FirebaseFirestore
) : MedicalRepository {

    override suspend fun getMedicalProfile(userId: String): Result<MedicalProfile> {
        return try {
            val document = firestore.collection("medical_profiles").document(userId).get()
            if (document.exists) {
                val profile = document.data<MedicalProfile>()
                Result.success(profile)
            } else {
                val defaultProfile = MedicalProfile(
                    userId = userId,
                    asmaType = AsmaType.INTERMITENTE,
                    severity = "Leve",
                    status = HealthStatus.ESTAVEL,
                    lastExam = MedicalExam(
                        id = "1",
                        type = ExamType.ESPIROMETRIA,
                        date = "12 de Outubro, 2023",
                        results = "Função pulmonar dentro dos parâmetros normais"
                    ),
                    emergencyProtocol = listOf(
                        EmergencyStep(1, "Sentar em posição vertical", "Tentar manter a calma"),
                        EmergencyStep(2, "Usar inalador de resgate", "Salbutamol: 2 jatos"),
                        EmergencyStep(3, "Se não houver melhora em 10 min", "Ligar para emergência")
                    )
                )
                Result.success(defaultProfile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMedicalProfile(profile: MedicalProfile): Result<Unit> {
        return try {
            firestore.collection("medical_profiles").document(profile.userId).set(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMedication(userId: String, medication: Medication): Result<Unit> {
        return try {
            firestore.collection("medications").document(medication.id).set(medication)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMedication(medication: Medication): Result<Unit> {
        return try {
            firestore.collection("medications").document(medication.id).set(medication)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMedication(medicationId: String): Result<Unit> {
        return try {
            firestore.collection("medications").document(medicationId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMedications(userId: String): Flow<List<Medication>> = flow {
        try {
            val snapshot = firestore.collection("medications")
                .where { "userId" equalTo userId }
                .snapshots
            
            snapshot.collect { querySnapshot ->
                val medications = querySnapshot.documents.map { doc ->
                    doc.data<Medication>()
                }
                emit(medications)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun addEmergencyContact(userId: String, contact: MedicalEmergencyContact): Result<Unit> {
        return try {
            firestore.collection("emergency_contacts").document(contact.id).set(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEmergencyContact(contact: MedicalEmergencyContact): Result<Unit> {
        return try {
            firestore.collection("emergency_contacts").document(contact.id).set(contact)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeEmergencyContact(contactId: String): Result<Unit> {
        return try {
            firestore.collection("emergency_contacts").document(contactId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEmergencyContacts(userId: String): Flow<List<MedicalEmergencyContact>> = flow {
        try {
            val snapshot = firestore.collection("emergency_contacts")
                .where { "userId" equalTo userId }
                .snapshots
            
            snapshot.collect { querySnapshot ->
                val contacts = querySnapshot.documents.map { doc ->
                    doc.data<MedicalEmergencyContact>()
                }
                emit(contacts)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun addMedicalExam(userId: String, exam: MedicalExam): Result<Unit> {
        return try {
            firestore.collection("medical_exams").document(exam.id).set(exam)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMedicalHistory(userId: String): Flow<List<MedicalExam>> = flow {
        try {
            val snapshot = firestore.collection("medical_exams")
                .where { "userId" equalTo userId }
                .orderBy("date", dev.gitlive.firebase.firestore.Direction.DESCENDING)
                .snapshots
            
            snapshot.collect { querySnapshot ->
                val exams = querySnapshot.documents.map { doc ->
                    doc.data<MedicalExam>()
                }
                emit(exams)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}