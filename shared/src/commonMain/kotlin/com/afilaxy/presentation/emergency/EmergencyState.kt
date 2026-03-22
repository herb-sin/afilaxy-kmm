package com.afilaxy.presentation.emergency

import com.afilaxy.domain.model.Emergency
import com.afilaxy.domain.model.Helper

/**
 * Estado da tela de Emergência
 */
data class EmergencyState(
    val currentEmergency: Emergency? = null,
    val emergencyId: String? = null,
    val emergencyStatus: String? = null,
    val emergencyExpiresAt: Long? = null,
    val nearbyHelpers: List<Helper> = emptyList(),
    val incomingEmergencies: List<Emergency> = emptyList(),
    val isLoading: Boolean = false,
    val isCreatingEmergency: Boolean = false,
    val error: String? = null,
    val isHelperMode: Boolean = false,
    val hasActiveEmergency: Boolean = false,
    val isRequester: Boolean = false, // true = criou a emergência, false = aceitou como helper
    val userLatitude: Double = 0.0,
    val userLongitude: Double = 0.0
)
