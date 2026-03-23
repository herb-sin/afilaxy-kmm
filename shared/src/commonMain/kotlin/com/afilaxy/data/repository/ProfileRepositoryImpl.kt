package com.afilaxy.data.repository

import com.afilaxy.domain.model.EmergencyContact
import com.afilaxy.domain.model.UserHealthData
import com.afilaxy.domain.model.UserProfile
import com.afilaxy.domain.repository.ProfileRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ProfileRepositoryImpl(
    private val firestore: FirebaseFirestore
) : ProfileRepository {
    
    override suspend fun getProfile(userId: String): Result<UserProfile?> {
        return try {
            val doc = firestore.collection("users").document(userId).get()
            if (!doc.exists) return Result.success(null)

            // Leitura campo a campo com tipo explícito — evita serializer de Any no Kotlin/Native
            val name: String = doc.get("name") ?: ""
            val email: String = doc.get("email") ?: ""
            val phone: String = doc.get("phone") ?: ""
            val photoUrl: String? = doc.get("photoUrl")

            val bloodType: String = doc.get("healthData.bloodType") ?: ""
            val notes: String = doc.get("healthData.notes") ?: ""
            val allergies: List<String> = doc.get("healthData.allergies") ?: emptyList()
            val medications: List<String> = doc.get("healthData.medications") ?: emptyList()
            val conditions: List<String> = doc.get("healthData.conditions") ?: emptyList()

            val contactName: String? = doc.get("emergencyContact.name")
            val contactPhone: String? = doc.get("emergencyContact.phone")
            val contactRel: String? = doc.get("emergencyContact.relationship")

            val hasHealth = bloodType.isNotEmpty() || notes.isNotEmpty() ||
                allergies.isNotEmpty() || medications.isNotEmpty() || conditions.isNotEmpty()
            val hasContact = contactName != null || contactPhone != null

            Result.success(UserProfile(
                uid = userId,
                name = name, email = email, phone = phone, photoUrl = photoUrl,
                healthData = if (hasHealth) UserHealthData(
                    bloodType = bloodType, allergies = allergies,
                    medications = medications, conditions = conditions, notes = notes
                ) else null,
                emergencyContact = if (hasContact) EmergencyContact(
                    name = contactName ?: "",
                    phone = contactPhone ?: "",
                    relationship = contactRel ?: ""
                ) else null
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            val healthMap: Map<String, Any> = profile.healthData?.let {
                mapOf(
                    "bloodType" to it.bloodType,
                    "allergies" to it.allergies,
                    "medications" to it.medications,
                    "conditions" to it.conditions,
                    "notes" to it.notes
                )
            } ?: emptyMap()

            val contactMap: Map<String, Any> = profile.emergencyContact?.let {
                mapOf(
                    "name" to it.name,
                    "phone" to it.phone,
                    "relationship" to it.relationship
                )
            } ?: emptyMap()

            val data: Map<String, Any> = buildMap {
                put("name", profile.name)
                put("email", profile.email)
                put("phone", profile.phone)
                profile.photoUrl?.let { put("photoUrl", it) }
                if (healthMap.isNotEmpty()) put("healthData", healthMap)
                if (contactMap.isNotEmpty()) put("emergencyContact", contactMap)
            }

            firestore.collection("users").document(profile.uid)
                .set(data, merge = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateHealthData(userId: String, healthData: UserHealthData): Result<Unit> {
        return try {
            val data: Map<String, Any> = mapOf(
                "healthData" to mapOf(
                    "bloodType" to healthData.bloodType,
                    "allergies" to healthData.allergies,
                    "medications" to healthData.medications,
                    "conditions" to healthData.conditions,
                    "notes" to healthData.notes
                )
            )
            firestore.collection("users").document(userId).set(data, merge = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateEmergencyContact(userId: String, contact: EmergencyContact): Result<Unit> {
        return try {
            val data: Map<String, Any> = mapOf(
                "emergencyContact" to mapOf(
                    "name" to contact.name,
                    "phone" to contact.phone,
                    "relationship" to contact.relationship
                )
            )
            firestore.collection("users").document(userId).set(data, merge = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
