package com.afilaxy.domain.repository

import com.afilaxy.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MedicalRepository {
    suspend fun getMedicalProfile(userId: String): Result<MedicalProfile>
    suspend fun updateMedicalProfile(profile: MedicalProfile): Result<Unit>
    suspend fun addMedication(userId: String, medication: Medication): Result<Unit>
    suspend fun updateMedication(medication: Medication): Result<Unit>
    suspend fun removeMedication(medicationId: String): Result<Unit>
    suspend fun getMedications(userId: String): Flow<List<Medication>>
    suspend fun addEmergencyContact(userId: String, contact: MedicalEmergencyContact): Result<Unit>
    suspend fun updateEmergencyContact(contact: MedicalEmergencyContact): Result<Unit>
    suspend fun removeEmergencyContact(contactId: String): Result<Unit>
    suspend fun getEmergencyContacts(userId: String): Flow<List<MedicalEmergencyContact>>
    suspend fun addMedicalExam(userId: String, exam: MedicalExam): Result<Unit>
    suspend fun getMedicalHistory(userId: String): Flow<List<MedicalExam>>
}