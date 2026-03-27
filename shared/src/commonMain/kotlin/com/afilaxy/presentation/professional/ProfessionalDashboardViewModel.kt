package com.afilaxy.presentation.professional

import com.afilaxy.domain.model.*
import com.afilaxy.domain.repository.ProfessionalRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfessionalDashboardState(
    val isLoading: Boolean = false,
    val dashboard: ProfessionalDashboard? = null,
    val patients: List<PatientSummary> = emptyList(),
    val consultations: List<Consultation> = emptyList(),
    val crisisAnalytics: List<CrisisData> = emptyList(),
    val alerts: List<PatientAlert> = emptyList(),
    val selectedPeriod: String = "MÊS",
    val error: String? = null
)

class ProfessionalDashboardViewModel(
    private val professionalRepository: ProfessionalRepository,
    private val professionalId: String
) : KMMViewModel() {

    private val _state = MutableStateFlow(ProfessionalDashboardState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ProfessionalDashboardState()
    )

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                professionalRepository.getDashboard(professionalId).collect { dashboard ->
                    _state.value = _state.value.copy(dashboard = dashboard)
                }
                
                professionalRepository.getPatients(professionalId).collect { patients ->
                    _state.value = _state.value.copy(patients = patients)
                }
                
                professionalRepository.getConsultations(professionalId).collect { consultations ->
                    _state.value = _state.value.copy(consultations = consultations)
                }
                
                professionalRepository.getPatientAlerts(professionalId).collect { alerts ->
                    _state.value = _state.value.copy(
                        alerts = alerts,
                        isLoading = false
                    )
                }
                
                loadCrisisAnalytics()
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    private fun loadCrisisAnalytics() {
        viewModelScope.coroutineScope.launch {
            try {
                professionalRepository.getCrisisAnalytics(
                    professionalId, 
                    _state.value.selectedPeriod
                ).collect { analytics ->
                    _state.value = _state.value.copy(crisisAnalytics = analytics)
                }
            } catch (e: Exception) {
                // Analytics are optional
            }
        }
    }

    fun selectPeriod(period: String) {
        _state.value = _state.value.copy(selectedPeriod = period)
        loadCrisisAnalytics()
    }

    fun markAlertAsRead(alertId: String) {
        viewModelScope.coroutineScope.launch {
            try {
                professionalRepository.markAlertAsRead(alertId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun updatePatientStatus(patientId: String, status: PatientStatus) {
        viewModelScope.coroutineScope.launch {
            try {
                professionalRepository.updatePatientStatus(patientId, status)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun getPriorityPatients(): List<PatientSummary> {
        return _state.value.patients.filter { 
            it.status == PatientStatus.URGENTE || it.status == PatientStatus.ALERTA 
        }.sortedBy { it.status }
    }

    fun getUrgentAlerts(): List<PatientAlert> {
        return _state.value.alerts.filter { it.isUrgent }
    }
}