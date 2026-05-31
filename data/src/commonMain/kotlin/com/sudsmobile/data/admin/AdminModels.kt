package com.sudsmobile.data.admin

import com.sudsmobile.data.booking.BookingReservationExtra

data class AdminRole(
    val uid: String,
    val email: String,
    val role: String,
) {
    val isAdmin: Boolean
        get() = role == "admin"
}

data class AdminBookingRequest(
    val id: String,
    val reservationCode: String,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val serviceId: String,
    val serviceName: String,
    val slotStartIso: String,
    val slotEndIso: String,
    val status: String,
    val paymentStatus: String,
    val vehicleType: String,
    val vehicleLabel: String,
    val priceCents: Int?,
    val extras: List<BookingReservationExtra>,
    val notes: String,
    val createdAtIso: String,
    val pendingExpiresAtIso: String?,
    val loyaltyRewardApplied: Boolean,
)

data class AdminBookingDecisionRequest(
    val reservationId: String,
    val rejectionReason: String = "",
)

data class AdminBookingDecisionReceipt(
    val reservationId: String,
    val reservationCode: String,
    val status: String,
)

sealed interface AdminRoleResult {
    data class Success(val role: AdminRole) : AdminRoleResult
    data class Failure(val error: AdminError) : AdminRoleResult
}

sealed interface AdminBookingRequestsResult {
    data class Success(val requests: List<AdminBookingRequest>) : AdminBookingRequestsResult
    data class Failure(val error: AdminError) : AdminBookingRequestsResult
}

sealed interface AdminBookingDecisionResult {
    data class Success(val receipt: AdminBookingDecisionReceipt) : AdminBookingDecisionResult
    data class Failure(val error: AdminError) : AdminBookingDecisionResult
}

sealed interface AdminError {
    val message: String

    data class Validation(override val message: String) : AdminError
    data class Permission(override val message: String) : AdminError
    data class Unauthenticated(override val message: String) : AdminError
    data class NotFound(override val message: String) : AdminError
    data class Conflict(override val message: String) : AdminError
    data class Unavailable(override val message: String) : AdminError
    data class Backend(override val message: String) : AdminError
}

interface AdminRepository {
    suspend fun syncMyRole(): AdminRoleResult
    suspend fun getPendingBookingRequests(): AdminBookingRequestsResult
    suspend fun acceptBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult
    suspend fun rejectBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult
}
