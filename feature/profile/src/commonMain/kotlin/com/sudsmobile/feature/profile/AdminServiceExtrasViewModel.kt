package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.admin.AdminServiceExtraArchiveRequest
import com.sudsmobile.data.admin.AdminServiceExtraItem
import com.sudsmobile.data.admin.AdminServiceExtraMutationRequest
import com.sudsmobile.data.admin.AdminServiceExtraMutationResult
import com.sudsmobile.data.admin.AdminServiceExtrasResult
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal data class AdminServiceExtraUi(
    val id: String,
    val name: String,
    val description: String,
    val priceLabel: String,
    val iconKey: String,
    val eligibleServiceIdsLabel: String,
    val active: Boolean,
    val sortOrder: Int,
    val auditLabels: List<String> = emptyList(),
)

internal data class AdminServiceExtraForm(
    val originalExtraId: String = "",
    val extraId: String = "",
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val iconKey: String = "auto_awesome",
    val eligibleServiceIds: String = "",
    val active: Boolean = true,
    val sortOrder: String = "999",
) {
    val isEditingExisting: Boolean
        get() = originalExtraId.isNotBlank()
}

internal sealed interface AdminServiceExtrasUiState {
    data object Idle : AdminServiceExtrasUiState
    data object Loading : AdminServiceExtrasUiState
    data object Unauthenticated : AdminServiceExtrasUiState
    data object NotAdmin : AdminServiceExtrasUiState
    data object Empty : AdminServiceExtrasUiState
    data class Loaded(
        val extras: List<AdminServiceExtraUi>,
        val form: AdminServiceExtraForm? = null,
    ) : AdminServiceExtrasUiState

    data class Error(val message: String, val retryable: Boolean) : AdminServiceExtrasUiState
}

internal sealed interface AdminServiceExtrasMutationState {
    data object Idle : AdminServiceExtrasMutationState
    data object Saving : AdminServiceExtrasMutationState
    data class Archiving(val extraId: String) : AdminServiceExtrasMutationState
    data class Success(val message: String) : AdminServiceExtrasMutationState
    data class Error(val message: String, val retryable: Boolean) : AdminServiceExtrasMutationState
}

internal class AdminServiceExtrasViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminServiceExtrasUiState>(AdminServiceExtrasUiState.Idle)
    val uiState: StateFlow<AdminServiceExtrasUiState> = _uiState.asStateFlow()
    private val _mutationState =
        MutableStateFlow<AdminServiceExtrasMutationState>(AdminServiceExtrasMutationState.Idle)
    val mutationState: StateFlow<AdminServiceExtrasMutationState> = _mutationState.asStateFlow()

    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedExtras()
                _uiState.value = AdminServiceExtrasUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedExtras()
                _uiState.value = currentSessionState.error.toAdminServiceExtrasState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedExtras()
                _uiState.value = AdminServiceExtrasUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val hasReusableState = _uiState.value is AdminServiceExtrasUiState.Loaded ||
            _uiState.value is AdminServiceExtrasUiState.Empty ||
            _uiState.value is AdminServiceExtrasUiState.NotAdmin
        if (!force && loadedUid == uid && hasReusableState) return
        loadExtras(force = force)
    }

    fun loadExtras(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedExtras()
                _uiState.value = AdminServiceExtrasUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedExtras()
                _uiState.value = currentSessionState.error.toAdminServiceExtrasState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedExtras()
                _uiState.value = AdminServiceExtrasUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val requestedUid = session.session.user.uid
        if (!force && loadingUid == requestedUid) return

        val requestSequence = ++loadSequence
        loadingUid = requestedUid
        viewModelScope.launch {
            try {
                _uiState.value = AdminServiceExtrasUiState.Loading
                val nextState = when (val result = adminRepository.getServiceExtrasConfiguration()) {
                    is AdminServiceExtrasResult.Success -> result.config.extras.toAdminServiceExtrasState()
                    is AdminServiceExtrasResult.Failure -> result.error.toAdminServiceExtrasState()
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

    fun startCreate() {
        val currentState = _uiState.value
        val extras = when (currentState) {
            is AdminServiceExtrasUiState.Loaded -> currentState.extras
            AdminServiceExtrasUiState.Empty -> emptyList()
            else -> return
        }
        _uiState.value = AdminServiceExtrasUiState.Loaded(
            extras = extras,
            form = AdminServiceExtraForm(sortOrder = nextExtraSortOrder(extras).toString()),
        )
        _mutationState.value = AdminServiceExtrasMutationState.Idle
    }

    fun editExtra(extraId: String) {
        val currentState = _uiState.value as? AdminServiceExtrasUiState.Loaded ?: return
        val extra = currentState.extras.firstOrNull { it.id == extraId } ?: return
        _uiState.value = currentState.copy(form = extra.toForm())
        _mutationState.value = AdminServiceExtrasMutationState.Idle
    }

    fun cancelEdit() {
        val currentState = _uiState.value as? AdminServiceExtrasUiState.Loaded ?: return
        _uiState.value = currentState.copy(form = null)
    }

    fun updateForm(form: AdminServiceExtraForm) {
        val currentState = _uiState.value as? AdminServiceExtrasUiState.Loaded ?: return
        _uiState.value = currentState.copy(form = form)
        _mutationState.value = AdminServiceExtrasMutationState.Idle
    }

    fun save() {
        if (_mutationState.value == AdminServiceExtrasMutationState.Saving) return
        val form = (_uiState.value as? AdminServiceExtrasUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedExtras()
            _uiState.value = AdminServiceExtrasUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toMutationRequest()) {
            is ParsedServiceExtraRequest.Invalid -> {
                _mutationState.value = AdminServiceExtrasMutationState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedServiceExtraRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _mutationState.value = AdminServiceExtrasMutationState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                _mutationState.value = AdminServiceExtrasMutationState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            val result = adminRepository.upsertServiceExtra(request)
            val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (latestUid != requestedUid) {
                _mutationState.value = AdminServiceExtrasMutationState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            when (result) {
                is AdminServiceExtraMutationResult.Success -> {
                    loadedUid = null
                    _mutationState.value = AdminServiceExtrasMutationState.Success("Extra guardado.")
                    loadExtras(force = true)
                }
                is AdminServiceExtraMutationResult.Failure -> {
                    _mutationState.value = result.error.toAdminServiceExtrasMutationState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminServiceExtrasUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedExtras()
                        _uiState.value = AdminServiceExtrasUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun archive(extraId: String) {
        val cleanExtraId = extraId.trim()
        if (cleanExtraId.isBlank()) {
            _mutationState.value = AdminServiceExtrasMutationState.Error(
                message = "O extra selecionado é inválido.",
                retryable = false,
            )
            return
        }
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedExtras()
            _uiState.value = AdminServiceExtrasUiState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _mutationState.value = AdminServiceExtrasMutationState.Archiving(cleanExtraId)
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                _mutationState.value = AdminServiceExtrasMutationState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            val result = adminRepository.archiveServiceExtra(
                AdminServiceExtraArchiveRequest(cleanExtraId),
            )
            val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (latestUid != requestedUid) {
                _mutationState.value = AdminServiceExtrasMutationState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            when (result) {
                is AdminServiceExtraMutationResult.Success -> {
                    loadedUid = null
                    _mutationState.value = AdminServiceExtrasMutationState.Success("Extra arquivado.")
                    loadExtras(force = true)
                }
                is AdminServiceExtraMutationResult.Failure -> {
                    _mutationState.value = result.error.toAdminServiceExtrasMutationState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminServiceExtrasUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedExtras()
                        _uiState.value = AdminServiceExtrasUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun clearMutationState() {
        _mutationState.value = AdminServiceExtrasMutationState.Idle
    }

    private fun clearLoadedExtras() {
        loadedUid = null
        loadingUid = null
        loadSequence += 1
    }

    private fun handleSessionChangedDuringRequest() {
        clearLoadedExtras()
        refreshForSession(force = true)
    }
}

private sealed interface ParsedServiceExtraRequest {
    data class Valid(val request: AdminServiceExtraMutationRequest) : ParsedServiceExtraRequest
    data class Invalid(val message: String) : ParsedServiceExtraRequest
}

private fun List<AdminServiceExtraItem>.toAdminServiceExtrasState(): AdminServiceExtrasUiState {
    val extras = map { it.toUi() }
    return if (extras.isEmpty()) {
        AdminServiceExtrasUiState.Empty
    } else {
        AdminServiceExtrasUiState.Loaded(extras)
    }
}

private fun AdminServiceExtraItem.toUi(): AdminServiceExtraUi = AdminServiceExtraUi(
    id = id,
    name = name.ifBlank { "Extra sem nome" },
    description = description,
    priceLabel = priceCents.toExtraEuroLabel(),
    iconKey = iconKey.ifBlank { "auto_awesome" },
    eligibleServiceIdsLabel = eligibleServiceIds.joinToString(", "),
    active = active,
    sortOrder = sortOrder,
    auditLabels = listOf(
        serviceExtraAuditLabel("Criado", createdAtIso, createdByUid),
        serviceExtraAuditLabel("Atualizado", updatedAtIso, updatedByUid),
        serviceExtraAuditLabel("Arquivado", archivedAtIso, archivedByUid),
    ).filter { it.isNotBlank() },
)

private fun AdminServiceExtraUi.toForm(): AdminServiceExtraForm = AdminServiceExtraForm(
    originalExtraId = id,
    extraId = id,
    name = name,
    description = description,
    price = priceLabel.removeSuffix(" €").replace(",", "."),
    iconKey = iconKey,
    eligibleServiceIds = eligibleServiceIdsLabel,
    active = active,
    sortOrder = sortOrder.toString(),
)

private fun AdminServiceExtraForm.toMutationRequest(): ParsedServiceExtraRequest {
    val priceCents = price.toExtraPriceCentsOrNull()
        ?: return ParsedServiceExtraRequest.Invalid("Indique o preço do extra.")
    val sortOrderValue = sortOrder.trim().toIntOrNull()
        ?: return ParsedServiceExtraRequest.Invalid("Indique uma ordenação válida.")
    val eligibleIds = eligibleServiceIds.toEligibleServiceIdsOrNull()
        ?: return ParsedServiceExtraRequest.Invalid("Indique IDs de serviços válidos.")

    return ParsedServiceExtraRequest.Valid(
        AdminServiceExtraMutationRequest(
            extraId = originalExtraId.ifBlank { extraId.trim() },
            name = name,
            description = description,
            priceCents = priceCents,
            iconKey = iconKey,
            eligibleServiceIds = eligibleIds,
            active = active,
            sortOrder = sortOrderValue,
        ),
    )
}

private fun String.toExtraPriceCentsOrNull(): Int? {
    val normalized = trim()
        .removeSuffix("€")
        .trim()
        .replace(",", ".")
    if (normalized.isBlank()) return null
    val euros = normalized.toDoubleOrNull() ?: return null
    if (euros < 0.0 || euros > 1000.0) return null
    return (euros * 100).roundToInt()
}

private fun String.toEligibleServiceIdsOrNull(): List<String>? {
    val seen = mutableSetOf<String>()
    return split(",", "\n", " ")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map {
            if (!Regex("^[A-Za-z0-9_-]{1,80}$").matches(it)) return null
            it
        }
        .filter { seen.add(it) }
        .take(40)
}

private fun Int.toExtraEuroLabel(): String {
    val euros = this.coerceAtLeast(0) / 100
    val cents = this.coerceAtLeast(0) % 100
    return "$euros,${cents.toString().padStart(2, '0')} €"
}

private fun nextExtraSortOrder(extras: List<AdminServiceExtraUi>): Int {
    return ((extras.maxOfOrNull { it.sortOrder } ?: 0) + 10).coerceAtMost(9999)
}

private fun serviceExtraAuditLabel(action: String, timestampIso: String, actorUid: String): String {
    val timestampLabel = timestampIso.toServiceExtraAuditDateTimeLabel() ?: return ""
    val actorLabel = actorUid.trim().takeIf { it.isNotBlank() }?.toShortServiceExtraAuditUid()
        ?.let { " por $it" }
        .orEmpty()
    return "$action $timestampLabel$actorLabel"
}

private fun String.toServiceExtraAuditDateTimeLabel(): String? {
    val value = trim()
    if (value.isBlank()) return null
    val date = value.substringBefore("T", missingDelimiterValue = "")
    val time = value.substringAfter("T", missingDelimiterValue = "").take(5)
    if (date.length != 10 || time.length != 5) return value
    return "$date $time UTC"
}

private fun String.toShortServiceExtraAuditUid(): String {
    val value = trim()
    return if (value.length <= 12) value else "${value.take(8)}..."
}

private fun AdminError.toAdminServiceExtrasState(): AdminServiceExtrasUiState {
    return when (this) {
        is AdminError.Permission -> AdminServiceExtrasUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminServiceExtrasUiState.Unauthenticated
        is AdminError.Unavailable,
        is AdminError.Backend -> AdminServiceExtrasUiState.Error(message = message, retryable = true)
        is AdminError.Validation,
        is AdminError.NotFound,
        is AdminError.Conflict -> AdminServiceExtrasUiState.Error(message = message, retryable = false)
    }
}

private fun AdminError.toAdminServiceExtrasMutationState(): AdminServiceExtrasMutationState.Error {
    return AdminServiceExtrasMutationState.Error(
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend || this is AdminError.Conflict,
    )
}

private fun AuthError.toAdminServiceExtrasState(): AdminServiceExtrasUiState.Error {
    return AdminServiceExtrasUiState.Error(
        message = message,
        retryable = this is AuthError.Unavailable || this is AuthError.Backend,
    )
}
