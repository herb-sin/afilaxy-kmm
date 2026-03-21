package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EmergencyRequest(
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    val isActive: Boolean = true,
    val expiresAt: Long = 0L
) {
    companion object {
        const val TIMEOUT_DURATION_MS = 3 * 60 * 1000L // 3 minutos
        
        fun create(
            id: String,
            requesterId: String,
            requesterName: String,
            latitude: Double,
            longitude: Double
        ): EmergencyRequest {
            val currentTime = getCurrentTimeMillis()
            return EmergencyRequest(
                id = id,
                requesterId = requesterId,
                requesterName = requesterName,
                latitude = latitude,
                longitude = longitude,
                timestamp = currentTime,
                isActive = true,
                expiresAt = currentTime + TIMEOUT_DURATION_MS
            )
        }
    }
    
    fun isExpired(): Boolean {
        return getCurrentTimeMillis() > expiresAt
    }
}
