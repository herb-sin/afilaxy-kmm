package com.afilaxy.domain.repository

import com.afilaxy.domain.model.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
    fun hasLocationPermission(): Boolean
}
