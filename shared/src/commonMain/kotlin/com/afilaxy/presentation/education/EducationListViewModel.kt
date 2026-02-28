package com.afilaxy.presentation.education

import com.afilaxy.domain.model.ContentCategory
import com.afilaxy.domain.model.EducationalContent
import com.afilaxy.domain.repository.EducationalContentRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EducationListViewModel(
    private val repository: EducationalContentRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(EducationListState())
    val state: StateFlow<EducationListState> = _state.asStateFlow()
    
    init {
        loadContent()
    }
    
    fun loadContent(category: ContentCategory? = null) {
        viewModelScope.coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val content = if (category != null) {
                    repository.getByCategory(category)
                } else {
                    repository.getAll()
                }
                
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
    
    fun filterByCategory(category: ContentCategory?) {
        loadContent(category)
    }
}

data class EducationListState(
    val content: List<EducationalContent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
