package com.sudsmobile.feature.cart

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingAvailabilityError
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilityRequest
import com.sudsmobile.data.booking.BookingAvailabilityResult
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.DefaultBusinessInfo
import com.sudsmobile.data.booking.BookingCancelError
import com.sudsmobile.data.booking.BookingCancelRequest
import com.sudsmobile.data.booking.BookingCancelResult
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingPaymentStatus
import com.sudsmobile.data.booking.BookingRescheduleError
import com.sudsmobile.data.booking.BookingRescheduleRequest
import com.sudsmobile.data.booking.BookingRescheduleResult
import com.sudsmobile.data.booking.BookingReservationStatus
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.booking.bookingPaymentStatus
import com.sudsmobile.data.booking.isCancelableReservation
import com.sudsmobile.data.booking.isCancelledReservation
import com.sudsmobile.data.booking.isReviewableReservation
import com.sudsmobile.data.booking.requiresPayment
import com.sudsmobile.data.booking.toBookingReservationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class BookingStatusUi(val label: String) {
    Pending("A aguardar validação"),
    Confirmed("Confirmado"),
    InProgress("A decorrer"),
    Completed("Concluído"),
    Cancelled("Cancelado"),
    Rejected("Rejeitado"),
    Expired("Expirado"),
    Unknown("A atualizar"),
}

internal data class BookingSummaryUi(
    val id: String,
    val service: String,
    val date: String,
    val time: String,
    val slotStartIso: String,
    val slotEndIso: String,
    val serviceDurationMinutes: Int,
    val vehicle: String,
    val price: String,
    val status: BookingStatusUi,
    val statusDescription: String,
    val icon: ImageVector,
    val showLocation: Boolean,
    val reviewed: Boolean,
    val reviewRating: Int?,
    val reviewTags: List<String>,
    val reviewComment: String,
    val reviewable: Boolean,
    val cancelable: Boolean,
    val paymentLabel: String,
    val requiresPayment: Boolean,
    val auditNotes: List<BookingAuditNoteUi>,
)

internal enum class BookingAuditToneUi {
    Neutral,
    Warning,
}

internal data class BookingAuditNoteUi(
    val title: String,
    val body: String,
    val tone: BookingAuditToneUi = BookingAuditToneUi.Neutral,
)

internal data class CartBusinessInfoUi(
    val addressLine1: String,
    val addressLine2: String,
)

internal sealed interface CartBookingsUiState {
    data object Idle : CartBookingsUiState
    data object Loading : CartBookingsUiState
    data object Unauthenticated : CartBookingsUiState
    data object Empty : CartBookingsUiState
    data class Loaded(
        val upcoming: List<BookingSummaryUi>,
        val completed: List<BookingSummaryUi>,
    ) : CartBookingsUiState
    data class Error(val message: String, val retryable: Boolean) : CartBookingsUiState
}

internal sealed interface CartBusinessInfoUiState {
    data object Idle : CartBusinessInfoUiState
    data object Loading : CartBusinessInfoUiState
    data class Loaded(val info: CartBusinessInfoUi) : CartBusinessInfoUiState
    data class Error(
        val fallbackInfo: CartBusinessInfoUi,
        val message: String,
        val retryable: Boolean,
    ) : CartBusinessInfoUiState
}

internal sealed interface BookingCancellationUiState {
    data object Idle : BookingCancellationUiState
    data class Loading(val reservationId: String) : BookingCancellationUiState
    data class Success(val reservationId: String) : BookingCancellationUiState
    data class Error(
        val reservationId: String,
        val message: String,
        val retryable: Boolean,
    ) : BookingCancellationUiState
}

internal data class BookingRescheduleDraft(
    val reservationId: String,
    val dateId: String,
    val time: String,
    val durationMinutes: Int,
)

internal sealed interface BookingRescheduleAvailabilityUiState {
    data object Idle : BookingRescheduleAvailabilityUiState
    data object Loading : BookingRescheduleAvailabilityUiState
    data class Loaded(val month: BookingAvailabilityMonth) : BookingRescheduleAvailabilityUiState
    data class Empty(val month: BookingAvailabilityMonth) : BookingRescheduleAvailabilityUiState
    data class Error(val message: String, val retryable: Boolean) : BookingRescheduleAvailabilityUiState
}

internal sealed interface BookingRescheduleUiState {
    data object Idle : BookingRescheduleUiState
    data class Loading(val reservationId: String) : BookingRescheduleUiState
    data class Success(val reservationId: String) : BookingRescheduleUiState
    data class Error(
        val reservationId: String,
        val message: String,
        val retryable: Boolean,
        val changeSlot: Boolean,
    ) : BookingRescheduleUiState
}

internal class CartBookingsViewModel(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val businessInfoRepository: BusinessInfoRepository,
    private val bookingChangeNotifier: BookingChangeNotifier = MutableBookingChangeNotifier(),
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision
    private val _uiState = MutableStateFlow<CartBookingsUiState>(CartBookingsUiState.Idle)
    val uiState: StateFlow<CartBookingsUiState> = _uiState.asStateFlow()
    private val _businessInfoState = MutableStateFlow<CartBusinessInfoUiState>(CartBusinessInfoUiState.Idle)
    val businessInfoState: StateFlow<CartBusinessInfoUiState> = _businessInfoState.asStateFlow()
    private val _cancellationState = MutableStateFlow<BookingCancellationUiState>(BookingCancellationUiState.Idle)
    val cancellationState: StateFlow<BookingCancellationUiState> = _cancellationState.asStateFlow()
    private val _rescheduleAvailabilityState =
        MutableStateFlow<BookingRescheduleAvailabilityUiState>(BookingRescheduleAvailabilityUiState.Idle)
    val rescheduleAvailabilityState: StateFlow<BookingRescheduleAvailabilityUiState> =
        _rescheduleAvailabilityState.asStateFlow()
    private val _rescheduleState = MutableStateFlow<BookingRescheduleUiState>(BookingRescheduleUiState.Idle)
    val rescheduleState: StateFlow<BookingRescheduleUiState> = _rescheduleState.asStateFlow()
    private var loadedUid: String? = null
    private var loadedRevision: Long? = null
    private var loadingUid: String? = null
    private var loadingRevision: Long? = null
    private var historyRequestRevision: Long = 0
    private var rescheduleAvailabilityRequestRevision: Long = 0

    fun loadBusinessInfo(force: Boolean = false) {
        if (_businessInfoState.value is CartBusinessInfoUiState.Loading) return
        if (!force && _businessInfoState.value is CartBusinessInfoUiState.Loaded) return

        viewModelScope.launch {
            _businessInfoState.value = CartBusinessInfoUiState.Loading
            _businessInfoState.value = when (val result = businessInfoRepository.getBusinessInfo()) {
                is BusinessInfoResult.Success -> CartBusinessInfoUiState.Loaded(result.info.toCartBusinessInfoUi())
                is BusinessInfoResult.Failure -> result.error.toCartBusinessInfoState()
            }
        }
    }

    fun refreshForSession() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                clearCancellationState()
                clearRescheduleState()
                _uiState.value = CartBookingsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                clearCancellationState()
                clearRescheduleState()
                _uiState.value = currentSessionState.error.toCartBookingsState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                clearCancellationState()
                clearRescheduleState()
                _uiState.value = CartBookingsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val revision = bookingRevision.value
        val hasReusableState = _uiState.value is CartBookingsUiState.Loaded ||
            _uiState.value is CartBookingsUiState.Empty
        if (loadedUid == uid && loadedRevision == revision && hasReusableState) return
        loadBookings()
    }

    fun loadBookings() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                clearCancellationState()
                clearRescheduleState()
                _uiState.value = CartBookingsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                clearCancellationState()
                clearRescheduleState()
                _uiState.value = currentSessionState.error.toCartBookingsState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                clearCancellationState()
                clearRescheduleState()
                _uiState.value = CartBookingsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }
        val requestedUid = session.session.user.uid
        val requestedRevision = bookingRevision.value
        if (
            _uiState.value is CartBookingsUiState.Loading &&
            loadingUid == requestedUid &&
            loadingRevision == requestedRevision
        ) {
            return
        }
        val requestRevision = ++historyRequestRevision
        loadingUid = requestedUid
        loadingRevision = requestedRevision

        viewModelScope.launch {
            _uiState.value = CartBookingsUiState.Loading
            val nextState = when (val result = bookingRepository.getMyBookings()) {
                is BookingHistoryResult.Success -> result.history.toUiState()
                is BookingHistoryResult.Failure -> result.error.toUiState()
            }
            if (requestRevision != historyRequestRevision) return@launch
            loadingUid = null
            loadingRevision = null
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedUid = requestedUid
                loadedRevision = requestedRevision
                _uiState.value = nextState
            } else {
                loadedUid = null
                loadedRevision = null
                _uiState.value = CartBookingsUiState.Unauthenticated
            }
        }
    }

    fun cancelBooking(reservationId: String) {
        if (_cancellationState.value is BookingCancellationUiState.Loading) return

        val expectedUid = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                _cancellationState.value = restoringCancellationState(reservationId)
                return
            }
            is AuthSessionState.RestoreFailed -> {
                _cancellationState.value = currentSessionState.error.toCancellationUiState(reservationId)
                return
            }
            AuthSessionState.Unauthenticated -> {
                _cancellationState.value = unauthenticatedCancellationState(reservationId)
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState.session.user.uid
        }

        viewModelScope.launch {
            _cancellationState.value = BookingCancellationUiState.Loading(reservationId)
            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    _cancellationState.value = restoringCancellationState(reservationId)
                    return@launch
                }
                is AuthSessionState.RestoreFailed -> {
                    _cancellationState.value = currentSessionState.error.toCancellationUiState(reservationId)
                    return@launch
                }
                AuthSessionState.Unauthenticated -> {
                    _cancellationState.value = unauthenticatedCancellationState(reservationId)
                    return@launch
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid != expectedUid) {
                        _cancellationState.value = changedSessionCancellationState(reservationId)
                        return@launch
                    }
                }
            }

            _cancellationState.value = when (
                val result = bookingRepository.cancelBooking(BookingCancelRequest(reservationId))
            ) {
                is BookingCancelResult.Success -> BookingCancellationUiState.Success(result.receipt.reservationId)
                is BookingCancelResult.Failure -> result.error.toCancellationUiState(reservationId)
            }
        }
    }

    fun clearCancellationState() {
        if (_cancellationState.value !is BookingCancellationUiState.Loading) {
            _cancellationState.value = BookingCancellationUiState.Idle
        }
    }

    fun loadRescheduleAvailability(serviceDurationMinutes: Int, anchorDate: String? = null) {
        val requestRevision = ++rescheduleAvailabilityRequestRevision
        val request = BookingAvailabilityRequest(
            anchorDate = anchorDate,
            serviceDurationMinutes = serviceDurationMinutes,
        )

        _rescheduleAvailabilityState.value = BookingRescheduleAvailabilityUiState.Loading
        viewModelScope.launch {
            val nextState = when (val result = bookingRepository.getAvailability(request)) {
                is BookingAvailabilityResult.Success -> {
                    if (result.month.days.any { it.available }) {
                        BookingRescheduleAvailabilityUiState.Loaded(result.month)
                    } else {
                        BookingRescheduleAvailabilityUiState.Empty(result.month)
                    }
                }
                is BookingAvailabilityResult.Failure -> result.error.toRescheduleAvailabilityState()
            }
            if (requestRevision == rescheduleAvailabilityRequestRevision) {
                _rescheduleAvailabilityState.value = nextState
            }
        }
    }

    fun rescheduleBooking(draft: BookingRescheduleDraft?) {
        if (_rescheduleState.value is BookingRescheduleUiState.Loading) return

        val request = draft?.toRescheduleRequest()
        if (request == null) {
            _rescheduleState.value = BookingRescheduleUiState.Error(
                reservationId = draft?.reservationId.orEmpty(),
                message = "Escolha uma nova data e hora para remarcar.",
                retryable = false,
                changeSlot = true,
            )
            return
        }

        val expectedUid = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                _rescheduleState.value = restoringRescheduleState(request.reservationId)
                return
            }
            is AuthSessionState.RestoreFailed -> {
                _rescheduleState.value = currentSessionState.error.toRescheduleState(request.reservationId)
                return
            }
            AuthSessionState.Unauthenticated -> {
                _rescheduleState.value = unauthenticatedRescheduleState(request.reservationId)
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState.session.user.uid
        }

        viewModelScope.launch {
            _rescheduleState.value = BookingRescheduleUiState.Loading(request.reservationId)
            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    _rescheduleState.value = restoringRescheduleState(request.reservationId)
                    return@launch
                }
                is AuthSessionState.RestoreFailed -> {
                    _rescheduleState.value = currentSessionState.error.toRescheduleState(request.reservationId)
                    return@launch
                }
                AuthSessionState.Unauthenticated -> {
                    _rescheduleState.value = unauthenticatedRescheduleState(request.reservationId)
                    return@launch
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid != expectedUid) {
                        _rescheduleState.value = changedSessionRescheduleState(request.reservationId)
                        return@launch
                    }
                }
            }

            _rescheduleState.value = when (val result = bookingRepository.rescheduleBooking(request)) {
                is BookingRescheduleResult.Success -> BookingRescheduleUiState.Success(result.receipt.reservationId)
                is BookingRescheduleResult.Failure -> result.error.toRescheduleState(request.reservationId)
            }
        }
    }

    fun clearRescheduleState() {
        if (_rescheduleState.value !is BookingRescheduleUiState.Loading) {
            _rescheduleState.value = BookingRescheduleUiState.Idle
        }
        if (_rescheduleAvailabilityState.value !is BookingRescheduleAvailabilityUiState.Loading) {
            _rescheduleAvailabilityState.value = BookingRescheduleAvailabilityUiState.Idle
        }
    }

    private fun BookingHistory.toUiState(): CartBookingsUiState {
        val mapped = reservations.mapNotNull { it.toUiModelOrNull() }
        if (mapped.isEmpty()) return CartBookingsUiState.Empty

        return CartBookingsUiState.Loaded(
            upcoming = mapped.filter { it.showLocation },
            completed = mapped.filterNot { it.showLocation },
        )
    }

    private fun BookingHistoryError.toUiState(): CartBookingsUiState {
        return when (this) {
            is BookingHistoryError.Unauthenticated -> CartBookingsUiState.Unauthenticated
            is BookingHistoryError.Permission -> CartBookingsUiState.Error(message = message, retryable = false)
            is BookingHistoryError.Unavailable,
            is BookingHistoryError.Backend -> CartBookingsUiState.Error(message = message, retryable = true)
        }
    }

    private fun BookingCancelError.toCancellationUiState(reservationId: String): BookingCancellationUiState.Error {
        val retryable = this is BookingCancelError.Unavailable ||
            this is BookingCancelError.Backend
        return BookingCancellationUiState.Error(
            reservationId = reservationId,
            message = message,
            retryable = retryable,
        )
    }

    private fun AuthError.toCancellationUiState(reservationId: String): BookingCancellationUiState.Error {
        return BookingCancellationUiState.Error(
            reservationId = reservationId,
            message = message,
            retryable = isRetryableSessionError(),
        )
    }

    private fun restoringCancellationState(reservationId: String): BookingCancellationUiState.Error {
        return BookingCancellationUiState.Error(
            reservationId = reservationId,
            message = "A sessão ainda está a ser validada. Tente novamente dentro de momentos.",
            retryable = true,
        )
    }

    private fun unauthenticatedCancellationState(reservationId: String): BookingCancellationUiState.Error {
        return BookingCancellationUiState.Error(
            reservationId = reservationId,
            message = "Inicie sessão para cancelar esta marcação.",
            retryable = false,
        )
    }

    private fun changedSessionCancellationState(reservationId: String): BookingCancellationUiState.Error {
        return BookingCancellationUiState.Error(
            reservationId = reservationId,
            message = "A sessão mudou antes de cancelarmos a marcação. Atualize a lista e tente novamente.",
            retryable = false,
        )
    }

    private fun BookingAvailabilityError.toRescheduleAvailabilityState(): BookingRescheduleAvailabilityUiState.Error {
        val retryable = this is BookingAvailabilityError.Unavailable ||
            this is BookingAvailabilityError.Backend
        return BookingRescheduleAvailabilityUiState.Error(message = message, retryable = retryable)
    }

    private fun BookingRescheduleError.toRescheduleState(reservationId: String): BookingRescheduleUiState.Error {
        val retryable = this is BookingRescheduleError.Unavailable ||
            this is BookingRescheduleError.Backend
        val changeSlot = this is BookingRescheduleError.Conflict
        return BookingRescheduleUiState.Error(
            reservationId = reservationId,
            message = message,
            retryable = retryable,
            changeSlot = changeSlot,
        )
    }

    private fun AuthError.toRescheduleState(reservationId: String): BookingRescheduleUiState.Error {
        return BookingRescheduleUiState.Error(
            reservationId = reservationId,
            message = message,
            retryable = isRetryableSessionError(),
            changeSlot = false,
        )
    }

    private fun restoringRescheduleState(reservationId: String): BookingRescheduleUiState.Error {
        return BookingRescheduleUiState.Error(
            reservationId = reservationId,
            message = "A sessão ainda está a ser validada. Tente novamente dentro de momentos.",
            retryable = true,
            changeSlot = false,
        )
    }

    private fun unauthenticatedRescheduleState(reservationId: String): BookingRescheduleUiState.Error {
        return BookingRescheduleUiState.Error(
            reservationId = reservationId,
            message = "Inicie sessão para remarcar esta marcação.",
            retryable = false,
            changeSlot = false,
        )
    }

    private fun changedSessionRescheduleState(reservationId: String): BookingRescheduleUiState.Error {
        return BookingRescheduleUiState.Error(
            reservationId = reservationId,
            message = "A sessão mudou antes de remarcarmos a marcação. Atualize a lista e tente novamente.",
            retryable = false,
            changeSlot = false,
        )
    }

    private fun AuthError.isRetryableSessionError(): Boolean {
        return this is AuthError.Unavailable || this is AuthError.Backend
    }

    private fun clearLoadedSession() {
        loadedUid = null
        loadedRevision = null
        loadingUid = null
        loadingRevision = null
        historyRequestRevision += 1
    }
}

internal fun CartBusinessInfoUiState.infoOrDefault(): CartBusinessInfoUi {
    return when (this) {
        CartBusinessInfoUiState.Idle,
        CartBusinessInfoUiState.Loading -> DefaultBusinessInfo.toCartBusinessInfoUi()
        is CartBusinessInfoUiState.Loaded -> info
        is CartBusinessInfoUiState.Error -> fallbackInfo
    }
}

private fun BusinessInfoError.toCartBusinessInfoState(): CartBusinessInfoUiState.Error {
    val retryable = this is BusinessInfoError.Unavailable || this is BusinessInfoError.Backend
    return CartBusinessInfoUiState.Error(
        fallbackInfo = DefaultBusinessInfo.toCartBusinessInfoUi(),
        message = message,
        retryable = retryable,
    )
}

private fun BusinessInfo.toCartBusinessInfoUi(): CartBusinessInfoUi = CartBusinessInfoUi(
    addressLine1 = addressLine1.trim().ifBlank { DefaultBusinessInfo.addressLine1 },
    addressLine2 = addressLine2.trim().ifBlank { DefaultBusinessInfo.addressLine2 },
)

private fun BookingHistoryReservation.toUiModelOrNull(): BookingSummaryUi? {
    if (id.isBlank() || slotStartIso.isBlank()) return null

    return BookingSummaryUi(
        id = id,
        service = serviceLabelWithExtras(),
        date = slotStartIso.toDateLabel(),
        time = slotStartIso.toTimeLabel(),
        slotStartIso = slotStartIso,
        slotEndIso = slotEndIso,
        serviceDurationMinutes = serviceDurationMinutes(),
        vehicle = vehicleLabel?.takeIf { it.isNotBlank() } ?: vehicleType.toVehicleLabel(),
        price = priceCents?.toEuroLabel() ?: "A confirmar",
        status = status.toStatusUi(),
        statusDescription = status.toStatusDescription(),
        icon = serviceIcon(),
        showLocation = upcoming,
        reviewed = reviewed,
        reviewRating = reviewRating?.takeIf { it in 1..5 },
        reviewTags = reviewTags.sanitizedReviewTags(),
        reviewComment = reviewComment.sanitizedReviewComment(),
        reviewable = isReviewableReservation(),
        cancelable = isCancelableReservation(),
        paymentLabel = bookingPaymentStatus().toPaymentLabel(),
        requiresPayment = requiresPayment(),
        auditNotes = auditNotes(),
    )
}

private fun List<String>.sanitizedReviewTags(): List<String> {
    return map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .take(8)
}

private fun String.sanitizedReviewComment(): String {
    val normalized = trim().replace(Regex("\\s+"), " ")
    if (normalized.length <= 360) return normalized
    return normalized.take(357).trimEnd() + "..."
}

private fun BookingHistoryReservation.auditNotes(): List<BookingAuditNoteUi> {
    val notes = mutableListOf<BookingAuditNoteUi>()

    if (rescheduleCount > 0 || !previousSlotStartIso.isNullOrBlank()) {
        val previousSlot = previousSlotStartIso?.toDateTimeLabel()
        val currentSlot = slotStartIso.toDateTimeLabel()
        val rescheduledAt = rescheduledAtIso?.toDateTimeLabel()
        val countLabel = when (rescheduleCount) {
            0, 1 -> "Marcação remarcada"
            else -> "Marcação remarcada $rescheduleCount vezes"
        }
        val body = when {
            previousSlot != null && currentSlot != null -> "De $previousSlot para $currentSlot."
            previousSlot != null -> "Horário anterior: $previousSlot."
            rescheduledAt != null -> "Alterada em $rescheduledAt."
            else -> "Esta marcação foi alterada."
        }
        notes += BookingAuditNoteUi(
            title = countLabel,
            body = body,
        )
    }

    if (isCancelledReservation()) {
        val cancelledAt = cancelledAtIso?.toDateTimeLabel()
        notes += BookingAuditNoteUi(
            title = "Marcação cancelada",
            body = if (cancelledAt != null) {
                "Cancelada em $cancelledAt."
            } else {
                "Esta marcação foi cancelada."
            },
            tone = BookingAuditToneUi.Warning,
        )
    }

    when (status.toBookingReservationStatus()) {
        BookingReservationStatus.Rejected -> {
            notes += BookingAuditNoteUi(
                title = "Pedido rejeitado",
                body = rejectionReason
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let { "Motivo: $it" }
                    ?: "Este pedido de marcação foi rejeitado.",
                tone = BookingAuditToneUi.Warning,
            )
        }
        BookingReservationStatus.Expired -> {
            notes += BookingAuditNoteUi(
                title = "Pedido expirado",
                body = "O prazo de validação deste pedido terminou.",
                tone = BookingAuditToneUi.Warning,
            )
        }
        BookingReservationStatus.Pending,
        BookingReservationStatus.Confirmed,
        BookingReservationStatus.InProgress,
        BookingReservationStatus.Completed,
        BookingReservationStatus.Cancelled,
        BookingReservationStatus.Unknown -> Unit
    }

    return notes
}

private fun BookingPaymentStatus.toPaymentLabel(): String = when (this) {
    BookingPaymentStatus.Pending -> "Pagamento pendente"
    BookingPaymentStatus.Paid -> "Pago"
    BookingPaymentStatus.CoveredByLoyalty -> "Coberto por recompensa"
    BookingPaymentStatus.Refunded -> "Reembolsado"
    BookingPaymentStatus.Failed -> "Pagamento falhou"
    BookingPaymentStatus.Unknown -> "Pagamento a confirmar"
}

private fun BookingHistoryReservation.serviceLabelWithExtras(): String {
    val baseLabel = serviceName.ifBlank { "Serviço" }
    if (extras.isEmpty()) return baseLabel

    val extrasLabel = if (extras.size == 1) "1 extra" else "${extras.size} extras"
    return "$baseLabel + $extrasLabel"
}

private fun BookingHistoryReservation.serviceIcon(): ImageVector {
    val key = "$serviceId $serviceName".lowercase()
    return if ("premium" in key || "detalh" in key) {
        Icons.Filled.AutoAwesome
    } else {
        Icons.Filled.DirectionsCar
    }
}

private fun String.toStatusUi(): BookingStatusUi {
    return when (toBookingReservationStatus()) {
        BookingReservationStatus.Pending -> BookingStatusUi.Pending
        BookingReservationStatus.Confirmed -> BookingStatusUi.Confirmed
        BookingReservationStatus.InProgress -> BookingStatusUi.InProgress
        BookingReservationStatus.Completed -> BookingStatusUi.Completed
        BookingReservationStatus.Cancelled -> BookingStatusUi.Cancelled
        BookingReservationStatus.Rejected -> BookingStatusUi.Rejected
        BookingReservationStatus.Expired -> BookingStatusUi.Expired
        BookingReservationStatus.Unknown -> BookingStatusUi.Unknown
    }
}

private fun String.toStatusDescription(): String {
    return when (toBookingReservationStatus()) {
        BookingReservationStatus.Pending -> "Pedido recebido. A equipa vai confirmar ou recusar a lavagem."
        BookingReservationStatus.Confirmed -> "Marcação aceite. A lavagem ainda não começou."
        BookingReservationStatus.InProgress -> "Lavagem a decorrer. Avisamos quando estiver concluída."
        BookingReservationStatus.Completed -> "Lavagem concluída e guardada no histórico."
        BookingReservationStatus.Cancelled -> "Marcação cancelada."
        BookingReservationStatus.Rejected -> "Pedido recusado pela equipa."
        BookingReservationStatus.Expired -> "O prazo de validação deste pedido terminou."
        BookingReservationStatus.Unknown -> "Estado em atualização."
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

private fun BookingHistoryReservation.serviceDurationMinutes(): Int {
    val start = slotStartIso.utcMinuteOfDayOrNull() ?: return 30
    val end = slotEndIso.utcMinuteOfDayOrNull() ?: return 30
    val duration = end - start
    return duration.takeIf { it in 5..480 } ?: 30
}

private fun String.utcMinuteOfDayOrNull(): Int? {
    val time = substringAfter("T", missingDelimiterValue = "").take(5)
    if (time.length != 5 || time[2] != ':') return null
    val hour = time.substring(0, 2).toIntOrNull() ?: return null
    val minute = time.substring(3, 5).toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun Int.toEuroLabel(): String {
    val euros = this / 100
    val remainder = this % 100
    return "$euros,${remainder.toString().padStart(2, '0')}€"
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

internal fun BookingRescheduleDraft.toRescheduleRequest(): BookingRescheduleRequest? {
    val slotStartIso = buildSlotIso(dateId = dateId, time = time) ?: return null
    val slotEndIso = buildSlotEndIso(dateId = dateId, time = time, durationMinutes = durationMinutes) ?: return null
    return BookingRescheduleRequest(
        reservationId = reservationId,
        slotStartIso = slotStartIso,
        slotEndIso = slotEndIso,
    )
}

private fun buildSlotIso(dateId: String, time: String): String? {
    val (hour, minute) = parseTime(time) ?: return null
    if (!isValidDateId(dateId)) return null
    return "$dateId" + "T" + hour.twoDigits() + ":" + minute.twoDigits() + ":00.000Z"
}

private fun buildSlotEndIso(dateId: String, time: String, durationMinutes: Int): String? {
    val (hour, minute) = parseTime(time) ?: return null
    if (!isValidDateId(dateId) || durationMinutes <= 0) return null

    val endTotalMinutes = hour * 60 + minute + durationMinutes
    if (endTotalMinutes >= 24 * 60) return null

    val endHour = endTotalMinutes / 60
    val endMinute = endTotalMinutes % 60
    return "$dateId" + "T" + endHour.twoDigits() + ":" + endMinute.twoDigits() + ":00.000Z"
}

private fun parseTime(time: String): Pair<Int, Int>? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

private fun isValidDateId(dateId: String): Boolean {
    if (dateId.length != 10) return false
    if (dateId[4] != '-' || dateId[7] != '-') return false

    val year = dateId.substring(0, 4).toIntOrNull() ?: return false
    val month = dateId.substring(5, 7).toIntOrNull() ?: return false
    val day = dateId.substring(8, 10).toIntOrNull() ?: return false
    return year > 0 && month in 1..12 && day in 1..daysInMonth(year, month)
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }
}

private fun isLeapYear(year: Int): Boolean {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}

private fun AuthError.toCartBookingsState(): CartBookingsUiState.Error {
    return CartBookingsUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}
