package com.afilaxy.presentation.emergency

import kotlinx.serialization.Serializable
import com.afilaxy.domain.model.Emergency
import com.afilaxy.domain.model.Helper

/**
 * Estado da tela de Emergência
 */
@Serializable
data class EmergencyState(
    val currentEmergency: Emergency? = null,
    val emergencyId: String? = null,
    val emergencyStatus: String? = null,
    val nearbyHelpers: List<Helper> = emptyList(),
    val isLoading: Boolean = false,
    val isCreatingEmergency: Boolean = false,
    val error: String? = null,
    val isHelperMode: Boolean = false,
    val hasActiveEmergency: Boolean = false,
    val userLatitude: Double = 0.0,
    val userLongitude: Double = 0.0
)
