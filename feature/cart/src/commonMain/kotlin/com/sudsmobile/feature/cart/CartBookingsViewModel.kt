package com.sudsmobile.feature.cart

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingCancelError
import com.sudsmobile.data.booking.BookingCancelRequest
import com.sudsmobile.data.booking.BookingCancelResult
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class BookingStatusUi(val label: String) {
    Confirmed("Confirmado"),
    Completed("Concluído"),
    Cancelled("Cancelado"),
}

internal data class BookingSummaryUi(
    val id: String,
    val service: String,
    val date: String,
    val time: String,
    val vehicle: String,
    val price: String,
    val status: BookingStatusUi,
    val icon: ImageVector,
    val showLocation: Boolean,
    val reviewed: Boolean,
    val reviewRating: Int?,
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

internal class CartBookingsViewModel(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val bookingChangeNotifier: BookingChangeNotifier = MutableBookingChangeNotifier(),
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision
    private val _uiState = MutableStateFlow<CartBookingsUiState>(CartBookingsUiState.Idle)
    val uiState: StateFlow<CartBookingsUiState> = _uiState.asStateFlow()
    private val _cancellationState = MutableStateFlow<BookingCancellationUiState>(BookingCancellationUiState.Idle)
    val cancellationState: StateFlow<BookingCancellationUiState> = _cancellationState.asStateFlow()
    private var loadedUid: String? = null
    private var loadedRevision: Long? = null

    fun refreshForSession() {
        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            loadedRevision = null
            clearCancellationState()
            _uiState.value = CartBookingsUiState.Unauthenticated
            return
        }

        val uid = session.session.user.uid
        val revision = bookingRevision.value
        val hasReusableState = _uiState.value is CartBookingsUiState.Loaded ||
            _uiState.value is CartBookingsUiState.Empty
        if (loadedUid == uid && loadedRevision == revision && hasReusableState) return
        loadBookings()
    }

    fun loadBookings() {
        if (_uiState.value is CartBookingsUiState.Loading) return

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            loadedRevision = null
            clearCancellationState()
            _uiState.value = CartBookingsUiState.Unauthenticated
            return
        }
        val requestedUid = session.session.user.uid
        val requestedRevision = bookingRevision.value

        viewModelScope.launch {
            _uiState.value = CartBookingsUiState.Loading
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
                _uiState.value = CartBookingsUiState.Unauthenticated
            }
        }
    }

    fun cancelBooking(reservationId: String) {
        if (_cancellationState.value is BookingCancellationUiState.Loading) return

        viewModelScope.launch {
            _cancellationState.value = BookingCancellationUiState.Loading(reservationId)
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
}

private fun BookingHistoryReservation.toUiModelOrNull(): BookingSummaryUi? {
    if (id.isBlank() || slotStartIso.isBlank()) return null

    return BookingSummaryUi(
        id = id,
        service = serviceName.ifBlank { "Serviço" },
        date = slotStartIso.toDateLabel(),
        time = slotStartIso.toTimeLabel(),
        vehicle = vehicleLabel?.takeIf { it.isNotBlank() } ?: vehicleType.toVehicleLabel(),
        price = priceCents?.toEuroLabel() ?: "A confirmar",
        status = status.toStatusUi(),
        icon = serviceIcon(),
        showLocation = upcoming,
        reviewed = reviewed,
        reviewRating = reviewRating?.takeIf { it in 1..5 },
    )
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
    val normalized = lowercase()
    return when {
        normalized in setOf("cancelled", "canceled", "cancelado") -> BookingStatusUi.Cancelled
        normalized in setOf("completed", "concluido", "concluído") -> BookingStatusUi.Completed
        else -> BookingStatusUi.Confirmed
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
