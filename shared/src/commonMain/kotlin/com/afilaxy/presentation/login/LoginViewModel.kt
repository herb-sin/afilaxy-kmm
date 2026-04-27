package com.afilaxy.presentation.login

import com.afilaxy.domain.repository.AuthRepository
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel compartilhado para Login
 * Funciona em Android e iOS!
 */
class LoginViewModel(
    private val authRepository: AuthRepository
) : KMMViewModel() {

    private companion object {
        const val ERROR_MESSAGE_MAX_LENGTH = 200
    }

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, error = null) }
    }
    
    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }
    
    fun onLoginClick() {
        val currentState = _state.value
        
        // Validação básica
        if (currentState.email.isBlank()) {
            _state.update { it.copy(error = "Email não pode estar vazio") }
            return
        }
        
        if (currentState.password.isBlank()) {
            _state.update { it.copy(error = "Senha não pode estar vazia") }
            return
        }
        
        // Chamar repositório
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            authRepository.login(currentState.email, currentState.password)
                .onSuccess { user ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            // Sanitiza mensagem — exception.message pode conter dados externos (CWE-117)
                            error = exception.message
                                ?.replace(Regex("[\\r\\n\\t\\x00-\\x1F]"), " ")
                                ?.take(ERROR_MESSAGE_MAX_LENGTH)
                                ?: "Erro ao fazer login"
                        )
                    }
                }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun currentState(): LoginState = _state.value
}
