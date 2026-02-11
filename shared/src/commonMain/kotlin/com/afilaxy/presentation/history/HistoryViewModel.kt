package com.afilaxy.presentation.history

import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.repository.EmergencyRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val emergencyRepository: EmergencyRepository,
    private val authRepository: AuthRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()
    
    init {
        loadHistory()
    }
    
    fun loadHistory() {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _state.update { it.copy(isLoading = false, error = "Usuário não autenticado") }
                return@launch
            }
            
            emergencyRepository.getUserEmergencyHistory(currentUser.uid)
                .onSuccess { history ->
                    _state.update { 
                        it.copy(
                            history = history,
                            filteredHistory = history,
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Erro ao carregar histórico"
                        )
                    }
                }
        }
    }
    
    fun applyFilter(filter: HistoryFilter) {
        viewModelScope.coroutineScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) return@launch
            
            val allHistory = _state.value.history
            
            val filtered = when (filter) {
                HistoryFilter.ALL -> allHistory
                HistoryFilter.RESOLVED -> allHistory.filter { it.status == "resolved" }
                HistoryFilter.CANCELLED -> allHistory.filter { it.status == "cancelled" }
                HistoryFilter.AS_REQUESTER -> allHistory.filter { it.requesterId == currentUser.uid }
                HistoryFilter.AS_HELPER -> allHistory.filter { it.helperId == currentUser.uid }
            }
            
            _state.update { 
                it.copy(
                    selectedFilter = filter,
                    filteredHistory = filtered
                )
            }
        }
    }
}
