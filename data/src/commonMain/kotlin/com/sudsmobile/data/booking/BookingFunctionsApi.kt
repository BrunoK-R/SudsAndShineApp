package com.sudsmobile.data.booking

interface BookingFunctionsApi {
    suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult
    suspend fun createReservation(request: BookingCreateRequest, idToken: String?): BookingCreateResult
    suspend fun getMyBookingPresets(idToken: String): BookingPresetListResult = BookingPresetListResult.Failure(
        BookingPresetError.Unavailable("As marcações favoritas estão indisponíveis."),
    )

    suspend fun upsertMyBookingPreset(
        request: BookingPresetUpsertRequest,
        idToken: String,
    ): BookingPresetSaveResult = BookingPresetSaveResult.Failure(
        BookingPresetError.Unavailable("Não foi possível guardar esta marcação favorita."),
    )

    suspend fun deleteMyBookingPreset(presetId: String, idToken: String): BookingPresetDeleteResult =
        BookingPresetDeleteResult.Failure(
            BookingPresetError.Unavailable("Não foi possível eliminar esta marcação favorita."),
        )
    suspend fun getMyWaitlist(idToken: String): BookingWaitlistListResult {
        return BookingWaitlistListResult.Failure(
            BookingWaitlistError.Unavailable("O serviço de avisos de vaga está indisponível."),
        )
    }

    suspend fun joinMyWaitlist(
        request: BookingWaitlistJoinRequest,
        idToken: String,
    ): BookingWaitlistActionResult {
        return BookingWaitlistActionResult.Failure(
            BookingWaitlistError.Unavailable("O serviço de avisos de vaga está indisponível."),
        )
    }

    suspend fun cancelMyWaitlist(waitlistId: String, idToken: String): BookingWaitlistActionResult {
        return BookingWaitlistActionResult.Failure(
            BookingWaitlistError.Unavailable("O serviço de avisos de vaga está indisponível."),
        )
    }
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
