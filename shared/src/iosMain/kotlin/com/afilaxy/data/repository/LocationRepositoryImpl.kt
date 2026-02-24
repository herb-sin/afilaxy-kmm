package com.afilaxy.data.repository

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.LocationRepository

/**
 * Implementação iOS do LocationRepository
 * TODO: Implementar CoreLocation quando necessário
 */
actual class LocationRepositoryImpl : LocationRepository {
    
    actual override suspend fun getCurrentLocation(): Location? {
        // Stub implementation - retorna localização mock
        // TODO: Implementar CoreLocation real
        return Location(
            latitude = -23.5505,
            longitude = -46.6333,
            address = "São Paulo, SP",
            timestamp = System.currentTimeMillis(),
            accuracy = 10.0f
        )
    }
    
    actual override fun hasLocationPermission(): Boolean {
        // Stub implementation - sempre retorna true
        // TODO: Verificar permissões reais do CoreLocation
        return true
    }
}
