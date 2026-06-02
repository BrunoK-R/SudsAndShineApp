package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminNotificationSettingsConfig
import com.sudsmobile.data.admin.AdminNotificationSettingsResult
import com.sudsmobile.data.admin.AdminNotificationSettingsUpdateRequest
import com.sudsmobile.data.admin.AdminNotificationTestRequest
import com.sudsmobile.data.admin.AdminNotificationTestResult
import com.sudsmobile.data.admin.AdminNotificationTemplateConfig
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class AdminNotificationSettingsForm(
    val bookingStatusEnabled: Boolean = true,
    val appointmentReminderEnabled: Boolean = true,
    val loyaltyEnabled: Boolean = true,
    val adminPendingAlertEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
    val reminderLeadMinutes: String = "120",
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00",
    val quietHoursTimeZone: String = "Europe/Lisbon",
    val templates: List<AdminNotificationTemplateForm> = defaultNotificationTemplateForms(),
    val updatedAuditLabel: String = "",
)

internal data class AdminNotificationTemplateForm(
    val key: String,
    val label: String,
    val enabled: Boolean,
    val title: String,
    val body: String,
)

internal sealed interface AdminNotificationSettingsUiState {
    data object Idle : AdminNotificationSettingsUiState
    data object Loading : AdminNotificationSettingsUiState
    data object Unauthenticated : AdminNotificationSettingsUiState
    data object NotAdmin : AdminNotificationSettingsUiState
    data class Loaded(val form: AdminNotificationSettingsForm) : AdminNotificationSettingsUiState
    data class Error(val message: String, val retryable: Boolean) : AdminNotificationSettingsUiState
}

internal sealed interface AdminNotificationSettingsSaveState {
    data object Idle : AdminNotificationSettingsSaveState
    data object Saving : AdminNotificationSettingsSaveState
    data class Success(val message: String) : AdminNotificationSettingsSaveState
    data class Error(val message: String, val retryable: Boolean) : AdminNotificationSettingsSaveState
}

internal sealed interface AdminNotificationTestState {
    data object Idle : AdminNotificationTestState
    data class Sending(val templateKey: String) : AdminNotificationTestState
    data class Success(val templateLabel: String, val message: String) : AdminNotificationTestState
    data class Error(val templateLabel: String, val message: String, val retryable: Boolean) : AdminNotificationTestState
}

internal class AdminNotificationSettingsViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminNotificationSettingsUiState>(AdminNotificationSettingsUiState.Idle)
    val uiState: StateFlow<AdminNotificationSettingsUiState> = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow<AdminNotificationSettingsSaveState>(
        AdminNotificationSettingsSaveState.Idle,
    )
    val saveState: StateFlow<AdminNotificationSettingsSaveState> = _saveState.asStateFlow()
    private val _testState = MutableStateFlow<AdminNotificationTestState>(AdminNotificationTestState.Idle)
    val testState: StateFlow<AdminNotificationTestState> = _testState.asStateFlow()
    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0
    private var testSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminNotificationSettingsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminNotificationSettingsState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminNotificationSettingsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        if (!force && loadedUid == uid && _uiState.value is AdminNotificationSettingsUiState.Loaded) return
        loadConfiguration()
    }

    fun loadConfiguration() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminNotificationSettingsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminNotificationSettingsState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminNotificationSettingsUiState.Unauthenticated
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
                _uiState.value = AdminNotificationSettingsUiState.Loading
                val nextState = when (val result = adminRepository.getNotificationSettingsConfiguration()) {
                    is AdminNotificationSettingsResult.Success ->
                        AdminNotificationSettingsUiState.Loaded(result.config.toForm())
                    is AdminNotificationSettingsResult.Failure -> result.error.toAdminNotificationSettingsState()
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

    fun updateForm(form: AdminNotificationSettingsForm) {
        if (_uiState.value is AdminNotificationSettingsUiState.Loaded) {
            _uiState.value = AdminNotificationSettingsUiState.Loaded(form)
            _saveState.value = AdminNotificationSettingsSaveState.Idle
            clearFinishedTestState()
        }
    }

    fun updateTemplate(template: AdminNotificationTemplateForm) {
        val loaded = _uiState.value as? AdminNotificationSettingsUiState.Loaded ?: return
        updateForm(
            loaded.form.copy(
                templates = loaded.form.templates.map {
                    if (it.key == template.key) template else it
                },
            ),
        )
    }

    fun save() {
        if (_saveState.value == AdminNotificationSettingsSaveState.Saving) return
        val form = (_uiState.value as? AdminNotificationSettingsUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminNotificationSettingsUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toUpdateRequest()) {
            is ParsedNotificationSettingsRequest.Invalid -> {
                _saveState.value = AdminNotificationSettingsSaveState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedNotificationSettingsRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _saveState.value = AdminNotificationSettingsSaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                _saveState.value = AdminNotificationSettingsSaveState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            val result = adminRepository.updateNotificationSettingsConfiguration(request)
            val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (latestUid != requestedUid) {
                _saveState.value = AdminNotificationSettingsSaveState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            when (result) {
                is AdminNotificationSettingsResult.Success -> {
                    loadedUid = requestedUid
                    _uiState.value = AdminNotificationSettingsUiState.Loaded(result.config.toForm())
                    _saveState.value = AdminNotificationSettingsSaveState.Success("Notificações guardadas.")
                }
                is AdminNotificationSettingsResult.Failure -> {
                    _saveState.value = result.error.toAdminNotificationSettingsSaveState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminNotificationSettingsUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedConfig()
                        _uiState.value = AdminNotificationSettingsUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun sendTest(templateKey: String) {
        if (_testState.value is AdminNotificationTestState.Sending) return
        val loaded = _uiState.value as? AdminNotificationSettingsUiState.Loaded ?: return
        val template = loaded.form.templates.firstOrNull { it.key == templateKey.trim() } ?: return
        if (!template.enabled) {
            _testState.value = AdminNotificationTestState.Error(
                templateLabel = template.label,
                message = "Ative este modelo antes de enviar um teste.",
                retryable = false,
            )
            return
        }
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminNotificationSettingsUiState.Unauthenticated
            return
        }

        val requestSequence = ++testSequence
        viewModelScope.launch {
            _testState.value = AdminNotificationTestState.Sending(template.key)
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                _testState.value = AdminNotificationTestState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            val result = adminRepository.sendNotificationTestToSelf(AdminNotificationTestRequest(template.key))
            if (requestSequence != testSequence) return@launch

            val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (latestUid != requestedUid) {
                _testState.value = AdminNotificationTestState.Idle
                handleSessionChangedDuringRequest()
                return@launch
            }

            when (result) {
                is AdminNotificationTestResult.Success -> {
                    if (result.receipt.isCurrentAdminSelfTest(requestedUid)) {
                        _testState.value = AdminNotificationTestState.Success(
                            templateLabel = template.label,
                            message = result.receipt.toSelfTestQueuedMessage("Teste de notificação"),
                        )
                    } else {
                        _testState.value = AdminNotificationTestState.Error(
                            templateLabel = template.label,
                            message = UnsafeAdminNotificationTestReceiptMessage,
                            retryable = false,
                        )
                    }
                }
                is AdminNotificationTestResult.Failure -> {
                    _testState.value = result.error.toAdminNotificationTestState(template.label)
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminNotificationSettingsUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedConfig()
                        _uiState.value = AdminNotificationSettingsUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = AdminNotificationSettingsSaveState.Idle
    }

    fun clearTestState() {
        testSequence += 1
        _testState.value = AdminNotificationTestState.Idle
    }

    private fun clearLoadedConfig() {
        loadedUid = null
        loadingUid = null
        loadSequence += 1
        clearTestState()
    }

    private fun clearFinishedTestState() {
        if (_testState.value !is AdminNotificationTestState.Sending) {
            clearTestState()
        }
    }

    private fun handleSessionChangedDuringRequest() {
        clearLoadedConfig()
        refreshForSession(force = true)
    }
}

private sealed interface ParsedNotificationSettingsRequest {
    data class Valid(val request: AdminNotificationSettingsUpdateRequest) : ParsedNotificationSettingsRequest
    data class Invalid(val message: String) : ParsedNotificationSettingsRequest
}

private fun AdminNotificationSettingsConfig.toForm(): AdminNotificationSettingsForm = AdminNotificationSettingsForm(
    bookingStatusEnabled = bookingStatusEnabled,
    appointmentReminderEnabled = appointmentReminderEnabled,
    loyaltyEnabled = loyaltyEnabled,
    adminPendingAlertEnabled = adminPendingAlertEnabled,
    marketingEnabled = marketingEnabled,
    reminderLeadMinutes = reminderLeadMinutes.toString(),
    quietHoursStart = quietHoursStart,
    quietHoursEnd = quietHoursEnd,
    quietHoursTimeZone = quietHoursTimeZone,
    templates = normalizedTemplates().map { it.toForm() },
    updatedAuditLabel = notificationSettingsAuditLabel(updatedAtIso, updatedByUid),
)

private fun AdminNotificationSettingsConfig.normalizedTemplates(): List<AdminNotificationTemplateConfig> {
    val byKey = templates.associateBy { it.key }
    return defaultNotificationTemplates().map { fallback ->
        byKey[fallback.key] ?: fallback
    }
}

private fun AdminNotificationTemplateConfig.toForm(): AdminNotificationTemplateForm = AdminNotificationTemplateForm(
    key = key,
    label = label,
    enabled = enabled,
    title = title,
    body = body,
)

private fun AdminNotificationSettingsForm.toUpdateRequest(): ParsedNotificationSettingsRequest {
    val reminderLead = reminderLeadMinutes.trim().toIntOrNull()
        ?: return ParsedNotificationSettingsRequest.Invalid("Indique a antecedência do lembrete.")
    val quietStart = quietHoursStart.trim()
    val quietEnd = quietHoursEnd.trim()
    val quietTimeZone = quietHoursTimeZone.trim().ifBlank { "Europe/Lisbon" }
    val templates = templates.map {
        it.copy(
            key = it.key.trim(),
            label = it.label.trim().replace(Regex("\\s+"), " "),
            title = it.title.trim().replace(Regex("\\s+"), " "),
            body = it.body.trim().replace(Regex("\\s+"), " "),
        )
    }

    return when {
        reminderLead !in 15..10080 ->
            ParsedNotificationSettingsRequest.Invalid("O lembrete deve ser enviado entre 15 minutos e 7 dias antes.")
        !quietStart.isAdminNotificationTime() || !quietEnd.isAdminNotificationTime() ->
            ParsedNotificationSettingsRequest.Invalid("As horas de silêncio devem estar no formato HH:MM.")
        !quietTimeZone.isAdminNotificationTimeZone() ->
            ParsedNotificationSettingsRequest.Invalid("Indique um fuso horário válido para o período de silêncio.")
        templates.map { it.key }.toSet() != NotificationTemplateKeys ->
            ParsedNotificationSettingsRequest.Invalid("Preencha todos os modelos de notificação.")
        templates.any { it.title.isBlank() || it.body.isBlank() } ->
            ParsedNotificationSettingsRequest.Invalid("Preencha todos os modelos de notificação.")
        templates.any { it.title.length > 120 } ->
            ParsedNotificationSettingsRequest.Invalid("Cada título de notificação deve ter no máximo 120 caracteres.")
        templates.any { it.body.length > 500 } ->
            ParsedNotificationSettingsRequest.Invalid("Cada mensagem de notificação deve ter no máximo 500 caracteres.")
        else -> ParsedNotificationSettingsRequest.Valid(
            AdminNotificationSettingsUpdateRequest(
                bookingStatusEnabled = bookingStatusEnabled,
                appointmentReminderEnabled = appointmentReminderEnabled,
                loyaltyEnabled = loyaltyEnabled,
                adminPendingAlertEnabled = adminPendingAlertEnabled,
                marketingEnabled = marketingEnabled,
                reminderLeadMinutes = reminderLead,
                quietHoursStart = quietStart,
                quietHoursEnd = quietEnd,
                quietHoursTimeZone = quietTimeZone,
                templates = templates.map {
                    AdminNotificationTemplateConfig(
                        key = it.key,
                        label = it.label,
                        enabled = it.enabled,
                        title = it.title,
                        body = it.body,
                    )
                },
            ),
        )
    }
}

private fun AdminError.toAdminNotificationSettingsState(): AdminNotificationSettingsUiState {
    return when (this) {
        is AdminError.Permission -> AdminNotificationSettingsUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminNotificationSettingsUiState.Unauthenticated
        is AdminError.Validation -> AdminNotificationSettingsUiState.Error(message = message, retryable = false)
        is AdminError.Conflict -> AdminNotificationSettingsUiState.Error(message = message, retryable = false)
        is AdminError.NotFound -> AdminNotificationSettingsUiState.Error(message = message, retryable = true)
        is AdminError.Unavailable -> AdminNotificationSettingsUiState.Error(message = message, retryable = true)
        is AdminError.Backend -> AdminNotificationSettingsUiState.Error(message = message, retryable = true)
    }
}

private fun AdminError.toAdminNotificationSettingsSaveState(): AdminNotificationSettingsSaveState.Error {
    return AdminNotificationSettingsSaveState.Error(
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend,
    )
}

private fun AdminError.toAdminNotificationTestState(templateLabel: String): AdminNotificationTestState.Error {
    return when (this) {
        is AdminError.Validation,
        is AdminError.Permission,
        is AdminError.Unauthenticated,
        is AdminError.NotFound,
        is AdminError.Conflict -> AdminNotificationTestState.Error(templateLabel, message, retryable = false)
        is AdminError.Unavailable,
        is AdminError.Backend -> AdminNotificationTestState.Error(templateLabel, message, retryable = true)
    }
}

private fun AuthError.toAdminNotificationSettingsState(): AdminNotificationSettingsUiState.Error {
    return AdminNotificationSettingsUiState.Error(message = message, retryable = isRetryable())
}

private fun AuthError.isRetryable(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private fun notificationSettingsAuditLabel(timestampIso: String, actorUid: String): String {
    val timestampLabel = timestampIso.toNotificationSettingsAuditDateTimeLabel() ?: return ""
    val actorLabel = actorUid.trim().takeIf { it.isNotBlank() }?.toShortAuditUid()?.let { " por $it" }.orEmpty()
    return "Atualizado $timestampLabel$actorLabel"
}

private fun String.toNotificationSettingsAuditDateTimeLabel(): String? {
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

private fun String.isAdminNotificationTime(): Boolean {
    return Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").matches(trim())
}

private fun String.isAdminNotificationTimeZone(): Boolean {
    val value = trim()
    return value.length in 1..80 && Regex("^[A-Za-z_]+(/[A-Za-z0-9_+\\-]+)*$").matches(value)
}

internal fun defaultNotificationTemplateForms(): List<AdminNotificationTemplateForm> {
    return defaultNotificationTemplates().map { it.toForm() }
}

private fun defaultNotificationTemplates(): List<AdminNotificationTemplateConfig> {
    return listOf(
        AdminNotificationTemplateConfig(
            key = "booking_request",
            label = "Pedido recebido",
            enabled = true,
            title = "Pedido de marcação recebido",
            body = "Recebemos o seu pedido e vamos confirmar a disponibilidade.",
        ),
        AdminNotificationTemplateConfig(
            key = "booking_accepted",
            label = "Marcação aceite",
            enabled = true,
            title = "Marcação confirmada",
            body = "A sua marcação foi confirmada. Até breve!",
        ),
        AdminNotificationTemplateConfig(
            key = "booking_rejected",
            label = "Marcação rejeitada",
            enabled = true,
            title = "Não foi possível confirmar a marcação",
            body = "Não conseguimos confirmar esta marcação. Consulte os detalhes na app.",
        ),
        AdminNotificationTemplateConfig(
            key = "booking_expired",
            label = "Pedido expirado",
            enabled = true,
            title = "Pedido de marcação expirado",
            body = "O pedido expirou antes da confirmação. Pode escolher outro horário na app.",
        ),
        AdminNotificationTemplateConfig(
            key = "booking_cancelled",
            label = "Marcação cancelada",
            enabled = true,
            title = "Marcação cancelada",
            body = "A sua marcação foi cancelada. Pode escolher outro horário na app.",
        ),
        AdminNotificationTemplateConfig(
            key = "booking_rescheduled",
            label = "Marcação remarcada",
            enabled = true,
            title = "Marcação remarcada",
            body = "A sua marcação foi remarcada para {{slotStart}}. Consulte os detalhes na app.",
        ),
        AdminNotificationTemplateConfig(
            key = "booking_reminder",
            label = "Lembrete de marcação",
            enabled = true,
            title = "A sua lavagem está quase a chegar",
            body = "Tem uma marcação em breve. Consulte a hora e morada na app.",
        ),
        AdminNotificationTemplateConfig(
            key = "review_prompt",
            label = "Pedido de avaliação",
            enabled = true,
            title = "Como correu a lavagem?",
            body = "Avalie o serviço para nos ajudar a melhorar.",
        ),
        AdminNotificationTemplateConfig(
            key = "loyalty_reward",
            label = "Recompensa de fidelização",
            enabled = true,
            title = "Recompensa disponível",
            body = "A sua recompensa {{rewardDescription}} está pronta. Use o código {{rewardCode}} na próxima marcação.",
        ),
        AdminNotificationTemplateConfig(
            key = "admin_pending_booking",
            label = "Alerta admin de pedido",
            enabled = true,
            title = "Novo pedido de marcação",
            body = "{{customerName}} pediu {{serviceName}} para {{slotStart}}.",
        ),
    )
}

private val NotificationTemplateKeys = setOf(
    "booking_request",
    "booking_accepted",
    "booking_rejected",
    "booking_expired",
    "booking_cancelled",
    "booking_rescheduled",
    "booking_reminder",
    "review_prompt",
    "loyalty_reward",
    "admin_pending_booking",
)
