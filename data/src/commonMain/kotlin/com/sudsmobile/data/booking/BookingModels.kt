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
    val status: String = "",
    val pendingExpiresAtIso: String? = null,
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
    val rewardType: String = "free_wash",
    val rewardValue: Int = 1,
    val rewardDescription: String = "1 lavagem grátis",
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
    val userVehicleId: String? = null,
    val vehicleLabel: String? = null,
    val priceCents: Int?,
    val upcoming: Boolean,
    val reviewed: Boolean = false,
    val reviewRating: Int? = null,
    val reviewTags: List<String> = emptyList(),
    val reviewComment: String = "",
    val extras: List<BookingReservationExtra> = emptyList(),
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
    val cancelledAtIso: String? = null,
    val rejectedAtIso: String? = null,
    val rejectionReason: String = "",
    val acceptedAtIso: String? = null,
    val startedAtIso: String? = null,
    val completedAtIso: String? = null,
    val paymentConfirmedAtIso: String? = null,
    val pendingExpiresAtIso: String? = null,
    val rescheduledAtIso: String? = null,
    val previousSlotStartIso: String? = null,
    val previousSlotEndIso: String? = null,
    val rescheduleCount: Int = 0,
    val loyaltyRewardApplied: Boolean = false,
    val loyaltyRewardCode: String = "",
    val loyaltyRewardDescription: String = "",
    val loyaltyStampGranted: Boolean? = null,
)

enum class BookingReservationStatus {
    Pending,
    Confirmed,
    InProgress,
    Completed,
    Cancelled,
    Rejected,
    Expired,
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

data class BookingRescheduleRequest(
    val reservationId: String,
    val slotStartIso: String,
    val slotEndIso: String,
)

data class BookingRescheduleReceipt(
    val reservationId: String,
    val status: String,
    val slotStartIso: String,
    val slotEndIso: String,
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
    val slotIntervalMinutes: Int? = null,
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
    val waitlistEligible: Boolean = false,
    val slots: List<BookingAvailabilitySlot>,
)

data class BookingAvailabilitySlot(
    val time: String,
    val available: Boolean,
    val remainingCapacity: Int,
)

data class BookingWaitlistJoinRequest(
    val dateId: String,
    val serviceId: String,
    val serviceName: String,
    val serviceDurationMinutes: Int,
)

data class BookingWaitlistEntry(
    val id: String,
    val dateId: String,
    val serviceId: String,
    val serviceName: String,
    val serviceDurationMinutes: Int,
    val status: String,
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
    val notifiedAtIso: String? = null,
)

data class BookingWaitlistActionReceipt(
    val waitlistId: String,
    val status: String,
    val entry: BookingWaitlistEntry? = null,
)

data class BookingPreset(
    val id: String,
    val label: String,
    val serviceId: String,
    val extraIds: List<String>,
    val userVehicleId: String? = null,
    val vehicleType: String,
    val vehicleLabel: String? = null,
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
)

data class BookingSelectionPreset(
    val serviceId: String,
    val extraIds: List<String> = emptyList(),
    val userVehicleId: String? = null,
    val vehicleType: String = "passenger",
    val vehicleLabel: String? = null,
)

fun BookingPreset.toSelectionPreset(): BookingSelectionPreset = BookingSelectionPreset(
    serviceId = serviceId,
    extraIds = extraIds,
    userVehicleId = userVehicleId,
    vehicleType = vehicleType,
    vehicleLabel = vehicleLabel,
)

data class BookingPresetUpsertRequest(
    val presetId: String? = null,
    val label: String,
    val serviceId: String,
    val extraIds: List<String>,
    val userVehicleId: String? = null,
    val vehicleType: String,
    val vehicleLabel: String? = null,
)

data class BookingPresetList(
    val presets: List<BookingPreset>,
    val maxPresets: Int = 5,
)

sealed interface BookingPresetListResult {
    data class Success(val list: BookingPresetList) : BookingPresetListResult
    data class Failure(val error: BookingPresetError) : BookingPresetListResult
}

sealed interface BookingPresetSaveResult {
    data class Success(val preset: BookingPreset, val maxPresets: Int = 5) : BookingPresetSaveResult
    data class Failure(val error: BookingPresetError) : BookingPresetSaveResult
}

sealed interface BookingPresetDeleteResult {
    data class Success(val presetId: String) : BookingPresetDeleteResult
    data class Failure(val error: BookingPresetError) : BookingPresetDeleteResult
}

sealed interface BookingPresetError {
    val message: String

    data class Validation(override val message: String) : BookingPresetError
    data class Permission(override val message: String) : BookingPresetError
    data class Unauthenticated(override val message: String) : BookingPresetError
    data class NotFound(override val message: String) : BookingPresetError
    data class LimitReached(override val message: String) : BookingPresetError
    data class Unavailable(override val message: String) : BookingPresetError
    data class Backend(override val message: String) : BookingPresetError
}

sealed interface BookingWaitlistListResult {
    data class Success(val entries: List<BookingWaitlistEntry>) : BookingWaitlistListResult
    data class Failure(val error: BookingWaitlistError) : BookingWaitlistListResult
}

sealed interface BookingWaitlistActionResult {
    data class Success(val receipt: BookingWaitlistActionReceipt) : BookingWaitlistActionResult
    data class Failure(val error: BookingWaitlistError) : BookingWaitlistActionResult
}

sealed interface BookingWaitlistError {
    val message: String

    data class Validation(override val message: String) : BookingWaitlistError
    data class Permission(override val message: String) : BookingWaitlistError
    data class Unauthenticated(override val message: String) : BookingWaitlistError
    data class NotFound(override val message: String) : BookingWaitlistError
    data class Unavailable(override val message: String) : BookingWaitlistError
    data class Backend(override val message: String) : BookingWaitlistError
}

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

sealed interface BookingRescheduleResult {
    data class Success(val receipt: BookingRescheduleReceipt) : BookingRescheduleResult
    data class Failure(val error: BookingRescheduleError) : BookingRescheduleResult
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

sealed interface BookingRescheduleError {
    val message: String

    data class Validation(override val message: String) : BookingRescheduleError
    data class Conflict(override val message: String) : BookingRescheduleError
    data class Permission(override val message: String) : BookingRescheduleError
    data class Unauthenticated(override val message: String) : BookingRescheduleError
    data class NotFound(override val message: String) : BookingRescheduleError
    data class NotReschedulable(override val message: String) : BookingRescheduleError
    data class Unavailable(override val message: String) : BookingRescheduleError
    data class Backend(override val message: String) : BookingRescheduleError
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
    suspend fun getMyBookingPresets(): BookingPresetListResult = BookingPresetListResult.Failure(
        BookingPresetError.Unavailable("As marcações favoritas ainda não estão disponíveis."),
    )

    suspend fun saveBookingPreset(request: BookingPresetUpsertRequest): BookingPresetSaveResult =
        BookingPresetSaveResult.Failure(
            BookingPresetError.Unavailable("Não foi possível guardar esta marcação favorita."),
        )

    suspend fun deleteBookingPreset(presetId: String): BookingPresetDeleteResult =
        BookingPresetDeleteResult.Failure(
            BookingPresetError.Unavailable("Não foi possível eliminar esta marcação favorita."),
        )
    suspend fun getMyWaitlist(): BookingWaitlistListResult {
        return BookingWaitlistListResult.Failure(
            BookingWaitlistError.Unavailable("Os avisos de vaga ainda não estão disponíveis."),
        )
    }

    suspend fun joinWaitlist(request: BookingWaitlistJoinRequest): BookingWaitlistActionResult {
        return BookingWaitlistActionResult.Failure(
            BookingWaitlistError.Unavailable("Os avisos de vaga ainda não estão disponíveis."),
        )
    }

    suspend fun cancelWaitlist(waitlistId: String): BookingWaitlistActionResult {
        return BookingWaitlistActionResult.Failure(
            BookingWaitlistError.Unavailable("Os avisos de vaga ainda não estão disponíveis."),
        )
    }
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

    suspend fun rescheduleBooking(request: BookingRescheduleRequest): BookingRescheduleResult {
        return BookingRescheduleResult.Failure(
            BookingRescheduleError.Unavailable("A remarcação ainda não está disponível."),
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
        "confirmed", "confirmado", "accepted", "aceite", "aceita", "aprovado", "aprovada" ->
            BookingReservationStatus.Confirmed
        "in_progress", "em_execucao", "em_execução", "em_curso", "a_decorrer", "decorrer", "running", "started" ->
            BookingReservationStatus.InProgress
        "completed", "complete", "done", "concluido", "concluído", "concluida", "concluída", "finalizado", "finalizada" ->
            BookingReservationStatus.Completed
        "cancelled", "canceled", "cancelado", "cancelada" -> BookingReservationStatus.Cancelled
        "rejected", "rejeitado", "rejeitada", "recusado", "recusada" -> BookingReservationStatus.Rejected
        "expired", "expirado", "expirada" -> BookingReservationStatus.Expired
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
        bookingReservationStatus() !in nonPayableReservationStatuses &&
        (priceCents ?: 0) > 0 &&
        bookingPaymentStatus() in payableBookingPaymentStatuses
}

fun BookingHistoryReservation.isCancelledReservation(): Boolean {
    return bookingReservationStatus() == BookingReservationStatus.Cancelled
}

fun BookingHistoryReservation.isCompletedReservation(): Boolean {
    val status = bookingReservationStatus()
    return status == BookingReservationStatus.Completed ||
        (!upcoming && status !in nonCompletedClosedReservationStatuses)
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

private val nonCompletedClosedReservationStatuses = setOf(
    BookingReservationStatus.Pending,
    BookingReservationStatus.Confirmed,
    BookingReservationStatus.InProgress,
    BookingReservationStatus.Cancelled,
    BookingReservationStatus.Rejected,
    BookingReservationStatus.Expired,
)

private val nonPayableReservationStatuses = setOf(
    BookingReservationStatus.Pending,
    BookingReservationStatus.Cancelled,
    BookingReservationStatus.Rejected,
    BookingReservationStatus.Expired,
)

private val payableBookingPaymentStatuses = setOf(
    BookingPaymentStatus.Pending,
    BookingPaymentStatus.Failed,
)
