package com.sudsmobile.feature.payment

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
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.booking.bookingPaymentStatus
import com.sudsmobile.data.booking.requiresPayment
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.DefaultBusinessInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class PaymentBookingUi(
    val id: String,
    val reference: String,
    val service: String,
    val date: String,
    val time: String,
    val vehicle: String,
    val price: String,
    val priceCents: Int,
    val statusLabel: String,
)

internal data class PaymentBusinessInfoUi(
    val phone: String,
    val addressLine1: String,
    val addressLine2: String,
)

internal sealed interface PaymentUiState {
    data object Idle : PaymentUiState
    data object Loading : PaymentUiState
    data object Unauthenticated : PaymentUiState
    data object Empty : PaymentUiState
    data class Loaded(
        val bookings: List<PaymentBookingUi>,
        val totalDue: String,
    ) : PaymentUiState
    data class Error(val message: String, val retryable: Boolean) : PaymentUiState
}

internal sealed interface PaymentBusinessInfoUiState {
    data object Idle : PaymentBusinessInfoUiState
    data object Loading : PaymentBusinessInfoUiState
    data class Loaded(val info: PaymentBusinessInfoUi) : PaymentBusinessInfoUiState
    data class Error(
        val fallbackInfo: PaymentBusinessInfoUi,
        val message: String,
        val retryable: Boolean,
    ) : PaymentBusinessInfoUiState
}

internal class PaymentViewModel(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val businessInfoRepository: BusinessInfoRepository,
    private val bookingChangeNotifier: BookingChangeNotifier = MutableBookingChangeNotifier(),
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _businessInfoState = MutableStateFlow<PaymentBusinessInfoUiState>(PaymentBusinessInfoUiState.Idle)
    val businessInfoState: StateFlow<PaymentBusinessInfoUiState> = _businessInfoState.asStateFlow()

    private var loadedUid: String? = null
    private var loadedRevision: Long? = null

    fun refreshForSession(force: Boolean = false) {
        when (val session = sessionState.value) {
            AuthSessionState.Restoring -> {
                loadedUid = null
                loadedRevision = null
                _uiState.value = PaymentUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                loadedUid = null
                loadedRevision = null
                _uiState.value = PaymentUiState.Error(
                    message = session.error.message,
                    retryable = session.error.isRetryable(),
                )
            }
            AuthSessionState.Unauthenticated -> {
                loadedUid = null
                loadedRevision = null
                _uiState.value = PaymentUiState.Unauthenticated
            }
            is AuthSessionState.Authenticated -> {
                val uid = session.session.user.uid
                val revision = bookingRevision.value
                val hasReusableState = _uiState.value is PaymentUiState.Loaded ||
                    _uiState.value is PaymentUiState.Empty
                if (!force && loadedUid == uid && loadedRevision == revision && hasReusableState) return
                loadPayments()
            }
        }
    }

    fun loadPayments() {
        if (_uiState.value is PaymentUiState.Loading) return

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            loadedRevision = null
            _uiState.value = PaymentUiState.Unauthenticated
            return
        }

        val requestedUid = session.session.user.uid
        val requestedRevision = bookingRevision.value
        viewModelScope.launch {
            _uiState.value = PaymentUiState.Loading
            val nextState = when (val result = bookingRepository.getMyBookings()) {
                is BookingHistoryResult.Success -> result.history.toPaymentUiState()
                is BookingHistoryResult.Failure -> result.error.toPaymentUiState()
            }

            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedUid = requestedUid
                loadedRevision = requestedRevision
                _uiState.value = nextState
            } else {
                loadedUid = null
                loadedRevision = null
                _uiState.value = PaymentUiState.Unauthenticated
            }
        }
    }

    fun loadBusinessInfo(force: Boolean = false) {
        if (_businessInfoState.value is PaymentBusinessInfoUiState.Loading) return
        if (!force && _businessInfoState.value is PaymentBusinessInfoUiState.Loaded) return

        viewModelScope.launch {
            _businessInfoState.value = PaymentBusinessInfoUiState.Loading
            _businessInfoState.value = when (val result = businessInfoRepository.getBusinessInfo()) {
                is BusinessInfoResult.Success -> PaymentBusinessInfoUiState.Loaded(result.info.toPaymentBusinessInfoUi())
                is BusinessInfoResult.Failure -> result.error.toPaymentBusinessInfoState()
            }
        }
    }

    private fun BookingHistory.toPaymentUiState(): PaymentUiState {
        val bookings = reservations
            .mapNotNull { it.toPaymentBookingUiOrNull() }
            .sortedBy { it.date + it.time }

        if (bookings.isEmpty()) return PaymentUiState.Empty

        return PaymentUiState.Loaded(
            bookings = bookings,
            totalDue = bookings.sumOf { it.priceCents }.toEuroLabel(),
        )
    }

    private fun BookingHistoryError.toPaymentUiState(): PaymentUiState {
        return when (this) {
            is BookingHistoryError.Unauthenticated -> PaymentUiState.Unauthenticated
            is BookingHistoryError.Permission -> PaymentUiState.Error(message = message, retryable = false)
            is BookingHistoryError.Unavailable,
            is BookingHistoryError.Backend -> PaymentUiState.Error(message = message, retryable = true)
        }
    }
}

internal fun PaymentBusinessInfoUiState.infoOrDefault(): PaymentBusinessInfoUi {
    return when (this) {
        PaymentBusinessInfoUiState.Idle,
        PaymentBusinessInfoUiState.Loading -> DefaultBusinessInfo.toPaymentBusinessInfoUi()
        is PaymentBusinessInfoUiState.Loaded -> info
        is PaymentBusinessInfoUiState.Error -> fallbackInfo
    }
}

private fun BusinessInfoError.toPaymentBusinessInfoState(): PaymentBusinessInfoUiState.Error {
    val retryable = this is BusinessInfoError.Unavailable || this is BusinessInfoError.Backend
    return PaymentBusinessInfoUiState.Error(
        fallbackInfo = DefaultBusinessInfo.toPaymentBusinessInfoUi(),
        message = message,
        retryable = retryable,
    )
}

private fun BusinessInfo.toPaymentBusinessInfoUi(): PaymentBusinessInfoUi = PaymentBusinessInfoUi(
    phone = phone.trim().ifBlank { DefaultBusinessInfo.phone },
    addressLine1 = addressLine1.trim().ifBlank { DefaultBusinessInfo.addressLine1 },
    addressLine2 = addressLine2.trim().ifBlank { DefaultBusinessInfo.addressLine2 },
)

private fun BookingHistoryReservation.toPaymentBookingUiOrNull(): PaymentBookingUi? {
    if (!requiresPayment()) return null
    val price = priceCents ?: return null

    return PaymentBookingUi(
        id = id,
        reference = reservationCode.ifBlank { id },
        service = serviceName.ifBlank { "Serviço" },
        date = slotStartIso.toDateLabel(),
        time = slotStartIso.toTimeLabel(),
        vehicle = vehicleLabel?.takeIf { it.isNotBlank() } ?: vehicleType.toVehicleLabel(),
        price = price.toEuroLabel(),
        priceCents = price,
        statusLabel = bookingPaymentStatus().toPaymentStatusLabel(),
    )
}

private fun BookingPaymentStatus.toPaymentStatusLabel(): String = when (this) {
    BookingPaymentStatus.Pending -> "Pendente"
    BookingPaymentStatus.Failed -> "Falhou"
    BookingPaymentStatus.Paid -> "Pago"
    BookingPaymentStatus.CoveredByLoyalty -> "Recompensa"
    BookingPaymentStatus.Refunded -> "Reembolsado"
    BookingPaymentStatus.Unknown -> "A confirmar"
}

private fun AuthError.isRetryable(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
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
    val month = parts[1].toIntOrNull()?.let { paymentMonthNames.getOrNull(it - 1) } ?: return date
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

private val paymentMonthNames = listOf(
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
