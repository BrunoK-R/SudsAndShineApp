package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.admin.AdminServiceCatalogArchiveRequest
import com.sudsmobile.data.admin.AdminServiceCatalogItem
import com.sudsmobile.data.admin.AdminServiceCatalogMutationRequest
import com.sudsmobile.data.admin.AdminServiceCatalogMutationResult
import com.sudsmobile.data.admin.AdminServiceCatalogResult
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal data class AdminServiceCatalogServiceUi(
    val id: String,
    val name: String,
    val description: String,
    val durationLabel: String,
    val passengerPriceLabel: String,
    val suvPriceLabel: String,
    val iconKey: String,
    val popular: Boolean,
    val active: Boolean,
    val sortOrder: Int,
)

internal data class AdminServiceCatalogForm(
    val originalServiceId: String = "",
    val serviceId: String = "",
    val name: String = "",
    val description: String = "",
    val durationMinutes: String = "30",
    val passengerPrice: String = "",
    val suvPrice: String = "",
    val iconKey: String = "car",
    val popular: Boolean = false,
    val active: Boolean = true,
    val sortOrder: String = "999",
) {
    val isEditingExisting: Boolean
        get() = originalServiceId.isNotBlank()
}

internal sealed interface AdminServiceCatalogUiState {
    data object Idle : AdminServiceCatalogUiState
    data object Loading : AdminServiceCatalogUiState
    data object Unauthenticated : AdminServiceCatalogUiState
    data object NotAdmin : AdminServiceCatalogUiState
    data object Empty : AdminServiceCatalogUiState
    data class Loaded(
        val services: List<AdminServiceCatalogServiceUi>,
        val form: AdminServiceCatalogForm? = null,
    ) : AdminServiceCatalogUiState

    data class Error(val message: String, val retryable: Boolean) : AdminServiceCatalogUiState
}

internal sealed interface AdminServiceCatalogMutationState {
    data object Idle : AdminServiceCatalogMutationState
    data object Saving : AdminServiceCatalogMutationState
    data class Archiving(val serviceId: String) : AdminServiceCatalogMutationState
    data class Success(val message: String) : AdminServiceCatalogMutationState
    data class Error(val message: String, val retryable: Boolean) : AdminServiceCatalogMutationState
}

internal class AdminServiceCatalogViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminServiceCatalogUiState>(AdminServiceCatalogUiState.Idle)
    val uiState: StateFlow<AdminServiceCatalogUiState> = _uiState.asStateFlow()
    private val _mutationState =
        MutableStateFlow<AdminServiceCatalogMutationState>(AdminServiceCatalogMutationState.Idle)
    val mutationState: StateFlow<AdminServiceCatalogMutationState> = _mutationState.asStateFlow()

    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var loadSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedCatalog()
                _uiState.value = AdminServiceCatalogUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedCatalog()
                _uiState.value = currentSessionState.error.toAdminServiceCatalogState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedCatalog()
                _uiState.value = AdminServiceCatalogUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val hasReusableState = _uiState.value is AdminServiceCatalogUiState.Loaded ||
            _uiState.value is AdminServiceCatalogUiState.Empty ||
            _uiState.value is AdminServiceCatalogUiState.NotAdmin
        if (!force && loadedUid == uid && hasReusableState) return
        loadCatalog(force = force)
    }

    fun loadCatalog(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedCatalog()
                _uiState.value = AdminServiceCatalogUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedCatalog()
                _uiState.value = currentSessionState.error.toAdminServiceCatalogState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedCatalog()
                _uiState.value = AdminServiceCatalogUiState.Unauthenticated
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
                _uiState.value = AdminServiceCatalogUiState.Loading
                val nextState = when (val result = adminRepository.getServiceCatalogConfiguration()) {
                    is AdminServiceCatalogResult.Success -> result.config.services.toAdminServiceCatalogState()
                    is AdminServiceCatalogResult.Failure -> result.error.toAdminServiceCatalogState()
                }
                if (requestSequence != loadSequence) return@launch

                val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                if (currentUid == requestedUid) {
                    loadedUid = requestedUid
                    _uiState.value = nextState
                } else {
                    clearLoadedCatalog()
                    _uiState.value = AdminServiceCatalogUiState.Unauthenticated
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
        val services = when (currentState) {
            is AdminServiceCatalogUiState.Loaded -> currentState.services
            AdminServiceCatalogUiState.Empty -> emptyList()
            else -> return
        }
        _uiState.value = AdminServiceCatalogUiState.Loaded(
            services = services,
            form = AdminServiceCatalogForm(sortOrder = nextSortOrder(services).toString()),
        )
        _mutationState.value = AdminServiceCatalogMutationState.Idle
    }

    fun editService(serviceId: String) {
        val currentState = _uiState.value as? AdminServiceCatalogUiState.Loaded ?: return
        val service = currentState.services.firstOrNull { it.id == serviceId } ?: return
        _uiState.value = currentState.copy(form = service.toForm())
        _mutationState.value = AdminServiceCatalogMutationState.Idle
    }

    fun cancelEdit() {
        val currentState = _uiState.value as? AdminServiceCatalogUiState.Loaded ?: return
        _uiState.value = currentState.copy(form = null)
    }

    fun updateForm(form: AdminServiceCatalogForm) {
        val currentState = _uiState.value as? AdminServiceCatalogUiState.Loaded ?: return
        _uiState.value = currentState.copy(form = form)
        _mutationState.value = AdminServiceCatalogMutationState.Idle
    }

    fun save() {
        if (_mutationState.value == AdminServiceCatalogMutationState.Saving) return
        val form = (_uiState.value as? AdminServiceCatalogUiState.Loaded)?.form ?: return
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedCatalog()
            _uiState.value = AdminServiceCatalogUiState.Unauthenticated
            return
        }

        val request = when (val parsed = form.toMutationRequest()) {
            is ParsedServiceCatalogRequest.Invalid -> {
                _mutationState.value = AdminServiceCatalogMutationState.Error(parsed.message, retryable = false)
                return
            }
            is ParsedServiceCatalogRequest.Valid -> parsed.request
        }

        viewModelScope.launch {
            _mutationState.value = AdminServiceCatalogMutationState.Saving
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedCatalog()
                _uiState.value = AdminServiceCatalogUiState.Unauthenticated
                _mutationState.value = AdminServiceCatalogMutationState.Idle
                return@launch
            }

            when (val result = adminRepository.upsertServiceCatalogItem(request)) {
                is AdminServiceCatalogMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = null
                        _mutationState.value = AdminServiceCatalogMutationState.Success("Serviço guardado.")
                        loadCatalog(force = true)
                    } else {
                        clearLoadedCatalog()
                        _uiState.value = AdminServiceCatalogUiState.Unauthenticated
                        _mutationState.value = AdminServiceCatalogMutationState.Idle
                    }
                }
                is AdminServiceCatalogMutationResult.Failure -> {
                    _mutationState.value = result.error.toAdminServiceCatalogMutationState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminServiceCatalogUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedCatalog()
                        _uiState.value = AdminServiceCatalogUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun archive(serviceId: String) {
        val cleanServiceId = serviceId.trim()
        if (cleanServiceId.isBlank()) {
            _mutationState.value = AdminServiceCatalogMutationState.Error(
                message = "O serviço selecionado é inválido.",
                retryable = false,
            )
            return
        }
        val requestedUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (requestedUid == null) {
            clearLoadedCatalog()
            _uiState.value = AdminServiceCatalogUiState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _mutationState.value = AdminServiceCatalogMutationState.Archiving(cleanServiceId)
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                clearLoadedCatalog()
                _uiState.value = AdminServiceCatalogUiState.Unauthenticated
                _mutationState.value = AdminServiceCatalogMutationState.Idle
                return@launch
            }

            when (
                val result = adminRepository.archiveServiceCatalogItem(
                    AdminServiceCatalogArchiveRequest(cleanServiceId),
                )
            ) {
                is AdminServiceCatalogMutationResult.Success -> {
                    val latestUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                    if (latestUid == requestedUid) {
                        loadedUid = null
                        _mutationState.value = AdminServiceCatalogMutationState.Success("Serviço arquivado.")
                        loadCatalog(force = true)
                    } else {
                        clearLoadedCatalog()
                        _uiState.value = AdminServiceCatalogUiState.Unauthenticated
                        _mutationState.value = AdminServiceCatalogMutationState.Idle
                    }
                }
                is AdminServiceCatalogMutationResult.Failure -> {
                    _mutationState.value = result.error.toAdminServiceCatalogMutationState()
                    if (result.error is AdminError.Permission) {
                        _uiState.value = AdminServiceCatalogUiState.NotAdmin
                    } else if (result.error is AdminError.Unauthenticated) {
                        clearLoadedCatalog()
                        _uiState.value = AdminServiceCatalogUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun clearMutationState() {
        _mutationState.value = AdminServiceCatalogMutationState.Idle
    }

    private fun clearLoadedCatalog() {
        loadedUid = null
        loadingUid = null
        loadSequence += 1
    }
}

private sealed interface ParsedServiceCatalogRequest {
    data class Valid(val request: AdminServiceCatalogMutationRequest) : ParsedServiceCatalogRequest
    data class Invalid(val message: String) : ParsedServiceCatalogRequest
}

private fun List<AdminServiceCatalogItem>.toAdminServiceCatalogState(): AdminServiceCatalogUiState {
    val services = map { it.toUi() }
    return if (services.isEmpty()) {
        AdminServiceCatalogUiState.Empty
    } else {
        AdminServiceCatalogUiState.Loaded(services)
    }
}

private fun AdminServiceCatalogItem.toUi(): AdminServiceCatalogServiceUi = AdminServiceCatalogServiceUi(
    id = id,
    name = name.ifBlank { "Serviço sem nome" },
    description = description,
    durationLabel = "$durationMinutes min",
    passengerPriceLabel = passengerPriceCents.toEuroLabel(),
    suvPriceLabel = suvPriceCents.toEuroLabel(),
    iconKey = iconKey.ifBlank { "car" },
    popular = popular,
    active = active,
    sortOrder = sortOrder,
)

private fun AdminServiceCatalogServiceUi.toForm(): AdminServiceCatalogForm = AdminServiceCatalogForm(
    originalServiceId = id,
    serviceId = id,
    name = name,
    description = description,
    durationMinutes = durationLabel.substringBefore(" ").trim(),
    passengerPrice = passengerPriceLabel.removeSuffix(" €").replace(",", "."),
    suvPrice = suvPriceLabel.removeSuffix(" €").replace(",", "."),
    iconKey = iconKey,
    popular = popular,
    active = active,
    sortOrder = sortOrder.toString(),
)

private fun AdminServiceCatalogForm.toMutationRequest(): ParsedServiceCatalogRequest {
    val duration = durationMinutes.trim().toIntOrNull()
        ?: return ParsedServiceCatalogRequest.Invalid("Indique uma duração válida.")
    val passengerPriceCents = passengerPrice.toPriceCentsOrNull()
        ?: return ParsedServiceCatalogRequest.Invalid("Indique o preço para ligeiros.")
    val suvPriceCents = suvPrice.toPriceCentsOrNull()
        ?: return ParsedServiceCatalogRequest.Invalid("Indique o preço para SUV.")
    val sortOrderValue = sortOrder.trim().toIntOrNull()
        ?: return ParsedServiceCatalogRequest.Invalid("Indique uma ordenação válida.")

    return ParsedServiceCatalogRequest.Valid(
        AdminServiceCatalogMutationRequest(
            serviceId = originalServiceId.ifBlank { serviceId.trim() },
            name = name,
            description = description,
            durationMinutes = duration,
            passengerPriceCents = passengerPriceCents,
            suvPriceCents = suvPriceCents,
            iconKey = iconKey,
            popular = popular,
            active = active,
            sortOrder = sortOrderValue,
        ),
    )
}

private fun String.toPriceCentsOrNull(): Int? {
    val normalized = trim()
        .removeSuffix("€")
        .trim()
        .replace(",", ".")
    if (normalized.isBlank()) return null
    val euros = normalized.toDoubleOrNull() ?: return null
    if (euros < 0.0 || euros > 1000.0) return null
    return (euros * 100).roundToInt()
}

private fun Int.toEuroLabel(): String {
    val euros = this.coerceAtLeast(0) / 100
    val cents = this.coerceAtLeast(0) % 100
    return "$euros,${cents.toString().padStart(2, '0')} €"
}

private fun nextSortOrder(services: List<AdminServiceCatalogServiceUi>): Int {
    return ((services.maxOfOrNull { it.sortOrder } ?: 0) + 10).coerceAtMost(9999)
}

private fun AdminError.toAdminServiceCatalogState(): AdminServiceCatalogUiState {
    return when (this) {
        is AdminError.Permission -> AdminServiceCatalogUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminServiceCatalogUiState.Unauthenticated
        is AdminError.Unavailable,
        is AdminError.Backend -> AdminServiceCatalogUiState.Error(message = message, retryable = true)
        is AdminError.Validation,
        is AdminError.NotFound,
        is AdminError.Conflict -> AdminServiceCatalogUiState.Error(message = message, retryable = false)
    }
}

private fun AdminError.toAdminServiceCatalogMutationState(): AdminServiceCatalogMutationState.Error {
    return AdminServiceCatalogMutationState.Error(
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend || this is AdminError.Conflict,
    )
}

private fun AuthError.toAdminServiceCatalogState(): AdminServiceCatalogUiState.Error {
    return AdminServiceCatalogUiState.Error(
        message = message,
        retryable = this is AuthError.Unavailable || this is AuthError.Backend,
    )
}
