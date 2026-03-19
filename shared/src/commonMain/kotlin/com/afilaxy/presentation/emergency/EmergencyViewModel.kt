package com.afilaxy.presentation.emergency

import com.afilaxy.domain.model.EmergencyStatus
import com.afilaxy.domain.usecase.CreateEmergencyUseCase
import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.domain.repository.LocationRepository
import com.afilaxy.domain.repository.NotificationRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel compartilhado para Emergências
 */
class EmergencyViewModel(
    private val emergencyRepository: EmergencyRepository,
    private val locationRepository: LocationRepository,
    private val notificationRepository: NotificationRepository,
    private val createEmergencyUseCase: CreateEmergencyUseCase
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(EmergencyState())
    val state: StateFlow<EmergencyState> = _state.asStateFlow()
    
    fun observeEmergencyStatus(emergencyId: String) {
        // no-op: status updates handled by onCancelEmergency/onResolveEmergency
    }
    
    fun onCreateEmergency() {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isCreatingEmergency = true, error = null) }
            
            // Obter localização
            val location = locationRepository.getCurrentLocation()
            if (location == null) {
                _state.update { 
                    it.copy(
                        isCreatingEmergency = false,
                        error = "Erro ao obter localização"
                    )
                }
                return@launch
            }
            
            _state.update { 
                it.copy(
                    userLatitude = location.latitude,
                    userLongitude = location.longitude
                )
            }
            
            // Criar emergência via use case (inclui validação de coordenadas)
            val emergency = com.afilaxy.domain.model.Emergency.create(
                id = "",
                userId = "",
                userName = "",
                location = location
            )
            createEmergencyUseCase.execute(emergency)
                .onSuccess { emergencyId ->
                    // Desativar helper mode — quem solicita ajuda não pode ser helper
                    if (_state.value.isHelperMode) {
                        emergencyRepository.deactivateHelper()
                    }
                    _state.update { 
                        it.copy(
                            emergencyId = emergencyId,
                            hasActiveEmergency = true,
                            isCreatingEmergency = false,
                            isHelperMode = false,
                            error = null
                        )
                    }
                    
                    // Observar status da emergência
                    observeEmergencyStatus(emergencyId)
                    
                    // Buscar helpers próximos
                    findNearbyHelpers()
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isCreatingEmergency = false,
                            error = exception.message ?: "Erro ao criar emergência"
                        )
                    }
                }
        }
    }
    
    fun onCancelEmergency() {
        val emergencyId = _state.value.emergencyId ?: return
        
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            emergencyRepository.cancelEmergency(emergencyId)
                .onSuccess {
                    _state.update { 
                        it.copy(
                            emergencyId = null,
                            hasActiveEmergency = false,
                            isLoading = false,
                            nearbyHelpers = emptyList()
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Erro ao cancelar emergência"
                        )
                    }
                }
        }
    }
    
    fun onToggleHelperMode(enable: Boolean) {
        _state.update {
            it.copy(
                isHelperMode = enable,
                isLoading = false,
                error = null
            )
        }
    }

    fun activateHelperWithLocation() {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val location = locationRepository.getCurrentLocation()
            if (location == null) {
                _state.update { it.copy(isLoading = false, error = "Erro ao obter localização") }
                return@launch
            }
            emergencyRepository.activateHelper(location.latitude, location.longitude)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isHelperMode = true,
                            isLoading = false,
                            userLatitude = location.latitude,
                            userLongitude = location.longitude
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = "Erro ao ativar helper: ${e.message}") }
                }
        }
    }

    fun deactivateHelper() {
        viewModelScope.coroutineScope.launch {
            emergencyRepository.deactivateHelper()
            _state.update { it.copy(isHelperMode = false, isLoading = false) }
        }
    }
    
    fun onAcceptEmergency(emergencyId: String) {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            emergencyRepository.acceptEmergency(emergencyId)
                .onSuccess {
                    _state.update { 
                        it.copy(
                            emergencyId = emergencyId,
                            hasActiveEmergency = true,
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Erro ao aceitar emergência"
                        )
                    }
                }
        }
    }
    
    fun onResolveEmergency() {
        val emergencyId = _state.value.emergencyId ?: return
        
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            emergencyRepository.updateEmergencyStatus(emergencyId, EmergencyStatus.RESOLVED)
                .onSuccess {
                    _state.update { 
                        it.copy(
                            emergencyId = null,
                            hasActiveEmergency = false,
                            isLoading = false,
                            nearbyHelpers = emptyList()
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Erro ao resolver emergência"
                        )
                    }
                }
        }
    }
    
    fun startObservingIncomingEmergencies(latitude: Double, longitude: Double) {
        observeIncomingEmergencies(latitude, longitude)
    }

    // Set persistente de IDs já notificados — sobrevive a recomposições do Compose
    private val _notifiedEmergencyIds = mutableSetOf<String>()

    fun isAlreadyNotified(emergencyId: String): Boolean = emergencyId in _notifiedEmergencyIds

    fun markAsNotified(emergencyId: String) { _notifiedEmergencyIds.add(emergencyId) }

    private fun observeIncomingEmergencies(latitude: Double, longitude: Double) {
        viewModelScope.coroutineScope.launch {
            try {
                emergencyRepository.observeNearbyEmergencies(latitude, longitude, 5.0)
                    .collect { emergencies ->
                        _state.update { it.copy(incomingEmergencies = emergencies) }
                    }
            } catch (e: Exception) {
                // Log para diagnóstico — não crashar o app
                com.afilaxy.util.Logger.e("EmergencyViewModel", "observeIncomingEmergencies failed", e)
            }
        }
    }

    private fun findNearbyHelpers() {
        val latitude = _state.value.userLatitude
        val longitude = _state.value.userLongitude
        
        if (latitude == 0.0 && longitude == 0.0) return
        
        viewModelScope.coroutineScope.launch {
            emergencyRepository.findNearbyHelpers(
                location = com.afilaxy.domain.model.Location(
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = com.afilaxy.domain.model.getCurrentTimeMillis()
                ),
                radiusKm = 5.0
            )
                .onSuccess { helpers ->
                    _state.update { it.copy(nearbyHelpers = helpers) }
                }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
