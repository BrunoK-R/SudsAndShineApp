package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminAvailabilityConfig
import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
import com.sudsmobile.data.admin.AdminBlockedSlotClearRequest
import com.sudsmobile.data.admin.AdminBlockedSlotItem
import com.sudsmobile.data.admin.AdminBlockedSlotMutationResult
import com.sudsmobile.data.admin.AdminBlockedSlotUpsertRequest
import com.sudsmobile.data.admin.AdminBusinessOpeningHours
import com.sudsmobile.data.admin.AdminCapacityOverrideClearRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideItem
import com.sudsmobile.data.admin.AdminCapacityOverrideMutationResult
import com.sudsmobile.data.admin.AdminCapacityOverrideUpsertRequest
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class AdminAvailabilityForm(
    val defaultMaxBookingsPerSlot: String = "",
    val openingHoursText: String = "",
    val capacityOverrides: List<AdminCapacityOverrideUi> = emptyList(),
    val blockedSlots: List<AdminBlockedSlotUi> = emptyList(),
    val overrideDate: String = "",
    val overrideMaxBookingsPerSlot: String = "",
    val blockedDate: String = "",
    val blockedStartTime: String = "09:00",
    val blockedEndTime: String = "10:00",
    val blockedReason: String = "",
)

internal data class AdminCapacityOverrideUi(
    val date: String,
    val maxBookingsPerSlot: Int,
) {
    val capacityLabel: String
        get() = if (maxBookingsPerSlot == 0) {
            "Fechado"
        } else {
            "$maxBookingsPerSlot por horário"
        }
}

internal data class AdminBlockedSlotUi(
    val blockedSlotId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val reason: String,
) {
    val timeLabel: String
        get() = "$startTime - $endTime"
}

internal sealed interface AdminAvailabilityUiState {
    data object Idle : AdminAvailabilityUiState
    data object Loading : AdminAvailabilityUiState
    data object Unauthenticated : AdminAvailabilityUiState
    data object NotAdmin : AdminAvailabilityUiState
    data class Loaded(val form: AdminAvailabilityForm) : AdminAvailabilityUiState
    data class Error(val message: String, val retryable: Boolean) : AdminAvailabilityUiState
}

internal sealed interface AdminAvailabilitySaveState {
    data object Idle : AdminAvailabilitySaveState
    data object Saving : AdminAvailabilitySaveState
    data class Success(val message: String) : AdminAvailabilitySaveState
    data class Error(val message: String, val retryable: Boolean) : AdminAvailabilitySaveState
}

internal class AdminAvailabilityViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminAvailabilityUiState>(AdminAvailabilityUiState.Idle)
    val uiState: StateFlow<AdminAvailabilityUiState> = _uiState.asStateFlow()
    private val _saveState = MutableStateFlow<AdminAvailabilitySaveState>(AdminAvailabilitySaveState.Idle)
    val saveState: StateFlow<AdminAvailabilitySaveState> = _saveState.asStateFlow()
    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminAvailabilityState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        if (!force && loadedUid == uid && _uiState.value is AdminAvailabilityUiState.Loaded) return
        loadConfiguration()
    }

    fun loadConfiguration() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedConfig()
                _uiState.value = currentSessionState.error.toAdminAvailabilityState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Unauthenticated
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
                _uiState.value = AdminAvailabilityUiState.Loading
                val nextState = when (val result = adminRepository.getAvailabilityConfiguration()) {
                    is AdminAvailabilityResult.Success -> AdminAvailabilityUiState.Loaded(result.config.toForm())
                    is AdminAvailabilityResult.Failure -> result.error.toAdminAvailabilityState()
                }
                if (requestSequence != loadSequence) return@launch

                val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                if (currentUid == requestedUid) {
                    loadedUid = requestedUid
                    _uiState.value = nextState
                } else {
                    clearLoadedConfig()
                    _uiState.value = AdminAvailabilityUiState.Unauthenticated
                }
            } finally {
                if (requestSequence == loadSequence) {
                    loadingUid = null
                }
            }
        }
    }

    fun updateForm(form: AdminAvailabilityForm) {
        if (_uiState.value is AdminAvailabilityUiState.Loaded) {
            _uiState.value = AdminAvailabilityUiState.Loaded(form)
            _saveState.value = AdminAvailabilitySaveState.Idle
        }
    }

    fun save() {
        if (_saveState.value == AdminAvailabilitySaveState.Saving) return
        val form = (_uiState.value as? AdminAvailabilityUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminAvailabilityUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toUpdateRequest()) {
            is ParsedAvailabilityRequest.Invalid -> {
                _saveState.value = AdminAvailabilitySaveState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedAvailabilityRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _saveState.value = AdminAvailabilitySaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Unauthenticated
                _saveState.value = AdminAvailabilitySaveState.Idle
                return@launch
            }

            when (val result = adminRepository.updateAvailabilityConfiguration(request)) {
                is AdminAvailabilityResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = requestedUid
                        _uiState.value = AdminAvailabilityUiState.Loaded(result.config.toForm())
                        _saveState.value = AdminAvailabilitySaveState.Success("Disponibilidade guardada.")
                    } else {
                        clearLoadedConfig()
                        _uiState.value = AdminAvailabilityUiState.Unauthenticated
                        _saveState.value = AdminAvailabilitySaveState.Idle
                    }
                }
                is AdminAvailabilityResult.Failure -> {
                    _saveState.value = result.error.toAdminAvailabilitySaveState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminAvailabilityUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedConfig()
                        _uiState.value = AdminAvailabilityUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun saveCapacityOverride() {
        if (_saveState.value == AdminAvailabilitySaveState.Saving) return
        val form = (_uiState.value as? AdminAvailabilityUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminAvailabilityUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toCapacityOverrideRequest()) {
            is ParsedCapacityOverrideRequest.Invalid -> {
                _saveState.value = AdminAvailabilitySaveState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedCapacityOverrideRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _saveState.value = AdminAvailabilitySaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Unauthenticated
                _saveState.value = AdminAvailabilitySaveState.Idle
                return@launch
            }

            when (val result = adminRepository.upsertCapacityOverride(request)) {
                is AdminCapacityOverrideMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = null
                        _saveState.value = AdminAvailabilitySaveState.Success("Exceção de capacidade guardada.")
                        loadConfiguration()
                    } else {
                        clearLoadedConfig()
                        _uiState.value = AdminAvailabilityUiState.Unauthenticated
                        _saveState.value = AdminAvailabilitySaveState.Idle
                    }
                }
                is AdminCapacityOverrideMutationResult.Failure -> handleAvailabilityMutationFailure(result.error)
            }
        }
    }

    fun clearCapacityOverride(date: String) {
        if (_saveState.value == AdminAvailabilitySaveState.Saving) return
        val cleanDate = date.trim()
        if (!cleanDate.isValidDateId()) {
            _saveState.value = AdminAvailabilitySaveState.Error(
                message = "A exceção selecionada é inválida.",
                retryable = false,
            )
            return
        }
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminAvailabilityUiState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _saveState.value = AdminAvailabilitySaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Unauthenticated
                _saveState.value = AdminAvailabilitySaveState.Idle
                return@launch
            }

            when (
                val result = adminRepository.clearCapacityOverride(
                    AdminCapacityOverrideClearRequest(cleanDate),
                )
            ) {
                is AdminCapacityOverrideMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = null
                        _saveState.value = AdminAvailabilitySaveState.Success("Exceção de capacidade limpa.")
                        loadConfiguration()
                    } else {
                        clearLoadedConfig()
                        _uiState.value = AdminAvailabilityUiState.Unauthenticated
                        _saveState.value = AdminAvailabilitySaveState.Idle
                    }
                }
                is AdminCapacityOverrideMutationResult.Failure -> handleAvailabilityMutationFailure(result.error)
            }
        }
    }

    fun saveBlockedSlot() {
        if (_saveState.value == AdminAvailabilitySaveState.Saving) return
        val form = (_uiState.value as? AdminAvailabilityUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminAvailabilityUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toBlockedSlotRequest()) {
            is ParsedBlockedSlotRequest.Invalid -> {
                _saveState.value = AdminAvailabilitySaveState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedBlockedSlotRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _saveState.value = AdminAvailabilitySaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Unauthenticated
                _saveState.value = AdminAvailabilitySaveState.Idle
                return@launch
            }

            when (val result = adminRepository.upsertBlockedSlot(request)) {
                is AdminBlockedSlotMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = null
                        _saveState.value = AdminAvailabilitySaveState.Success("Bloqueio de horário guardado.")
                        loadConfiguration()
                    } else {
                        clearLoadedConfig()
                        _uiState.value = AdminAvailabilityUiState.Unauthenticated
                        _saveState.value = AdminAvailabilitySaveState.Idle
                    }
                }
                is AdminBlockedSlotMutationResult.Failure -> handleAvailabilityMutationFailure(result.error)
            }
        }
    }

    fun clearBlockedSlot(blockedSlotId: String) {
        if (_saveState.value == AdminAvailabilitySaveState.Saving) return
        val cleanBlockedSlotId = blockedSlotId.trim()
        if (!cleanBlockedSlotId.isValidBlockedSlotId()) {
            _saveState.value = AdminAvailabilitySaveState.Error(
                message = "O bloqueio selecionado é inválido.",
                retryable = false,
            )
            return
        }
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedConfig()
            _uiState.value = AdminAvailabilityUiState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _saveState.value = AdminAvailabilitySaveState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedConfig()
                _uiState.value = AdminAvailabilityUiState.Unauthenticated
                _saveState.value = AdminAvailabilitySaveState.Idle
                return@launch
            }

            when (
                val result = adminRepository.clearBlockedSlot(
                    AdminBlockedSlotClearRequest(cleanBlockedSlotId),
                )
            ) {
                is AdminBlockedSlotMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = null
                        _saveState.value = AdminAvailabilitySaveState.Success("Bloqueio de horário limpo.")
                        loadConfiguration()
                    } else {
                        clearLoadedConfig()
                        _uiState.value = AdminAvailabilityUiState.Unauthenticated
                        _saveState.value = AdminAvailabilitySaveState.Idle
                    }
                }
                is AdminBlockedSlotMutationResult.Failure -> handleAvailabilityMutationFailure(result.error)
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = AdminAvailabilitySaveState.Idle
    }

    private fun handleAvailabilityMutationFailure(error: AdminError) {
        _saveState.value = error.toAdminAvailabilitySaveState()
        if (error is AdminError.Permission) {
            _uiState.value = AdminAvailabilityUiState.NotAdmin
        } else if (error is AdminError.Unauthenticated) {
            clearLoadedConfig()
            _uiState.value = AdminAvailabilityUiState.Unauthenticated
        }
    }

    private fun clearLoadedConfig() {
        loadedUid = null
        loadingUid = null
        loadSequence += 1
    }
}

private sealed interface ParsedAvailabilityRequest {
    data class Valid(val request: AdminAvailabilityUpdateRequest) : ParsedAvailabilityRequest
    data class Invalid(val message: String) : ParsedAvailabilityRequest
}

private sealed interface ParsedCapacityOverrideRequest {
    data class Valid(val request: AdminCapacityOverrideUpsertRequest) : ParsedCapacityOverrideRequest
    data class Invalid(val message: String) : ParsedCapacityOverrideRequest
}

private sealed interface ParsedBlockedSlotRequest {
    data class Valid(val request: AdminBlockedSlotUpsertRequest) : ParsedBlockedSlotRequest
    data class Invalid(val message: String) : ParsedBlockedSlotRequest
}

private fun AdminAvailabilityConfig.toForm(): AdminAvailabilityForm = AdminAvailabilityForm(
    defaultMaxBookingsPerSlot = defaultMaxBookingsPerSlot.toString(),
    openingHoursText = openingHours.joinToString("\n") { hours ->
        listOf(
            hours.dayLabel,
            hours.hoursLabel,
            if (hours.closed) "fechado" else "",
        ).filter { it.isNotBlank() }.joinToString(" | ")
    },
    capacityOverrides = capacityOverrides.map { it.toUi() },
    blockedSlots = blockedSlots.map { it.toUi() },
)

private fun AdminCapacityOverrideItem.toUi(): AdminCapacityOverrideUi = AdminCapacityOverrideUi(
    date = date,
    maxBookingsPerSlot = maxBookingsPerSlot,
)

private fun AdminBlockedSlotItem.toUi(): AdminBlockedSlotUi = AdminBlockedSlotUi(
    blockedSlotId = blockedSlotId,
    date = date,
    startTime = slotStartIso.isoTimeLabel(),
    endTime = slotEndIso.isoTimeLabel(),
    reason = reason,
)

private fun AdminAvailabilityForm.toUpdateRequest(): ParsedAvailabilityRequest {
    val capacity = defaultMaxBookingsPerSlot.trim().toIntOrNull()
        ?: return ParsedAvailabilityRequest.Invalid("Indique uma capacidade válida.")
    val openingHours = openingHoursText.parseAvailabilityOpeningHours()
        ?: return ParsedAvailabilityRequest.Invalid("Revise os horários.")

    return ParsedAvailabilityRequest.Valid(
        AdminAvailabilityUpdateRequest(
            defaultMaxBookingsPerSlot = capacity,
            openingHours = openingHours,
        ),
    )
}

private fun AdminAvailabilityForm.toBlockedSlotRequest(): ParsedBlockedSlotRequest {
    val cleanDate = blockedDate.trim()
    if (!cleanDate.isValidDateId()) {
        return ParsedBlockedSlotRequest.Invalid("Indique a data do bloqueio no formato AAAA-MM-DD.")
    }
    val cleanStartTime = blockedStartTime.trim()
    val cleanEndTime = blockedEndTime.trim()
    val startMinutes = cleanStartTime.minutesSinceMidnightOrNull()
        ?: return ParsedBlockedSlotRequest.Invalid("Indique a hora de início no formato HH:MM.")
    val endMinutes = cleanEndTime.minutesSinceMidnightOrNull()
        ?: return ParsedBlockedSlotRequest.Invalid("Indique a hora de fim no formato HH:MM.")
    if (endMinutes <= startMinutes) {
        return ParsedBlockedSlotRequest.Invalid("A hora de fim deve ser posterior ao início.")
    }

    return ParsedBlockedSlotRequest.Valid(
        AdminBlockedSlotUpsertRequest(
            date = cleanDate,
            slotStartIso = "${cleanDate}T${cleanStartTime}:00.000Z",
            slotEndIso = "${cleanDate}T${cleanEndTime}:00.000Z",
            reason = blockedReason.trim(),
        ),
    )
}

private fun AdminAvailabilityForm.toCapacityOverrideRequest(): ParsedCapacityOverrideRequest {
    val cleanDate = overrideDate.trim()
    if (!cleanDate.isValidDateId()) {
        return ParsedCapacityOverrideRequest.Invalid("Indique a data da exceção no formato AAAA-MM-DD.")
    }
    val capacity = overrideMaxBookingsPerSlot.trim().toIntOrNull()
        ?: return ParsedCapacityOverrideRequest.Invalid("Indique uma capacidade para a exceção.")
    if (capacity !in 0..20) {
        return ParsedCapacityOverrideRequest.Invalid("A capacidade da exceção deve estar entre 0 e 20.")
    }

    return ParsedCapacityOverrideRequest.Valid(
        AdminCapacityOverrideUpsertRequest(
            date = cleanDate,
            maxBookingsPerSlot = capacity,
        ),
    )
}

private fun String.parseAvailabilityOpeningHours(): List<AdminBusinessOpeningHours>? {
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

private fun String.isValidDateId(): Boolean {
    val value = trim()
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)) return false
    val year = value.substring(0, 4).toIntOrNull() ?: return false
    val month = value.substring(5, 7).toIntOrNull() ?: return false
    val day = value.substring(8, 10).toIntOrNull() ?: return false
    if (month !in 1..12) return false
    val maxDay = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year.isLeapYear()) 29 else 28
        else -> return false
    }
    return day in 1..maxDay
}

private fun String.isValidBlockedSlotId(): Boolean {
    return Regex("^[A-Za-z0-9_-]{1,120}$").matches(trim()) && !contains("/")
}

private fun String.minutesSinceMidnightOrNull(): Int? {
    val value = trim()
    if (!Regex("^\\d{2}:\\d{2}$").matches(value)) return null
    val hour = value.substring(0, 2).toIntOrNull() ?: return null
    val minute = value.substring(3, 5).toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun String.isoTimeLabel(): String {
    val value = trim()
    return if (value.length >= 16 && value[10] == 'T') {
        value.substring(11, 16)
    } else {
        ""
    }
}

private fun Int.isLeapYear(): Boolean {
    return this % 4 == 0 && (this % 100 != 0 || this % 400 == 0)
}

private fun AdminError.toAdminAvailabilityState(): AdminAvailabilityUiState {
    return when (this) {
        is AdminError.Permission -> AdminAvailabilityUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminAvailabilityUiState.Unauthenticated
        is AdminError.Unavailable,
        is AdminError.Backend -> AdminAvailabilityUiState.Error(message = message, retryable = true)
        is AdminError.Validation,
        is AdminError.NotFound,
        is AdminError.Conflict -> AdminAvailabilityUiState.Error(message = message, retryable = false)
    }
}

private fun AdminError.toAdminAvailabilitySaveState(): AdminAvailabilitySaveState.Error {
    return AdminAvailabilitySaveState.Error(
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend,
    )
}

private fun AuthError.toAdminAvailabilityState(): AdminAvailabilityUiState.Error {
    return AdminAvailabilityUiState.Error(
        message = message,
        retryable = this is AuthError.Unavailable || this is AuthError.Backend,
    )
}
