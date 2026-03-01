package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Emergency(
    val id: String,
    val userId: String,
    val userName: String,
    val location: Location,
    val description: String = "Emergency assistance needed",
    val timestamp: Long,
    val status: EmergencyStatus = EmergencyStatus.ACTIVE,
    val assignedHelperId: String? = null,
    val resolvedAt: Long? = null
) {
    companion object {
        fun create(
            id: String,
            userId: String,
            userName: String,
            location: Location
        ): Emergency {
            return Emergency(
                id = id,
                userId = userId,
                userName = userName,
                location = location,
                timestamp = getCurrentTimeMillis()
            )
        }
    }
}

@Serializable
enum class EmergencyStatus(val dbValue: String) {
    ACTIVE("waiting"),
    HELPER_RESPONDING("matched"),
    RESOLVED("resolved"),
    CANCELLED("cancelled");

    companion object {
        fun fromDb(value: String): EmergencyStatus =
            entries.firstOrNull { it.dbValue == value } ?: ACTIVE
    }
}
