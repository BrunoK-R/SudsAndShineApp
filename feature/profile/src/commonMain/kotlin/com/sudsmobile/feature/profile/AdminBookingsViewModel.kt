package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingRequest
import com.sudsmobile.data.admin.AdminBookingRequestsResult
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.admin.AdminRoleResult
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingReservationExtra
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface AdminAccessUiState {
    data object Idle : AdminAccessUiState
    data object Loading : AdminAccessUiState
    data object NotAdmin : AdminAccessUiState
    data object Admin : AdminAccessUiState
    data class Error(val message: String, val retryable: Boolean) : AdminAccessUiState
}

internal data class AdminBookingRequestUi(
    val id: String,
    val reference: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val service: String,
    val date: String,
    val time: String,
    val vehicle: String,
    val price: String,
    val paymentStatus: String,
    val extras: List<AdminBookingExtraUi>,
    val notes: String,
    val createdAt: String,
    val expiresAt: String,
    val loyaltyRewardApplied: Boolean,
)

internal data class AdminBookingExtraUi(
    val name: String,
    val price: String,
)

internal sealed interface AdminBookingsUiState {
    data object Idle : AdminBookingsUiState
    data object Loading : AdminBookingsUiState
    data object Unauthenticated : AdminBookingsUiState
    data object NotAdmin : AdminBookingsUiState
    data object Empty : AdminBookingsUiState
    data class Loaded(val requests: List<AdminBookingRequestUi>) : AdminBookingsUiState
    data class Error(val message: String, val retryable: Boolean) : AdminBookingsUiState
}

internal sealed interface AdminBookingDecisionUiState {
    data object Idle : AdminBookingDecisionUiState
    data class Loading(val reservationId: String, val action: AdminBookingDecisionAction) : AdminBookingDecisionUiState
    data class Success(val message: String) : AdminBookingDecisionUiState
    data class Error(
        val reservationId: String,
        val message: String,
        val retryable: Boolean,
    ) : AdminBookingDecisionUiState
}

internal enum class AdminBookingDecisionAction {
    Accept,
    Reject,
}

internal class AdminAccessViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _uiState = MutableStateFlow<AdminAccessUiState>(AdminAccessUiState.Idle)
    val uiState: StateFlow<AdminAccessUiState> = _uiState.asStateFlow()
    private var loadedUid: String? = null
    private var loadingUid: String? = null

    fun refreshForSession() {
        when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                loadedUid = null
                loadingUid = null
                _uiState.value = AdminAccessUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                loadedUid = null
                loadingUid = null
                _uiState.value = currentSessionState.error.toAdminAccessError()
            }
            AuthSessionState.Unauthenticated -> {
                loadedUid = null
                loadingUid = null
                _uiState.value = AdminAccessUiState.NotAdmin
            }
            is AuthSessionState.Authenticated -> {
                val uid = currentSessionState.session.user.uid
                if (loadedUid == uid && _uiState.value == AdminAccessUiState.Admin) return
                syncRole(uid)
            }
        }
    }

    private fun syncRole(requestedUid: String) {
        if (loadingUid == requestedUid) return

        loadingUid = requestedUid
        viewModelScope.launch {
            _uiState.value = AdminAccessUiState.Loading
            val nextState = when (val result = adminRepository.syncMyRole()) {
                is AdminRoleResult.Success -> {
                    if (result.role.isAdmin) AdminAccessUiState.Admin else AdminAccessUiState.NotAdmin
                }
                is AdminRoleResult.Failure -> result.error.toAdminAccessState()
            }

            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedUid = requestedUid
                _uiState.value = nextState
            } else {
                loadedUid = null
                _uiState.value = AdminAccessUiState.NotAdmin
            }
            if (loadingUid == requestedUid) {
                loadingUid = null
            }
        }
    }
}

internal class AdminBookingsViewModel(
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
    private val bookingChangeNotifier: BookingChangeNotifier = MutableBookingChangeNotifier(),
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision
    private val _uiState = MutableStateFlow<AdminBookingsUiState>(AdminBookingsUiState.Idle)
    val uiState: StateFlow<AdminBookingsUiState> = _uiState.asStateFlow()
    private val _decisionState =
        MutableStateFlow<AdminBookingDecisionUiState>(AdminBookingDecisionUiState.Idle)
    val decisionState: StateFlow<AdminBookingDecisionUiState> = _decisionState.asStateFlow()
    private var loadedUid: String? = null
    private var loadedRevision: Long? = null
    private var loadingUid: String? = null
    private var loadingRevision: Long? = null
    private var requestsSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedRequests()
                _uiState.value = AdminBookingsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedRequests()
                _uiState.value = currentSessionState.error.toAdminBookingsState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedRequests()
                _uiState.value = AdminBookingsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val revision = bookingRevision.value
        val hasReusableState = _uiState.value is AdminBookingsUiState.Loaded ||
            _uiState.value is AdminBookingsUiState.Empty ||
            _uiState.value is AdminBookingsUiState.NotAdmin
        if (!force && loadedUid == uid && loadedRevision == revision && hasReusableState) return
        loadRequests()
    }

    fun loadRequests() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedRequests()
                _uiState.value = AdminBookingsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedRequests()
                _uiState.value = currentSessionState.error.toAdminBookingsState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedRequests()
                _uiState.value = AdminBookingsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val requestedUid = session.session.user.uid
        val requestedRevision = bookingRevision.value
        val sameRequestInFlight = loadingUid == requestedUid && loadingRevision == requestedRevision
        if (sameRequestInFlight) return

        val requestSequence = ++requestsSequence
        loadingUid = requestedUid
        loadingRevision = requestedRevision
        viewModelScope.launch {
            try {
                _uiState.value = AdminBookingsUiState.Loading
                val nextState = when (val result = adminRepository.getPendingBookingRequests()) {
                    is AdminBookingRequestsResult.Success -> result.requests.toAdminBookingsState()
                    is AdminBookingRequestsResult.Failure -> result.error.toAdminBookingsState()
                }
                if (requestSequence != requestsSequence) return@launch

                val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
                if (currentUid == requestedUid) {
                    loadedUid = requestedUid
                    loadedRevision = requestedRevision
                    _uiState.value = nextState
                } else {
                    clearLoadedRequests()
                    _uiState.value = AdminBookingsUiState.Unauthenticated
                }
            } finally {
                if (requestSequence == requestsSequence) {
                    loadingUid = null
                    loadingRevision = null
                }
            }
        }
    }

    fun acceptRequest(reservationId: String) {
        decideRequest(
            reservationId = reservationId,
            action = AdminBookingDecisionAction.Accept,
            rejectionReason = "",
        )
    }

    fun rejectRequest(reservationId: String, rejectionReason: String) {
        decideRequest(
            reservationId = reservationId,
            action = AdminBookingDecisionAction.Reject,
            rejectionReason = rejectionReason,
        )
    }

    fun clearDecisionState() {
        _decisionState.value = AdminBookingDecisionUiState.Idle
    }

    private fun decideRequest(
        reservationId: String,
        action: AdminBookingDecisionAction,
        rejectionReason: String,
    ) {
        if (_decisionState.value is AdminBookingDecisionUiState.Loading) return

        val cleanReservationId = reservationId.trim()
        if (cleanReservationId.isBlank()) {
            _decisionState.value = AdminBookingDecisionUiState.Error(
                reservationId = reservationId,
                message = "A marcação selecionada é inválida.",
                retryable = false,
            )
            return
        }

        viewModelScope.launch {
            _decisionState.value = AdminBookingDecisionUiState.Loading(cleanReservationId, action)
            val request = AdminBookingDecisionRequest(
                reservationId = cleanReservationId,
                rejectionReason = rejectionReason,
            )
            val result = when (action) {
                AdminBookingDecisionAction.Accept -> adminRepository.acceptBookingRequest(request)
                AdminBookingDecisionAction.Reject -> adminRepository.rejectBookingRequest(request)
            }
            _decisionState.value = when (result) {
                is AdminBookingDecisionResult.Success -> {
                    loadedUid = null
                    loadedRevision = null
                    loadRequests()
                    val message = when (action) {
                        AdminBookingDecisionAction.Accept -> "Marcação aceite."
                        AdminBookingDecisionAction.Reject -> "Marcação rejeitada."
                    }
                    AdminBookingDecisionUiState.Success(message)
                }
                is AdminBookingDecisionResult.Failure -> result.error.toDecisionState(cleanReservationId)
            }
        }
    }

    private fun clearLoadedRequests() {
        loadedUid = null
        loadedRevision = null
        loadingUid = null
        loadingRevision = null
        requestsSequence += 1
    }
}

private fun List<AdminBookingRequest>.toAdminBookingsState(): AdminBookingsUiState {
    val requests = map { it.toUi() }
    return if (requests.isEmpty()) {
        AdminBookingsUiState.Empty
    } else {
        AdminBookingsUiState.Loaded(requests)
    }
}

private fun AdminBookingRequest.toUi(): AdminBookingRequestUi = AdminBookingRequestUi(
    id = id,
    reference = reservationCode.ifBlank { id },
    customerName = customerName.ifBlank { "Cliente" },
    customerEmail = customerEmail,
    customerPhone = customerPhone,
    service = serviceName.ifBlank { "Serviço" },
    date = slotStartIso.toDateLabel(),
    time = "${slotStartIso.toTimeLabel()} - ${slotEndIso.toTimeLabel()}",
    vehicle = vehicleLabel.ifBlank { vehicleType.toVehicleLabel() },
    price = priceCents?.toEuroLabel() ?: "A confirmar",
    paymentStatus = paymentStatus.toPaymentLabel(),
    extras = extras.toAdminExtraUi(),
    notes = notes,
    createdAt = createdAtIso.toDateTimeLabel() ?: "Data a confirmar",
    expiresAt = pendingExpiresAtIso?.toDateTimeLabel() ?: "Sem expiração automática",
    loyaltyRewardApplied = loyaltyRewardApplied,
)

private fun List<BookingReservationExtra>.toAdminExtraUi(): List<AdminBookingExtraUi> {
    return mapNotNull { extra ->
        val name = extra.name.trim()
        if (name.isBlank()) return@mapNotNull null
        AdminBookingExtraUi(
            name = name,
            price = if (extra.priceCents > 0) extra.priceCents.toEuroLabel() else "Incluído",
        )
    }.take(12)
}

private fun AdminError.toAdminAccessState(): AdminAccessUiState {
    return when (this) {
        is AdminError.Permission -> AdminAccessUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminAccessUiState.NotAdmin
        is AdminError.Unavailable,
        is AdminError.Backend -> AdminAccessUiState.Error(message = message, retryable = true)
        is AdminError.Validation,
        is AdminError.NotFound,
        is AdminError.Conflict -> AdminAccessUiState.Error(message = message, retryable = false)
    }
}

private fun AdminError.toAdminBookingsState(): AdminBookingsUiState {
    return when (this) {
        is AdminError.Permission -> AdminBookingsUiState.NotAdmin
        is AdminError.Unauthenticated -> AdminBookingsUiState.Unauthenticated
        is AdminError.Unavailable,
        is AdminError.Backend -> AdminBookingsUiState.Error(message = message, retryable = true)
        is AdminError.Validation,
        is AdminError.NotFound,
        is AdminError.Conflict -> AdminBookingsUiState.Error(message = message, retryable = false)
    }
}

private fun AdminError.toDecisionState(reservationId: String): AdminBookingDecisionUiState.Error {
    return AdminBookingDecisionUiState.Error(
        reservationId = reservationId,
        message = message,
        retryable = this is AdminError.Unavailable || this is AdminError.Backend || this is AdminError.Conflict,
    )
}

private fun AuthError.toAdminAccessError(): AdminAccessUiState.Error = AdminAccessUiState.Error(
    message = message,
    retryable = isRetryableSessionError(),
)

private fun AuthError.toAdminBookingsState(): AdminBookingsUiState.Error = AdminBookingsUiState.Error(
    message = message,
    retryable = isRetryableSessionError(),
)

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private fun String.toPaymentLabel(): String {
    return when (
        trim()
            .lowercase()
            .replace("-", "_")
            .replace(" ", "_")
    ) {
        "paid", "pago", "succeeded", "complete", "completed" -> "Pago"
        "covered_by_loyalty", "loyalty", "reward", "recompensa" -> "Recompensa"
        "failed", "declined", "falhou" -> "Falhou"
        "refunded", "refund", "reembolsado" -> "Reembolsado"
        else -> "Pendente"
    }
}

private fun String.toVehicleLabel(): String = when (lowercase()) {
    "suv" -> "SUV"
    "passageiros", "passenger" -> "Passageiros"
    else -> replaceFirstChar { it.titlecase() }
}

private fun String.toDateLabel(): String {
    val date = substringBefore("T")
    val parts = date.split("-")
    if (parts.size != 3) return date.ifBlank { "Data a confirmar" }
    val year = parts[0]
    val month = parts[1].toIntOrNull()?.let { monthNames.getOrNull(it - 1) } ?: return date
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "$day de $month, $year"
}

private fun String.toTimeLabel(): String {
    val time = substringAfter("T", missingDelimiterValue = "")
    return time.takeIf { it.length >= 5 }?.take(5) ?: "Hora a confirmar"
}

private fun String.toDateTimeLabel(): String? {
    if (isBlank()) return null
    val date = toDateLabel().takeUnless { it == "Data a confirmar" } ?: return null
    val time = toTimeLabel().takeUnless { it == "Hora a confirmar" }
    return if (time == null) date else "$date às $time"
}

private fun Int.toEuroLabel(): String {
    val euros = this / 100
    val remainder = this % 100
    return "$euros,${remainder.toString().padStart(2, '0')} €"
}

private val monthNames = listOf(
    "janeiro",
    "fevereiro",
    "março",
    "abril",
    "maio",
    "junho",
    "julho",
    "agosto",
    "setembro",
    "outubro",
    "novembro",
    "dezembro",
)
