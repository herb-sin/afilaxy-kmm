package com.afilaxy.presentation.profile

import com.afilaxy.domain.model.EmergencyContact
import com.afilaxy.domain.model.UserHealthData
import com.afilaxy.domain.model.UserProfile
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.ProfileRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init {
        loadProfile()
    }
    
    fun loadProfile() {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _state.update { it.copy(isLoading = false, error = "Usuário não autenticado") }
                return@launch
            }
            
            profileRepository.getProfile(currentUser.uid)
                .onSuccess { profile ->
                    _state.update { 
                        it.copy(
                            profile = profile ?: UserProfile(
                                uid = currentUser.uid,
                                name = currentUser.name ?: "",
                                email = currentUser.email
                            ),
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Erro ao carregar perfil"
                        )
                    }
                }
        }
    }
    
    fun updateProfile(profile: UserProfile) {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isSaving = true, error = null, successMessage = null) }
            
            profileRepository.updateProfile(profile)
                .onSuccess {
                    _state.update { 
                        it.copy(
                            profile = profile,
                            isSaving = false,
                            successMessage = "Perfil atualizado com sucesso"
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isSaving = false,
                            error = exception.message ?: "Erro ao atualizar perfil"
                        )
                    }
                }
        }
    }
    
    fun updateHealthData(healthData: UserHealthData) {
        val currentProfile = _state.value.profile ?: return
        
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isSaving = true, error = null, successMessage = null) }
            
            profileRepository.updateHealthData(currentProfile.uid, healthData)
                .onSuccess {
                    _state.update { 
                        it.copy(
                            profile = currentProfile.copy(healthData = healthData),
                            isSaving = false,
                            successMessage = "Dados de saúde atualizados"
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isSaving = false,
                            error = exception.message ?: "Erro ao atualizar dados de saúde"
                        )
                    }
                }
        }
    }
    
    fun updateEmergencyContact(contact: EmergencyContact) {
        val currentProfile = _state.value.profile ?: return
        
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isSaving = true, error = null, successMessage = null) }
            
            profileRepository.updateEmergencyContact(currentProfile.uid, contact)
                .onSuccess {
                    _state.update { 
                        it.copy(
                            profile = currentProfile.copy(emergencyContact = contact),
                            isSaving = false,
                            successMessage = "Contato de emergência atualizado"
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isSaving = false,
                            error = exception.message ?: "Erro ao atualizar contato"
                        )
                    }
                }
        }
    }
    
    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}
