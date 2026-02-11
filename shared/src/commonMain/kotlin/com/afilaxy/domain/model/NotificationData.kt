package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationData(
    val type: String,
    val title: String,
    val body: String,
    val emergencyId: String? = null
)
