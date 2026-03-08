package com.afilaxy.presentation.ubs

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.UBS
import com.afilaxy.domain.repository.LocationRepository
import com.afilaxy.domain.repository.UBSRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UBSMapViewModel(
    private val ubsRepository: UBSRepository,
    private val locationRepository: LocationRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(UBSMapState())
    val state: StateFlow<UBSMapState> = _state.asStateFlow()
    
    init {
        loadNearbyUBS()
    }
    
    fun loadNearbyUBS() {
        viewModelScope.coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val currentLocation = locationRepository.getCurrentLocation()
                
                if (currentLocation != null) {
                    val nearbyUBS = ubsRepository.getNearby(currentLocation, radiusKm = 50.0)
                    
                    _state.value = _state.value.copy(
                        ubsList = nearbyUBS,
                        currentLocation = currentLocation,
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Não foi possível obter sua localização"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar UBS próximas"
                )
            }
        }
    }
}

data class UBSMapState(
    val ubsList: List<UBS> = emptyList(),
    val currentLocation: Location? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
