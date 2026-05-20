package com.sudsmobile.data.booking

import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState

class FirebaseBookingRepository(
    private val api: BookingFunctionsApi,
    private val authRepository: AuthRepository,
) : BookingRepository {
    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        val validationError = validate(request)
        if (validationError != null) {
            return BookingAvailabilityResult.Failure(validationError)
        }

        return api.getAvailability(request)
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        val validationError = validate(request)
        if (validationError != null) {
            return BookingCreateResult.Failure(validationError)
        }

        return api.createReservation(request.normalized(), currentIdTokenOrNull())
    }

    override suspend fun getMyBookings(): BookingHistoryResult {
        val session = authRepository.sessionState.value as? AuthSessionState.Authenticated
            ?: return BookingHistoryResult.Failure(
                BookingHistoryError.Unauthenticated("Inicie sessão para ver as suas marcações."),
            )

        return api.getMyReservations(session.session.idToken)
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
            !request.gdprConsent -> BookingCreateError.Validation("Aceite a política de privacidade para continuar.")
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
    )

    private fun currentIdTokenOrNull(): String? {
        val session = authRepository.sessionState.value as? AuthSessionState.Authenticated
        return session?.session?.idToken
    }
}

private fun isValidDateId(dateId: String): Boolean {
    if (dateId.length != 10) return false
    return dateId[4] == '-' && dateId[7] == '-'
}
