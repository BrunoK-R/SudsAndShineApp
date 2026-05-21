package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun refreshForSession() {
        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            _uiState.value = PersonalDataUiState.Unauthenticated
            _saveState.value = PersonalDataSaveUiState.Idle
            return
        }

        val uid = session.session.user.uid
        if (loadedUid == uid && _uiState.value is PersonalDataUiState.Loaded) return
        loadProfile()
    }

    fun loadProfile() {
        if (_uiState.value is PersonalDataUiState.Loading) return

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            _uiState.value = PersonalDataUiState.Unauthenticated
            return
        }
        val requestedUid = session.session.user.uid

        viewModelScope.launch {
            _uiState.value = PersonalDataUiState.Loading
            _saveState.value = PersonalDataSaveUiState.Idle
            val nextState = when (val result = profileRepository.getMyProfile()) {
                is UserProfileResult.Success -> PersonalDataUiState.Loaded(result.profile.toFormUi())
                is UserProfileResult.Failure -> result.error.toUiState()
            }
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedUid = requestedUid
                _uiState.value = nextState
            } else {
                loadedUid = null
                _uiState.value = PersonalDataUiState.Unauthenticated
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

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            _uiState.value = PersonalDataUiState.Unauthenticated
            _saveState.value = PersonalDataSaveUiState.Idle
            return
        }
        val requestedUid = session.session.user.uid

        viewModelScope.launch {
            _saveState.value = PersonalDataSaveUiState.Saving
            val result = profileRepository.updateMyProfile(
                UserProfileSaveRequest(
                    displayName = form.displayName,
                    phoneNumber = form.phoneNumber,
                    marketingOptIn = form.marketingOptIn,
                    appointmentReminderOptIn = form.appointmentReminderOptIn,
                ),
            )
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                loadedUid = null
                _uiState.value = PersonalDataUiState.Unauthenticated
                _saveState.value = PersonalDataSaveUiState.Idle
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

private val phoneSeparators = setOf('+', '-', '(', ')', '.', ' ')

private fun UserProfile.toFormUi(): PersonalDataFormUi = PersonalDataFormUi(
    displayName = displayName,
    email = email,
    phoneNumber = phoneNumber,
    marketingOptIn = marketingOptIn,
    appointmentReminderOptIn = appointmentReminderOptIn,
)
