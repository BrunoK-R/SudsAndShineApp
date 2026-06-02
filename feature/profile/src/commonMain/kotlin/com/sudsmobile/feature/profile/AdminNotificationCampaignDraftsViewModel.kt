package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminNotificationCampaignDraft
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftArchiveRequest
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftMutationRequest
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftMutationResult
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftsResult
import com.sudsmobile.data.admin.AdminNotificationTestRequest
import com.sudsmobile.data.admin.AdminNotificationTestResult
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class AdminNotificationCampaignDraftUi(
    val campaignId: String,
    val title: String,
    val body: String,
    val targetAudience: String,
    val targetAudienceLabel: String,
    val status: String,
    val statusLabel: String,
    val scheduledAtIso: String,
    val scheduledAtLabel: String,
    val notes: String,
    val sendBlocked: Boolean,
    val sendBlockedReason: String,
    val createdAuditLabel: String,
    val updatedAuditLabel: String,
    val archivedAuditLabel: String,
)

internal data class AdminNotificationCampaignDraftForm(
    val originalCampaignId: String = "",
    val campaignId: String = "",
    val title: String = "",
    val body: String = "",
    val targetAudience: String = "test_users",
    val scheduledAtIso: String = "",
    val notes: String = "",
    val pushEnabled: Boolean = true,
) {
    val isEditingExisting: Boolean
        get() = originalCampaignId.isNotBlank()
}

internal sealed interface AdminNotificationCampaignDraftsUiState {
    data object Idle : AdminNotificationCampaignDraftsUiState
    data object Loading : AdminNotificationCampaignDraftsUiState
    data object Unauthenticated : AdminNotificationCampaignDraftsUiState
    data object NotAdmin : AdminNotificationCampaignDraftsUiState
    data object Empty : AdminNotificationCampaignDraftsUiState
    data class Loaded(
        val drafts: List<AdminNotificationCampaignDraftUi>,
        val form: AdminNotificationCampaignDraftForm? = null,
    ) : AdminNotificationCampaignDraftsUiState

    data class Error(val message: String, val retryable: Boolean) : AdminNotificationCampaignDraftsUiState
}

internal sealed interface AdminNotificationCampaignDraftMutationState {
    data object Idle : AdminNotificationCampaignDraftMutationState
    data object Saving : AdminNotificationCampaignDraftMutationState
    data class Archiving(val campaignId: String) : AdminNotificationCampaignDraftMutationState
    data class Testing(val campaignId: String) : AdminNotificationCampaignDraftMutationState
    data class Success(val message: String) : AdminNotificationCampaignDraftMutationState
    data class Error(val message: String, val retryable: Boolean) : AdminNotificationCampaignDraftMutationState
}

internal class AdminNotificationCampaignDraftsViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminNotificationCampaignDraftsUiState>(
        AdminNotificationCampaignDraftsUiState.Idle,
    )
    val uiState: StateFlow<AdminNotificationCampaignDraftsUiState> = _uiState.asStateFlow()
    private val _mutationState = MutableStateFlow<AdminNotificationCampaignDraftMutationState>(
        AdminNotificationCampaignDraftMutationState.Idle,
    )
    val mutationState: StateFlow<AdminNotificationCampaignDraftMutationState> = _mutationState.asStateFlow()

    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedDrafts()
                _uiState.value = AdminNotificationCampaignDraftsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedDrafts()
                _uiState.value = currentSessionState.error.toCampaignDraftsState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedDrafts()
                _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val hasReusableState = _uiState.value is AdminNotificationCampaignDraftsUiState.Loaded ||
            _uiState.value is AdminNotificationCampaignDraftsUiState.Empty ||
            _uiState.value is AdminNotificationCampaignDraftsUiState.NotAdmin
        if (!force && loadedUid == uid && hasReusableState) return
        loadDrafts(force = force)
    }

    fun loadDrafts(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedDrafts()
                _uiState.value = AdminNotificationCampaignDraftsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedDrafts()
                _uiState.value = currentSessionState.error.toCampaignDraftsState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedDrafts()
                _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
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
                _uiState.value = AdminNotificationCampaignDraftsUiState.Loading
                val nextState = when (val result = adminRepository.getNotificationCampaignDrafts()) {
                    is AdminNotificationCampaignDraftsResult.Success -> result.config.campaigns.toCampaignDraftsState()
                    is AdminNotificationCampaignDraftsResult.Failure -> result.error.toCampaignDraftsState()
                }
                if (requestSequence != loadSequence) return@launch

                val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                if (currentUid == requestedUid) {
                    loadedUid = requestedUid
                    _uiState.value = nextState
                } else {
                    clearLoadedDrafts()
                    _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
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
        val drafts = when (currentState) {
            is AdminNotificationCampaignDraftsUiState.Loaded -> currentState.drafts
            AdminNotificationCampaignDraftsUiState.Empty -> emptyList()
            else -> return
        }
        _uiState.value = AdminNotificationCampaignDraftsUiState.Loaded(
            drafts = drafts,
            form = AdminNotificationCampaignDraftForm(),
        )
        _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
    }

    fun editDraft(campaignId: String) {
        val currentState = _uiState.value as? AdminNotificationCampaignDraftsUiState.Loaded ?: return
        val draft = currentState.drafts.firstOrNull { it.campaignId == campaignId } ?: return
        _uiState.value = currentState.copy(form = draft.toForm())
        _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
    }

    fun cancelEdit() {
        val currentState = _uiState.value as? AdminNotificationCampaignDraftsUiState.Loaded ?: return
        _uiState.value = currentState.copy(form = null)
    }

    fun updateForm(form: AdminNotificationCampaignDraftForm) {
        val currentState = _uiState.value as? AdminNotificationCampaignDraftsUiState.Loaded ?: return
        _uiState.value = currentState.copy(form = form)
        _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
    }

    fun save() {
        if (_mutationState.value == AdminNotificationCampaignDraftMutationState.Saving) return
        val form = (_uiState.value as? AdminNotificationCampaignDraftsUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedDrafts()
            _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toMutationRequest()) {
            is ParsedCampaignDraftRequest.Invalid -> {
                _mutationState.value = AdminNotificationCampaignDraftMutationState.Error(
                    message = parsed.message,
                    retryable = false,
                )
                return
            }
            is ParsedCampaignDraftRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _mutationState.value = AdminNotificationCampaignDraftMutationState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedDrafts()
                _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
                return@launch
            }

            when (val result = adminRepository.upsertNotificationCampaignDraft(request)) {
                is AdminNotificationCampaignDraftMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = null
                        _mutationState.value = AdminNotificationCampaignDraftMutationState.Success(
                            "Rascunho guardado sem envio.",
                        )
                        loadDrafts(force = true)
                    } else {
                        clearLoadedDrafts()
                        _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                        _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
                    }
                }
                is AdminNotificationCampaignDraftMutationResult.Failure -> {
                    _mutationState.value = result.error.toCampaignDraftMutationState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminNotificationCampaignDraftsUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedDrafts()
                        _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun archive(campaignId: String) {
        val cleanCampaignId = campaignId.trim()
        if (cleanCampaignId.isBlank()) {
            _mutationState.value = AdminNotificationCampaignDraftMutationState.Error(
                message = "O rascunho selecionado é inválido.",
                retryable = false,
            )
            return
        }
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedDrafts()
            _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _mutationState.value = AdminNotificationCampaignDraftMutationState.Archiving(cleanCampaignId)
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedDrafts()
                _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
                return@launch
            }

            when (
                val result = adminRepository.archiveNotificationCampaignDraft(
                    AdminNotificationCampaignDraftArchiveRequest(cleanCampaignId),
                )
            ) {
                is AdminNotificationCampaignDraftMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = null
                        _mutationState.value = AdminNotificationCampaignDraftMutationState.Success(
                            "Rascunho arquivado.",
                        )
                        loadDrafts(force = true)
                    } else {
                        clearLoadedDrafts()
                        _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                        _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
                    }
                }
                is AdminNotificationCampaignDraftMutationResult.Failure -> {
                    _mutationState.value = result.error.toCampaignDraftMutationState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminNotificationCampaignDraftsUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedDrafts()
                        _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun sendTest(campaignId: String) {
        val cleanCampaignId = campaignId.trim()
        val loaded = _uiState.value as? AdminNotificationCampaignDraftsUiState.Loaded
        val draft = loaded?.drafts?.firstOrNull { it.campaignId == cleanCampaignId }
        if (cleanCampaignId.isBlank() || !cleanCampaignId.isValidCampaignDraftId() || draft == null) {
            _mutationState.value = AdminNotificationCampaignDraftMutationState.Error(
                message = "O rascunho selecionado é inválido.",
                retryable = false,
            )
            return
        }
        if (draft.status == "archived") {
            _mutationState.value = AdminNotificationCampaignDraftMutationState.Error(
                message = "Apenas rascunhos ativos podem ser testados.",
                retryable = false,
            )
            return
        }
        if (_mutationState.value is AdminNotificationCampaignDraftMutationState.Testing) return

        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedDrafts()
            _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _mutationState.value = AdminNotificationCampaignDraftMutationState.Testing(cleanCampaignId)
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedDrafts()
                _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
                return@launch
            }

            when (
                val result = adminRepository.sendNotificationTestToSelf(
                    AdminNotificationTestRequest(campaignId = cleanCampaignId),
                )
            ) {
                is AdminNotificationTestResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        _mutationState.value = if (result.receipt.isCurrentAdminSelfTest(requestedUid)) {
                            AdminNotificationCampaignDraftMutationState.Success(
                                result.receipt.toSelfTestQueuedMessage("Teste de campanha"),
                            )
                        } else {
                            AdminNotificationCampaignDraftMutationState.Error(
                                message = UnsafeAdminNotificationTestReceiptMessage,
                                retryable = false,
                            )
                        }
                    } else {
                        clearLoadedDrafts()
                        _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                        _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
                    }
                }
                is AdminNotificationTestResult.Failure -> {
                    _mutationState.value = result.error.toCampaignDraftMutationState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminNotificationCampaignDraftsUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedDrafts()
                        _uiState.value = AdminNotificationCampaignDraftsUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun clearMutationState() {
        _mutationState.value = AdminNotificationCampaignDraftMutationState.Idle
    }

    private fun clearLoadedDrafts() {
        loadedUid = null
        loadingUid = null
        loadSequence += 1
    }
}

private sealed interface ParsedCampaignDraftRequest {
    data class Valid(val request: AdminNotificationCampaignDraftMutationRequest) : ParsedCampaignDraftRequest
    data class Invalid(val message: String) : ParsedCampaignDraftRequest
}

private fun List<AdminNotificationCampaignDraft>.toCampaignDraftsState(): AdminNotificationCampaignDraftsUiState {
    val drafts = map { it.toUi() }
    return if (drafts.isEmpty()) {
        AdminNotificationCampaignDraftsUiState.Empty
    } else {
        AdminNotificationCampaignDraftsUiState.Loaded(drafts)
    }
}

private fun AdminNotificationCampaignDraft.toUi(): AdminNotificationCampaignDraftUi {
    return AdminNotificationCampaignDraftUi(
        campaignId = campaignId,
        title = title.ifBlank { "Campanha sem título" },
        body = body,
        targetAudience = targetAudience.ifBlank { "test_users" },
        targetAudienceLabel = targetAudience.toCampaignAudienceLabel(),
        status = status.ifBlank { "draft" },
        statusLabel = status.toCampaignStatusLabel(),
        scheduledAtIso = scheduledAtIso,
        scheduledAtLabel = scheduledAtIso.ifBlank { "Sem agendamento" },
        notes = notes,
        sendBlocked = sendBlocked,
        sendBlockedReason = sendBlockedReason,
        createdAuditLabel = campaignAuditLabel("Criado", createdAtIso, createdByUid),
        updatedAuditLabel = campaignAuditLabel("Atualizado", updatedAtIso, updatedByUid),
        archivedAuditLabel = campaignAuditLabel("Arquivado", archivedAtIso, archivedByUid),
    )
}

private fun AdminNotificationCampaignDraftUi.toForm(): AdminNotificationCampaignDraftForm {
    return AdminNotificationCampaignDraftForm(
        originalCampaignId = campaignId,
        campaignId = campaignId,
        title = title,
        body = body,
        targetAudience = targetAudience,
        scheduledAtIso = scheduledAtIso,
        notes = notes,
        pushEnabled = true,
    )
}

private fun AdminNotificationCampaignDraftForm.toMutationRequest(): ParsedCampaignDraftRequest {
    val cleanId = campaignId.trim()
    val cleanTitle = title.trim()
    val cleanBody = body.trim()
    val cleanAudience = targetAudience.trim()
    val cleanSchedule = scheduledAtIso.trim()
    val cleanNotes = notes.trim()

    return when {
        cleanId.isNotBlank() && !cleanId.isValidCampaignDraftId() ->
            ParsedCampaignDraftRequest.Invalid("O identificador deve ter 3 a 80 caracteres seguros.")
        cleanTitle.isBlank() ->
            ParsedCampaignDraftRequest.Invalid("Indique o título da campanha.")
        cleanTitle.length > 120 ->
            ParsedCampaignDraftRequest.Invalid("O título da campanha deve ter no máximo 120 caracteres.")
        cleanBody.isBlank() ->
            ParsedCampaignDraftRequest.Invalid("Indique a mensagem da campanha.")
        cleanBody.length > 1000 ->
            ParsedCampaignDraftRequest.Invalid("A mensagem da campanha deve ter no máximo 1000 caracteres.")
        cleanAudience !in CampaignDraftAudienceKeys ->
            ParsedCampaignDraftRequest.Invalid("Escolha um público seguro para a campanha.")
        cleanSchedule.isNotBlank() && !cleanSchedule.isValidCampaignScheduleIso() ->
            ParsedCampaignDraftRequest.Invalid("Use ISO UTC, por exemplo 2026-06-10T10:00:00.000Z.")
        cleanNotes.length > 500 ->
            ParsedCampaignDraftRequest.Invalid("As notas devem ter no máximo 500 caracteres.")
        !pushEnabled ->
            ParsedCampaignDraftRequest.Invalid("Os rascunhos de campanha só suportam push.")
        else -> ParsedCampaignDraftRequest.Valid(
            AdminNotificationCampaignDraftMutationRequest(
                campaignId = cleanId,
                title = cleanTitle,
                body = cleanBody,
                targetAudience = cleanAudience,
                scheduledAtIso = cleanSchedule,
                notes = cleanNotes,
                pushEnabled = true,
            ),
        )
    }
}

private fun AdminError.toCampaignDraftsState(): AdminNotificationCampaignDraftsUiState {
    return when (this) {
        is AdminError.Permission -> AdminNotificationCampaignDraftsUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminNotificationCampaignDraftsUiState.Unauthenticated
        is AdminError.Validation -> AdminNotificationCampaignDraftsUiState.Error(message = message, retryable = false)
        is AdminError.Conflict -> AdminNotificationCampaignDraftsUiState.Error(message = message, retryable = false)
        is AdminError.NotFound -> AdminNotificationCampaignDraftsUiState.Error(message = message, retryable = true)
        is AdminError.Unavailable -> AdminNotificationCampaignDraftsUiState.Error(message = message, retryable = true)
        is AdminError.Backend -> AdminNotificationCampaignDraftsUiState.Error(message = message, retryable = true)
    }
}

private fun AdminError.toCampaignDraftMutationState(): AdminNotificationCampaignDraftMutationState.Error {
    return AdminNotificationCampaignDraftMutationState.Error(
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend,
    )
}

private fun AuthError.toCampaignDraftsState(): AdminNotificationCampaignDraftsUiState.Error {
    return AdminNotificationCampaignDraftsUiState.Error(message = message, retryable = isRetryable())
}

private fun AuthError.isRetryable(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private fun String.toCampaignAudienceLabel(): String {
    return when (trim()) {
        "marketing_opt_in_users" -> "Clientes com opt-in marketing"
        else -> "Utilizadores de teste"
    }
}

private fun String.toCampaignStatusLabel(): String {
    return when (trim()) {
        "archived" -> "Arquivado"
        else -> "Rascunho"
    }
}

private fun campaignAuditLabel(action: String, timestampIso: String, actorUid: String): String {
    val timestampLabel = timestampIso.toCampaignAuditDateTimeLabel() ?: return ""
    val actorLabel = actorUid.trim().takeIf { it.isNotBlank() }?.toShortAuditUid()?.let { " por $it" }.orEmpty()
    return "$action $timestampLabel$actorLabel"
}

private fun String.toCampaignAuditDateTimeLabel(): String? {
    val value = trim()
    if (value.isBlank()) return null
    val date = value.substringBefore("T", missingDelimiterValue = "")
    val time = value.substringAfter("T", missingDelimiterValue = "").take(5)
    if (date.length != 10 || time.length != 5) return value
    return "$date $time UTC"
}

private fun String.toShortAuditUid(): String {
    val value = trim()
    return if (value.length <= 12) value else "${value.take(8)}..."
}

private fun String.isValidCampaignDraftId(): Boolean {
    return Regex("^[A-Za-z0-9_-]{3,80}$").matches(this) && !contains("/")
}

private fun String.isValidCampaignScheduleIso(): Boolean {
    return Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z$").matches(this)
}

internal val CampaignDraftAudienceKeys = setOf("test_users", "marketing_opt_in_users")
