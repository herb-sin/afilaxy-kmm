package com.afilaxy.data.repository

import com.afilaxy.domain.model.EmergencyContact
import com.afilaxy.domain.model.UserHealthData
import com.afilaxy.domain.model.UserProfile
import com.afilaxy.domain.repository.ProfileRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ProfileRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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

            val notes: String = doc.get("healthData.notes") ?: ""
            val allergies: List<String> = doc.get("healthData.allergies") ?: emptyList()
            val medications: List<String> = doc.get("healthData.medications") ?: emptyList()
            val conditions: List<String> = doc.get("healthData.conditions") ?: emptyList()
            val hasGerd: Boolean = doc.get("healthData.hasGerd") ?: false
            val hasSleepApnea: Boolean = doc.get("healthData.hasSleepApnea") ?: false
            val hasRhinitis: Boolean = doc.get("healthData.hasRhinitis") ?: false
            val hasObesity: Boolean = doc.get("healthData.hasObesity") ?: false
            val hasFoodAllergy: Boolean = doc.get("healthData.hasFoodAllergy") ?: false
            val hasNsaidAllergy: Boolean = doc.get("healthData.hasNsaidAllergy") ?: false
            val hasInhalantAllergy: Boolean = doc.get("healthData.hasInhalantAllergy") ?: false
            val hasWheelchair: Boolean = doc.get("healthData.hasWheelchair") ?: false
            val hasLowVision: Boolean = doc.get("healthData.hasLowVision") ?: false
            val hasSpecialCondition: Boolean = doc.get("healthData.hasSpecialCondition") ?: false

            val contactName: String? = doc.get("emergencyContact.name")
            val contactPhone: String? = doc.get("emergencyContact.phone")
            val contactRel: String? = doc.get("emergencyContact.relationship")

            val hasHealth = notes.isNotEmpty() ||
                allergies.isNotEmpty() || medications.isNotEmpty() || conditions.isNotEmpty() ||
                hasGerd || hasSleepApnea || hasRhinitis || hasObesity ||
                hasFoodAllergy || hasNsaidAllergy || hasInhalantAllergy ||
                hasWheelchair || hasLowVision || hasSpecialCondition
            val hasContact = contactName != null || contactPhone != null

            Result.success(UserProfile(
                uid = userId,
                name = name, email = email, phone = phone, photoUrl = photoUrl,
                healthData = if (hasHealth) UserHealthData(
                    allergies = allergies,
                    medications = medications, conditions = conditions, notes = notes,
                    hasGerd = hasGerd, hasSleepApnea = hasSleepApnea, hasRhinitis = hasRhinitis,
                    hasObesity = hasObesity,
                    hasFoodAllergy = hasFoodAllergy, hasNsaidAllergy = hasNsaidAllergy,
                    hasInhalantAllergy = hasInhalantAllergy,
                    hasWheelchair = hasWheelchair, hasLowVision = hasLowVision,
                    hasSpecialCondition = hasSpecialCondition
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
                    "allergies" to it.allergies,
                    "medications" to it.medications,
                    "conditions" to it.conditions,
                    "notes" to it.notes,
                    "hasGerd" to it.hasGerd,
                    "hasSleepApnea" to it.hasSleepApnea,
                    "hasRhinitis" to it.hasRhinitis,
                    "hasObesity" to it.hasObesity,
                    "hasFoodAllergy" to it.hasFoodAllergy,
                    "hasNsaidAllergy" to it.hasNsaidAllergy,
                    "hasInhalantAllergy" to it.hasInhalantAllergy,
                    "hasWheelchair" to it.hasWheelchair,
                    "hasLowVision" to it.hasLowVision,
                    "hasSpecialCondition" to it.hasSpecialCondition
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

            // Mantém o displayName do Firebase Auth em sincronia com o nome do perfil —
            // várias telas (chat, relatório PDF, nome de solicitante/helper numa emergência)
            // leem currentUser.displayName em vez do Firestore, e ficavam presas no nome
            // antigo (ex: do cadastro) mesmo depois do usuário editar o nome aqui.
            // Best-effort: não falha o save do perfil se só essa sincronização falhar.
            if (profile.name.isNotBlank()) {
                try {
                    auth.currentUser?.updateProfile(displayName = profile.name)
                } catch (e: Exception) {
                    com.afilaxy.util.Logger.e("ProfileRepo", "Falha ao sincronizar displayName: ${e.message}", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateHealthData(userId: String, healthData: UserHealthData): Result<Unit> {
        return try {
            val data: Map<String, Any> = mapOf(
                "healthData" to mapOf(
                    "allergies" to healthData.allergies,
                    "medications" to healthData.medications,
                    "conditions" to healthData.conditions,
                    "notes" to healthData.notes,
                    "hasGerd" to healthData.hasGerd,
                    "hasSleepApnea" to healthData.hasSleepApnea,
                    "hasRhinitis" to healthData.hasRhinitis,
                    "hasObesity" to healthData.hasObesity,
                    "hasFoodAllergy" to healthData.hasFoodAllergy,
                    "hasNsaidAllergy" to healthData.hasNsaidAllergy,
                    "hasInhalantAllergy" to healthData.hasInhalantAllergy,
                    "hasWheelchair" to healthData.hasWheelchair,
                    "hasLowVision" to healthData.hasLowVision,
                    "hasSpecialCondition" to healthData.hasSpecialCondition
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
