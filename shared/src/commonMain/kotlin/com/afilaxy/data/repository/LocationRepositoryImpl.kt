package com.afilaxy.data.repository

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.LocationRepository

/**
 * Repositório de localização compartilhado
 * Usa expect/actual para implementações específicas de plataforma
 */
expect class LocationRepositoryImpl : LocationRepository {
    override suspend fun getCurrentLocation(): Location?
    override fun hasLocationPermission(): Boolean
}
