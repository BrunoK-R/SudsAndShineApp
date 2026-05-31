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

    override suspend fun getAvailabilityConfiguration(): AdminAvailabilityResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminAvailabilityResult.Failure(unauthenticatedError())

        return api.getAvailabilityConfiguration(idToken)
    }

    override suspend fun getBookingPolicyConfiguration(): AdminBookingPolicyResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingPolicyResult.Failure(unauthenticatedError())

        return api.getBookingPolicyConfiguration(idToken)
    }

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminServiceCatalogResult.Failure(unauthenticatedError())

        return api.getServiceCatalogConfiguration(idToken)
    }

    override suspend fun getServiceExtrasConfiguration(): AdminServiceExtrasResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminServiceExtrasResult.Failure(unauthenticatedError())

        return api.getServiceExtrasConfiguration(idToken)
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

    override suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
    ): AdminAvailabilityResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminAvailabilityResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminAvailabilityResult.Failure(unauthenticatedError())

        return api.updateAvailabilityConfiguration(normalizedRequest, idToken)
    }

    override suspend fun updateBookingPolicyConfiguration(
        request: AdminBookingPolicyUpdateRequest,
    ): AdminBookingPolicyResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBookingPolicyResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingPolicyResult.Failure(unauthenticatedError())

        return api.updateBookingPolicyConfiguration(normalizedRequest, idToken)
    }

    override suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
    ): AdminCapacityOverrideMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminCapacityOverrideMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminCapacityOverrideMutationResult.Failure(unauthenticatedError())

        return api.upsertCapacityOverride(normalizedRequest, idToken)
    }

    override suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
    ): AdminCapacityOverrideMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminCapacityOverrideMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminCapacityOverrideMutationResult.Failure(unauthenticatedError())

        return api.clearCapacityOverride(normalizedRequest, idToken)
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

    override suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
    ): AdminServiceExtraMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminServiceExtraMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminServiceExtraMutationResult.Failure(unauthenticatedError())

        return api.upsertServiceExtra(normalizedRequest, idToken)
    }

    override suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
    ): AdminServiceExtraMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminServiceExtraMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminServiceExtraMutationResult.Failure(unauthenticatedError())

        return api.archiveServiceExtra(normalizedRequest, idToken)
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

    private fun validate(request: AdminServiceExtraMutationRequest): AdminError.Validation? {
        return when {
            request.extraId.isNotBlank() && !request.extraId.isValidCatalogId() ->
                AdminError.Validation("O identificador do extra é inválido.")
            request.name.isBlank() ->
                AdminError.Validation("Indique o nome do extra.")
            request.name.length > MaxServiceNameLength ->
                AdminError.Validation("O nome do extra deve ter no máximo 120 caracteres.")
            request.description.length > MaxServiceDescriptionLength ->
                AdminError.Validation("A descrição do extra deve ter no máximo 1000 caracteres.")
            request.priceCents !in MinServicePriceCents..MaxServicePriceCents ->
                AdminError.Validation("O preço deve estar entre 0,00 € e 1000,00 €.")
            request.iconKey.length > MaxServiceIconKeyLength ->
                AdminError.Validation("O ícone deve ter no máximo 40 caracteres.")
            request.eligibleServiceIds.size > MaxEligibleServiceLinks ->
                AdminError.Validation("Indique no máximo 40 serviços elegíveis.")
            request.eligibleServiceIds.any { !it.isValidCatalogId() } ->
                AdminError.Validation("Um dos serviços elegíveis é inválido.")
            request.sortOrder !in MinServiceSortOrder..MaxServiceSortOrder ->
                AdminError.Validation("A ordenação deve estar entre 0 e 9999.")
            else -> null
        }
    }

    private fun validate(request: AdminServiceExtraArchiveRequest): AdminError.Validation? {
        return when {
            request.extraId.isBlank() || !request.extraId.isValidCatalogId() ->
                AdminError.Validation("O identificador do extra é inválido.")
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

    private fun validate(request: AdminAvailabilityUpdateRequest): AdminError.Validation? {
        return when {
            request.defaultMaxBookingsPerSlot !in MinAvailabilityCapacity..MaxAvailabilityCapacity ->
                AdminError.Validation("A capacidade deve estar entre 0 e 20 marcações por horário.")
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
            request.openingHours.any { !it.closed && !it.hoursLabel.hasAvailabilityTimeRange() } ->
                AdminError.Validation("Cada dia aberto deve ter um intervalo HH:MM válido.")
            else -> null
        }
    }

    private fun validate(request: AdminCapacityOverrideUpsertRequest): AdminError.Validation? {
        return when {
            !request.date.isValidDateId() ->
                AdminError.Validation("Indique uma data válida para a exceção.")
            request.maxBookingsPerSlot !in MinAvailabilityCapacity..MaxAvailabilityCapacity ->
                AdminError.Validation("A capacidade da exceção deve estar entre 0 e 20 marcações.")
            else -> null
        }
    }

    private fun validate(request: AdminCapacityOverrideClearRequest): AdminError.Validation? {
        return when {
            !request.date.isValidDateId() ->
                AdminError.Validation("Indique uma data válida para limpar a exceção.")
            else -> null
        }
    }

    private fun validate(request: AdminBookingPolicyUpdateRequest): AdminError.Validation? {
        return when {
            request.pendingHoldMinutes !in MinPendingHoldMinutes..MaxPolicyWindowMinutes ->
                AdminError.Validation("A reserva pendente deve ficar ativa entre 15 minutos e 7 dias.")
            request.cancellationWindowMinutes !in MinPolicyWindowMinutes..MaxPolicyWindowMinutes ->
                AdminError.Validation("A antecedência de cancelamento deve estar entre 0 minutos e 7 dias.")
            request.rescheduleWindowMinutes !in MinPolicyWindowMinutes..MaxPolicyWindowMinutes ->
                AdminError.Validation("A antecedência de remarcação deve estar entre 0 minutos e 7 dias.")
            request.paymentEligibilityCopy.isBlank() ->
                AdminError.Validation("Indique a mensagem de pagamento.")
            request.paymentEligibilityCopy.length > MaxPaymentEligibilityCopyLength ->
                AdminError.Validation("A mensagem de pagamento deve ter no máximo 500 caracteres.")
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

    private fun AdminServiceExtraMutationRequest.normalized(): AdminServiceExtraMutationRequest {
        val seen = mutableSetOf<String>()
        val normalizedEligibleServiceIds = eligibleServiceIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { seen.add(it) }
        return copy(
            extraId = extraId.trim(),
            name = name.normalizeAdminText(),
            description = description.normalizeAdminText(),
            iconKey = iconKey.trim().ifBlank { "auto_awesome" },
            eligibleServiceIds = normalizedEligibleServiceIds,
        )
    }

    private fun AdminServiceExtraArchiveRequest.normalized(): AdminServiceExtraArchiveRequest = copy(
        extraId = extraId.trim(),
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

    private fun AdminAvailabilityUpdateRequest.normalized(): AdminAvailabilityUpdateRequest = copy(
        openingHours = openingHours.map {
            it.copy(
                dayLabel = it.dayLabel.normalizeAdminText(),
                hoursLabel = it.hoursLabel.normalizeAdminText(),
            )
        },
    )

    private fun AdminCapacityOverrideUpsertRequest.normalized(): AdminCapacityOverrideUpsertRequest = copy(
        date = date.trim(),
    )

    private fun AdminCapacityOverrideClearRequest.normalized(): AdminCapacityOverrideClearRequest = copy(
        date = date.trim(),
    )

    private fun AdminBookingPolicyUpdateRequest.normalized(): AdminBookingPolicyUpdateRequest = copy(
        paymentEligibilityCopy = paymentEligibilityCopy.normalizeAdminText(),
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
private const val MaxEligibleServiceLinks = 40
private const val MinServiceSortOrder = 0
private const val MaxServiceSortOrder = 9999
private const val MaxBusinessPhoneLength = 60
private const val MaxBusinessAddressLength = 160
private const val MaxBusinessHoursLabelLength = 80
private const val MaxBusinessOpeningRows = 10
private const val MaxBusinessSocialLinks = 8
private const val MinAvailabilityCapacity = 0
private const val MaxAvailabilityCapacity = 20
private const val MinPendingHoldMinutes = 15
private const val MinPolicyWindowMinutes = 0
private const val MaxPolicyWindowMinutes = 7 * 24 * 60
private const val MaxPaymentEligibilityCopyLength = 500
private val CatalogIdRegex = Regex("^[A-Za-z0-9_-]{1,80}$")
private val AvailabilityTimeRangeRegex = Regex("([0-2]?\\d):([0-5]\\d)\\D+([0-2]?\\d):([0-5]\\d)")

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

private fun String.isValidDateId(): Boolean {
    val value = trim()
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)) return false
    val year = value.substring(0, 4).toIntOrNull() ?: return false
    val month = value.substring(5, 7).toIntOrNull() ?: return false
    val day = value.substring(8, 10).toIntOrNull() ?: return false
    if (month !in 1..12) return false
    val maxDay = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year.isLeapYear()) 29 else 28
        else -> return false
    }
    return day in 1..maxDay
}

private fun Int.isLeapYear(): Boolean {
    return this % 4 == 0 && (this % 100 != 0 || this % 400 == 0)
}

private fun String.hasAvailabilityTimeRange(): Boolean {
    return AvailabilityTimeRangeRegex.findAll(this).any { match ->
        val openHour = match.groupValues[1].toIntOrNull() ?: return@any false
        val openMinute = match.groupValues[2].toIntOrNull() ?: return@any false
        val closeHour = match.groupValues[3].toIntOrNull() ?: return@any false
        val closeMinute = match.groupValues[4].toIntOrNull() ?: return@any false
        val open = openHour * 60 + openMinute
        val close = closeHour * 60 + closeMinute
        open in 0 until (24 * 60) && close in 1..(24 * 60) && close > open
    }
}
