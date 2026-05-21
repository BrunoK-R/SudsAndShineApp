package com.sudsmobile.data.booking

interface BookingFunctionsApi {
    suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult
    suspend fun createReservation(request: BookingCreateRequest, idToken: String?): BookingCreateResult
    suspend fun getMyReservations(idToken: String): BookingHistoryResult
    suspend fun getMyLoyalty(idToken: String): BookingLoyaltyResult {
        return BookingLoyaltyResult.Failure(
            BookingLoyaltyError.Unavailable("O serviço de recompensas está indisponível."),
        )
    }

    suspend fun submitReservationReview(request: BookingReviewRequest, idToken: String): BookingReviewResult
    suspend fun cancelMyReservation(request: BookingCancelRequest, idToken: String): BookingCancelResult
    suspend fun rescheduleMyReservation(request: BookingRescheduleRequest, idToken: String): BookingRescheduleResult
    suspend fun redeemMyLoyaltyReward(idToken: String): BookingRewardRedemptionResult
}
