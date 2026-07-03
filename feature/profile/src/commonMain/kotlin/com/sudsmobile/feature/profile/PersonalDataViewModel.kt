package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.profile.UserProfile
import com.sudsmobile.data.profile.UserProfileError
import com.sudsmobile.data.profile.UserProfileMutationResult
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.profile.UserProfileResult
import com.sudsmobile.data.profile.UserProfileSaveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class PersonalDataFormUi(
    val displayName: String,
    val email: String,
    val phoneNumber: String,
    val marketingOptIn: Boolean,
    val appointmentReminderOptIn: Boolean = false,
    val photoUrl: String = "",
)

internal sealed interface PersonalDataUiState {
    data object Idle : PersonalDataUiState
    data object Loading : PersonalDataUiState
    data object Unauthenticated : PersonalDataUiState
    data class Loaded(val form: PersonalDataFormUi) : PersonalDataUiState
    data class Error(val message: String, val retryable: Boolean) : PersonalDataUiState
}

internal sealed interface PersonalDataSaveUiState {
    data object Idle : PersonalDataSaveUiState
    data object Saving : PersonalDataSaveUiState
    data class Saved(val message: String) : PersonalDataSaveUiState
    data class ValidationError(val message: String) : PersonalDataSaveUiState
    data class Error(val message: String, val retryable: Boolean) : PersonalDataSaveUiState
}

internal class PersonalDataViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: UserProfileRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState

    private val _uiState = MutableStateFlow<PersonalDataUiState>(PersonalDataUiState.Idle)
    val uiState: StateFlow<PersonalDataUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<PersonalDataSaveUiState>(PersonalDataSaveUiState.Idle)
    val saveState: StateFlow<PersonalDataSaveUiState> = _saveState.asStateFlow()

    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var profileRequestRevision: Long = 0

    fun refreshForSession() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = PersonalDataUiState.Loading
                _saveState.value = PersonalDataSaveUiState.Idle
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toPersonalDataState()
                _saveState.value = PersonalDataSaveUiState.Idle
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = PersonalDataUiState.Unauthenticated
                _saveState.value = PersonalDataSaveUiState.Idle
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        if (loadedUid == uid && _uiState.value is PersonalDataUiState.Loaded) return
        loadProfile()
    }

    fun loadProfile() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = PersonalDataUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toPersonalDataState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = PersonalDataUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }
        val requestedUid = session.session.user.uid
        if (_uiState.value is PersonalDataUiState.Loading && loadingUid == requestedUid) return

        val requestRevision = ++profileRequestRevision
        loadingUid = requestedUid

        viewModelScope.launch {
            _uiState.value = PersonalDataUiState.Loading
            _saveState.value = PersonalDataSaveUiState.Idle
            val nextState = when (val result = profileRepository.getMyProfile()) {
                is UserProfileResult.Success -> PersonalDataUiState.Loaded(result.profile.toFormUi())
                is UserProfileResult.Failure -> result.error.toUiState()
            }
            if (requestRevision != profileRequestRevision) return@launch
            loadingUid = null

            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    clearLoadedSession()
                    _uiState.value = PersonalDataUiState.Loading
                }
                is AuthSessionState.RestoreFailed -> {
                    clearLoadedSession()
                    _uiState.value = currentSessionState.error.toPersonalDataState()
                }
                AuthSessionState.Unauthenticated -> {
                    clearLoadedSession()
                    _uiState.value = PersonalDataUiState.Unauthenticated
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid == requestedUid) {
                        loadedUid = requestedUid
                        _uiState.value = nextState
                    } else {
                        clearLoadedSession()
                        _uiState.value = PersonalDataUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun saveProfile(form: PersonalDataFormUi) {
        if (_saveState.value is PersonalDataSaveUiState.Saving) return

        val validationError = validate(form)
        if (validationError != null) {
            _saveState.value = PersonalDataSaveUiState.ValidationError(validationError)
            return
        }

        val requestedUid = authenticatedUidOrUpdateState() ?: return

        viewModelScope.launch {
            _saveState.value = PersonalDataSaveUiState.Saving
            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    _saveState.value = restoringSaveState()
                    return@launch
                }
                is AuthSessionState.RestoreFailed -> {
                    _saveState.value = currentSessionState.error.toPersonalDataSaveState()
                    return@launch
                }
                AuthSessionState.Unauthenticated -> {
                    clearLoadedSession()
                    _uiState.value = PersonalDataUiState.Unauthenticated
                    _saveState.value = PersonalDataSaveUiState.Idle
                    return@launch
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid != requestedUid) {
                        _saveState.value = changedSessionSaveState()
                        return@launch
                    }
                }
            }

            val result = profileRepository.updateMyProfile(
                UserProfileSaveRequest(
                    displayName = form.displayName,
                    phoneNumber = form.phoneNumber,
                    marketingOptIn = form.marketingOptIn,
                    appointmentReminderOptIn = form.appointmentReminderOptIn,
                    photoUrl = form.photoUrl,
                ),
            )
            if ((sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid != requestedUid) {
                _saveState.value = changedSessionSaveState()
                return@launch
            }

            when (result) {
                is UserProfileMutationResult.Success -> {
                    loadedUid = requestedUid
                    _uiState.value = PersonalDataUiState.Loaded(result.profile.toFormUi())
                    _saveState.value = PersonalDataSaveUiState.Saved("Dados pessoais atualizados.")
                }
                is UserProfileMutationResult.Failure -> {
                    _saveState.value = result.error.toSaveState()
                }
            }
        }
    }

    private fun authenticatedUidOrUpdateState(): String? {
        return when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = PersonalDataUiState.Loading
                _saveState.value = PersonalDataSaveUiState.Idle
                null
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toPersonalDataState()
                _saveState.value = PersonalDataSaveUiState.Idle
                null
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = PersonalDataUiState.Unauthenticated
                _saveState.value = PersonalDataSaveUiState.Idle
                null
            }
            is AuthSessionState.Authenticated -> currentSessionState.session.user.uid
        }
    }

    private fun clearLoadedSession() {
        loadedUid = null
        loadingUid = null
        profileRequestRevision += 1
    }

    fun clearSaveState() {
        if (_saveState.value !is PersonalDataSaveUiState.Saving) {
            _saveState.value = PersonalDataSaveUiState.Idle
        }
    }

    private fun validate(form: PersonalDataFormUi): String? {
        val phone = form.phoneNumber.trim()
        return when {
            form.displayName.isBlank() -> "Indique o nome para guardar o perfil."
            phone.length < 6 -> "Indique um telemóvel válido."
            phone.length > 32 || !phone.all { it.isDigit() || it in phoneSeparators } ->
                "Indique um telemóvel válido."
            else -> null
        }
    }

    private fun UserProfileError.toUiState(): PersonalDataUiState {
        return when (this) {
            is UserProfileError.Unauthenticated -> PersonalDataUiState.Unauthenticated
            is UserProfileError.Permission,
            is UserProfileError.Validation -> PersonalDataUiState.Error(message = message, retryable = false)
            is UserProfileError.Unavailable,
            is UserProfileError.Backend -> PersonalDataUiState.Error(message = message, retryable = true)
        }
    }

    private fun UserProfileError.toSaveState(): PersonalDataSaveUiState {
        return when (this) {
            is UserProfileError.Validation -> PersonalDataSaveUiState.ValidationError(message)
            is UserProfileError.Unauthenticated -> PersonalDataSaveUiState.Error(message, retryable = false)
            is UserProfileError.Permission -> PersonalDataSaveUiState.Error(message, retryable = false)
            is UserProfileError.Unavailable,
            is UserProfileError.Backend -> PersonalDataSaveUiState.Error(message, retryable = true)
        }
    }
}

private fun restoringSaveState(): PersonalDataSaveUiState.Error {
    return PersonalDataSaveUiState.Error(
        message = "A sessão ainda está a ser validada. Tente novamente dentro de momentos.",
        retryable = true,
    )
}

private fun changedSessionSaveState(): PersonalDataSaveUiState.Error {
    return PersonalDataSaveUiState.Error(
        message = "A sessão mudou antes de guardarmos os dados. Atualize e tente novamente.",
        retryable = false,
    )
}

private val phoneSeparators = setOf('+', '-', '(', ')', '.', ' ')

private fun UserProfile.toFormUi(): PersonalDataFormUi = PersonalDataFormUi(
    displayName = displayName,
    email = email,
    phoneNumber = phoneNumber,
    marketingOptIn = marketingOptIn,
    appointmentReminderOptIn = appointmentReminderOptIn,
    photoUrl = photoUrl,
)

private fun AuthError.toPersonalDataState(): PersonalDataUiState.Error {
    return PersonalDataUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.toPersonalDataSaveState(): PersonalDataSaveUiState.Error {
    return PersonalDataSaveUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}
