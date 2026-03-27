package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SocialPost(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val content: String,
    val timestamp: Long,
    val type: PostType,
    val likes: Int = 0,
    val comments: Int = 0,
    val isLiked: Boolean = false,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
enum class PostType {
    SUPPORT,
    EMERGENCY,
    MEDICAL,
    TIP,
    QUESTION
}

@Serializable
data class CommunityStats(
    val onlineCount: Int,
    val activeUsers: List<User>,
    val totalMembers: Int
)

@Serializable
data class QuickAction(
    val id: String,
    val title: String,
    val icon: String,
    val action: ActionType,
    val isEnabled: Boolean = true
)

@Serializable
enum class ActionType {
    FIND_PHARMACIES,
    EMERGENCY_CONTACTS,
    CRISIS_PROTOCOL,
    REQUEST_HELP
}

@Serializable
data class AirQuality(
    val index: Int,
    val status: String,
    val pollenCount: String,
    val location: String,
    val recommendation: String
)