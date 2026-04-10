package com.afilaxy.presentation.risk

import com.afilaxy.domain.model.RiskScore
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.EnvironmentalRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RiskState(
    val riskScore: RiskScore? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class RiskViewModel(
    private val environmentalRepository: EnvironmentalRepository,
    private val authRepository: AuthRepository
) : KMMViewModel() {

    private val _state = MutableStateFlow(RiskState())
    val state: StateFlow<RiskState> = _state.asStateFlow()

    /**
     * Calcula o score de risco para as coordenadas fornecidas.
     * Chamado pela HomeScreen após obter a localização do usuário.
     */
    fun loadRiskScore(latitude: Double, longitude: Double) {
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            environmentalRepository.calculateRiskScore(userId, latitude, longitude)
                .onSuccess { score ->
                    _state.update { it.copy(riskScore = score, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
