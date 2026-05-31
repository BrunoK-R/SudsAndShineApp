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

data class AdminServiceCatalogMutationRequest(
    val serviceId: String = "",
    val name: String,
    val description: String = "",
    val durationMinutes: Int,
    val passengerPriceCents: Int,
    val suvPriceCents: Int,
    val iconKey: String = "car",
    val popular: Boolean = false,
    val active: Boolean = true,
    val sortOrder: Int = 999,
)

data class AdminServiceCatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val passengerPriceCents: Int,
    val suvPriceCents: Int,
    val iconKey: String,
    val popular: Boolean,
    val active: Boolean,
    val sortOrder: Int,
)

data class AdminServiceCatalogConfig(
    val services: List<AdminServiceCatalogItem>,
)

data class AdminServiceCatalogArchiveRequest(
    val serviceId: String,
)

data class AdminServiceCatalogMutationReceipt(
    val serviceId: String,
    val status: String,
    val created: Boolean = false,
)

data class AdminBusinessInfoConfig(
    val phone: String,
    val email: String,
    val addressLine1: String,
    val addressLine2: String,
    val mapsUri: String,
    val whatsappUri: String,
    val openingHours: List<AdminBusinessOpeningHours>,
    val socialLinks: List<AdminBusinessSocialLink>,
)

data class AdminBusinessOpeningHours(
    val dayLabel: String,
    val hoursLabel: String,
    val closed: Boolean,
)

data class AdminBusinessSocialLink(
    val label: String,
    val uri: String,
)

data class AdminBusinessInfoUpdateRequest(
    val phone: String,
    val email: String,
    val addressLine1: String,
    val addressLine2: String,
    val mapsUri: String,
    val whatsappUri: String,
    val openingHours: List<AdminBusinessOpeningHours>,
    val socialLinks: List<AdminBusinessSocialLink>,
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

sealed interface AdminServiceCatalogMutationResult {
    data class Success(val receipt: AdminServiceCatalogMutationReceipt) : AdminServiceCatalogMutationResult
    data class Failure(val error: AdminError) : AdminServiceCatalogMutationResult
}

sealed interface AdminServiceCatalogResult {
    data class Success(val config: AdminServiceCatalogConfig) : AdminServiceCatalogResult
    data class Failure(val error: AdminError) : AdminServiceCatalogResult
}

sealed interface AdminBusinessInfoResult {
    data class Success(val config: AdminBusinessInfoConfig) : AdminBusinessInfoResult
    data class Failure(val error: AdminError) : AdminBusinessInfoResult
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
    suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult
    suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult
    suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult

    suspend fun acceptBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult
    suspend fun rejectBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult
    suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
    ): AdminServiceCatalogMutationResult

    suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
    ): AdminServiceCatalogMutationResult
}
