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
        if (session == null) {
            val unauthenticatedMessage = when {
                !normalizedRequest.loyaltyRewardCode.isNullOrBlank() ->
                    "Inicie sessão para aplicar esta recompensa."
                !normalizedRequest.userVehicleId.isNullOrBlank() ->
                    "Inicie sessão para usar este veículo guardado."
                else -> null
            }
            if (unauthenticatedMessage != null) {
                return BookingCreateResult.Failure(
                    BookingCreateError.Unauthenticated(unauthenticatedMessage),
                )
            }
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

    override suspend fun getMyLoyalty(): BookingLoyaltyResult {
        val session = authRepository.currentSession()
            ?: return BookingLoyaltyResult.Failure(
                BookingLoyaltyError.Unauthenticated("Inicie sessão para ver as suas recompensas."),
            )

        return api.getMyLoyalty(session.idToken)
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

    override suspend fun rescheduleBooking(request: BookingRescheduleRequest): BookingRescheduleResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) {
            return BookingRescheduleResult.Failure(validationError)
        }

        val session = authRepository.currentSession()
            ?: return BookingRescheduleResult.Failure(
                BookingRescheduleError.Unauthenticated("Inicie sessão para remarcar esta marcação."),
            )

        return api.rescheduleMyReservation(normalizedRequest, session.idToken)
            .also { result ->
                if (result is BookingRescheduleResult.Success) {
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
            request.slotIntervalMinutes != null && request.slotIntervalMinutes !in 5..240 ->
                BookingAvailabilityError.Validation("O intervalo de horários é inválido.")
            else -> null
        }
    }

    private fun validate(request: BookingCreateRequest): BookingCreateError? {
        return when {
            request.customerName.isBlank() -> BookingCreateError.Validation("Indique o nome para a marcação.")
            !request.customerEmail.trim().contains("@") -> BookingCreateError.Validation("Indique um email válido.")
            request.serviceId.isBlank() -> BookingCreateError.Validation("Escolha um serviço antes de confirmar.")
            !isValidSlotIso(request.slotStartIso) || !isValidSlotIso(request.slotEndIso) ->
                BookingCreateError.Validation("Escolha uma data e hora válidas.")
            request.slotEndIso <= request.slotStartIso ->
                BookingCreateError.Validation("Escolha uma hora de fim válida.")
            request.vehicleType !in setOf("passageiros", "suv") ->
                BookingCreateError.Validation("Escolha um tipo de veículo válido.")
            request.userVehicleId?.contains("/") == true ->
                BookingCreateError.Validation("Escolha um veículo guardado válido.")
            request.loyaltyRewardCode?.contains("/") == true || request.loyaltyRewardCode?.length.orZero() > 80 ->
                BookingCreateError.Validation("Indique um código de recompensa válido.")
            request.extraIds.size > 12 ->
                BookingCreateError.Validation("Escolha no máximo 12 extras para esta marcação.")
            request.extraIds.any { it.isBlank() || it.contains("/") || it.length > 120 } ->
                BookingCreateError.Validation("Escolha extras válidos para esta marcação.")
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

    private fun validate(request: BookingRescheduleRequest): BookingRescheduleError? {
        return when {
            request.reservationId.isBlank() || request.reservationId.contains("/") ->
                BookingRescheduleError.Validation("A marcação selecionada é inválida.")
            request.reservationId.length > 160 ->
                BookingRescheduleError.Validation("A marcação selecionada é inválida.")
            !isValidSlotIso(request.slotStartIso) || !isValidSlotIso(request.slotEndIso) ->
                BookingRescheduleError.Validation("Escolha uma data e hora válidas.")
            request.slotEndIso <= request.slotStartIso ->
                BookingRescheduleError.Validation("Escolha uma hora de fim válida.")
            else -> null
        }
    }

    private fun BookingCreateRequest.normalized(): BookingCreateRequest = copy(
        customerName = customerName.trim(),
        customerEmail = customerEmail.trim().lowercase(),
        customerPhone = customerPhone.trim(),
        serviceId = serviceId.trim(),
        serviceName = serviceName.trim(),
        slotStartIso = slotStartIso.trim(),
        slotEndIso = slotEndIso.trim(),
        vehicleType = vehicleType.trim(),
        notes = notes.trim(),
        userVehicleId = userVehicleId?.trim()?.takeIf { it.isNotBlank() },
        vehicleLabel = vehicleLabel?.trim()?.takeIf { it.isNotBlank() },
        loyaltyRewardCode = loyaltyRewardCode
            ?.trim()
            ?.replace(Regex("\\s+"), "")
            ?.uppercase()
            ?.takeIf { it.isNotBlank() },
        extraIds = extraIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() },
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

    private fun BookingRescheduleRequest.normalized(): BookingRescheduleRequest = copy(
        reservationId = reservationId.trim(),
        slotStartIso = slotStartIso.trim(),
        slotEndIso = slotEndIso.trim(),
    )
}

private fun isValidDateId(dateId: String): Boolean {
    if (dateId.length != 10) return false
    if (dateId[4] != '-' || dateId[7] != '-') return false

    val year = dateId.substring(0, 4).toIntOrNull() ?: return false
    val month = dateId.substring(5, 7).toIntOrNull() ?: return false
    val day = dateId.substring(8, 10).toIntOrNull() ?: return false
    return year > 0 && month in 1..12 && day in 1..daysInMonth(year, month)
}

private fun Int?.orZero(): Int = this ?: 0

private fun isValidSlotIso(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.length < 20) return false
    if (!trimmed.contains("T")) return false
    val date = trimmed.substringBefore("T")
    val time = trimmed.substringAfter("T").take(5)
    if (time.length != 5 || time[2] != ':') return false
    val hour = time.substring(0, 2).toIntOrNull() ?: return false
    val minute = time.substring(3, 5).toIntOrNull() ?: return false
    return isValidDateId(date) && hour in 0..23 && minute in 0..59
}

private fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }
}

private fun isLeapYear(year: Int): Boolean {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
