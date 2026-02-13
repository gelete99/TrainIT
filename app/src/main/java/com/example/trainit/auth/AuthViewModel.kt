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

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val s = _uiState.value
            val result = repo.login(s.email.trim(), s.password)

            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Error al iniciar sesión") }
                }
        }
    }

    fun register(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val s = _uiState.value
            val result = repo.register(s.email.trim(), s.password)

            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { onSuccess() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Error al registrarse") }
                }
        }
    }
}
