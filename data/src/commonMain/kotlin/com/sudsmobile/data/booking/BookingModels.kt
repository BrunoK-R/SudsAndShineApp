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

sealed interface BookingCreateResult {
    data class Success(val receipt: BookingReceipt) : BookingCreateResult
    data class Failure(val error: BookingCreateError) : BookingCreateResult
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

interface BookingRepository {
    suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult
}
