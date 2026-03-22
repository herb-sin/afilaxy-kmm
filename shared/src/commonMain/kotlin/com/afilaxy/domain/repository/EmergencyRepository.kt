package com.afilaxy.domain.repository

import com.afilaxy.domain.model.Emergency
import com.afilaxy.domain.model.EmergencyStatus
import com.afilaxy.domain.model.Helper
import com.afilaxy.domain.model.Location
import kotlinx.coroutines.flow.Flow

interface EmergencyRepository {
    suspend fun createEmergency(emergency: Emergency): Result<String>
    suspend fun createEmergency(latitude: Double, longitude: Double): Result<String>
    suspend fun cancelEmergency(emergencyId: String): Result<Boolean>
    suspend fun activateHelper(latitude: Double, longitude: Double): Result<Boolean>
    suspend fun deactivateHelper(): Result<Boolean>
    suspend fun acceptEmergency(emergencyId: String): Result<Boolean>
    suspend fun getActiveEmergency(): Result<String?>
    suspend fun clearUserEmergencies(): Result<Boolean>
    suspend fun isHelperActive(): Result<Boolean>
    suspend fun findNearbyHelpers(location: Location, radiusKm: Double): Result<List<Helper>>
    suspend fun updateEmergencyStatus(emergencyId: String, status: EmergencyStatus): Result<Unit>
    suspend fun finishEmergency(emergencyId: String): Result<Boolean>
    suspend fun getUserEmergencyHistory(userId: String): Result<List<com.afilaxy.domain.model.EmergencyHistory>>
    fun observeNearbyEmergencies(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<Emergency>>
    fun observeEmergencyStatus(emergencyId: String): Flow<String?>
    suspend fun getEmergencyExpiresAt(emergencyId: String): Long?
}
