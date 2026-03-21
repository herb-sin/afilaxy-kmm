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
    PNEUMOLOGIST,
    ALLERGIST,
    PHYSIOTHERAPIST
}

@Serializable
enum class SubscriptionPlan {
    NONE,
    // Mensal
    BASIC,
    PRO,
    PREMIUM,
    // Trimestral (3 meses)
    BASIC_QUARTERLY,
    PRO_QUARTERLY,
    PREMIUM_QUARTERLY,
    // Semestral (6 meses)
    BASIC_SEMIANNUAL,
    PRO_SEMIANNUAL,
    PREMIUM_SEMIANNUAL,
    // Anual (12 meses)
    BASIC_ANNUAL,
    PRO_ANNUAL,
    PREMIUM_ANNUAL;

    fun tier(): PlanTier = when (this) {
        NONE -> PlanTier.NONE
        BASIC, BASIC_QUARTERLY, BASIC_SEMIANNUAL, BASIC_ANNUAL -> PlanTier.BASIC
        PRO, PRO_QUARTERLY, PRO_SEMIANNUAL, PRO_ANNUAL -> PlanTier.PRO
        PREMIUM, PREMIUM_QUARTERLY, PREMIUM_SEMIANNUAL, PREMIUM_ANNUAL -> PlanTier.PREMIUM
    }

    fun durationMonths(): Int = when (this) {
        NONE -> 0
        BASIC, PRO, PREMIUM -> 1
        BASIC_QUARTERLY, PRO_QUARTERLY, PREMIUM_QUARTERLY -> 3
        BASIC_SEMIANNUAL, PRO_SEMIANNUAL, PREMIUM_SEMIANNUAL -> 6
        BASIC_ANNUAL, PRO_ANNUAL, PREMIUM_ANNUAL -> 12
    }
}

@Serializable
enum class PlanTier { NONE, BASIC, PRO, PREMIUM }
