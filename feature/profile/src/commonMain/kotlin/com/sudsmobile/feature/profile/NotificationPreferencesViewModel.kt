package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.notification.NotificationError
import com.sudsmobile.data.notification.NotificationPreferences
import com.sudsmobile.data.notification.NotificationPreferencesMutationResult
import com.sudsmobile.data.notification.NotificationPreferencesResult
import com.sudsmobile.data.notification.NotificationPreferencesUpdateRequest
import com.sudsmobile.data.notification.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class NotificationPreferencesForm(
    val bookingStatusEnabled: Boolean = true,
    val appointmentReminderEnabled: Boolean = true,
    val loyaltyEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
)

internal sealed interface NotificationPreferencesUiState {
    data object Idle : NotificationPreferencesUiState
    data object Loading : NotificationPreferencesUiState
    data object Unauthenticated : NotificationPreferencesUiState
    data class Loaded(val form: NotificationPreferencesForm) : NotificationPreferencesUiState
    data class Error(val message: String, val retryable: Boolean) : NotificationPreferencesUiState
}

internal sealed interface NotificationPreferencesSaveState {
    data object Idle : NotificationPreferencesSaveState
    data object Saving : NotificationPreferencesSaveState
    data class Success(val message: String) : NotificationPreferencesSaveState
    data class Error(val message: String, val retryable: Boolean) : NotificationPreferencesSaveState
}

internal class NotificationPreferencesViewModel(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<NotificationPreferencesUiState>(NotificationPreferencesUiState.Idle)
    val uiState: StateFlow<NotificationPreferencesUiState> = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow<NotificationPreferencesSaveState>(NotificationPreferencesSaveState.Idle)
    val saveState: StateFlow<NotificationPreferencesSaveState> = _saveState.asStateFlow()

    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedPreferences()
                _uiState.value = NotificationPreferencesUiState.Loading
                _saveState.value = NotificationPreferencesSaveState.Idle
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedPreferences()
                _uiState.value = currentSessionState.error.toNotificationPreferencesState()
                _saveState.value = NotificationPreferencesSaveState.Idle
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedPreferences()
                _uiState.value = NotificationPreferencesUiState.Unauthenticated
                _saveState.value = NotificationPreferencesSaveState.Idle
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        if (!force && loadedUid == uid && _uiState.value is NotificationPreferencesUiState.Loaded) return
        loadPreferences()
    }

    fun loadPreferences() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedPreferences()
                _uiState.value = NotificationPreferencesUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedPreferences()
                _uiState.value = currentSessionState.error.toNotificationPreferencesState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedPreferences()
                _uiState.value = NotificationPreferencesUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val requestedUid = session.session.user.uid
        if (loadingUid == requestedUid) return

        val requestSequence = ++loadSequence
        loadingUid = requestedUid
        viewModelScope.launch {
            try {
                _uiState.value = NotificationPreferencesUiState.Loading
                val nextState = when (val result = notificationRepository.getMyNotificationPreferences()) {
                    is NotificationPreferencesResult.Success ->
                        NotificationPreferencesUiState.Loaded(result.preferences.toForm())
                    is NotificationPreferencesResult.Failure -> result.error.toNotificationPreferencesState()
                }
                if (requestSequence != loadSequence) return@launch

                val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                if (currentUid == requestedUid) {
                    loadedUid = requestedUid
                    _uiState.value = nextState
                } else {
                    clearLoadedPreferences()
                    _uiState.value = NotificationPreferencesUiState.Unauthenticated
                }
            } finally {
                if (requestSequence == loadSequence) {
                    loadingUid = null
                }
            }
        }
    }

    fun updateForm(form: NotificationPreferencesForm) {
        if (_uiState.value is NotificationPreferencesUiState.Loaded) {
            _uiState.value = NotificationPreferencesUiState.Loaded(form)
            _saveState.value = NotificationPreferencesSaveState.Idle
        }
    }

    fun save() {
        if (_saveState.value == NotificationPreferencesSaveState.Saving) return
        val form = (_uiState.value as? NotificationPreferencesUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedPreferences()
            _uiState.value = NotificationPreferencesUiState.Unauthenticated
            _saveState.value = NotificationPreferencesSaveState.Idle
            return
        }

        viewModelScope.launch {
            _saveState.value = NotificationPreferencesSaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedPreferences()
                _uiState.value = NotificationPreferencesUiState.Unauthenticated
                _saveState.value = NotificationPreferencesSaveState.Idle
                return@launch
            }

            when (val result = notificationRepository.updateMyNotificationPreferences(form.toRequest())) {
                is NotificationPreferencesMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = requestedUid
                        _uiState.value = NotificationPreferencesUiState.Loaded(result.preferences.toForm())
                        _saveState.value =
                            NotificationPreferencesSaveState.Success("Preferências de notificações guardadas.")
                    } else {
                        clearLoadedPreferences()
                        _uiState.value = NotificationPreferencesUiState.Unauthenticated
                        _saveState.value = NotificationPreferencesSaveState.Idle
                    }
                }
                is NotificationPreferencesMutationResult.Failure -> {
                    _saveState.value = result.error.toNotificationPreferencesSaveState()
                    if (result.error is NotificationError.Unauthenticated) {
                        clearLoadedPreferences()
                        _uiState.value = NotificationPreferencesUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun clearSaveState() {
        if (_saveState.value !is NotificationPreferencesSaveState.Saving) {
            _saveState.value = NotificationPreferencesSaveState.Idle
        }
    }

    private fun clearLoadedPreferences() {
        loadedUid = null
        loadingUid = null
        loadSequence += 1
    }
}

private fun NotificationPreferences.toForm(): NotificationPreferencesForm = NotificationPreferencesForm(
    bookingStatusEnabled = bookingStatusEnabled,
    appointmentReminderEnabled = appointmentReminderEnabled,
    loyaltyEnabled = loyaltyEnabled,
    marketingEnabled = marketingEnabled,
)

private fun NotificationPreferencesForm.toRequest(): NotificationPreferencesUpdateRequest {
    return NotificationPreferencesUpdateRequest(
        bookingStatusEnabled = bookingStatusEnabled,
        appointmentReminderEnabled = appointmentReminderEnabled,
        loyaltyEnabled = loyaltyEnabled,
        marketingEnabled = marketingEnabled,
    )
}

private fun NotificationError.toNotificationPreferencesState(): NotificationPreferencesUiState {
    return when (this) {
        is NotificationError.Unauthenticated -> NotificationPreferencesUiState.Unauthenticated
        is NotificationError.Permission,
        is NotificationError.Validation -> NotificationPreferencesUiState.Error(message = message, retryable = false)
        is NotificationError.Unavailable,
        is NotificationError.Backend -> NotificationPreferencesUiState.Error(message = message, retryable = true)
    }
}

private fun NotificationError.toNotificationPreferencesSaveState(): NotificationPreferencesSaveState.Error {
    return when (this) {
        is NotificationError.Validation,
        is NotificationError.Permission,
        is NotificationError.Unauthenticated -> NotificationPreferencesSaveState.Error(message, retryable = false)
        is NotificationError.Unavailable,
        is NotificationError.Backend -> NotificationPreferencesSaveState.Error(message, retryable = true)
    }
}

private fun AuthError.toNotificationPreferencesState(): NotificationPreferencesUiState.Error {
    return NotificationPreferencesUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}
