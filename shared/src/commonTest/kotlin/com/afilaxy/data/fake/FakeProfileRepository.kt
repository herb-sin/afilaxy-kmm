package com.afilaxy.data.fake

import com.afilaxy.domain.model.EmergencyContact
import com.afilaxy.domain.model.UserHealthData
import com.afilaxy.domain.model.UserProfile
import com.afilaxy.domain.repository.ProfileRepository

/**
 * Fake implementation of ProfileRepository for testing
 */
class FakeProfileRepository(
    private var currentProfile: UserProfile? = null,
    private var shouldSucceed: Boolean = true
) : ProfileRepository {

    var updateProfileCallCount = 0
    var updateHealthDataCallCount = 0
    var updateEmergencyContactCallCount = 0

    override suspend fun getProfile(userId: String): Result<UserProfile?> {
        return if (shouldSucceed) Result.success(currentProfile)
        else Result.failure(Exception("Erro ao carregar perfil"))
    }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        updateProfileCallCount++
        return if (shouldSucceed) {
            currentProfile = profile
            Result.success(Unit)
        } else Result.failure(Exception("Erro ao atualizar perfil"))
    }

    override suspend fun updateHealthData(userId: String, healthData: UserHealthData): Result<Unit> {
        updateHealthDataCallCount++
        return if (shouldSucceed) {
            currentProfile = currentProfile?.copy(healthData = healthData)
            Result.success(Unit)
        } else Result.failure(Exception("Erro ao atualizar dados de saúde"))
    }

    override suspend fun updateEmergencyContact(userId: String, contact: EmergencyContact): Result<Unit> {
        updateEmergencyContactCallCount++
        return if (shouldSucceed) {
            currentProfile = currentProfile?.copy(emergencyContact = contact)
            Result.success(Unit)
        } else Result.failure(Exception("Erro ao atualizar contato"))
    }

    // Test helpers
    fun setCurrentProfile(profile: UserProfile?) { currentProfile = profile }
    fun setShouldSucceed(value: Boolean) { shouldSucceed = value }
}
