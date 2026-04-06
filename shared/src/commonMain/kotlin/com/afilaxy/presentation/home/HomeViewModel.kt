package com.afilaxy.presentation.home

import com.afilaxy.domain.model.*
import com.afilaxy.domain.repository.*
import com.afilaxy.util.TimeUtils
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.*
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
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeState()
    )

    fun requestHelp() {
        viewModelScope.coroutineScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                location?.let {
                    val emergency = Emergency(
                        id = "",
                        userId = "",
                        userName = "User",
                        location = it,
                        description = "Preciso de ajuda com bombinha de resgate",
                        timestamp = TimeUtils.currentTimeMillis()
                    )
                    emergencyRepository.createEmergency(emergency)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}