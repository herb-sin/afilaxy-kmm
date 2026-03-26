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
    val bloodType: String = "",
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val notes: String = ""
)

@Serializable
data class EmergencyContact(
    val name: String = "",
    val phone: String = "",
    val relationship: String = ""
)
