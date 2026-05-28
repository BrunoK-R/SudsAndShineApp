package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingPaymentStatus
import com.sudsmobile.data.booking.BookingReservationStatus
import com.sudsmobile.data.booking.BookingReservationExtra
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.booking.bookingPaymentStatus
import com.sudsmobile.data.booking.isCancelledReservation
import com.sudsmobile.data.booking.isCompletedReservation
import com.sudsmobile.data.booking.isReviewableReservation
import com.sudsmobile.data.booking.toBookingReservationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ProfileHistorySummaryUi(
    val washCount: String,
    val totalSpent: String,
)

internal enum class ProfileHistoryStatusUi(val label: String) {
    Completed("Concluído"),
    Cancelled("Cancelado"),
    Past("Concluído"),
}

internal data class ProfileHistoryItemUi(
    val id: String,
    val reference: String,
    val service: String,
    val date: String,
    val time: String,
    val vehicle: String,
    val price: String,
    val priceCents: Int?,
    val paymentStatus: String,
    val extras: List<ProfileHistoryExtraUi>,
    val status: ProfileHistoryStatusUi,
    val rebookServiceId: String?,
    val reviewed: Boolean,
    val reviewable: Boolean,
    val reviewRating: Int?,
    val reviewTags: List<String>,
    val reviewComment: String,
    val auditNotes: List<ProfileHistoryAuditNoteUi>,
)

internal data class ProfileHistoryExtraUi(
    val name: String,
    val price: String,
)

internal data class ProfileHistoryAuditNoteUi(
    val title: String,
    val body: String,
    val tone: ProfileHistoryAuditToneUi = ProfileHistoryAuditToneUi.Neutral,
)

internal enum class ProfileHistoryAuditToneUi {
    Neutral,
    Warning,
}

internal sealed interface ProfileHistoryUiState {
    data object Idle : ProfileHistoryUiState
    data object Loading : ProfileHistoryUiState
    data object Unauthenticated : ProfileHistoryUiState
    data object Empty : ProfileHistoryUiState
    data class Loaded(
        val summary: ProfileHistorySummaryUi,
        val items: List<ProfileHistoryItemUi>,
    ) : ProfileHistoryUiState
    data class Error(val message: String, val retryable: Boolean) : ProfileHistoryUiState
}

internal class ProfileHistoryViewModel(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val bookingChangeNotifier: BookingChangeNotifier = MutableBookingChangeNotifier(),
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision
    private val _uiState = MutableStateFlow<ProfileHistoryUiState>(ProfileHistoryUiState.Idle)
    val uiState: StateFlow<ProfileHistoryUiState> = _uiState.asStateFlow()
    private var loadedUid: String? = null
    private var loadedRevision: Long? = null

    fun refreshForSession() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = ProfileHistoryUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toProfileHistoryState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = ProfileHistoryUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val revision = bookingRevision.value
        if (loadedUid == uid && loadedRevision == revision && _uiState.value is ProfileHistoryUiState.Loaded) return
        loadHistory()
    }

    fun loadHistory() {
        if (_uiState.value is ProfileHistoryUiState.Loading) return

        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = ProfileHistoryUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toProfileHistoryState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = ProfileHistoryUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }
        val requestedUid = session.session.user.uid
        val requestedRevision = bookingRevision.value

        viewModelScope.launch {
            _uiState.value = ProfileHistoryUiState.Loading
            val nextState = when (val result = bookingRepository.getMyBookings()) {
                is BookingHistoryResult.Success -> result.history.toUiState()
                is BookingHistoryResult.Failure -> result.error.toUiState()
            }
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedUid = requestedUid
                loadedRevision = requestedRevision
                _uiState.value = nextState
            } else {
                loadedUid = null
                loadedRevision = null
                _uiState.value = ProfileHistoryUiState.Unauthenticated
            }
        }
    }

    private fun BookingHistory.toUiState(): ProfileHistoryUiState {
        val items = reservations
            .filter { it.isProfileHistoryReservation() }
            .mapNotNull { it.toHistoryItemOrNull() }

        if (items.isEmpty()) return ProfileHistoryUiState.Empty

        val completedItems = items.filterNot { it.status == ProfileHistoryStatusUi.Cancelled }
        val totalCents = completedItems.sumOf { it.priceCents ?: 0 }
        return ProfileHistoryUiState.Loaded(
            summary = ProfileHistorySummaryUi(
                washCount = completedItems.size.toString(),
                totalSpent = totalCents.toEuroLabel(),
            ),
            items = items,
        )
    }

    private fun BookingHistoryError.toUiState(): ProfileHistoryUiState {
        return when (this) {
            is BookingHistoryError.Unauthenticated -> ProfileHistoryUiState.Unauthenticated
            is BookingHistoryError.Permission -> ProfileHistoryUiState.Error(message = message, retryable = false)
            is BookingHistoryError.Unavailable,
            is BookingHistoryError.Backend -> ProfileHistoryUiState.Error(message = message, retryable = true)
        }
    }

    private fun clearLoadedSession() {
        loadedUid = null
        loadedRevision = null
    }
}

private fun BookingHistoryReservation.toHistoryItemOrNull(): ProfileHistoryItemUi? {
    if (id.isBlank() || slotStartIso.isBlank()) return null

    return ProfileHistoryItemUi(
        id = id,
        reference = reservationCode.ifBlank { id },
        service = serviceLabelWithExtras(),
        date = slotStartIso.toDateLabel(),
        time = slotStartIso.toTimeLabel(),
        vehicle = vehicleLabel?.takeIf { it.isNotBlank() } ?: vehicleType.toVehicleLabel(),
        price = priceCents?.toEuroLabel() ?: "A confirmar",
        priceCents = priceCents,
        paymentStatus = bookingPaymentStatus().toHistoryPaymentLabel(),
        extras = extras.toHistoryExtraUi(),
        status = status.toHistoryStatusUi(),
        rebookServiceId = serviceId.normalizedRebookServiceId(),
        reviewed = reviewed,
        reviewable = isReviewableReservation() && !reviewed,
        reviewRating = reviewRating?.takeIf { it in 1..5 },
        reviewTags = reviewTags.sanitizedReviewTags(),
        reviewComment = reviewComment.sanitizedReviewComment(),
        auditNotes = auditNotes(),
    )
}

private fun BookingHistoryReservation.isProfileHistoryReservation(): Boolean {
    return isCompletedReservation() || isCancelledReservation()
}

private fun String.normalizedRebookServiceId(): String? = trim().takeIf { it.isNotBlank() }

private fun List<BookingReservationExtra>.toHistoryExtraUi(): List<ProfileHistoryExtraUi> {
    return mapNotNull { extra ->
        val name = extra.name.trim()
        if (name.isBlank()) return@mapNotNull null
        ProfileHistoryExtraUi(
            name = name,
            price = if (extra.priceCents > 0) extra.priceCents.toEuroLabel() else "Incluído",
        )
    }.take(8)
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

private fun BookingHistoryReservation.auditNotes(): List<ProfileHistoryAuditNoteUi> {
    val notes = mutableListOf<ProfileHistoryAuditNoteUi>()

    if (rescheduleCount > 0 || !previousSlotStartIso.isNullOrBlank()) {
        val previousSlot = previousSlotStartIso?.toDateTimeLabel()
        val currentSlot = slotStartIso.toDateTimeLabel()
        val rescheduledAt = rescheduledAtIso?.toDateTimeLabel()
        val body = when {
            previousSlot != null && currentSlot != null -> "De $previousSlot para $currentSlot."
            previousSlot != null -> "Horário anterior: $previousSlot."
            rescheduledAt != null -> "Alterada em $rescheduledAt."
            else -> "Esta lavagem foi remarcada antes da conclusão."
        }

        notes += ProfileHistoryAuditNoteUi(
            title = if (rescheduleCount > 1) {
                "Remarcada $rescheduleCount vezes"
            } else {
                "Remarcada"
            },
            body = body,
        )
    }

    if (isCancelledReservation()) {
        val cancelledAt = cancelledAtIso?.toDateTimeLabel()
        notes += ProfileHistoryAuditNoteUi(
            title = "Cancelada",
            body = if (cancelledAt != null) {
                "Cancelada em $cancelledAt."
            } else {
                "Esta marcação foi cancelada."
            },
            tone = ProfileHistoryAuditToneUi.Warning,
        )
    }

    return notes
}

private fun BookingHistoryReservation.serviceLabelWithExtras(): String {
    val baseLabel = serviceName.ifBlank { "Serviço" }
    val extraCount = extras.count { it.name.trim().isNotBlank() }
    if (extraCount == 0) return baseLabel

    val extrasLabel = if (extraCount == 1) "1 extra" else "$extraCount extras"
    return "$baseLabel + $extrasLabel"
}

private fun String.toHistoryStatusUi(): ProfileHistoryStatusUi {
    return when (toBookingReservationStatus()) {
        BookingReservationStatus.Completed -> ProfileHistoryStatusUi.Completed
        BookingReservationStatus.Cancelled -> ProfileHistoryStatusUi.Cancelled
        BookingReservationStatus.Pending,
        BookingReservationStatus.Confirmed,
        BookingReservationStatus.InProgress,
        BookingReservationStatus.Unknown -> ProfileHistoryStatusUi.Past
    }
}

private fun BookingPaymentStatus.toHistoryPaymentLabel(): String = when (this) {
    BookingPaymentStatus.Pending -> "Pendente"
    BookingPaymentStatus.Paid -> "Pago"
    BookingPaymentStatus.CoveredByLoyalty -> "Recompensa"
    BookingPaymentStatus.Refunded -> "Reembolsado"
    BookingPaymentStatus.Failed -> "Falhou"
    BookingPaymentStatus.Unknown -> "A confirmar"
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

private fun AuthError.toProfileHistoryState(): ProfileHistoryUiState.Error {
    return ProfileHistoryUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}
