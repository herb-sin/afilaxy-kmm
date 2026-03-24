package com.afilaxy.presentation.professional

import com.rickclephas.kmm.viewmodel.KMMViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CrmResult(
    val name: String,
    val specialty: String,
    val situation: String,
    val uf: String,
    val crm: String,
)

sealed class CrmLookupState {
    object Idle : CrmLookupState()
    object Loading : CrmLookupState()
    data class Success(val result: CrmResult) : CrmLookupState()
    object NotFound : CrmLookupState()
    data class Error(val message: String) : CrmLookupState()
}

class CrmLookupViewModel : KMMViewModel() {
    private val _state = MutableStateFlow<CrmLookupState>(CrmLookupState.Idle)
    val state: StateFlow<CrmLookupState> = _state.asStateFlow()

    fun setState(state: CrmLookupState) {
        _state.value = state
    }

    fun reset() {
        _state.value = CrmLookupState.Idle
    }
}
