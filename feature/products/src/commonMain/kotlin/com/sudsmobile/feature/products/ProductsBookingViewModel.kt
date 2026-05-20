package com.sudsmobile.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingAvailabilityError
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilityRequest
import com.sudsmobile.data.booking.BookingAvailabilityResult
import com.sudsmobile.data.booking.BookingCreateError
import com.sudsmobile.data.booking.BookingCreateRequest
import com.sudsmobile.data.booking.BookingCreateResult
import com.sudsmobile.data.booking.BookingReceipt
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.vehicle.UserVehicle
import com.sudsmobile.data.vehicle.UserVehicleError
import com.sudsmobile.data.vehicle.UserVehicleListResult
import com.sudsmobile.data.vehicle.UserVehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductsBookingDraft(
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val serviceId: String,
    val serviceName: String,
    val dateId: String,
    val time: String,
    val serviceDurationMinutes: Int,
    val vehicleType: String,
    val gdprConsent: Boolean,
    val notes: String,
    val userVehicleId: String? = null,
    val vehicleLabel: String? = null,
)

data class BookingVehicleUi(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val userVehicleId: String?,
    val vehicleLabel: String?,
)

sealed interface BookingSubmitUiState {
    data object Idle : BookingSubmitUiState
    data object Loading : BookingSubmitUiState
    data class Success(val receipt: BookingReceipt) : BookingSubmitUiState
    data class Error(
        val message: String,
        val retryable: Boolean,
        val resolution: BookingSubmitResolution,
    ) : BookingSubmitUiState
}

enum class BookingSubmitResolution {
    None,
    Retry,
    ChangeSlot,
    SignIn,
}

sealed interface BookingAvailabilityUiState {
    data object Idle : BookingAvailabilityUiState
    data object Loading : BookingAvailabilityUiState
    data class Loaded(val month: BookingAvailabilityMonth) : BookingAvailabilityUiState
    data class Empty(val month: BookingAvailabilityMonth) : BookingAvailabilityUiState
    data class Error(val message: String, val retryable: Boolean) : BookingAvailabilityUiState
}

sealed interface BookingVehiclesUiState {
    data object Idle : BookingVehiclesUiState
    data object Loading : BookingVehiclesUiState
    data object Unauthenticated : BookingVehiclesUiState
    data object Empty : BookingVehiclesUiState
    data class Loaded(val vehicles: List<BookingVehicleUi>) : BookingVehiclesUiState
    data class Error(val message: String, val retryable: Boolean) : BookingVehiclesUiState
}

class ProductsBookingViewModel(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val userVehicleRepository: UserVehicleRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _availabilityState = MutableStateFlow<BookingAvailabilityUiState>(BookingAvailabilityUiState.Idle)
    val availabilityState: StateFlow<BookingAvailabilityUiState> = _availabilityState.asStateFlow()

    private val _submitState = MutableStateFlow<BookingSubmitUiState>(BookingSubmitUiState.Idle)
    val submitState: StateFlow<BookingSubmitUiState> = _submitState.asStateFlow()

    private val _vehiclesState = MutableStateFlow<BookingVehiclesUiState>(BookingVehiclesUiState.Idle)
    val vehiclesState: StateFlow<BookingVehiclesUiState> = _vehiclesState.asStateFlow()
    private var loadedVehiclesUid: String? = null

    fun refreshVehiclesForSession() {
        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedVehiclesUid = null
            _vehiclesState.value = BookingVehiclesUiState.Unauthenticated
            return
        }

        val uid = session.session.user.uid
        val alreadyLoadedForUser = loadedVehiclesUid == uid &&
            (_vehiclesState.value is BookingVehiclesUiState.Loaded || _vehiclesState.value is BookingVehiclesUiState.Empty)
        if (alreadyLoadedForUser) return
        loadVehicles()
    }

    fun loadVehicles() {
        if (_vehiclesState.value is BookingVehiclesUiState.Loading) return

        val session = authRepository.sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedVehiclesUid = null
            _vehiclesState.value = BookingVehiclesUiState.Unauthenticated
            return
        }

        val requestedUid = session.session.user.uid
        viewModelScope.launch {
            _vehiclesState.value = BookingVehiclesUiState.Loading
            val nextState = when (val result = userVehicleRepository.getMyVehicles()) {
                is UserVehicleListResult.Success -> result.vehicles.toVehiclesUiState()
                is UserVehicleListResult.Failure -> result.error.toVehiclesUiState()
            }
            val currentUid = (authRepository.sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedVehiclesUid = requestedUid
                _vehiclesState.value = nextState
            } else {
                loadedVehiclesUid = null
                _vehiclesState.value = BookingVehiclesUiState.Unauthenticated
            }
        }
    }

    fun loadAvailability(serviceDurationMinutes: Int, anchorDate: String? = null) {
        if (_availabilityState.value is BookingAvailabilityUiState.Loading) return

        viewModelScope.launch {
            _availabilityState.value = BookingAvailabilityUiState.Loading
            _availabilityState.value = when (
                val result = bookingRepository.getAvailability(
                    BookingAvailabilityRequest(
                        anchorDate = anchorDate,
                        serviceDurationMinutes = serviceDurationMinutes,
                    ),
                )
            ) {
                is BookingAvailabilityResult.Success -> {
                    if (result.month.days.any { it.available }) {
                        BookingAvailabilityUiState.Loaded(result.month)
                    } else {
                        BookingAvailabilityUiState.Empty(result.month)
                    }
                }
                is BookingAvailabilityResult.Failure -> result.error.toUiState()
            }
        }
    }

    fun submitBooking(draft: ProductsBookingDraft?) {
        if (_submitState.value is BookingSubmitUiState.Loading) return

        val request = draft?.toCreateRequest()
        if (request == null) {
            _submitState.value = BookingSubmitUiState.Error(
                message = "Complete os dados da marcação antes de confirmar.",
                retryable = false,
                resolution = BookingSubmitResolution.None,
            )
            return
        }

        viewModelScope.launch {
            _submitState.value = BookingSubmitUiState.Loading
            _submitState.value = when (val result = bookingRepository.createBooking(request)) {
                is BookingCreateResult.Success -> BookingSubmitUiState.Success(result.receipt)
                is BookingCreateResult.Failure -> result.error.toUiState()
            }
        }
    }

    fun clearSubmitError() {
        if (_submitState.value is BookingSubmitUiState.Error) {
            _submitState.value = BookingSubmitUiState.Idle
        }
    }

    fun consumeSuccess() {
        if (_submitState.value is BookingSubmitUiState.Success) {
            _submitState.value = BookingSubmitUiState.Idle
        }
    }

    private fun BookingCreateError.toUiState(): BookingSubmitUiState.Error {
        return when (this) {
            is BookingCreateError.Conflict -> BookingSubmitUiState.Error(
                message = message,
                retryable = false,
                resolution = BookingSubmitResolution.ChangeSlot,
            )
            is BookingCreateError.Unauthenticated -> BookingSubmitUiState.Error(
                message = message,
                retryable = false,
                resolution = BookingSubmitResolution.SignIn,
            )
            is BookingCreateError.Unavailable,
            is BookingCreateError.Backend -> BookingSubmitUiState.Error(
                message = message,
                retryable = true,
                resolution = BookingSubmitResolution.Retry,
            )
            is BookingCreateError.Validation,
            is BookingCreateError.Permission -> BookingSubmitUiState.Error(
                message = message,
                retryable = false,
                resolution = BookingSubmitResolution.None,
            )
        }
    }

    private fun BookingAvailabilityError.toUiState(): BookingAvailabilityUiState.Error {
        val retryable = this is BookingAvailabilityError.Unavailable ||
            this is BookingAvailabilityError.Backend
        return BookingAvailabilityUiState.Error(message = message, retryable = retryable)
    }

    private fun List<UserVehicle>.toVehiclesUiState(): BookingVehiclesUiState {
        val vehicles = mapNotNull { it.toBookingVehicleUiOrNull() }
            .sortedWith(compareBy<BookingVehicleUi> { it.name.lowercase() }.thenBy { it.description.lowercase() })
        return if (vehicles.isEmpty()) BookingVehiclesUiState.Empty else BookingVehiclesUiState.Loaded(vehicles)
    }

    private fun UserVehicleError.toVehiclesUiState(): BookingVehiclesUiState {
        return when (this) {
            is UserVehicleError.Unauthenticated -> BookingVehiclesUiState.Unauthenticated
            is UserVehicleError.Permission,
            is UserVehicleError.Validation -> BookingVehiclesUiState.Error(message = message, retryable = false)
            is UserVehicleError.NotFound,
            is UserVehicleError.Unavailable,
            is UserVehicleError.Backend -> BookingVehiclesUiState.Error(message = message, retryable = true)
        }
    }
}

private fun UserVehicle.toBookingVehicleUiOrNull(): BookingVehicleUi? {
    if (id.isBlank() || brand.isBlank() || model.isBlank() || plate.isBlank()) return null
    val label = "$brand $model".trim()
    val normalizedType = type.toBookingVehicleType()
    val colorDetail = color.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()

    return BookingVehicleUi(
        id = "saved:$id",
        name = label,
        description = "$plate$colorDetail • ${normalizedType.toVehicleTypeLabel()}",
        type = normalizedType,
        userVehicleId = id,
        vehicleLabel = label,
    )
}

private fun String.toBookingVehicleType(): String = when (trim().lowercase()) {
    "suv" -> "suv"
    else -> "passenger"
}

private fun String.toVehicleTypeLabel(): String = when (this) {
    "suv" -> "SUV"
    else -> "Passageiros"
}

internal fun ProductsBookingDraft.toCreateRequest(): BookingCreateRequest? {
    val slotStartIso = buildSlotIso(dateId = dateId, time = time) ?: return null
    val slotEndIso = buildSlotEndIso(
        dateId = dateId,
        time = time,
        durationMinutes = serviceDurationMinutes,
    ) ?: return null

    return BookingCreateRequest(
        customerName = customerName,
        customerEmail = customerEmail,
        customerPhone = customerPhone,
        serviceId = serviceId,
        serviceName = serviceName,
        slotStartIso = slotStartIso,
        slotEndIso = slotEndIso,
        vehicleType = when (vehicleType) {
            "passenger" -> "passageiros"
            else -> vehicleType
        },
        userVehicleId = userVehicleId,
        vehicleLabel = vehicleLabel,
        gdprConsent = gdprConsent,
        notes = notes,
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
    return year > 0 && month in 1..12 && day in 1..31
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')

internal fun BookingAvailabilityMonth.monthAnchorDate(): String? {
    return days.firstOrNull()?.id.toMonthAnchorDateOrNull()
}

internal fun String?.toMonthAnchorDateOrNull(): String? {
    val dateId = this ?: return null
    if (!isValidDateId(dateId)) return null
    return dateId.substring(0, 8) + "01"
}

internal fun shiftMonthAnchorDate(dateId: String?, monthOffset: Int): String? {
    val monthAnchor = dateId.toMonthAnchorDateOrNull() ?: return null
    val year = monthAnchor.substring(0, 4).toIntOrNull() ?: return null
    val month = monthAnchor.substring(5, 7).toIntOrNull() ?: return null
    val zeroBasedMonth = month - 1
    val shiftedMonthIndex = year * 12 + zeroBasedMonth + monthOffset
    if (shiftedMonthIndex < 12) return null

    val shiftedYear = shiftedMonthIndex / 12
    val shiftedMonth = shiftedMonthIndex % 12 + 1
    return "$shiftedYear-${shiftedMonth.twoDigits()}-01"
}
