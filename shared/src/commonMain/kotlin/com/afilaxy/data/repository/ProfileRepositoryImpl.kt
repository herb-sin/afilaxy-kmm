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
            
            if (!doc.exists) {
                return Result.success(null)
            }
            
            // Usa doc.data (raw map) para evitar serializer de Any que falha no iOS/Kotlin Native
            val rawData: Map<String, Any?> = doc.data() ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val healthDataMap = rawData["healthData"] as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val emergencyContactMap = rawData["emergencyContact"] as? Map<String, Any?>
            
            val profile = UserProfile(
                uid = userId,
                name = doc.get("name") ?: "",
                email = doc.get("email") ?: "",
                phone = doc.get("phone") ?: "",
                photoUrl = doc.get("photoUrl"),
                healthData = healthDataMap?.let {
                    UserHealthData(
                        bloodType = it["bloodType"] as? String ?: "",
                        @Suppress("UNCHECKED_CAST")
                        allergies = (it["allergies"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        @Suppress("UNCHECKED_CAST")
                        medications = (it["medications"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        @Suppress("UNCHECKED_CAST")
                        conditions = (it["conditions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        notes = it["notes"] as? String ?: ""
                    )
                },
                emergencyContact = emergencyContactMap?.let {
                    EmergencyContact(
                        name = it["name"] as? String ?: "",
                        phone = it["phone"] as? String ?: "",
                        relationship = it["relationship"] as? String ?: ""
                    )
                }
            )
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            val data = mutableMapOf<String, Any?>(
                "name" to profile.name,
                "email" to profile.email,
                "phone" to profile.phone,
                "photoUrl" to profile.photoUrl
            )
            
            profile.healthData?.let {
                data["healthData"] = mapOf(
                    "bloodType" to it.bloodType,
                    "allergies" to it.allergies,
                    "medications" to it.medications,
                    "conditions" to it.conditions,
                    "notes" to it.notes
                )
            }
            
            profile.emergencyContact?.let {
                data["emergencyContact"] = mapOf(
                    "name" to it.name,
                    "phone" to it.phone,
                    "relationship" to it.relationship
                )
            }
            
            firestore.collection("users").document(profile.uid).update(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateHealthData(userId: String, healthData: UserHealthData): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).update(
                "healthData" to mapOf(
                    "bloodType" to healthData.bloodType,
                    "allergies" to healthData.allergies,
                    "medications" to healthData.medications,
                    "conditions" to healthData.conditions,
                    "notes" to healthData.notes
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateEmergencyContact(userId: String, contact: EmergencyContact): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).update(
                "emergencyContact" to mapOf(
                    "name" to contact.name,
                    "phone" to contact.phone,
                    "relationship" to contact.relationship
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
