package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminAvailabilityConfig
import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
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
    val overrideDate: String = "",
    val overrideMaxBookingsPerSlot: String = "",
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
                is AdminCapacityOverrideMutationResult.Failure -> handleCapacityOverrideFailure(result.error)
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
                is AdminCapacityOverrideMutationResult.Failure -> handleCapacityOverrideFailure(result.error)
            }
        }
    }

    fun clearSaveState() {
        _saveState.value = AdminAvailabilitySaveState.Idle
    }

    private fun handleCapacityOverrideFailure(error: AdminError) {
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
)

private fun AdminCapacityOverrideItem.toUi(): AdminCapacityOverrideUi = AdminCapacityOverrideUi(
    date = date,
    maxBookingsPerSlot = maxBookingsPerSlot,
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
