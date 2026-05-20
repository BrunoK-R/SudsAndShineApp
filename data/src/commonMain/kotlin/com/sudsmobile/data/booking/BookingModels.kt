package com.sudsmobile.data.booking

data class BookingCreateRequest(
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val serviceId: String,
    val serviceName: String,
    val slotStartIso: String,
    val slotEndIso: String,
    val vehicleType: String,
    val gdprConsent: Boolean,
    val notes: String,
)

data class BookingReceipt(
    val reservationId: String,
    val reservationCode: String,
)

data class BookingAvailabilityRequest(
    val anchorDate: String? = null,
    val serviceDurationMinutes: Int = 30,
    val slotIntervalMinutes: Int = 30,
)

data class BookingAvailabilityMonth(
    val monthTitle: String,
    val leadingEmptyCells: Int,
    val days: List<BookingAvailabilityDay>,
)

data class BookingAvailabilityDay(
    val id: String,
    val dayOfMonth: Int,
    val dateLabel: String,
    val summaryLabel: String,
    val available: Boolean,
    val slots: List<BookingAvailabilitySlot>,
)

data class BookingAvailabilitySlot(
    val time: String,
    val available: Boolean,
    val remainingCapacity: Int,
)

sealed interface BookingCreateResult {
    data class Success(val receipt: BookingReceipt) : BookingCreateResult
    data class Failure(val error: BookingCreateError) : BookingCreateResult
}

sealed interface BookingAvailabilityResult {
    data class Success(val month: BookingAvailabilityMonth) : BookingAvailabilityResult
    data class Failure(val error: BookingAvailabilityError) : BookingAvailabilityResult
}

sealed interface BookingCreateError {
    val message: String

    data class Validation(override val message: String) : BookingCreateError
    data class Conflict(override val message: String) : BookingCreateError
    data class Permission(override val message: String) : BookingCreateError
    data class Unauthenticated(override val message: String) : BookingCreateError
    data class Unavailable(override val message: String) : BookingCreateError
    data class Backend(override val message: String) : BookingCreateError
}

sealed interface BookingAvailabilityError {
    val message: String

    data class Validation(override val message: String) : BookingAvailabilityError
    data class Permission(override val message: String) : BookingAvailabilityError
    data class Unauthenticated(override val message: String) : BookingAvailabilityError
    data class Unavailable(override val message: String) : BookingAvailabilityError
    data class Backend(override val message: String) : BookingAvailabilityError
}

interface BookingRepository {
    suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult
    suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult
}
