package com.example.trainit.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val username = state.username.trim()
            val password = state.password

            if (username.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Introduce tu nombre de usuario") }
                return@launch
            }
            if (password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Introduce tu contraseña") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repo.login(username, password)
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Error al iniciar sesión") }
                }
        }
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val username = state.username.trim()
            val email = state.email.trim()
            val password = state.password
            val confirm = state.confirmPassword

            if (username.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Introduce un nombre de usuario") }
                return@launch
            }
            if (email.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Introduce un email") }
                return@launch
            }
            if (password.length < 6) {
                _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres") }
                return@launch
            }
            if (password != confirm) {
                _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repo.register(username, email, password)
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Error al registrarse") }
                }
        }
    }
}
