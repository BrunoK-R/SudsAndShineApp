package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminBookingPolicyConfig
import com.sudsmobile.data.admin.AdminBookingPolicyResult
import com.sudsmobile.data.admin.AdminBookingPolicyUpdateRequest
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class AdminBookingPolicyForm(
    val pendingHoldMinutes: String = "",
    val cancellationWindowMinutes: String = "",
    val rescheduleWindowMinutes: String = "",
    val paymentEligibilityCopy: String = "",
    val updatedAuditLabel: String = "",
)

internal sealed interface AdminBookingPolicyUiState {
    data object Idle : AdminBookingPolicyUiState
    data object Loading : AdminBookingPolicyUiState
    data object Unauthenticated : AdminBookingPolicyUiState
    data object NotAdmin : AdminBookingPolicyUiState
    data class Loaded(val form: AdminBookingPolicyForm) : AdminBookingPolicyUiState
    data class Error(val message: String, val retryable: Boolean) : AdminBookingPolicyUiState
}

internal sealed interface AdminBookingPolicySaveState {
    data object Idle : AdminBookingPolicySaveState
    data object Saving : AdminBookingPolicySaveState
    data class Success(val message: String) : AdminBookingPolicySaveState
    data class Error(val message: String, val retryable: Boolean) : AdminBookingPolicySaveState
}

internal class AdminBookingPolicyViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminBookingPolicyUiState>(AdminBookingPolicyUiState.Idle)
    val uiState: StateFlow<AdminBookingPolicyUiState> = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow<AdminBookingPolicySaveState>(AdminBookingPolicySaveState.Idle)
    val saveState: StateFlow<AdminBookingPolicySaveState> = _saveState.asStateFlow()
    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminBookingPolicyUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminBookingPolicyState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminBookingPolicyUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        if (!force && loadedUid == uid && _uiState.value is AdminBookingPolicyUiState.Loaded) return
        loadConfiguration()
    }

    fun loadConfiguration() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminBookingPolicyUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminBookingPolicyState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminBookingPolicyUiState.Unauthenticated
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
                _uiState.value = AdminBookingPolicyUiState.Loading
                val nextState = when (val result = adminRepository.getBookingPolicyConfiguration()) {
                    is AdminBookingPolicyResult.Success -> AdminBookingPolicyUiState.Loaded(result.config.toForm())
                    is AdminBookingPolicyResult.Failure -> result.error.toAdminBookingPolicyState()
                }
                if (requestSequence != loadSequence) return@launch

                val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                if (currentUid == requestedUid) {
                    loadedUid = requestedUid
                    _uiState.value = nextState
                } else {
                    handleSessionChangedDuringRequest()
                }
            } finally {
                if (requestSequence == loadSequence) {
                    loadingUid = null
                }
            }
        }
    }

    fun updateForm(form: AdminBookingPolicyForm) {
        if (_uiState.value is AdminBookingPolicyUiState.Loaded) {
            _uiState.value = AdminBookingPolicyUiState.Loaded(form)
            _saveState.value = AdminBookingPolicySaveState.Idle
        }
    }

    fun save() {
        if (_saveState.value == AdminBookingPolicySaveState.Saving) return
        val form = (_uiState.value as? AdminBookingPolicyUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminBookingPolicyUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toUpdateRequest()) {
            is ParsedBookingPolicyRequest.Invalid -> {
                _saveState.value = AdminBookingPolicySaveState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedBookingPolicyRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _saveState.value = AdminBookingPolicySaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                _saveState.value = AdminBookingPolicySaveState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            val result = adminRepository.updateBookingPolicyConfiguration(request)
            val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (latestUid != requestedUid) {
                _saveState.value = AdminBookingPolicySaveState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            when (result) {
                is AdminBookingPolicyResult.Success -> {
                    loadedUid = requestedUid
                    _uiState.value = AdminBookingPolicyUiState.Loaded(result.config.toForm())
                    _saveState.value = AdminBookingPolicySaveState.Success("Política guardada.")
                }
                is AdminBookingPolicyResult.Failure -> {
                    _saveState.value = result.error.toAdminBookingPolicySaveState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminBookingPolicyUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedConfig()
                        _uiState.value = AdminBookingPolicyUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = AdminBookingPolicySaveState.Idle
    }

    private fun clearLoadedConfig() {
        loadedUid = null
        loadingUid = null
        loadSequence += 1
    }

    private fun handleSessionChangedDuringRequest() {
        clearLoadedConfig()
        refreshForSession(force = true)
    }
}

private sealed interface ParsedBookingPolicyRequest {
    data class Valid(val request: AdminBookingPolicyUpdateRequest) : ParsedBookingPolicyRequest
    data class Invalid(val message: String) : ParsedBookingPolicyRequest
}

private fun AdminBookingPolicyConfig.toForm(): AdminBookingPolicyForm = AdminBookingPolicyForm(
    pendingHoldMinutes = pendingHoldMinutes.toString(),
    cancellationWindowMinutes = cancellationWindowMinutes.toString(),
    rescheduleWindowMinutes = rescheduleWindowMinutes.toString(),
    paymentEligibilityCopy = paymentEligibilityCopy,
    updatedAuditLabel = bookingPolicyAuditLabel(updatedAtIso, updatedByUid),
)

private fun bookingPolicyAuditLabel(timestampIso: String, actorUid: String): String {
    val timestampLabel = timestampIso.toBookingPolicyAuditDateTimeLabel() ?: return ""
    val actorLabel = actorUid.trim().takeIf { it.isNotBlank() }?.toShortBookingPolicyAuditUid()
        ?.let { " por $it" }
        .orEmpty()
    return "Atualizado $timestampLabel$actorLabel"
}

private fun String.toBookingPolicyAuditDateTimeLabel(): String? {
    val value = trim()
    if (value.isBlank()) return null
    val date = value.substringBefore("T", missingDelimiterValue = "")
    val time = value.substringAfter("T", missingDelimiterValue = "").take(5)
    if (date.length != 10 || time.length != 5) return value
    return "$date $time UTC"
}

private fun String.toShortBookingPolicyAuditUid(): String {
    val value = trim()
    return if (value.length <= 12) value else "${value.take(8)}..."
}

private fun AdminBookingPolicyForm.toUpdateRequest(): ParsedBookingPolicyRequest {
    val pendingHold = pendingHoldMinutes.trim().toIntOrNull()
        ?: return ParsedBookingPolicyRequest.Invalid("Indique a duração da reserva pendente em minutos.")
    val cancellationWindow = cancellationWindowMinutes.trim().toIntOrNull()
        ?: return ParsedBookingPolicyRequest.Invalid("Indique a antecedência de cancelamento em minutos.")
    val rescheduleWindow = rescheduleWindowMinutes.trim().toIntOrNull()
        ?: return ParsedBookingPolicyRequest.Invalid("Indique a antecedência de remarcação em minutos.")
    val copy = paymentEligibilityCopy.trim().replace(Regex("\\s+"), " ")

    return when {
        pendingHold !in 15..10080 ->
            ParsedBookingPolicyRequest.Invalid("A reserva pendente deve ficar ativa entre 15 minutos e 7 dias.")
        cancellationWindow !in 0..10080 ->
            ParsedBookingPolicyRequest.Invalid("A antecedência de cancelamento deve estar entre 0 minutos e 7 dias.")
        rescheduleWindow !in 0..10080 ->
            ParsedBookingPolicyRequest.Invalid("A antecedência de remarcação deve estar entre 0 minutos e 7 dias.")
        copy.isBlank() ->
            ParsedBookingPolicyRequest.Invalid("Indique a mensagem de pagamento.")
        copy.length > 500 ->
            ParsedBookingPolicyRequest.Invalid("A mensagem de pagamento deve ter no máximo 500 caracteres.")
        else -> ParsedBookingPolicyRequest.Valid(
            AdminBookingPolicyUpdateRequest(
                pendingHoldMinutes = pendingHold,
                cancellationWindowMinutes = cancellationWindow,
                rescheduleWindowMinutes = rescheduleWindow,
                paymentEligibilityCopy = copy,
            ),
        )
    }
}

private fun AdminError.toAdminBookingPolicyState(): AdminBookingPolicyUiState {
    return when (this) {
        is AdminError.Permission -> AdminBookingPolicyUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminBookingPolicyUiState.Unauthenticated
        is AdminError.Validation -> AdminBookingPolicyUiState.Error(message = message, retryable = false)
        is AdminError.Conflict -> AdminBookingPolicyUiState.Error(message = message, retryable = false)
        is AdminError.NotFound -> AdminBookingPolicyUiState.Error(message = message, retryable = true)
        is AdminError.Unavailable -> AdminBookingPolicyUiState.Error(message = message, retryable = true)
        is AdminError.Backend -> AdminBookingPolicyUiState.Error(message = message, retryable = true)
    }
}

private fun AdminError.toAdminBookingPolicySaveState(): AdminBookingPolicySaveState.Error {
    return AdminBookingPolicySaveState.Error(
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend,
    )
}

private fun AuthError.toAdminBookingPolicyState(): AdminBookingPolicyUiState.Error {
    return AdminBookingPolicyUiState.Error(
        message = message,
        retryable = true,
    )
}
