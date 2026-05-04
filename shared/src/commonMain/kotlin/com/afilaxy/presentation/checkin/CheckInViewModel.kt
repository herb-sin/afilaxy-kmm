package com.afilaxy.presentation.checkin

import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.CheckInType
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.CheckInRepository
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
    val rescueInhalerName: String? = null,
    val asmaType: String? = null,
    val asmaTypeSeverity: String? = null,
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val alreadyDoneToday: Boolean = false,
    val error: String? = null,
    // Contexto ambiental e clínico para enriquecer o dado para ML
    val riskScore: Int? = null,
    val aqi: Int? = null,
    val temperature: Float? = null,
    val humidity: Float? = null
)

class CheckInViewModel(
    private val checkInRepository: CheckInRepository,
    private val authRepository: AuthRepository
) : KMMViewModel() {

    private val _state = MutableStateFlow(CheckInState())
    val state: StateFlow<CheckInState> = _state.asStateFlow()

    /** Inicializa o check-in: verifica se já foi feito hoje e busca o nome da bombinha. */
    fun initialize(
        type: CheckInType,
        riskScore: Int? = null,
        aqi: Int? = null,
        temperature: Float? = null,
        humidity: Float? = null
    ) {
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(type = type, isLoading = true,
                riskScore = riskScore, aqi = aqi,
                temperature = temperature, humidity = humidity) }

            // Verifica se já respondeu hoje
            val existing = checkInRepository.getTodayCheckIn(userId, type).getOrNull()
            if (existing != null) {
                _state.update { it.copy(isLoading = false, alreadyDoneToday = true) }
                return@launch
            }

            // Busca nome da bombinha e tipo de asma do perfil médico (contexto clínico para ML)
            val inhalerName = checkInRepository.getRescueInhalerName(userId).getOrNull()
            val (asmaType, asmaTypeSeverity) = checkInRepository.getAsmaTypeInfo(userId).getOrNull()
                ?: Pair(null, null)
            _state.update {
                it.copy(
                    isLoading = false,
                    rescueInhalerName = inhalerName,
                    asmaType = asmaType,
                    asmaTypeSeverity = asmaTypeSeverity
                )
            }
        }
    }

    /** Check-in matinal: usuário responde se está com a bombinha. */
    fun submitMorningCheckIn(hasInhaler: Boolean) {
        val userId = authRepository.getCurrentUserId() ?: return
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true) }

            val response = CheckInResponse(
                userId = userId,
                type = CheckInType.MORNING.name,
                timestamp = getCurrentTimeMillis(),
                hasRescueInhaler = hasInhaler,
                rescueInhalerName = _state.value.rescueInhalerName,
                riskScore = _state.value.riskScore,
                aqi = _state.value.aqi,
                temperature = _state.value.temperature,
                humidity = _state.value.humidity,
                hourOfDay = now.hour,
                dayOfWeek = now.dayOfWeek.ordinal + 1,
                monthOfYear = now.monthNumber,
                asmaType = _state.value.asmaType,
                asmaTypeSeverity = _state.value.asmaTypeSeverity
            )

            checkInRepository.saveCheckIn(response)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isSubmitted = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Erro ao salvar check-in") }
                }
        }
    }

    /** Check-in noturno: usuário responde se teve crise hoje. */
    fun submitEveningCheckIn(
        hadCrisis: Boolean,
        severity: String? = null,
        usedRescueInhaler: Boolean? = null
    ) {
        val userId = authRepository.getCurrentUserId() ?: return
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true) }

            val response = CheckInResponse(
                userId = userId,
                type = CheckInType.EVENING.name,
                timestamp = getCurrentTimeMillis(),
                hadCrisisToday = hadCrisis,
                crisisSeverity = if (hadCrisis) severity else null,
                usedRescueInhaler = if (hadCrisis) usedRescueInhaler else null,
                riskScore = _state.value.riskScore,
                aqi = _state.value.aqi,
                temperature = _state.value.temperature,
                humidity = _state.value.humidity,
                hourOfDay = now.hour,
                dayOfWeek = now.dayOfWeek.ordinal + 1,
                monthOfYear = now.monthNumber,
                asmaType = _state.value.asmaType,
                asmaTypeSeverity = _state.value.asmaTypeSeverity
            )

            checkInRepository.saveCheckIn(response)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, isSubmitted = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Erro ao salvar check-in") }
                }
        }
    }
}
