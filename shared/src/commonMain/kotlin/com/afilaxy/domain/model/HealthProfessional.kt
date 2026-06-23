package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthProfessional(
    val id: String = "",
    val name: String = "",
    val specialty: Specialty = Specialty.PNEUMOLOGIST,
    val crm: String = "",
    val subscriptionPlan: SubscriptionPlan = SubscriptionPlan.NONE,
    val subscriptionExpiry: Long = 0L,
    val profilePhoto: String? = null,
    val bio: String = "",
    val phone: String? = null,
    val whatsapp: String? = null,
    val clinicAddress: String? = null,
    val location: Location? = null,
    val rating: Double = 0.0,
    val totalReviews: Int = 0,
    val isVerified: Boolean = false
)

@Serializable
enum class Specialty {
    PNEUMOLOGIST,       // Pneumologista
    ALLERGIST,          // Alergologista
    PHYSIOTHERAPIST,    // Fisioterapeuta
    PSYCHOLOGIST,       // Psicologia
    PSYCHIATRIST,       // Psiquiatria
    OTOLARYNGOLOGIST,   // Otorrino
    ENDOCRINOLOGIST,    // Endocrino
    GASTROENTEROLOGIST, // Gastro
    CARDIOLOGIST,       // Cardio
    CLINIC              // Clínicas
}

@Serializable
enum class SubscriptionPlan {
    NONE,
    PARTNER;

    fun isActive(): Boolean = this == PARTNER
}
