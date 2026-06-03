package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminBusinessInfoConfig
import com.sudsmobile.data.admin.AdminBusinessInfoResult
import com.sudsmobile.data.admin.AdminBusinessInfoUpdateRequest
import com.sudsmobile.data.admin.AdminBusinessOpeningHours
import com.sudsmobile.data.admin.AdminBusinessSocialLink
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class AdminBusinessInfoForm(
    val phone: String = "",
    val email: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val mapsUri: String = "",
    val whatsappUri: String = "",
    val openingHoursText: String = "",
    val socialLinksText: String = "",
    val updatedAuditLabel: String = "",
)

internal sealed interface AdminBusinessInfoUiState {
    data object Idle : AdminBusinessInfoUiState
    data object Loading : AdminBusinessInfoUiState
    data object Unauthenticated : AdminBusinessInfoUiState
    data object NotAdmin : AdminBusinessInfoUiState
    data class Loaded(val form: AdminBusinessInfoForm) : AdminBusinessInfoUiState
    data class Error(val message: String, val retryable: Boolean) : AdminBusinessInfoUiState
}

internal sealed interface AdminBusinessInfoSaveState {
    data object Idle : AdminBusinessInfoSaveState
    data object Saving : AdminBusinessInfoSaveState
    data class Success(val message: String) : AdminBusinessInfoSaveState
    data class Error(val message: String, val retryable: Boolean) : AdminBusinessInfoSaveState
}

internal class AdminBusinessInfoViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminBusinessInfoUiState>(AdminBusinessInfoUiState.Idle)
    val uiState: StateFlow<AdminBusinessInfoUiState> = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow<AdminBusinessInfoSaveState>(AdminBusinessInfoSaveState.Idle)
    val saveState: StateFlow<AdminBusinessInfoSaveState> = _saveState.asStateFlow()
    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminBusinessInfoUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminBusinessInfoState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminBusinessInfoUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        if (!force && loadedUid == uid && _uiState.value is AdminBusinessInfoUiState.Loaded) return
        loadConfiguration()
    }

    fun loadConfiguration() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminBusinessInfoUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminBusinessInfoState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminBusinessInfoUiState.Unauthenticated
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
                _uiState.value = AdminBusinessInfoUiState.Loading
                val nextState = when (val result = adminRepository.getBusinessInfoConfiguration()) {
                    is AdminBusinessInfoResult.Success -> AdminBusinessInfoUiState.Loaded(result.config.toForm())
                    is AdminBusinessInfoResult.Failure -> result.error.toAdminBusinessInfoState()
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

    fun updateForm(form: AdminBusinessInfoForm) {
        if (_uiState.value is AdminBusinessInfoUiState.Loaded) {
            _uiState.value = AdminBusinessInfoUiState.Loaded(form)
            _saveState.value = AdminBusinessInfoSaveState.Idle
        }
    }

    fun save() {
        if (_saveState.value == AdminBusinessInfoSaveState.Saving) return
        val form = (_uiState.value as? AdminBusinessInfoUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminBusinessInfoUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toUpdateRequest()) {
            is ParsedBusinessInfoRequest.Invalid -> {
                _saveState.value = AdminBusinessInfoSaveState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedBusinessInfoRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _saveState.value = AdminBusinessInfoSaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                _saveState.value = AdminBusinessInfoSaveState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            val result = adminRepository.updateBusinessInfoConfiguration(request)
            val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (latestUid != requestedUid) {
                _saveState.value = AdminBusinessInfoSaveState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            when (result) {
                is AdminBusinessInfoResult.Success -> {
                    loadedUid = requestedUid
                    _uiState.value = AdminBusinessInfoUiState.Loaded(result.config.toForm())
                    _saveState.value = AdminBusinessInfoSaveState.Success("Configuração guardada.")
                }
                is AdminBusinessInfoResult.Failure -> {
                    _saveState.value = result.error.toAdminBusinessInfoSaveState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminBusinessInfoUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedConfig()
                        _uiState.value = AdminBusinessInfoUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = AdminBusinessInfoSaveState.Idle
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

private sealed interface ParsedBusinessInfoRequest {
    data class Valid(val request: AdminBusinessInfoUpdateRequest) : ParsedBusinessInfoRequest
    data class Invalid(val message: String) : ParsedBusinessInfoRequest
}

private fun AdminBusinessInfoConfig.toForm(): AdminBusinessInfoForm = AdminBusinessInfoForm(
    phone = phone,
    email = email,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    mapsUri = mapsUri,
    whatsappUri = whatsappUri,
    openingHoursText = openingHours.joinToString("\n") { hours ->
        listOf(
            hours.dayLabel,
            hours.hoursLabel,
            if (hours.closed) "fechado" else "",
        ).filter { it.isNotBlank() }.joinToString(" | ")
    },
    socialLinksText = socialLinks.joinToString("\n") { link ->
        "${link.label} | ${link.uri}"
    },
    updatedAuditLabel = businessInfoAuditLabel(updatedAtIso, updatedByUid),
)

private fun businessInfoAuditLabel(timestampIso: String, actorUid: String): String {
    val timestampLabel = timestampIso.toBusinessInfoAuditDateTimeLabel() ?: return ""
    val actorLabel = actorUid.trim().takeIf { it.isNotBlank() }?.toShortBusinessInfoAuditUid()
        ?.let { " por $it" }
        .orEmpty()
    return "Atualizado $timestampLabel$actorLabel"
}

private fun String.toBusinessInfoAuditDateTimeLabel(): String? {
    val value = trim()
    if (value.isBlank()) return null
    val date = value.substringBefore("T", missingDelimiterValue = "")
    val time = value.substringAfter("T", missingDelimiterValue = "").take(5)
    if (date.length != 10 || time.length != 5) return value
    return "$date $time UTC"
}

private fun String.toShortBusinessInfoAuditUid(): String {
    val value = trim()
    return if (value.length <= 12) value else "${value.take(8)}..."
}

private fun AdminBusinessInfoForm.toUpdateRequest(): ParsedBusinessInfoRequest {
    val openingHours = openingHoursText.parseOpeningHours()
        ?: return ParsedBusinessInfoRequest.Invalid("Revise os horários.")
    val socialLinks = socialLinksText.parseSocialLinks()
        ?: return ParsedBusinessInfoRequest.Invalid("Revise as redes sociais.")

    return ParsedBusinessInfoRequest.Valid(
        AdminBusinessInfoUpdateRequest(
            phone = phone,
            email = email,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2,
            mapsUri = mapsUri,
            whatsappUri = whatsappUri,
            openingHours = openingHours,
            socialLinks = socialLinks,
        ),
    )
}

private fun String.parseOpeningHours(): List<AdminBusinessOpeningHours>? {
    val rows = mutableListOf<AdminBusinessOpeningHours>()
    for (line in lineSequence().map { it.trim() }.filter { it.isNotBlank() }) {
        val parts = line.split("|").map { it.trim() }
        if (parts.size < 2) return null
        rows += AdminBusinessOpeningHours(
                dayLabel = parts[0],
                hoursLabel = parts[1],
                closed = parts.drop(2).any {
                    it.equals("fechado", ignoreCase = true) || it.equals("closed", ignoreCase = true)
                },
        )
    }
    return rows.takeIf { it.isNotEmpty() && it.size <= 10 }
}

private fun String.parseSocialLinks(): List<AdminBusinessSocialLink>? {
    val rows = mutableListOf<AdminBusinessSocialLink>()
    for (line in lineSequence().map { it.trim() }.filter { it.isNotBlank() }) {
        val parts = line.split("|", limit = 2).map { it.trim() }
        if (parts.size != 2) return null
        rows += AdminBusinessSocialLink(label = parts[0], uri = parts[1])
    }
    return rows.takeIf { it.size <= 8 }
}

private fun AdminError.toAdminBusinessInfoState(): AdminBusinessInfoUiState {
    return when (this) {
        is AdminError.Permission -> AdminBusinessInfoUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminBusinessInfoUiState.Unauthenticated
        is AdminError.Unavailable,
        is AdminError.Backend -> AdminBusinessInfoUiState.Error(message = message, retryable = true)
        is AdminError.Validation,
        is AdminError.NotFound,
        is AdminError.Conflict -> AdminBusinessInfoUiState.Error(message = message, retryable = false)
    }
}

private fun AdminError.toAdminBusinessInfoSaveState(): AdminBusinessInfoSaveState.Error {
    return AdminBusinessInfoSaveState.Error(
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend,
    )
}

private fun AuthError.toAdminBusinessInfoState(): AdminBusinessInfoUiState.Error {
    return AdminBusinessInfoUiState.Error(
        message = message,
        retryable = this is AuthError.Unavailable || this is AuthError.Backend,
    )
}
