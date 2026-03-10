package com.afilaxy.data.repository

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.LocationRepository

/**
 * Implementação iOS do LocationRepository
 * Lê de IOSLocationBridge — singleton Kotlin que o código Swift atualiza via CLLocationManager
 */
actual class LocationRepositoryImpl : LocationRepository {

    actual override suspend fun getCurrentLocation(): Location? {
        if (!IOSLocationBridge.hasPermission) return null

        val lat = IOSLocationBridge.latitude
        val lon = IOSLocationBridge.longitude

        // 0,0 indica que ainda não temos localização real
        if (lat == 0.0 && lon == 0.0) return null

        return Location(
            latitude = lat,
            longitude = lon,
            address = null,
            timestamp = getCurrentTimeMillis(),
            accuracy = 10.0f
        )
    }

    actual override fun hasLocationPermission(): Boolean {
        return IOSLocationBridge.hasPermission
    }
}
