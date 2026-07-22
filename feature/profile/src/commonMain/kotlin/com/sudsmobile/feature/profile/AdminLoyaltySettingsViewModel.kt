package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminLoyaltySettingsConfig
import com.sudsmobile.data.admin.AdminLoyaltySettingsResult
import com.sudsmobile.data.admin.AdminLoyaltySettingsUpdateRequest
import com.sudsmobile.data.admin.AdminLoyaltyReport
import com.sudsmobile.data.admin.AdminLoyaltyReportResult
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

internal data class AdminLoyaltySettingsForm(
    val stampsRequired: String = "",
    val rewardType: String = "free_wash",
    val rewardValue: String = "",
    val rewardDescription: String = "",
    val updatedAuditLabel: String = "",
)

internal sealed interface AdminLoyaltySettingsUiState {
    data object Idle : AdminLoyaltySettingsUiState
    data object Loading : AdminLoyaltySettingsUiState
    data object Unauthenticated : AdminLoyaltySettingsUiState
    data object NotAdmin : AdminLoyaltySettingsUiState
    data class Loaded(val form: AdminLoyaltySettingsForm) : AdminLoyaltySettingsUiState
    data class Error(val message: String, val retryable: Boolean) : AdminLoyaltySettingsUiState
}

internal sealed interface AdminLoyaltySettingsSaveState {
    data object Idle : AdminLoyaltySettingsSaveState
    data object Saving : AdminLoyaltySettingsSaveState
    data class Success(val message: String) : AdminLoyaltySettingsSaveState
    data class Error(val message: String, val retryable: Boolean) : AdminLoyaltySettingsSaveState
}

internal sealed interface AdminLoyaltyReportUiState {
    data object Idle : AdminLoyaltyReportUiState
    data object Loading : AdminLoyaltyReportUiState
    data object Unauthenticated : AdminLoyaltyReportUiState
    data object NotAdmin : AdminLoyaltyReportUiState
    data class Loaded(val report: AdminLoyaltyReport) : AdminLoyaltyReportUiState
    data class Error(val message: String, val retryable: Boolean) : AdminLoyaltyReportUiState
}

internal class AdminLoyaltySettingsViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminLoyaltySettingsUiState>(AdminLoyaltySettingsUiState.Idle)
    val uiState: StateFlow<AdminLoyaltySettingsUiState> = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow<AdminLoyaltySettingsSaveState>(AdminLoyaltySettingsSaveState.Idle)
    val saveState: StateFlow<AdminLoyaltySettingsSaveState> = _saveState.asStateFlow()
    private val _reportState = MutableStateFlow<AdminLoyaltyReportUiState>(AdminLoyaltyReportUiState.Idle)
    val reportState: StateFlow<AdminLoyaltyReportUiState> = _reportState.asStateFlow()
    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminLoyaltySettingsUiState.Loading
                _reportState.value = AdminLoyaltyReportUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminLoyaltySettingsState()
                _reportState.value = AdminLoyaltyReportUiState.Error(
                    message = currentSessionState.error.message,
                    retryable = currentSessionState.error.isRetryable(),
                )
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminLoyaltySettingsUiState.Unauthenticated
                _reportState.value = AdminLoyaltyReportUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        if (!force && loadedUid == uid && _uiState.value is AdminLoyaltySettingsUiState.Loaded) return
        loadConfiguration()
    }

    fun loadConfiguration() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminLoyaltySettingsUiState.Loading
                _reportState.value = AdminLoyaltyReportUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminLoyaltySettingsState()
                _reportState.value = AdminLoyaltyReportUiState.Error(
                    message = currentSessionState.error.message,
                    retryable = currentSessionState.error.isRetryable(),
                )
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminLoyaltySettingsUiState.Unauthenticated
                _reportState.value = AdminLoyaltyReportUiState.Unauthenticated
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
                _uiState.value = AdminLoyaltySettingsUiState.Loading
                _reportState.value = AdminLoyaltyReportUiState.Loading
                val settingsRequest = async { adminRepository.getLoyaltySettingsConfiguration() }
                val reportRequest = async { adminRepository.getLoyaltyReport() }
                val nextState = when (val result = settingsRequest.await()) {
                    is AdminLoyaltySettingsResult.Success -> AdminLoyaltySettingsUiState.Loaded(result.config.toForm())
                    is AdminLoyaltySettingsResult.Failure -> result.error.toAdminLoyaltySettingsState()
                }
                val nextReportState = when (val result = reportRequest.await()) {
                    is AdminLoyaltyReportResult.Success -> AdminLoyaltyReportUiState.Loaded(result.report)
                    is AdminLoyaltyReportResult.Failure -> result.error.toAdminLoyaltyReportState()
                }
                if (requestSequence != loadSequence) return@launch

                val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                if (currentUid == requestedUid) {
                    loadedUid = requestedUid
                    _uiState.value = nextState
                    _reportState.value = nextReportState
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

    fun updateForm(form: AdminLoyaltySettingsForm) {
        if (_uiState.value is AdminLoyaltySettingsUiState.Loaded) {
            _uiState.value = AdminLoyaltySettingsUiState.Loaded(form)
            _saveState.value = AdminLoyaltySettingsSaveState.Idle
        }
    }

    fun save() {
        if (_saveState.value == AdminLoyaltySettingsSaveState.Saving) return
        val form = (_uiState.value as? AdminLoyaltySettingsUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminLoyaltySettingsUiState.Unauthenticated
            _reportState.value = AdminLoyaltyReportUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toUpdateRequest()) {
            is ParsedLoyaltySettingsRequest.Invalid -> {
                _saveState.value = AdminLoyaltySettingsSaveState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedLoyaltySettingsRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _saveState.value = AdminLoyaltySettingsSaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                _saveState.value = AdminLoyaltySettingsSaveState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            val result = adminRepository.updateLoyaltySettingsConfiguration(request)
            val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (latestUid != requestedUid) {
                _saveState.value = AdminLoyaltySettingsSaveState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            when (result) {
                is AdminLoyaltySettingsResult.Success -> {
                    loadedUid = requestedUid
                    _uiState.value = AdminLoyaltySettingsUiState.Loaded(result.config.toForm())
                    _saveState.value = AdminLoyaltySettingsSaveState.Success("Fidelização guardada.")
                }
                is AdminLoyaltySettingsResult.Failure -> {
                    _saveState.value = result.error.toAdminLoyaltySettingsSaveState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminLoyaltySettingsUiState.NotAdmin
                        _reportState.value = AdminLoyaltyReportUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedConfig()
                        _uiState.value = AdminLoyaltySettingsUiState.Unauthenticated
                        _reportState.value = AdminLoyaltyReportUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = AdminLoyaltySettingsSaveState.Idle
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

private sealed interface ParsedLoyaltySettingsRequest {
    data class Valid(val request: AdminLoyaltySettingsUpdateRequest) : ParsedLoyaltySettingsRequest
    data class Invalid(val message: String) : ParsedLoyaltySettingsRequest
}

private fun AdminLoyaltySettingsConfig.toForm(): AdminLoyaltySettingsForm = AdminLoyaltySettingsForm(
    stampsRequired = stampsRequired.toString(),
    rewardType = rewardType.normalizeRewardType(),
    rewardValue = rewardValue.toString(),
    rewardDescription = rewardDescription,
    updatedAuditLabel = loyaltySettingsAuditLabel(updatedAtIso, updatedByUid),
)

private fun AdminLoyaltySettingsForm.toUpdateRequest(): ParsedLoyaltySettingsRequest {
    val stamps = stampsRequired.trim().toIntOrNull()
        ?: return ParsedLoyaltySettingsRequest.Invalid("Indique o número de lavagens necessárias.")
    val type = rewardType.normalizeRewardType()
    val value = rewardValue.trim().toIntOrNull()
        ?: return ParsedLoyaltySettingsRequest.Invalid("Indique o valor da recompensa.")
    val description = rewardDescription.trim().replace(Regex("\\s+"), " ")
    val valueRange = type.rewardValueRange()

    return when {
        stamps !in 1..50 ->
            ParsedLoyaltySettingsRequest.Invalid("O prémio deve exigir entre 1 e 50 lavagens.")
        value !in valueRange ->
            ParsedLoyaltySettingsRequest.Invalid("O valor do prémio não é válido para o tipo selecionado.")
        description.isBlank() ->
            ParsedLoyaltySettingsRequest.Invalid("Indique a descrição do prémio.")
        description.length > 200 ->
            ParsedLoyaltySettingsRequest.Invalid("A descrição do prémio deve ter no máximo 200 caracteres.")
        else -> ParsedLoyaltySettingsRequest.Valid(
            AdminLoyaltySettingsUpdateRequest(
                stampsRequired = stamps,
                rewardType = type,
                rewardValue = value,
                rewardDescription = description,
            ),
        )
    }
}

internal fun String.normalizeRewardType(): String {
    return trim()
        .lowercase()
        .replace("-", "_")
        .replace(" ", "_")
        .takeIf { it in LoyaltyRewardTypes }
        ?: "free_wash"
}

internal fun String.rewardValueRange(): IntRange {
    return when (normalizeRewardType()) {
        "discount_percent" -> 1..100
        "discount_amount" -> 1..100000
        else -> 1..10
    }
}

internal fun String.rewardTypeLabel(): String {
    return when (normalizeRewardType()) {
        "discount_percent" -> "Percentagem"
        "discount_amount" -> "Valor fixo"
        else -> "Lavagem grátis"
    }
}

internal const val RewardTypeFreeWash = "free_wash"
internal const val RewardTypeDiscountAmount = "discount_amount"
internal const val RewardTypeDiscountPercent = "discount_percent"

internal fun String.loyaltyRewardTypeLabel(): String = rewardTypeLabel()

private val LoyaltyRewardTypes = setOf("free_wash", "discount_amount", "discount_percent")

private fun AdminError.toAdminLoyaltySettingsState(): AdminLoyaltySettingsUiState {
    return when (this) {
        is AdminError.Permission -> AdminLoyaltySettingsUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminLoyaltySettingsUiState.Unauthenticated
        is AdminError.Validation -> AdminLoyaltySettingsUiState.Error(message = message, retryable = false)
        is AdminError.Conflict -> AdminLoyaltySettingsUiState.Error(message = message, retryable = false)
        is AdminError.NotFound -> AdminLoyaltySettingsUiState.Error(message = message, retryable = true)
        is AdminError.Unavailable -> AdminLoyaltySettingsUiState.Error(message = message, retryable = true)
        is AdminError.Backend -> AdminLoyaltySettingsUiState.Error(message = message, retryable = true)
    }
}

private fun AdminError.toAdminLoyaltyReportState(): AdminLoyaltyReportUiState {
    return when (this) {
        is AdminError.Permission -> AdminLoyaltyReportUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminLoyaltyReportUiState.Unauthenticated
        is AdminError.Validation,
        is AdminError.Conflict -> AdminLoyaltyReportUiState.Error(message = message, retryable = false)
        is AdminError.NotFound,
        is AdminError.Unavailable,
        is AdminError.Backend -> AdminLoyaltyReportUiState.Error(message = message, retryable = true)
    }
}

private fun AdminError.toAdminLoyaltySettingsSaveState(): AdminLoyaltySettingsSaveState.Error {
    return AdminLoyaltySettingsSaveState.Error(
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend,
    )
}

private fun AuthError.toAdminLoyaltySettingsState(): AdminLoyaltySettingsUiState.Error {
    return AdminLoyaltySettingsUiState.Error(message = message, retryable = isRetryable())
}

private fun AuthError.isRetryable(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private fun loyaltySettingsAuditLabel(timestampIso: String, actorUid: String): String {
    val timestampLabel = timestampIso.toLoyaltySettingsAuditDateTimeLabel() ?: return ""
    val actorLabel = actorUid.trim().takeIf { it.isNotBlank() }?.toShortLoyaltyAuditUid()?.let { " por $it" }.orEmpty()
    return "Atualizado $timestampLabel$actorLabel"
}

private fun String.toLoyaltySettingsAuditDateTimeLabel(): String? {
    val value = trim()
    if (value.isBlank()) return null
    val date = value.substringBefore("T", missingDelimiterValue = "")
    val time = value.substringAfter("T", missingDelimiterValue = "").take(5)
    if (date.length != 10 || time.length != 5) return value
    return "$date $time UTC"
}

private fun String.toShortLoyaltyAuditUid(): String {
    val value = trim()
    return if (value.length <= 12) value else "${value.take(8)}..."
}
