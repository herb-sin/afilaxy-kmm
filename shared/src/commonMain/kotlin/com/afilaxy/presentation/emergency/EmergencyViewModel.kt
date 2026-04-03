package com.afilaxy.presentation.emergency

import com.afilaxy.domain.model.EmergencyStatus
import com.afilaxy.domain.usecase.CreateEmergencyUseCase
import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.domain.repository.LocationRepository
import com.afilaxy.domain.repository.NotificationRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.Job
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
    
    /** Pré-carrega o emergencyId no estado quando a tela abre via notificação.
     *  Sempre sobrescreve — o ID anterior pode ser de uma emergência já encerrada. */
    fun preloadEmergencyId(emergencyId: String) {
        if (_state.value.emergencyId != emergencyId) {
            _state.update { it.copy(emergencyId = emergencyId, hasActiveEmergency = false) }
        }
    }

    fun fetchEmergencyExpiresAt(emergencyId: String) {
        viewModelScope.coroutineScope.launch {
            try {
                val doc = emergencyRepository.getEmergencyExpiresAt(emergencyId)
                if (doc != null) _state.update { it.copy(emergencyExpiresAt = doc) }
            } catch (e: Exception) {
                com.afilaxy.util.Logger.e("EmergencyViewModel", "fetchEmergencyExpiresAt failed: ${e.message}")
            }
        }
    }

    /** Injetado quando o Firestore não retornou expiresAt (tipo incompatível ou campo ausente).
     *  Usa 3 minutos a partir de agora como estimativa conservadora para destravar o countdown. */
    fun applyFallbackExpiresAt() {
        if (_state.value.emergencyExpiresAt == null) {
            _state.update { it.copy(emergencyExpiresAt = com.afilaxy.domain.model.getCurrentTimeMillis() + 180_000L) }
        }
    }

    private var statusObservedId: String? = null
    private var statusObserverJob: Job? = null

    fun observeEmergencyStatus(emergencyId: String) {
        if (statusObservedId == emergencyId) return
        statusObservedId = emergencyId
        statusObserverJob?.cancel()
        statusObserverJob = viewModelScope.coroutineScope.launch {
            emergencyRepository.observeEmergencyStatus(emergencyId)
                .collect { status ->
                    _state.update { it.copy(emergencyStatus = status) }
                }
        }
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
                    com.afilaxy.util.Logger.d("EmergencyViewModel", "onCreateEmergency success emergencyId=$emergencyId")
                    if (_state.value.isHelperMode) {
                        emergencyRepository.deactivateHelper()
                    }
                    val expiresAt = com.afilaxy.domain.model.getCurrentTimeMillis() + 180000L
                    _state.update {
                        it.copy(
                            emergencyId = emergencyId,
                            hasActiveEmergency = true,
                            isCreatingEmergency = false,
                            isHelperMode = false,
                            isRequester = true,
                            emergencyExpiresAt = expiresAt,
                            error = null
                        )
                    }
                    observeEmergencyStatus(emergencyId)
                    findNearbyHelpers()
                }
                .onFailure { exception ->
                    com.afilaxy.util.Logger.e("EmergencyViewModel", "onCreateEmergency failed: ${exception.message}", exception)
                    _state.update {
                        it.copy(
                            isCreatingEmergency = false,
                            error = exception.message ?: "Erro ao criar emergência"
                        )
                    }
                }
        }
    }
    
    // Evita re-navegação para EmergencyRequestScreen após já ter navegado
    private val _navigatedEmergencyIds = mutableSetOf<String>()
    fun markNavigatedToRequest(emergencyId: String) { _navigatedEmergencyIds.add(emergencyId) }
    fun wasNavigatedToRequest(emergencyId: String) = emergencyId in _navigatedEmergencyIds

    /** Limpa apenas o estado local — sem I/O. Seguro para chamar do iOS. */
    fun onClearEmergencyState() {
        _state.update {
            it.copy(
                emergencyId = null,
                hasActiveEmergency = false,
                isLoading = false,
                nearbyHelpers = emptyList(),
                emergencyStatus = null,
                isRequester = false,
                emergencyExpiresAt = null  // CRÍTICO: evita stale expiresAt de emergência anterior
            )
        }
        // Para o observer explicitamente — onCancelEmergency também faz isso,
        // mas onClearEmergencyState é chamado independentemente (ex: logout, iOS freezeSwift).
        statusObserverJob?.cancel()
        statusObserverJob = null
        statusObservedId = null
    }

    /**
     * Cancela todos os coroutines de observação em longa duração.
     * Chamado pelo iOS em freezeSwift() durante o logout, ANTES de onClearEmergencyState().
     * Sem isso, os Jobs continuam rodando no viewModelScope e quando o Firestore emite um
     * evento pós-logout o runtime Kotlin/Native aborta (SIGABRT).
     */
    fun cancelAllObservations() {
        statusObserverJob?.cancel()
        statusObserverJob = null
        statusObservedId = null

        incomingEmergenciesObserverJob?.cancel()
        incomingEmergenciesObserverJob = null
        emergencyObserverStarted = false

        helpersObserverJob?.cancel()
        helpersObserverJob = null
        helpersObserverStarted = false
    }

    fun onCancelEmergency() {
        val emergencyId = _state.value.emergencyId ?: return
        // Limpa estado local imediatamente — independente do resultado do Firestore
        _state.update {
            it.copy(
                emergencyId = null,
                hasActiveEmergency = false,
                isLoading = false,
                nearbyHelpers = emptyList(),
                emergencyStatus = null,
                emergencyExpiresAt = null  // CRÍTICO: evita stale expiresAt de emergência anterior
            )
        }
        // Para o observer ANTES de limpar o guard — evita que o job antigo emita
        // status=cancelled e sobrescreva o estado de uma emergência futura.
        statusObserverJob?.cancel()
        statusObserverJob = null
        statusObservedId = null
        viewModelScope.coroutineScope.launch {
            emergencyRepository.cancelEmergency(emergencyId)
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
                    // Layer 1 — observer ligado ao helper mode, coordenadas garantidas
                    startObservingIncomingEmergencies(location.latitude, location.longitude)
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

    /** Chamado no logout: cancela emergência ativa e desativa helper mode. */
    fun onLogout() {
        viewModelScope.coroutineScope.launch {
            val emergencyId = _state.value.emergencyId
            if (_state.value.hasActiveEmergency && emergencyId != null) {
                emergencyRepository.cancelEmergency(emergencyId)
            }
            if (_state.value.isHelperMode) {
                emergencyRepository.deactivateHelper()
            }
            _state.update { EmergencyState() }
        }
    }
    
    fun onAcceptEmergency(emergencyId: String) {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            emergencyRepository.acceptEmergency(emergencyId)
                .onSuccess {
                    com.afilaxy.util.Logger.d("EmergencyViewModel", "onAcceptEmergency success emergencyId=$emergencyId")
                    _state.update {
                        it.copy(
                            emergencyId = emergencyId,
                            hasActiveEmergency = true,
                            isRequester = false,
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    com.afilaxy.util.Logger.e("EmergencyViewModel", "onAcceptEmergency failed emergencyId=$emergencyId: ${exception.message}")
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
                            isRequester = false,
                            isLoading = false,
                            nearbyHelpers = emptyList(),
                            emergencyStatus = null
                        )
                    }
                    statusObservedId = null
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
    
    private var emergencyObserverStarted = false
    private var incomingEmergenciesObserverJob: Job? = null

    fun startObservingIncomingEmergencies(latitude: Double, longitude: Double) {
        if (emergencyObserverStarted) return
        emergencyObserverStarted = true
        observeIncomingEmergencies(latitude, longitude)
    }

    // Set persistente de IDs já notificados — sobrevive a recomposições do Compose
    private val _notifiedEmergencyIds = mutableSetOf<String>()

    fun isAlreadyNotified(emergencyId: String): Boolean = emergencyId in _notifiedEmergencyIds

    fun markAsNotified(emergencyId: String) { _notifiedEmergencyIds.add(emergencyId) }

    private fun observeIncomingEmergencies(latitude: Double, longitude: Double) {
        incomingEmergenciesObserverJob?.cancel()
        incomingEmergenciesObserverJob = viewModelScope.coroutineScope.launch {
            try {
                emergencyRepository.observeNearbyEmergencies(latitude, longitude, 0.25)
                    .collect { emergencies ->
                        _state.update { it.copy(incomingEmergencies = emergencies) }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // deixa o job cancelar normalmente
            } catch (e: Exception) {
                com.afilaxy.util.Logger.e("EmergencyViewModel", "observeIncomingEmergencies failed", e)
                emergencyObserverStarted = false
                // Reinicia o observer após 3s para recuperar de erros transitórios
                kotlinx.coroutines.delay(3000)
                val lat = _state.value.userLatitude
                val lon = _state.value.userLongitude
                if (_state.value.isHelperMode && lat != 0.0 && lon != 0.0) {
                    startObservingIncomingEmergencies(lat, lon)
                }
            } finally {
                emergencyObserverStarted = false
            }
        }
    }

    private var helpersObserverStarted = false
    private var helpersObserverJob: Job? = null

    /** Inicia o observer em tempo real de helpers próximos.
     *  Chamado pelo MapScreen (Android) e MapView (iOS) ao abrir o mapa.
     *  Idempotente: chamadas repetidas são ignoradas. */
    fun startObservingNearbyHelpers(latitude: Double, longitude: Double) {
        if (helpersObserverStarted) return
        helpersObserverStarted = true
        helpersObserverJob?.cancel()
        helpersObserverJob = viewModelScope.coroutineScope.launch {
            try {
                emergencyRepository.observeNearbyHelpers(latitude, longitude, 5.0)
                    .collect { helpers ->
                        _state.update { it.copy(nearbyHelpers = helpers) }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                com.afilaxy.util.Logger.e("EmergencyViewModel", "startObservingNearbyHelpers failed: ${e.message}")
            } finally {
                helpersObserverStarted = false
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
