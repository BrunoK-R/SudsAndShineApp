package com.sudsmobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: AuthUser) : AuthUiState
    data class Error(val message: String, val retryable: Boolean) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var providerSignInInProgress: Boolean = false

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
                            if (!providerSignInInProgress) {
                                _uiState.value = AuthUiState.Idle
                            }
                        }
                    }
                }
            }
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        if (_uiState.value is AuthUiState.Loading) return

        providerSignInInProgress = true
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                _uiState.value = when (val result = authRepository.signInWithGoogleIdToken(idToken)) {
                    is AuthResult.Success -> AuthUiState.Authenticated(result.session.user)
                    is AuthResult.Failure -> result.error.toUiState()
                }
            } finally {
                providerSignInInProgress = false
            }
        }
    }

    fun showGoogleSignInError(message: String) {
        _uiState.update { current ->
            if (current is AuthUiState.Loading) {
                current
            } else {
                AuthUiState.Error(message = message, retryable = true)
            }
        }
    }

    fun clearTransientState() {
        _uiState.update { current ->
            if (current is AuthUiState.Loading) current else AuthUiState.Idle
        }
    }

    private fun AuthError.toUiState(): AuthUiState.Error {
        val retryable = this is AuthError.Unavailable ||
            this is AuthError.Backend
        return AuthUiState.Error(message = message, retryable = retryable)
    }
}
