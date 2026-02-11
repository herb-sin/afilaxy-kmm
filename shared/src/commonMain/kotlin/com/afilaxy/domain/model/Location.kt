package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val timestamp: Long,
    val accuracy: Float? = null
) {
    companion object {
        fun create(
            latitude: Double,
            longitude: Double,
            accuracy: Float? = null
        ): Location {
            return Location(
                latitude = latitude,
                longitude = longitude,
                address = "",
                timestamp = getCurrentTimeMillis(),
                accuracy = accuracy
            )
        }
    }
}

// Platform-specific time function
expect fun getCurrentTimeMillis(): Long
