package com.sudsmobile.data.admin

import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier

class FirebaseAdminRepository(
    private val api: AdminFunctionsApi,
    private val authRepository: AuthRepository,
    private val bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
) : AdminRepository {
    override suspend fun syncMyRole(): AdminRoleResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminRoleResult.Failure(unauthenticatedError())

        return api.syncMyRole(idToken)
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingRequestsResult.Failure(unauthenticatedError())

        return api.getPendingBookingRequests(idToken)
    }

    override suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminBusinessInfoResult.Failure(unauthenticatedError())

        return api.getBusinessInfoConfiguration(idToken)
    }

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminServiceCatalogResult.Failure(unauthenticatedError())

        return api.getServiceCatalogConfiguration(idToken)
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBusinessInfoResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBusinessInfoResult.Failure(unauthenticatedError())

        return api.updateBusinessInfoConfiguration(normalizedRequest, idToken)
    }

    override suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBookingDecisionResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingDecisionResult.Failure(unauthenticatedError())

        return api.acceptBookingRequest(normalizedRequest, idToken)
            .also { result ->
                if (result is AdminBookingDecisionResult.Success) {
                    bookingChangeNotifier.notifyBookingsChanged()
                }
            }
    }

    override suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBookingDecisionResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingDecisionResult.Failure(unauthenticatedError())

        return api.rejectBookingRequest(normalizedRequest, idToken)
            .also { result ->
                if (result is AdminBookingDecisionResult.Success) {
                    bookingChangeNotifier.notifyBookingsChanged()
                }
            }
    }

    override suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
    ): AdminServiceCatalogMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminServiceCatalogMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminServiceCatalogMutationResult.Failure(unauthenticatedError())

        return api.upsertServiceCatalogItem(normalizedRequest, idToken)
    }

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
    ): AdminServiceCatalogMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminServiceCatalogMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminServiceCatalogMutationResult.Failure(unauthenticatedError())

        return api.archiveServiceCatalogItem(normalizedRequest, idToken)
    }

    private suspend fun currentIdTokenOrNull(): String? = authRepository.currentSession()?.idToken

    private fun validate(request: AdminBookingDecisionRequest): AdminError.Validation? {
        return when {
            request.reservationId.isBlank() || request.reservationId.contains("/") ->
                AdminError.Validation("A marcação selecionada é inválida.")
            request.reservationId.length > 160 ->
                AdminError.Validation("A marcação selecionada é inválida.")
            request.rejectionReason.length > MaxRejectionReasonLength ->
                AdminError.Validation("O motivo deve ter no máximo 500 caracteres.")
            else -> null
        }
    }

    private fun validate(request: AdminServiceCatalogMutationRequest): AdminError.Validation? {
        return when {
            request.serviceId.isNotBlank() && !request.serviceId.isValidCatalogId() ->
                AdminError.Validation("O identificador do serviço é inválido.")
            request.name.isBlank() ->
                AdminError.Validation("Indique o nome do serviço.")
            request.name.length > MaxServiceNameLength ->
                AdminError.Validation("O nome do serviço deve ter no máximo 120 caracteres.")
            request.description.length > MaxServiceDescriptionLength ->
                AdminError.Validation("A descrição do serviço deve ter no máximo 1000 caracteres.")
            request.durationMinutes !in MinServiceDurationMinutes..MaxServiceDurationMinutes ->
                AdminError.Validation("A duração deve estar entre 5 e 480 minutos.")
            request.passengerPriceCents !in MinServicePriceCents..MaxServicePriceCents ||
                request.suvPriceCents !in MinServicePriceCents..MaxServicePriceCents ->
                AdminError.Validation("Os preços devem estar entre 0,00 € e 1000,00 €.")
            request.iconKey.length > MaxServiceIconKeyLength ->
                AdminError.Validation("O ícone deve ter no máximo 40 caracteres.")
            request.sortOrder !in MinServiceSortOrder..MaxServiceSortOrder ->
                AdminError.Validation("A ordenação deve estar entre 0 e 9999.")
            else -> null
        }
    }

    private fun validate(request: AdminServiceCatalogArchiveRequest): AdminError.Validation? {
        return when {
            request.serviceId.isBlank() || !request.serviceId.isValidCatalogId() ->
                AdminError.Validation("O identificador do serviço é inválido.")
            else -> null
        }
    }

    private fun validate(request: AdminBusinessInfoUpdateRequest): AdminError.Validation? {
        return when {
            request.phone.isBlank() ->
                AdminError.Validation("Indique o telefone.")
            request.phone.length > MaxBusinessPhoneLength ->
                AdminError.Validation("O telefone deve ter no máximo 60 caracteres.")
            !request.email.isValidEmail() ->
                AdminError.Validation("Indique um email válido.")
            request.addressLine1.isBlank() || request.addressLine2.isBlank() ->
                AdminError.Validation("Indique a morada completa.")
            request.addressLine1.length > MaxBusinessAddressLength ||
                request.addressLine2.length > MaxBusinessAddressLength ->
                AdminError.Validation("Cada linha da morada deve ter no máximo 160 caracteres.")
            !request.mapsUri.isValidWebUrl() ->
                AdminError.Validation("Indique um link de mapa válido.")
            !request.whatsappUri.isValidWebUrl() ->
                AdminError.Validation("Indique um link de WhatsApp válido.")
            request.openingHours.isEmpty() ->
                AdminError.Validation("Indique pelo menos um horário.")
            request.openingHours.size > MaxBusinessOpeningRows ->
                AdminError.Validation("Indique no máximo 10 horários.")
            request.openingHours.any { it.dayLabel.isBlank() || it.hoursLabel.isBlank() } ->
                AdminError.Validation("Preencha o dia e o horário em todas as linhas.")
            request.openingHours.any {
                it.dayLabel.length > MaxBusinessHoursLabelLength ||
                    it.hoursLabel.length > MaxBusinessHoursLabelLength
            } ->
                AdminError.Validation("Cada horário deve ter no máximo 80 caracteres.")
            request.socialLinks.size > MaxBusinessSocialLinks ->
                AdminError.Validation("Indique no máximo 8 redes sociais.")
            request.socialLinks.any { it.label.isBlank() || !it.uri.isValidWebUrl() } ->
                AdminError.Validation("Preencha cada rede social com nome e link válido.")
            else -> null
        }
    }

    private fun AdminBookingDecisionRequest.normalized(): AdminBookingDecisionRequest = copy(
        reservationId = reservationId.trim(),
        rejectionReason = rejectionReason.trim().replace(Regex("\\s+"), " "),
    )

    private fun AdminServiceCatalogMutationRequest.normalized(): AdminServiceCatalogMutationRequest = copy(
        serviceId = serviceId.trim(),
        name = name.normalizeAdminText(),
        description = description.normalizeAdminText(),
        iconKey = iconKey.trim().ifBlank { "car" },
    )

    private fun AdminServiceCatalogArchiveRequest.normalized(): AdminServiceCatalogArchiveRequest = copy(
        serviceId = serviceId.trim(),
    )

    private fun AdminBusinessInfoUpdateRequest.normalized(): AdminBusinessInfoUpdateRequest = copy(
        phone = phone.normalizeAdminText(),
        email = email.trim().lowercase(),
        addressLine1 = addressLine1.normalizeAdminText(),
        addressLine2 = addressLine2.normalizeAdminText(),
        mapsUri = mapsUri.trim(),
        whatsappUri = whatsappUri.trim(),
        openingHours = openingHours.map {
            it.copy(
                dayLabel = it.dayLabel.normalizeAdminText(),
                hoursLabel = it.hoursLabel.normalizeAdminText(),
            )
        },
        socialLinks = socialLinks.map {
            it.copy(
                label = it.label.normalizeAdminText(),
                uri = it.uri.trim(),
            )
        },
    )
}

private const val MaxRejectionReasonLength = 500
private const val MaxServiceNameLength = 120
private const val MaxServiceDescriptionLength = 1000
private const val MinServiceDurationMinutes = 5
private const val MaxServiceDurationMinutes = 480
private const val MinServicePriceCents = 0
private const val MaxServicePriceCents = 100000
private const val MaxServiceIconKeyLength = 40
private const val MinServiceSortOrder = 0
private const val MaxServiceSortOrder = 9999
private const val MaxBusinessPhoneLength = 60
private const val MaxBusinessAddressLength = 160
private const val MaxBusinessHoursLabelLength = 80
private const val MaxBusinessOpeningRows = 10
private const val MaxBusinessSocialLinks = 8
private val CatalogIdRegex = Regex("^[A-Za-z0-9_-]{1,80}$")

private fun unauthenticatedError(): AdminError.Unauthenticated {
    return AdminError.Unauthenticated("Inicie sessão para gerir a área administrativa.")
}

private fun String.isValidCatalogId(): Boolean {
    return CatalogIdRegex.matches(this)
}

private fun String.normalizeAdminText(): String {
    return trim().replace(Regex("\\s+"), " ")
}

private fun String.isValidEmail(): Boolean {
    return length in 5..320 && contains("@") && substringAfter("@").contains(".")
}

private fun String.isValidWebUrl(): Boolean {
    val value = trim().lowercase()
    return value.length in 8..1000 &&
        (value.startsWith("https://") || value.startsWith("http://")) &&
        !value.startsWith("javascript:")
}
