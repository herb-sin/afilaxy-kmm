package com.afilaxy.presentation.professional

import com.afilaxy.domain.model.HealthProfessional
import com.afilaxy.domain.repository.HealthProfessionalRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfessionalDetailViewModel(
    private val repository: HealthProfessionalRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(ProfessionalDetailState())
    val state: StateFlow<ProfessionalDetailState> = _state.asStateFlow()
    
    fun loadProfessional(id: String) {
        viewModelScope.coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val professional = repository.getById(id)
                _state.value = _state.value.copy(
                    professional = professional,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar profissional"
                )
            }
        }
    }
}

data class ProfessionalDetailState(
    val professional: HealthProfessional? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
