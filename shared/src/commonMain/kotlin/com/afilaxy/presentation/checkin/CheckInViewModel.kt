package com.afilaxy.presentation.checkin

import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.CheckInType
import com.afilaxy.domain.model.HealthSnapshot
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.CheckInRepository
import com.afilaxy.domain.repository.HealthRepository
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class CheckInState(
    val type: CheckInType = CheckInType.MORNING,
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val alreadyDoneToday: Boolean = false,
    val showCriticalWellbeingCard: Boolean = false,
    val error: String? = null,
    val riskScore: Int? = null,
    val aqi: Int? = null,
    val temperature: Float? = null,
    val humidity: Float? = null,
    val healthSnapshot: HealthSnapshot? = null,
    val healthAvailable: Boolean = false,
    val healthPermissionsGranted: Boolean = false
)

class CheckInViewModel(
    private val checkInRepository: CheckInRepository,
    private val authRepository: AuthRepository,
    private val healthRepository: HealthRepository
) : KMMViewModel() {

    private val _state = MutableStateFlow(CheckInState())
    val state: StateFlow<CheckInState> = _state.asStateFlow()

    fun initialize(
        type: CheckInType,
        riskScore: Int? = null,
        aqi: Int? = null,
        temperature: Float? = null,
        humidity: Float? = null
    ) {
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.coroutineScope.launch {
            _state.update {
                it.copy(
                    type = type, isLoading = true,
                    riskScore = riskScore, aqi = aqi,
                    temperature = temperature, humidity = humidity
                )
            }

            val existing = checkInRepository.getTodayCheckIn(userId, type).getOrNull()
            if (existing != null) {
                _state.update { it.copy(isLoading = false, alreadyDoneToday = true) }
                return@launch
            }

            val healthAvailable = healthRepository.isAvailable()
            val hasPerms = if (healthAvailable) {
                try { healthRepository.hasPermissions() } catch (e: Exception) { false }
            } else false
            val snapshot = if (hasPerms) {
                try { healthRepository.getSnapshot(isEvening = type == CheckInType.EVENING) }
                catch (e: Exception) { null }
            } else null

            _state.update {
                it.copy(
                    isLoading = false,
                    healthAvailable = healthAvailable,
                    healthPermissionsGranted = hasPerms,
                    healthSnapshot = snapshot
                )
            }
        }
    }

    fun reloadHealthSnapshot() {
        val type = _state.value.type
        viewModelScope.coroutineScope.launch {
            val hasPerms = try { healthRepository.hasPermissions() } catch (e: Exception) { false }
            val snapshot = if (hasPerms) {
                try { healthRepository.getSnapshot(isEvening = type == CheckInType.EVENING) }
                catch (e: Exception) { null }
            } else null
            _state.update {
                it.copy(healthPermissionsGranted = hasPerms, healthSnapshot = snapshot)
            }
        }
    }

    fun requestHealthPermissions() {
        viewModelScope.coroutineScope.launch {
            val granted = try { healthRepository.requestPermissions() } catch (e: Exception) { false }
            if (granted) reloadHealthSnapshot()
        }
    }

    fun dismissCriticalCard() {
        _state.update { it.copy(showCriticalWellbeingCard = false) }
    }

    fun submitMorningCheckIn(wellbeingA: Boolean, wellbeingB: Boolean, wellbeingC: Boolean) {
        val userId = authRepository.getCurrentUserId() ?: return
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true) }

            val response = CheckInResponse(
                userId = userId,
                type = CheckInType.MORNING.name,
                timestamp = getCurrentTimeMillis(),
                wellbeingA = wellbeingA,
                wellbeingB = wellbeingB,
                wellbeingC = wellbeingC,
                riskScore = _state.value.riskScore,
                aqi = _state.value.aqi,
                temperature = _state.value.temperature,
                humidity = _state.value.humidity,
                hourOfDay = now.hour,
                dayOfWeek = now.dayOfWeek.ordinal + 1,
                monthOfYear = now.monthNumber
            )

            val critical = wellbeingA == false && wellbeingB == false && wellbeingC == false
            checkInRepository.saveCheckIn(response)
                .onSuccess { _state.update { it.copy(isLoading = false, isSubmitted = true, showCriticalWellbeingCard = critical) } }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Erro ao salvar check-in") }
                }
        }
    }

    fun submitEveningCheckIn(wellbeingA: Boolean, wellbeingB: Boolean, wellbeingC: Boolean) {
        val userId = authRepository.getCurrentUserId() ?: return
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true) }

            val response = CheckInResponse(
                userId = userId,
                type = CheckInType.EVENING.name,
                timestamp = getCurrentTimeMillis(),
                wellbeingA = wellbeingA,
                wellbeingB = wellbeingB,
                wellbeingC = wellbeingC,
                riskScore = _state.value.riskScore,
                aqi = _state.value.aqi,
                temperature = _state.value.temperature,
                humidity = _state.value.humidity,
                hourOfDay = now.hour,
                dayOfWeek = now.dayOfWeek.ordinal + 1,
                monthOfYear = now.monthNumber
            )

            val critical = wellbeingA == false && wellbeingB == false && wellbeingC == false
            checkInRepository.saveCheckIn(response)
                .onSuccess { _state.update { it.copy(isLoading = false, isSubmitted = true, showCriticalWellbeingCard = critical) } }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Erro ao salvar check-in") }
                }
        }
    }
}
