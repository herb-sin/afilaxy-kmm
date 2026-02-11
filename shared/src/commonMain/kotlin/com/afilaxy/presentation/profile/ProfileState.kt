package com.afilaxy.presentation.profile

import com.afilaxy.domain.model.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class ProfileState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
