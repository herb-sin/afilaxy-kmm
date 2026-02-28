package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UBS(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val location: Location? = null,
    val phone: String? = null,
    val openingHours: String = "",
    val hasAsthmaProgram: Boolean = false,
    val hasFreemedication: Boolean = true,
    val distanceKm: Double = 0.0
)
