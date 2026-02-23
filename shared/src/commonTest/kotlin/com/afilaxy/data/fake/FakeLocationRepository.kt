package com.afilaxy.data.fake

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.LocationRepository

/**
 * Fake implementation of LocationRepository for testing
 */
class FakeLocationRepository(
    private var currentLocation: Location? = Location(
        latitude = -23.5505,
        longitude = -46.6333,
        timestamp = 1000L
    ),
    private var hasPermission: Boolean = true
) : LocationRepository {

    override suspend fun getCurrentLocation(): Location? = currentLocation

    override fun hasLocationPermission(): Boolean = hasPermission

    // Test helpers
    fun setCurrentLocation(location: Location?) { currentLocation = location }
    fun setHasPermission(value: Boolean) { hasPermission = value }
}
