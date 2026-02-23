package com.afilaxy.data.fake

import com.afilaxy.domain.model.EmergencyHistory
import com.afilaxy.domain.model.Emergency
import com.afilaxy.domain.model.Helper
import com.afilaxy.domain.model.Location
import com.afilaxy.domain.repository.EmergencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of EmergencyRepository for testing
 */
class FakeEmergencyRepository(
    private var shouldSucceed: Boolean = true,
    private var activeEmergencyId: String? = null,
    private var nearbyHelpers: List<Helper> = emptyList()
) : EmergencyRepository {

    private val statusFlow = MutableStateFlow<String?>(null)
    var createEmergencyCallCount = 0
    var cancelEmergencyCallCount = 0
    var activateHelperCallCount = 0
    var deactivateHelperCallCount = 0
    var notifyCallCount = 0

    override suspend fun createEmergency(emergency: Emergency): Result<String> {
        createEmergencyCallCount++
        return if (shouldSucceed) Result.success("test-emergency-id")
        else Result.failure(Exception("Erro ao criar emergência"))
    }

    override suspend fun createEmergency(latitude: Double, longitude: Double): Result<String> {
        createEmergencyCallCount++
        return if (shouldSucceed) {
            activeEmergencyId = "test-emergency-id"
            Result.success("test-emergency-id")
        } else Result.failure(Exception("Erro ao criar emergência"))
    }

    override suspend fun cancelEmergency(emergencyId: String): Result<Boolean> {
        cancelEmergencyCallCount++
        return if (shouldSucceed) {
            activeEmergencyId = null
            Result.success(true)
        } else Result.failure(Exception("Erro ao cancelar emergência"))
    }

    override suspend fun activateHelper(latitude: Double, longitude: Double): Result<Boolean> {
        activateHelperCallCount++
        return if (shouldSucceed) Result.success(true)
        else Result.failure(Exception("Erro ao ativar helper"))
    }

    override suspend fun deactivateHelper(): Result<Boolean> {
        deactivateHelperCallCount++
        return if (shouldSucceed) Result.success(true)
        else Result.failure(Exception("Erro ao desativar helper"))
    }

    override suspend fun acceptEmergency(emergencyId: String): Result<Boolean> {
        return if (shouldSucceed) Result.success(true)
        else Result.failure(Exception("Erro ao aceitar emergência"))
    }

    override suspend fun getActiveEmergency(): Result<String?> {
        return Result.success(activeEmergencyId)
    }

    override suspend fun clearUserEmergencies(): Result<Boolean> {
        return if (shouldSucceed) {
            activeEmergencyId = null
            Result.success(true)
        } else Result.failure(Exception("Erro ao limpar emergências"))
    }

    override suspend fun isHelperActive(): Result<Boolean> {
        return Result.success(false)
    }

    override suspend fun findNearbyHelpers(location: Location, radiusKm: Double): Result<List<Helper>> {
        return if (shouldSucceed) Result.success(nearbyHelpers)
        else Result.failure(Exception("Erro ao buscar helpers"))
    }

    override suspend fun updateEmergencyStatus(emergencyId: String, status: String): Result<Unit> {
        statusFlow.value = status
        return if (shouldSucceed) Result.success(Unit)
        else Result.failure(Exception("Erro ao atualizar status"))
    }

    override suspend fun finishEmergency(emergencyId: String): Result<Boolean> {
        return if (shouldSucceed) Result.success(true)
        else Result.failure(Exception("Erro ao finalizar emergência"))
    }

    override suspend fun getUserEmergencyHistory(userId: String): Result<List<EmergencyHistory>> {
        return if (shouldSucceed) Result.success(
            listOf(
                EmergencyHistory("h1", userId, "Test User", null, null, -23.5, -46.6, "resolved", 1000L),
                EmergencyHistory("h2", "other-user", "Other User", userId, "Test Helper", -23.6, -46.7, "cancelled", 2000L)
            )
        ) else Result.failure(Exception("Erro ao carregar histórico"))
    }

    override fun observeEmergencyStatus(emergencyId: String): Flow<String?> = statusFlow

    // Test helpers
    fun setShouldSucceed(value: Boolean) { shouldSucceed = value }
    fun setActiveEmergencyId(id: String?) { activeEmergencyId = id }
    fun setNearbyHelpers(helpers: List<Helper>) { nearbyHelpers = helpers }
    fun emitStatus(status: String?) { statusFlow.value = status }
}
