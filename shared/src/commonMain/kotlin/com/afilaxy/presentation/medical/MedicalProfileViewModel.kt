package com.afilaxy.presentation.medical

import com.afilaxy.domain.model.*
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.MedicalRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MedicalProfileState(
    val isLoading: Boolean = false,
    val medicalProfile: MedicalProfile? = null,
    val medications: List<Medication> = emptyList(),
    val emergencyContacts: List<MedicalEmergencyContact> = emptyList(),
    val medicalHistory: List<MedicalExam> = emptyList(),
    val error: String? = null
)

class MedicalProfileViewModel(
    private val medicalRepository: MedicalRepository,
    private val authRepository: AuthRepository
) : KMMViewModel() {

    /** userId resolvido em runtime — nunca fica vazio se o usuário está autenticado */
    private val userId: String
        get() = authRepository.getCurrentUserId() ?: ""

    private companion object {
        const val SUBSCRIBE_TIMEOUT_MS = 5_000L
        const val EMERGENCY_STEP_WAIT_MINUTES = 10
        const val RESCUE_INHALER_DOSES = 2
    }

    private val _state = MutableStateFlow(MedicalProfileState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS),
        MedicalProfileState()
    )

    init {
        loadMedicalData()
    }

    private fun loadMedicalData() {
        viewModelScope.coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val profileResult = medicalRepository.getMedicalProfile(userId)
                profileResult.onSuccess { profile ->
                    _state.value = _state.value.copy(medicalProfile = profile)
                }
                
                medicalRepository.getMedications(userId).collect { medications ->
                    _state.value = _state.value.copy(medications = medications)
                }
                
                medicalRepository.getEmergencyContacts(userId).collect { contacts ->
                    _state.value = _state.value.copy(emergencyContacts = contacts)
                }
                
                medicalRepository.getMedicalHistory(userId).collect { history ->
                    _state.value = _state.value.copy(
                        medicalHistory = history,
                        isLoading = false
                    )
                }
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun addMedication(medication: Medication) {
        viewModelScope.coroutineScope.launch {
            try {
                medicalRepository.addMedication(userId, medication)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.coroutineScope.launch {
            try {
                medicalRepository.updateMedication(medication)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun removeMedication(medicationId: String) {
        viewModelScope.coroutineScope.launch {
            try {
                medicalRepository.removeMedication(medicationId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun addEmergencyContact(contact: MedicalEmergencyContact) {
        viewModelScope.coroutineScope.launch {
            try {
                medicalRepository.addEmergencyContact(userId, contact)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun updateMedicalProfile(profile: MedicalProfile) {
        viewModelScope.coroutineScope.launch {
            try {
                medicalRepository.updateMedicalProfile(profile)
                _state.value = _state.value.copy(medicalProfile = profile)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun getEmergencyProtocol(): List<EmergencyStep> {
        return _state.value.medicalProfile?.emergencyProtocol ?: listOf(
            EmergencyStep(1, "Sentar em posição vertical", "Tentar manter a calma"),
            EmergencyStep(2, "Usar inalador de resgate", "Salbutamol: $RESCUE_INHALER_DOSES jatos"),
            EmergencyStep(3, "Se não houver melhora em $EMERGENCY_STEP_WAIT_MINUTES min", "Ligar para emergência")
        )
    }
}
