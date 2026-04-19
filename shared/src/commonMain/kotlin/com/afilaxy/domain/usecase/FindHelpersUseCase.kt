package com.afilaxy.domain.usecase

import com.afilaxy.domain.model.Helper
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.EmergencyRepository

class FindHelpersUseCase(
    private val repository: EmergencyRepository
) {
    suspend fun execute(location: Location, radiusKm: Double = 0.25): List<Helper> {
        // Input validation
        if (location.latitude < -90 || location.latitude > 90) {
            throw IllegalArgumentException("Invalid latitude")
        }
        
        if (location.longitude < -180 || location.longitude > 180) {
            throw IllegalArgumentException("Invalid longitude")
        }
        
        if (radiusKm <= 0) {
            throw IllegalArgumentException("Radius must be positive")
        }
        
        return repository.findNearbyHelpers(location, radiusKm).getOrElse { emptyList() }
    }
}
