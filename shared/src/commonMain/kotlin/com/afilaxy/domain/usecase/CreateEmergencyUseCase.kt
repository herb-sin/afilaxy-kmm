package com.afilaxy.domain.usecase

import com.afilaxy.domain.model.Emergency
import com.afilaxy.domain.repository.EmergencyRepository

class CreateEmergencyUseCase(
    private val repository: EmergencyRepository
) {
    suspend fun execute(emergency: Emergency): Result<String> {
        // Input validation
        if (emergency.location.latitude < -90 || emergency.location.latitude > 90) {
            return Result.failure(IllegalArgumentException("Invalid latitude"))
        }
        
        if (emergency.location.longitude < -180 || emergency.location.longitude > 180) {
            return Result.failure(IllegalArgumentException("Invalid longitude"))
        }
        
        return repository.createEmergency(emergency)
    }
}
