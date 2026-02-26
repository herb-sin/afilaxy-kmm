package com.afilaxy.presentation.professional

import com.afilaxy.domain.model.HealthProfessional
import com.afilaxy.domain.model.Specialty
import com.afilaxy.domain.repository.HealthProfessionalRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfessionalListViewModel(
    private val repository: HealthProfessionalRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(ProfessionalListState())
    val state: StateFlow<ProfessionalListState> = _state.asStateFlow()
    
    init {
        loadProfessionals()
    }
    
    fun loadProfessionals(specialty: Specialty? = null) {
        viewModelScope.coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val professionals = if (specialty != null) {
                    repository.findBySpecialty(specialty)
                } else {
                    repository.getAll()
                }
                
                _state.value = _state.value.copy(
                    professionals = professionals,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar profissionais"
                )
            }
        }
    }
    
    fun filterBySpecialty(specialty: Specialty?) {
        loadProfessionals(specialty)
    }
}

data class ProfessionalListState(
    val professionals: List<HealthProfessional> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
