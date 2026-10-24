package com.afilaxy.presentation.act

import com.afilaxy.domain.model.AsthmaControlTestResult
import com.afilaxy.domain.model.getCurrentTimeMillis
import com.afilaxy.domain.repository.ActRepository
import com.afilaxy.domain.repository.AuthRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActState(
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val latestResult: AsthmaControlTestResult? = null,
    val history: List<AsthmaControlTestResult> = emptyList(),
    val error: String? = null
)

class ActViewModel(
    private val actRepository: ActRepository,
    private val authRepository: AuthRepository
) : KMMViewModel() {

    private val _state = MutableStateFlow(ActState())
    val state: StateFlow<ActState> = _state.asStateFlow()

    /** Carrega o resultado mais recente — usado pra tela de entrada (Perfil / card da Home). */
    fun loadLatest() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true) }
            actRepository.getLatestResult(userId)
                .onSuccess { result -> _state.update { it.copy(isLoading = false, latestResult = result) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun loadHistory() {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true) }
            actRepository.getHistory(userId)
                .onSuccess { history -> _state.update { it.copy(isLoading = false, history = history) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    /** Reseta o estado de "respondido" para permitir refazer o teste. */
    fun startNewTest() {
        _state.update { it.copy(isSubmitted = false) }
    }

    fun submitTest(
        activityImpairment: Int,
        shortnessOfBreath: Int,
        nighttimeSymptoms: Int,
        rescueMedicationUse: Int,
        selfRatedControl: Int
    ) {
        val userId = authRepository.getCurrentUserId() ?: return
        val score = activityImpairment + shortnessOfBreath + nighttimeSymptoms +
            rescueMedicationUse + selfRatedControl

        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = AsthmaControlTestResult(
                userId = userId,
                timestamp = getCurrentTimeMillis(),
                activityImpairment = activityImpairment,
                shortnessOfBreath = shortnessOfBreath,
                nighttimeSymptoms = nighttimeSymptoms,
                rescueMedicationUse = rescueMedicationUse,
                selfRatedControl = selfRatedControl,
                score = score
            )

            actRepository.saveResult(result)
                .onSuccess {
                    _state.update {
                        it.copy(isLoading = false, isSubmitted = true, latestResult = result)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Erro ao salvar resultado") }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
