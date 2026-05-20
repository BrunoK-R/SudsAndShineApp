package com.sudsmobile.data.booking

import com.sudsmobile.data.auth.AuthRepository

class FirebaseBookingRepository(
    private val api: BookingFunctionsApi,
    private val authRepository: AuthRepository,
    private val bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
) : BookingRepository {
    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        val validationError = validate(request)
        if (validationError != null) {
            return BookingAvailabilityResult.Failure(validationError)
        }

        return api.getAvailability(request)
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) {
            return BookingCreateResult.Failure(validationError)
        }

        val session = authRepository.currentSession()
        if (!normalizedRequest.loyaltyRewardCode.isNullOrBlank() && session == null) {
            return BookingCreateResult.Failure(
                BookingCreateError.Unauthenticated("Inicie sessão para aplicar esta recompensa."),
            )
        }

        return api.createReservation(normalizedRequest, session?.idToken)
            .also { result ->
                if (result is BookingCreateResult.Success) {
                    bookingChangeNotifier.notifyBookingsChanged()
                }
            }
    }

    override suspend fun getMyBookings(): BookingHistoryResult {
        val session = authRepository.currentSession()
            ?: return BookingHistoryResult.Failure(
                BookingHistoryError.Unauthenticated("Inicie sessão para ver as suas marcações."),
            )

        return api.getMyReservations(session.idToken)
    }

    override suspend fun submitReview(request: BookingReviewRequest): BookingReviewResult {
        val validationError = validate(request)
        if (validationError != null) {
            return BookingReviewResult.Failure(validationError)
        }

        val session = authRepository.currentSession()
            ?: return BookingReviewResult.Failure(
                BookingReviewError.Unauthenticated("Inicie sessão para avaliar esta marcação."),
            )

        return api.submitReservationReview(request.normalized(), session.idToken)
            .also { result ->
                if (result is BookingReviewResult.Success) {
                    bookingChangeNotifier.notifyBookingsChanged()
                }
            }
    }

    override suspend fun cancelBooking(request: BookingCancelRequest): BookingCancelResult {
        val validationError = validate(request)
        if (validationError != null) {
            return BookingCancelResult.Failure(validationError)
        }

        val session = authRepository.currentSession()
            ?: return BookingCancelResult.Failure(
                BookingCancelError.Unauthenticated("Inicie sessão para cancelar esta marcação."),
            )

        return api.cancelMyReservation(request.normalized(), session.idToken)
            .also { result ->
                if (result is BookingCancelResult.Success) {
                    bookingChangeNotifier.notifyBookingsChanged()
                }
            }
    }

    override suspend fun redeemLoyaltyReward(): BookingRewardRedemptionResult {
        val session = authRepository.currentSession()
            ?: return BookingRewardRedemptionResult.Failure(
                BookingRewardRedemptionError.Unauthenticated("Inicie sessão para resgatar recompensas."),
            )

        return api.redeemMyLoyaltyReward(session.idToken)
            .also { result ->
                if (result is BookingRewardRedemptionResult.Success) {
                    bookingChangeNotifier.notifyBookingsChanged()
                }
            }
    }

    private fun validate(request: BookingAvailabilityRequest): BookingAvailabilityError? {
        return when {
            request.anchorDate != null && !isValidDateId(request.anchorDate) ->
                BookingAvailabilityError.Validation("A data de disponibilidade é inválida.")
            request.serviceDurationMinutes !in 5..480 ->
                BookingAvailabilityError.Validation("A duração do serviço é inválida.")
            request.slotIntervalMinutes !in 5..240 ->
                BookingAvailabilityError.Validation("O intervalo de horários é inválido.")
            else -> null
        }
    }

    private fun validate(request: BookingCreateRequest): BookingCreateError? {
        return when {
            request.customerName.isBlank() -> BookingCreateError.Validation("Indique o nome para a marcação.")
            !request.customerEmail.trim().contains("@") -> BookingCreateError.Validation("Indique um email válido.")
            request.serviceId.isBlank() -> BookingCreateError.Validation("Escolha um serviço antes de confirmar.")
            request.slotStartIso.isBlank() || request.slotEndIso.isBlank() ->
                BookingCreateError.Validation("Escolha uma data e hora válidas.")
            request.vehicleType !in setOf("passageiros", "suv") ->
                BookingCreateError.Validation("Escolha um tipo de veículo válido.")
            request.userVehicleId?.contains("/") == true ->
                BookingCreateError.Validation("Escolha um veículo guardado válido.")
            request.loyaltyRewardCode?.contains("/") == true || request.loyaltyRewardCode?.length.orZero() > 80 ->
                BookingCreateError.Validation("Indique um código de recompensa válido.")
            !request.gdprConsent -> BookingCreateError.Validation("Aceite a política de privacidade para continuar.")
            else -> null
        }
    }

    private fun validate(request: BookingReviewRequest): BookingReviewError? {
        return when {
            request.reservationId.isBlank() || request.reservationId.contains("/") ->
                BookingReviewError.Validation("A marcação selecionada é inválida.")
            request.rating !in 1..5 ->
                BookingReviewError.Validation("Escolha uma avaliação entre 1 e 5 estrelas.")
            request.tags.any { it.isBlank() || it.length > 40 } ->
                BookingReviewError.Validation("Os destaques da avaliação são inválidos.")
            request.comment.length > 1000 ->
                BookingReviewError.Validation("O comentário deve ter no máximo 1000 caracteres.")
            else -> null
        }
    }

    private fun validate(request: BookingCancelRequest): BookingCancelError? {
        return when {
            request.reservationId.isBlank() || request.reservationId.contains("/") ->
                BookingCancelError.Validation("A marcação selecionada é inválida.")
            request.reservationId.length > 160 ->
                BookingCancelError.Validation("A marcação selecionada é inválida.")
            else -> null
        }
    }

    private fun BookingCreateRequest.normalized(): BookingCreateRequest = copy(
        customerName = customerName.trim(),
        customerEmail = customerEmail.trim().lowercase(),
        customerPhone = customerPhone.trim(),
        serviceId = serviceId.trim(),
        serviceName = serviceName.trim(),
        vehicleType = vehicleType.trim(),
        notes = notes.trim(),
        userVehicleId = userVehicleId?.trim()?.takeIf { it.isNotBlank() },
        vehicleLabel = vehicleLabel?.trim()?.takeIf { it.isNotBlank() },
        loyaltyRewardCode = loyaltyRewardCode
            ?.trim()
            ?.replace(Regex("\\s+"), "")
            ?.uppercase()
            ?.takeIf { it.isNotBlank() },
    )

    private fun BookingReviewRequest.normalized(): BookingReviewRequest = copy(
        reservationId = reservationId.trim(),
        tags = tags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(8),
        comment = comment.trim(),
    )

    private fun BookingCancelRequest.normalized(): BookingCancelRequest = copy(
        reservationId = reservationId.trim(),
    )

}

private fun isValidDateId(dateId: String): Boolean {
    if (dateId.length != 10) return false
    return dateId[4] == '-' && dateId[7] == '-'
}

private fun Int?.orZero(): Int = this ?: 0
