package com.afilaxy.presentation.home

import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.domain.repository.LocationRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val location = locationRepository.getCurrentLocation()
                if (location != null) {
                    emergencyRepository.createEmergency(location.latitude, location.longitude)
                        .onFailure { e ->
                            _state.value = _state.value.copy(isLoading = false, error = e.message)
                            return@launch
                        }
                }
                _state.value = _state.value.copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
