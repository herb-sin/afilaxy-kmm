package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String,
    val email: String,
    val name: String? = null,
    val displayName: String? = null,
    val fcmToken: String? = null,
    val isHelper: Boolean = false
)
