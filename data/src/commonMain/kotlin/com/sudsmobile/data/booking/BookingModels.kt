package com.sudsmobile.data.booking

import com.sudsmobile.shared.loyalty.LoyaltyProgress

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
    val userVehicleId: String? = null,
    val vehicleLabel: String? = null,
    val loyaltyRewardCode: String? = null,
    val extraIds: List<String> = emptyList(),
)

data class BookingReceipt(
    val reservationId: String,
    val reservationCode: String,
    val loyaltyRewardApplied: Boolean = false,
    val loyaltyRewardCode: String? = null,
    val priceCents: Int? = null,
    val discountCents: Int? = null,
    val extras: List<BookingReservationExtra> = emptyList(),
    val paymentStatus: String = "",
)

data class BookingReservationExtra(
    val id: String,
    val name: String,
    val priceCents: Int,
)

data class BookingHistory(
    val reservations: List<BookingHistoryReservation>,
    val loyalty: BookingLoyaltySummary? = null,
)

data class BookingLoyaltySummary(
    val totalWashes: Int,
    val currentWashes: Int,
    val targetWashes: Int,
    val remainingWashes: Int,
    val progress: Float,
    val rewardReady: Boolean,
    val completedRewards: Int,
    val claimedRewards: Int,
    val availableRewards: Int,
)

data class BookingLoyalty(
    val summary: BookingLoyaltySummary,
    val stampHistory: List<BookingLoyaltyStamp>,
    val redemptions: List<BookingLoyaltyRedemption>,
)

data class BookingLoyaltyStamp(
    val id: String,
    val serviceId: String,
    val serviceName: String,
    val slotStartIso: String,
    val slotEndIso: String,
    val points: Int,
)

data class BookingLoyaltyRedemption(
    val id: String,
    val rewardCode: String,
    val rewardNumber: Int?,
    val status: String,
    val createdAtIso: String,
)

data class BookingHistoryReservation(
    val id: String,
    val reservationCode: String,
    val serviceId: String,
    val serviceName: String,
    val slotStartIso: String,
    val slotEndIso: String,
    val status: String,
    val paymentStatus: String = "",
    val vehicleType: String,
    val vehicleLabel: String? = null,
    val priceCents: Int?,
    val upcoming: Boolean,
    val reviewed: Boolean = false,
    val reviewRating: Int? = null,
    val reviewTags: List<String> = emptyList(),
    val extras: List<BookingReservationExtra> = emptyList(),
)

enum class BookingReservationStatus {
    Pending,
    Confirmed,
    InProgress,
    Completed,
    Cancelled,
    Unknown,
}

enum class BookingPaymentStatus {
    Pending,
    Paid,
    CoveredByLoyalty,
    Refunded,
    Failed,
    Unknown,
}

data class BookingReviewRequest(
    val reservationId: String,
    val rating: Int,
    val tags: List<String>,
    val comment: String,
)

data class BookingReviewReceipt(
    val reviewId: String,
    val reservationId: String,
)

data class BookingCancelRequest(
    val reservationId: String,
)

data class BookingCancelReceipt(
    val reservationId: String,
    val status: String,
)

data class BookingRewardRedemptionReceipt(
    val redemptionId: String,
    val rewardCode: String,
    val rewardNumber: Int,
    val status: String,
    val loyalty: BookingLoyaltySummary,
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

sealed interface BookingHistoryResult {
    data class Success(val history: BookingHistory) : BookingHistoryResult
    data class Failure(val error: BookingHistoryError) : BookingHistoryResult
}

sealed interface BookingLoyaltyResult {
    data class Success(val loyalty: BookingLoyalty) : BookingLoyaltyResult
    data class Failure(val error: BookingLoyaltyError) : BookingLoyaltyResult
}

sealed interface BookingReviewResult {
    data class Success(val receipt: BookingReviewReceipt) : BookingReviewResult
    data class Failure(val error: BookingReviewError) : BookingReviewResult
}

sealed interface BookingCancelResult {
    data class Success(val receipt: BookingCancelReceipt) : BookingCancelResult
    data class Failure(val error: BookingCancelError) : BookingCancelResult
}

sealed interface BookingRewardRedemptionResult {
    data class Success(val receipt: BookingRewardRedemptionReceipt) : BookingRewardRedemptionResult
    data class Failure(val error: BookingRewardRedemptionError) : BookingRewardRedemptionResult
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

sealed interface BookingHistoryError {
    val message: String

    data class Permission(override val message: String) : BookingHistoryError
    data class Unauthenticated(override val message: String) : BookingHistoryError
    data class Unavailable(override val message: String) : BookingHistoryError
    data class Backend(override val message: String) : BookingHistoryError
}

sealed interface BookingLoyaltyError {
    val message: String

    data class Permission(override val message: String) : BookingLoyaltyError
    data class Unauthenticated(override val message: String) : BookingLoyaltyError
    data class Unavailable(override val message: String) : BookingLoyaltyError
    data class Backend(override val message: String) : BookingLoyaltyError
}

sealed interface BookingReviewError {
    val message: String

    data class Validation(override val message: String) : BookingReviewError
    data class Permission(override val message: String) : BookingReviewError
    data class Unauthenticated(override val message: String) : BookingReviewError
    data class NotFound(override val message: String) : BookingReviewError
    data class NotReviewable(override val message: String) : BookingReviewError
    data class Unavailable(override val message: String) : BookingReviewError
    data class Backend(override val message: String) : BookingReviewError
}

sealed interface BookingCancelError {
    val message: String

    data class Validation(override val message: String) : BookingCancelError
    data class Permission(override val message: String) : BookingCancelError
    data class Unauthenticated(override val message: String) : BookingCancelError
    data class NotFound(override val message: String) : BookingCancelError
    data class NotCancelable(override val message: String) : BookingCancelError
    data class Unavailable(override val message: String) : BookingCancelError
    data class Backend(override val message: String) : BookingCancelError
}

sealed interface BookingRewardRedemptionError {
    val message: String

    data class Permission(override val message: String) : BookingRewardRedemptionError
    data class Unauthenticated(override val message: String) : BookingRewardRedemptionError
    data class NotAvailable(override val message: String) : BookingRewardRedemptionError
    data class AlreadyClaimed(override val message: String) : BookingRewardRedemptionError
    data class Unavailable(override val message: String) : BookingRewardRedemptionError
    data class Backend(override val message: String) : BookingRewardRedemptionError
}

interface BookingRepository {
    suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult
    suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult
    suspend fun getMyBookings(): BookingHistoryResult
    suspend fun getMyLoyalty(): BookingLoyaltyResult {
        return BookingLoyaltyResult.Failure(
            BookingLoyaltyError.Unavailable("O programa de recompensas ainda não está disponível."),
        )
    }

    suspend fun submitReview(request: BookingReviewRequest): BookingReviewResult {
        return BookingReviewResult.Failure(
            BookingReviewError.Unavailable("O envio de avaliações ainda não está disponível."),
        )
    }

    suspend fun cancelBooking(request: BookingCancelRequest): BookingCancelResult {
        return BookingCancelResult.Failure(
            BookingCancelError.Unavailable("O cancelamento de marcações ainda não está disponível."),
        )
    }

    suspend fun redeemLoyaltyReward(): BookingRewardRedemptionResult {
        return BookingRewardRedemptionResult.Failure(
            BookingRewardRedemptionError.Unavailable("O resgate de recompensas ainda não está disponível."),
        )
    }
}

fun BookingLoyaltySummary.toLoyaltyProgress(): LoyaltyProgress = LoyaltyProgress(
    totalWashes = totalWashes,
    currentWashes = currentWashes,
    targetWashes = targetWashes.coerceAtLeast(1),
    remainingWashes = remainingWashes.coerceAtLeast(0),
    progress = progress.coerceIn(0f, 1f),
    rewardReady = rewardReady,
    completedRewards = completedRewards.coerceAtLeast(0),
    claimedRewards = claimedRewards.coerceAtLeast(0),
    availableRewards = availableRewards.coerceAtLeast(0),
)

fun String.toBookingReservationStatus(): BookingReservationStatus {
    val normalized = trim()
        .lowercase()
        .replace("-", "_")
        .replace(" ", "_")

    return when (normalized) {
        "pending", "novo", "new" -> BookingReservationStatus.Pending
        "confirmed", "confirmado" -> BookingReservationStatus.Confirmed
        "in_progress", "em_execucao", "em_execução" -> BookingReservationStatus.InProgress
        "completed", "complete", "done", "concluido", "concluído" -> BookingReservationStatus.Completed
        "cancelled", "canceled", "cancelado" -> BookingReservationStatus.Cancelled
        else -> BookingReservationStatus.Unknown
    }
}

fun BookingHistoryReservation.bookingReservationStatus(): BookingReservationStatus {
    return status.toBookingReservationStatus()
}

fun String.toBookingPaymentStatus(): BookingPaymentStatus {
    val normalized = trim()
        .lowercase()
        .replace("-", "_")
        .replace(" ", "_")

    return when (normalized) {
        "pending", "unpaid", "waiting_for_payment", "awaiting_payment", "pendente" -> BookingPaymentStatus.Pending
        "paid", "pago", "succeeded", "complete", "completed" -> BookingPaymentStatus.Paid
        "covered_by_loyalty", "loyalty", "reward", "recompensa" -> BookingPaymentStatus.CoveredByLoyalty
        "refunded", "refund", "reembolsado" -> BookingPaymentStatus.Refunded
        "failed", "declined", "falhou" -> BookingPaymentStatus.Failed
        else -> BookingPaymentStatus.Unknown
    }
}

fun BookingHistoryReservation.bookingPaymentStatus(): BookingPaymentStatus {
    return paymentStatus.toBookingPaymentStatus()
}

fun BookingHistoryReservation.requiresPayment(): Boolean {
    return upcoming &&
        !isCancelledReservation() &&
        (priceCents ?: 0) > 0 &&
        bookingPaymentStatus() in payableBookingPaymentStatuses
}

fun BookingHistoryReservation.isCancelledReservation(): Boolean {
    return bookingReservationStatus() == BookingReservationStatus.Cancelled
}

fun BookingHistoryReservation.isCompletedReservation(): Boolean {
    return bookingReservationStatus() == BookingReservationStatus.Completed ||
        (!upcoming && !isCancelledReservation())
}

fun BookingHistoryReservation.isReviewableReservation(): Boolean {
    return isCompletedReservation()
}

fun BookingHistoryReservation.isCancelableReservation(): Boolean {
    return upcoming && bookingReservationStatus() in cancelableReservationStatuses
}

private val cancelableReservationStatuses = setOf(
    BookingReservationStatus.Pending,
    BookingReservationStatus.Confirmed,
)

private val payableBookingPaymentStatuses = setOf(
    BookingPaymentStatus.Pending,
    BookingPaymentStatus.Failed,
)
