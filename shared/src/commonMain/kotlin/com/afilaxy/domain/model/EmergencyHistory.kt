package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EmergencyHistory(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val helperId: String? = null,
    val helperName: String? = null,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val timestamp: Long,
    val resolvedAt: Long? = null,
    val cancelledAt: Long? = null
)
