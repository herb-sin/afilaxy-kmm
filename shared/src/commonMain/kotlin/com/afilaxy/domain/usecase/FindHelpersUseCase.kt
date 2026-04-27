package com.afilaxy.domain.usecase

import com.afilaxy.domain.model.Helper
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.EmergencyRepository

class FindHelpersUseCase(
    private val repository: EmergencyRepository
) {
    private companion object {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
        const val DEFAULT_RADIUS_KM = 0.25
    }

    suspend fun execute(location: Location, radiusKm: Double = DEFAULT_RADIUS_KM): List<Helper> {
        require(location.latitude in MIN_LATITUDE..MAX_LATITUDE) { "Invalid latitude" }
        require(location.longitude in MIN_LONGITUDE..MAX_LONGITUDE) { "Invalid longitude" }
        require(radiusKm > 0) { "Radius must be positive" }

        return repository.findNearbyHelpers(location, radiusKm).getOrElse { emptyList() }
    }
}
