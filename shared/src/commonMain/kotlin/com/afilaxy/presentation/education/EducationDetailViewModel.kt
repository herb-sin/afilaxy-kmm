package com.afilaxy.presentation.education

import com.afilaxy.domain.model.EducationalContent
import com.afilaxy.domain.repository.EducationalContentRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EducationDetailViewModel(
    private val repository: EducationalContentRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(EducationDetailState())
    val state: StateFlow<EducationDetailState> = _state.asStateFlow()
    
    fun loadContent(id: String) {
        viewModelScope.coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val content = repository.getById(id)
                _state.value = _state.value.copy(
                    content = content,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar conteúdo"
                )
            }
        }
    }
}

data class EducationDetailState(
    val content: EducationalContent? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
