package com.example.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, passwordRaw: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.login(email, passwordRaw)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email to receive a password reset link.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.ResetEmailSent("Password reset link sent to $email. Please check your inbox.")
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email.")
            }
        }
    }

    fun register(username: String, email: String, displayName: String, passwordRaw: String, avatarUrl: String? = null) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.register(username, email, displayName, passwordRaw, null, avatarUrl)
            if (result.isSuccess) {
                // Auto-login after register
                val loginResult = authRepository.login(email, passwordRaw)
                if (loginResult.isSuccess) {
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Registered successfully! Please login with your credentials.")
                }
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class ResetEmailSent(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
