package com.sudsmobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: AuthUser) : AuthUiState
    data object PasswordResetSent : AuthUiState
    data class Error(val message: String, val retryable: Boolean) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collect { sessionState ->
                when (sessionState) {
                    AuthSessionState.Restoring -> {
                        if (_uiState.value == AuthUiState.Idle) {
                            _uiState.value = AuthUiState.Loading
                        }
                    }
                    is AuthSessionState.Authenticated -> {
                        _uiState.value = AuthUiState.Authenticated(sessionState.session.user)
                    }
                    is AuthSessionState.RestoreFailed -> {
                        if (_uiState.value == AuthUiState.Loading || _uiState.value == AuthUiState.Idle) {
                            _uiState.value = sessionState.error.toUiState()
                        }
                    }
                    AuthSessionState.Unauthenticated -> {
                        if (_uiState.value == AuthUiState.Loading ||
                            _uiState.value is AuthUiState.Authenticated
                        ) {
                            _uiState.value = AuthUiState.Idle
                        }
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (_uiState.value is AuthUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> AuthUiState.Authenticated(result.session.user)
                is AuthResult.Failure -> result.error.toUiState()
            }
        }
    }

    fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
        acceptsTerms: Boolean,
    ) {
        if (_uiState.value is AuthUiState.Loading) return

        if (!acceptsTerms) {
            _uiState.value = AuthUiState.Error(
                message = "Aceite a política de privacidade para continuar.",
                retryable = false,
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (
                val result = authRepository.register(
                    displayName = displayName,
                    email = email,
                    phoneNumber = phoneNumber,
                    password = password,
                )
            ) {
                is AuthResult.Success -> AuthUiState.Authenticated(result.session.user)
                is AuthResult.Failure -> result.error.toUiState()
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (_uiState.value is AuthUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = authRepository.sendPasswordReset(email)) {
                AuthActionResult.Success -> AuthUiState.PasswordResetSent
                is AuthActionResult.Failure -> result.error.toUiState()
            }
        }
    }

    fun clearTransientState() {
        if (_uiState.value !is AuthUiState.Loading) {
            _uiState.value = AuthUiState.Idle
        }
    }

    private fun AuthError.toUiState(): AuthUiState.Error {
        val retryable = this is AuthError.Unavailable ||
            this is AuthError.Backend
        return AuthUiState.Error(message = message, retryable = retryable)
    }
}
