package com.afilaxy.presentation.home

import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.domain.repository.LocationRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val emergencyRepository: EmergencyRepository,
    private val locationRepository: LocationRepository
) : KMMViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun requestHelp() {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val location = locationRepository.getCurrentLocation()
                if (location == null) {
                    _state.update { it.copy(isLoading = false, error = "Não foi possível obter localização") }
                    return@launch
                }
                if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
                    _state.update { it.copy(isLoading = false, error = "Localização inválida") }
                    return@launch
                }
                emergencyRepository.createEmergency(location.latitude, location.longitude)
                    .onFailure { e ->
                        _state.update { it.copy(isLoading = false, error = e.message) }
                        return@launch
                    }
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
