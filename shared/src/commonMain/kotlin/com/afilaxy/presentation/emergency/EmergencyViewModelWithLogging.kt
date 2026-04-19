package com.afilaxy.presentation.emergency

import com.afilaxy.domain.model.EmergencyStatus
import com.afilaxy.domain.usecase.CreateEmergencyUseCase
import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.domain.repository.LocationRepository
import com.afilaxy.domain.repository.NotificationRepository
import com.afilaxy.util.Logger
import com.afilaxy.util.logDebug
import com.afilaxy.util.logError
import com.afilaxy.util.logInfo
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel compartilhado para Emergências
 * 
 * EXEMPLO DE INTEGRAÇÃO COM LOGGER:
 * - logDebug() para debug detalhado
 * - logInfo() para eventos importantes
 * - logError() para erros
 */
class EmergencyViewModelWithLogging(
    private val emergencyRepository: EmergencyRepository,
    private val locationRepository: LocationRepository,
    private val notificationRepository: NotificationRepository,
    private val createEmergencyUseCase: CreateEmergencyUseCase
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(EmergencyState())
    val state: StateFlow<EmergencyState> = _state.asStateFlow()
    
    init {
        logDebug("EmergencyViewModel initialized")
        checkActiveEmergency()
    }
    
    private fun checkActiveEmergency() {
        viewModelScope.coroutineScope.launch {
            logDebug("Checking for active emergency")
            emergencyRepository.getActiveEmergency()
                .onSuccess { emergencyId ->
                    if (emergencyId != null) {
                        logInfo("Found active emergency: $emergencyId")
                    } else {
                        logDebug("No active emergency found")
                    }
                    _state.update { 
                        it.copy(
                            emergencyId = emergencyId,
                            hasActiveEmergency = emergencyId != null
                        )
                    }
                    
                    if (emergencyId != null) {
                        observeEmergencyStatus(emergencyId)
                    }
                }
                .onFailure { exception ->
                    logError("Failed to check active emergency", exception)
                }
        }
    }
    
    fun observeEmergencyStatus(emergencyId: String) {
        viewModelScope.coroutineScope.launch {
            logDebug("Starting to observe emergency status: $emergencyId")
            emergencyRepository.observeEmergencyStatus(emergencyId)
                .collect { status ->
                    logInfo("Emergency status changed: $status")
                    _state.update { it.copy(emergencyStatus = status) }
                }
        }
    }
    
    fun onCreateEmergency() {
        viewModelScope.coroutineScope.launch {
            logInfo("Creating new emergency")
            _state.update { it.copy(isCreatingEmergency = true, error = null) }
            
            val location = locationRepository.getCurrentLocation()
            if (location == null) {
                logError("Failed to get location for emergency")
                _state.update { 
                    it.copy(
                        isCreatingEmergency = false,
                        error = "Erro ao obter localização"
                    )
                }
                return@launch
            }
            
            logDebug("Location obtained: lat=${location.latitude}, lon=${location.longitude}")
            
            _state.update { 
                it.copy(
                    userLatitude = location.latitude,
                    userLongitude = location.longitude
                )
            }
            
            val emergency = com.afilaxy.domain.model.Emergency.create(
                id = "",
                userId = "",
                userName = "",
                location = location
            )
            
            createEmergencyUseCase.execute(emergency)
                .onSuccess { emergencyId ->
                    logInfo("Emergency created successfully: $emergencyId")
                    _state.update { 
                        it.copy(
                            emergencyId = emergencyId,
                            hasActiveEmergency = true,
                            isCreatingEmergency = false,
                            error = null
                        )
                    }
                    
                    observeEmergencyStatus(emergencyId)
                    findNearbyHelpers()
                }
                .onFailure { exception ->
                    logError("Failed to create emergency: ${exception.message}", exception)
                    _state.update { 
                        it.copy(
                            isCreatingEmergency = false,
                            error = exception.message ?: "Erro ao criar emergência"
                        )
                    }
                }
        }
    }
    
    private fun findNearbyHelpers() {
        val latitude = _state.value.userLatitude
        val longitude = _state.value.userLongitude
        
        if (latitude == 0.0 && longitude == 0.0) {
            logError("Cannot find helpers: invalid coordinates")
            return
        }
        
        viewModelScope.coroutineScope.launch {
            logDebug("Finding nearby helpers at lat=$latitude, lon=$longitude")
            emergencyRepository.findNearbyHelpers(
                location = com.afilaxy.domain.model.Location(
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = com.afilaxy.domain.model.getCurrentTimeMillis()
                ),
                radiusKm = 0.25 // ~250m — resposta a pé em ~3 min
            )
                .onSuccess { helpers ->
                    logInfo("Found ${helpers.size} nearby helpers")
                    _state.update { it.copy(nearbyHelpers = helpers) }
                }
                .onFailure { exception ->
                    logError("Failed to find nearby helpers: ${exception.message}", exception)
                }
        }
    }
}
