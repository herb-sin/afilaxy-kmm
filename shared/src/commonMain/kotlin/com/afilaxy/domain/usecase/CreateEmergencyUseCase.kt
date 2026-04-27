package com.afilaxy.domain.usecase

import com.afilaxy.domain.model.Emergency
import com.afilaxy.domain.repository.EmergencyRepository

class CreateEmergencyUseCase(
    private val repository: EmergencyRepository
) {
    private companion object {
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }

    suspend fun execute(emergency: Emergency): Result<String> {
        if (emergency.location.latitude < MIN_LATITUDE || emergency.location.latitude > MAX_LATITUDE) {
            return Result.failure(IllegalArgumentException("Invalid latitude"))
        }

        if (emergency.location.longitude < MIN_LONGITUDE || emergency.location.longitude > MAX_LONGITUDE) {
            return Result.failure(IllegalArgumentException("Invalid longitude"))
        }

        return repository.createEmergency(emergency)
    }
}
