package com.sudsmobile.data.booking

class FirebaseBookingRepository(
    private val api: BookingFunctionsApi,
) : BookingRepository {
    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        val validationError = validate(request)
        if (validationError != null) {
            return BookingCreateResult.Failure(validationError)
        }

        return api.createReservation(request.normalized())
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
    )
}
