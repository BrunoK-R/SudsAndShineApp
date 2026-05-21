package com.sudsmobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.profile.UserProfile
import com.sudsmobile.data.profile.UserProfileError
import com.sudsmobile.data.profile.UserProfileMutationResult
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.profile.UserProfileSaveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: AuthUser) : AuthUiState
    data object PasswordResetSent : AuthUiState
    data class RegistrationProfileError(
        val user: AuthUser,
        val message: String,
        val retryable: Boolean,
    ) : AuthUiState
    data class Error(val message: String, val retryable: Boolean) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: UserProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var registrationProfileSyncActive: Boolean = false
    private var pendingRegistrationProfile: PendingRegistrationProfile? = null

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
                        if (!registrationProfileSyncActive) {
                            _uiState.value = AuthUiState.Authenticated(sessionState.session.user)
                        }
                    }
                    is AuthSessionState.RestoreFailed -> {
                        if (_uiState.value == AuthUiState.Loading || _uiState.value == AuthUiState.Idle) {
                            _uiState.value = sessionState.error.toUiState()
                        }
                    }
                    AuthSessionState.Unauthenticated -> {
                        if (!registrationProfileSyncActive) {
                            pendingRegistrationProfile = null
                            if (_uiState.value == AuthUiState.Loading ||
                                _uiState.value is AuthUiState.Authenticated ||
                                _uiState.value is AuthUiState.RegistrationProfileError
                            ) {
                                _uiState.value = AuthUiState.Idle
                            }
                        }
                    }
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (_uiState.value is AuthUiState.Loading) return

        registrationProfileSyncActive = false
        pendingRegistrationProfile = null
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

        registrationProfileSyncActive = true
        pendingRegistrationProfile = null
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (
                val result = authRepository.register(
                    displayName = displayName,
                    email = email,
                    phoneNumber = phoneNumber,
                    password = password,
                )
            ) {
                is AuthResult.Success -> {
                    val pendingProfile = PendingRegistrationProfile(
                        user = result.session.user,
                        displayName = displayName.trim(),
                        phoneNumber = phoneNumber.trim(),
                    )
                    pendingRegistrationProfile = pendingProfile
                    saveRegistrationProfile(pendingProfile)
                }
                is AuthResult.Failure -> {
                    registrationProfileSyncActive = false
                    _uiState.value = result.error.toUiState()
                }
            }
        }
    }

    fun retryRegistrationProfileSave() {
        if (_uiState.value is AuthUiState.Loading) return

        val pendingProfile = pendingRegistrationProfile ?: return
        viewModelScope.launch {
            saveRegistrationProfile(pendingProfile)
        }
    }

    fun continueAfterRegistrationProfileError() {
        val user = pendingRegistrationProfile?.user
            ?: (_uiState.value as? AuthUiState.RegistrationProfileError)?.user
            ?: return
        pendingRegistrationProfile = null
        registrationProfileSyncActive = false
        _uiState.value = AuthUiState.Authenticated(user)
    }

    fun sendPasswordReset(email: String) {
        if (_uiState.value is AuthUiState.Loading) return

        registrationProfileSyncActive = false
        pendingRegistrationProfile = null
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = authRepository.sendPasswordReset(email)) {
                AuthActionResult.Success -> AuthUiState.PasswordResetSent
                is AuthActionResult.Failure -> result.error.toUiState()
            }
        }
    }

    fun clearTransientState() {
        if (_uiState.value !is AuthUiState.Loading &&
            _uiState.value !is AuthUiState.RegistrationProfileError
        ) {
            _uiState.value = AuthUiState.Idle
        }
    }

    private suspend fun saveRegistrationProfile(pendingProfile: PendingRegistrationProfile) {
        _uiState.value = AuthUiState.Loading
        when (
            val result = profileRepository.updateMyProfile(
                UserProfileSaveRequest(
                    displayName = pendingProfile.displayName,
                    phoneNumber = pendingProfile.phoneNumber,
                    marketingOptIn = false,
                    appointmentReminderOptIn = false,
                ),
            )
        ) {
            is UserProfileMutationResult.Success -> {
                pendingRegistrationProfile = null
                registrationProfileSyncActive = false
                _uiState.value = AuthUiState.Authenticated(
                    pendingProfile.user.withSavedProfile(result.profile),
                )
            }
            is UserProfileMutationResult.Failure -> {
                _uiState.value = AuthUiState.RegistrationProfileError(
                    user = pendingProfile.user,
                    message = result.error.message,
                    retryable = result.error.isRetryable(),
                )
            }
        }
    }

    private fun AuthError.toUiState(): AuthUiState.Error {
        val retryable = this is AuthError.Unavailable ||
            this is AuthError.Backend
        return AuthUiState.Error(message = message, retryable = retryable)
    }

    private fun UserProfileError.isRetryable(): Boolean {
        return this is UserProfileError.Unavailable ||
            this is UserProfileError.Backend
    }
}

private data class PendingRegistrationProfile(
    val user: AuthUser,
    val displayName: String,
    val phoneNumber: String,
)

private fun AuthUser.withSavedProfile(profile: UserProfile): AuthUser = copy(
    displayName = profile.displayName.ifBlank { displayName },
    phoneNumber = profile.phoneNumber.ifBlank { phoneNumber },
)
