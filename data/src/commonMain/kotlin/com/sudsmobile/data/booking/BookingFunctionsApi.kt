package com.sudsmobile.data.booking

interface BookingFunctionsApi {
    suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult
    suspend fun createReservation(request: BookingCreateRequest, idToken: String?): BookingCreateResult
    suspend fun getMyReservations(idToken: String): BookingHistoryResult
    suspend fun submitReservationReview(request: BookingReviewRequest, idToken: String): BookingReviewResult
    suspend fun cancelMyReservation(request: BookingCancelRequest, idToken: String): BookingCancelResult
    suspend fun redeemMyLoyaltyReward(idToken: String): BookingRewardRedemptionResult
}
