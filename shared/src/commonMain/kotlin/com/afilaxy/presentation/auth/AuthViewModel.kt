package com.afilaxy.presentation.auth

import com.afilaxy.domain.model.User
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.domain.validation.Validator
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for authentication
 */
data class AuthState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

/**
 * Shared ViewModel for Authentication
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : KMMViewModel() {
    
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()
    
    init {
        checkAuthState()
        observeAuthState()
    }
    
    /**
     * Check current authentication state on init
     */
    fun checkAuthState() {
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val currentUser = authRepository.getCurrentUser()
            _state.update {
                it.copy(
                    user = currentUser,
                    isAuthenticated = currentUser != null,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Observe auth state changes
     */
    private fun observeAuthState() {
        viewModelScope.coroutineScope.launch {
            authRepository.observeAuthState().collect { user ->
                _state.update {
                    it.copy(
                        user = user,
                        isAuthenticated = user != null
                    )
                }
            }
        }
    }
    
    /**
     * Login with email and password
     */
    fun onLogin(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Email e senha são obrigatórios") }
            return
        }
        
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            authRepository.login(email, password)
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            user = user,
                            isAuthenticated = true,
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = getErrorMessage(exception)
                        )
                    }
                }
        }
    }
    
    /**
     * Register new user
     */
    fun onRegister(email: String, password: String, name: String) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _state.update { it.copy(error = "Todos os campos são obrigatórios") }
            return
        }
        
        if (!Validator.isValidPassword(password)) {
            _state.update { it.copy(error = "A senha deve ter no mínimo 8 caracteres, 1 letra maiúscula e 1 número") }
            return
        }
        
        viewModelScope.coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            authRepository.register(email, password, name)
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            user = user,
                            isAuthenticated = true,
                            isLoading = false
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = getErrorMessage(exception)
                        )
                    }
                }
        }
    }
    
    /**
     * Logout current user
     */
    fun onLogout() {
        viewModelScope.coroutineScope.launch {
            authRepository.logout()
            _state.update {
                it.copy(
                    user = null,
                    isAuthenticated = false
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * Convert exception to user-friendly error message
     */
    private fun getErrorMessage(exception: Throwable): String {
        return when {
            exception.message?.contains("INVALID_LOGIN_CREDENTIALS") == true -> 
                "Email ou senha incorretos"
            exception.message?.contains("EMAIL_EXISTS") == true -> 
                "Este email já está cadastrado"
            exception.message?.contains("WEAK_PASSWORD") == true -> 
                "Senha muito fraca. Use no mínimo 8 caracteres"
            exception.message?.contains("INVALID_EMAIL") == true -> 
                "Email inválido"
            exception.message?.contains("network") == true -> 
                "Erro de conexão. Verifique sua internet"
            else -> exception.message ?: "Erro desconhecido"
        }
    }
}
