package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HealthData(
    val userId: String,
    val condition: HealthCondition,
    val severity: Severity,
    val medications: List<String> = emptyList(),
    val emergencyContact: String? = null,
    val allergies: List<String> = emptyList(),
    val lastUpdated: Long
) {
    companion object {
        fun create(
            userId: String,
            condition: HealthCondition,
            severity: Severity,
            medications: List<String> = emptyList(),
            emergencyContact: String? = null,
            allergies: List<String> = emptyList()
        ): HealthData {
            return HealthData(
                userId = userId,
                condition = condition,
                severity = severity,
                medications = medications,
                emergencyContact = emergencyContact,
                allergies = allergies,
                lastUpdated = getCurrentTimeMillis()
            )
        }
    }
}

@Serializable
enum class HealthCondition {
    ASMA,
    DPOC,
    ASMA_E_DPOC,
    OUTROS
}

@Serializable
enum class Severity {
    LEVE,
    MODERADA,
    GRAVE,
    MUITO_GRAVE
}
