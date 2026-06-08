package com.sudsmobile.data.admin

import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.booking.MutableBookingChangeNotifier

class FirebaseAdminRepository(
    private val api: AdminFunctionsApi,
    private val authRepository: AuthRepository,
    private val bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
) : AdminRepository {
    override suspend fun syncMyRole(): AdminRoleResult {
        val session = authRepository.currentSession()
            ?: return AdminRoleResult.Failure(unauthenticatedError())

        return when (val result = api.syncMyRole(session.idToken)) {
            is AdminRoleResult.Failure -> result
            is AdminRoleResult.Success -> {
                if (!result.role.belongsTo(session)) {
                    return AdminRoleResult.Failure(roleMismatchError())
                }
                when (val refreshResult = authRepository.refreshCurrentSession()) {
                    is AuthResult.Success -> {
                        if (refreshResult.session.user.uid != session.user.uid) {
                            AdminRoleResult.Failure(roleMismatchError())
                        } else {
                            result
                        }
                    }
                    is AuthResult.Failure -> AdminRoleResult.Failure(refreshResult.error.toAdminError())
                }
            }
        }
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingRequestsResult.Failure(unauthenticatedError())

        return api.getPendingBookingRequests(idToken)
    }

    override suspend fun getAcceptedBookingRequests(): AdminBookingRequestsResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingRequestsResult.Failure(unauthenticatedError())

        return api.getAcceptedBookingRequests(idToken)
    }

    override suspend fun getCompletableBookingRequests(): AdminBookingRequestsResult {
        return getAcceptedBookingRequests()
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

    override suspend fun getLoyaltySettingsConfiguration(): AdminLoyaltySettingsResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminLoyaltySettingsResult.Failure(unauthenticatedError())

        return api.getLoyaltySettingsConfiguration(idToken)
    }

    override suspend fun getNotificationSettingsConfiguration(): AdminNotificationSettingsResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminNotificationSettingsResult.Failure(unauthenticatedError())

        return api.getNotificationSettingsConfiguration(idToken)
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

    override suspend fun updateLoyaltySettingsConfiguration(
        request: AdminLoyaltySettingsUpdateRequest,
    ): AdminLoyaltySettingsResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminLoyaltySettingsResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminLoyaltySettingsResult.Failure(unauthenticatedError())

        return api.updateLoyaltySettingsConfiguration(normalizedRequest, idToken)
    }

    override suspend fun updateNotificationSettingsConfiguration(
        request: AdminNotificationSettingsUpdateRequest,
    ): AdminNotificationSettingsResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminNotificationSettingsResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminNotificationSettingsResult.Failure(unauthenticatedError())

        return api.updateNotificationSettingsConfiguration(normalizedRequest, idToken)
    }

    override suspend fun sendNotificationTestToSelf(
        request: AdminNotificationTestRequest,
    ): AdminNotificationTestResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminNotificationTestResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminNotificationTestResult.Failure(unauthenticatedError())

        return api.sendNotificationTestToSelf(normalizedRequest, idToken)
    }

    override suspend fun getNotificationCampaignDrafts(): AdminNotificationCampaignDraftsResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminNotificationCampaignDraftsResult.Failure(unauthenticatedError())

        return api.getNotificationCampaignDrafts(idToken)
    }

    override suspend fun upsertNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftMutationRequest,
    ): AdminNotificationCampaignDraftMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminNotificationCampaignDraftMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminNotificationCampaignDraftMutationResult.Failure(unauthenticatedError())

        return api.upsertNotificationCampaignDraft(normalizedRequest, idToken)
    }

    override suspend fun archiveNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftArchiveRequest,
    ): AdminNotificationCampaignDraftMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminNotificationCampaignDraftMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminNotificationCampaignDraftMutationResult.Failure(unauthenticatedError())

        return api.archiveNotificationCampaignDraft(normalizedRequest, idToken)
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

    override suspend fun upsertBlockedSlot(
        request: AdminBlockedSlotUpsertRequest,
    ): AdminBlockedSlotMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBlockedSlotMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBlockedSlotMutationResult.Failure(unauthenticatedError())

        return api.upsertBlockedSlot(normalizedRequest, idToken)
    }

    override suspend fun clearBlockedSlot(
        request: AdminBlockedSlotClearRequest,
    ): AdminBlockedSlotMutationResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBlockedSlotMutationResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBlockedSlotMutationResult.Failure(unauthenticatedError())

        return api.clearBlockedSlot(normalizedRequest, idToken)
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

    override suspend fun completeBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBookingDecisionResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingDecisionResult.Failure(unauthenticatedError())

        return api.completeBookingRequest(normalizedRequest, idToken)
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

    private fun com.sudsmobile.data.auth.AuthError.toAdminError(): AdminError = when (this) {
        is com.sudsmobile.data.auth.AuthError.Validation -> AdminError.Validation(message)
        is com.sudsmobile.data.auth.AuthError.InvalidCredentials -> AdminError.Unauthenticated(message)
        is com.sudsmobile.data.auth.AuthError.EmailInUse -> AdminError.Backend(message)
        is com.sudsmobile.data.auth.AuthError.Permission -> AdminError.Permission(message)
        is com.sudsmobile.data.auth.AuthError.Unavailable -> AdminError.Unavailable(message)
        is com.sudsmobile.data.auth.AuthError.Backend -> AdminError.Backend(message)
    }

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
            request.defaultSlotIntervalMinutes !in MinAvailabilitySlotIntervalMinutes..MaxAvailabilitySlotIntervalMinutes ->
                AdminError.Validation("O intervalo entre horários deve estar entre 5 e 240 minutos.")
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

    private fun validate(request: AdminBlockedSlotUpsertRequest): AdminError.Validation? {
        return when {
            request.blockedSlotId.isNotBlank() && !request.blockedSlotId.isValidBlockedSlotId() ->
                AdminError.Validation("O identificador do bloqueio é inválido.")
            !request.date.isValidDateId() ->
                AdminError.Validation("Indique uma data válida para o bloqueio.")
            !request.slotStartIso.isValidUtcInstantIso() || !request.slotEndIso.isValidUtcInstantIso() ->
                AdminError.Validation("Indique horas válidas para o bloqueio.")
            request.slotStartIso.substring(0, 10) != request.date ||
                request.slotEndIso.substring(0, 10) != request.date ->
                AdminError.Validation("As horas do bloqueio devem pertencer à data indicada.")
            request.slotEndIso <= request.slotStartIso ->
                AdminError.Validation("A hora de fim deve ser posterior ao início.")
            request.reason.length > MaxBlockedSlotReasonLength ->
                AdminError.Validation("O motivo deve ter no máximo 160 caracteres.")
            else -> null
        }
    }

    private fun validate(request: AdminBlockedSlotClearRequest): AdminError.Validation? {
        return when {
            request.blockedSlotId.isBlank() || !request.blockedSlotId.isValidBlockedSlotId() ->
                AdminError.Validation("O identificador do bloqueio é inválido.")
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

    private fun validate(request: AdminLoyaltySettingsUpdateRequest): AdminError.Validation? {
        val valueRange = request.rewardType.rewardValueRangeOrNull()
        return when {
            request.stampsRequired !in MinLoyaltyStampsRequired..MaxLoyaltyStampsRequired ->
                AdminError.Validation("O prémio deve exigir entre 1 e 50 lavagens.")
            valueRange == null ->
                AdminError.Validation("Escolha um tipo de prémio válido.")
            request.rewardValue !in valueRange ->
                AdminError.Validation("O valor do prémio não é válido para o tipo selecionado.")
            request.rewardDescription.isBlank() ->
                AdminError.Validation("Indique a descrição do prémio.")
            request.rewardDescription.length > MaxLoyaltyRewardDescriptionLength ->
                AdminError.Validation("A descrição do prémio deve ter no máximo 200 caracteres.")
            else -> null
        }
    }

    private fun validate(request: AdminNotificationSettingsUpdateRequest): AdminError.Validation? {
        return when {
            request.reminderLeadMinutes !in MinNotificationReminderLeadMinutes..MaxNotificationReminderLeadMinutes ->
                AdminError.Validation("O lembrete deve ser enviado entre 15 minutos e 7 dias antes.")
            !request.quietHoursStart.isValidAdminTime() || !request.quietHoursEnd.isValidAdminTime() ->
                AdminError.Validation("As horas de silêncio devem estar no formato HH:MM.")
            !request.quietHoursTimeZone.isValidNotificationTimeZone() ->
                AdminError.Validation("Indique um fuso horário válido para o período de silêncio.")
            request.templates.map { it.key }.toSet() != NotificationTemplateKeys ->
                AdminError.Validation("Preencha todos os modelos de notificação.")
            request.templates.any { it.title.isBlank() || it.body.isBlank() } ->
                AdminError.Validation("Preencha todos os modelos de notificação.")
            request.templates.any { it.title.length > MaxNotificationTemplateTitleLength } ->
                AdminError.Validation("Cada título de notificação deve ter no máximo 120 caracteres.")
            request.templates.any { it.body.length > MaxNotificationTemplateBodyLength } ->
                AdminError.Validation("Cada mensagem de notificação deve ter no máximo 500 caracteres.")
            else -> null
        }
    }

    private fun validate(request: AdminNotificationTestRequest): AdminError.Validation? {
        return when {
            request.campaignId.isNotBlank() && request.templateKey.isNotBlank() ->
                AdminError.Validation("Escolha um modelo ou um rascunho de campanha.")
            request.campaignId.isNotBlank() && !request.campaignId.isValidCampaignId() ->
                AdminError.Validation("O identificador da campanha é inválido.")
            request.campaignId.isBlank() && request.templateKey !in NotificationTemplateKeys ->
                AdminError.Validation("Escolha um modelo de notificação válido.")
            else -> null
        }
    }

    private fun validate(request: AdminNotificationCampaignDraftMutationRequest): AdminError.Validation? {
        return when {
            request.campaignId.isNotBlank() && !request.campaignId.isValidCampaignId() ->
                AdminError.Validation("O identificador da campanha é inválido.")
            request.title.isBlank() ->
                AdminError.Validation("Indique o título da campanha.")
            request.title.length > MaxNotificationCampaignTitleLength ->
                AdminError.Validation("O título da campanha deve ter no máximo 120 caracteres.")
            request.body.isBlank() ->
                AdminError.Validation("Indique a mensagem da campanha.")
            request.body.length > MaxNotificationCampaignBodyLength ->
                AdminError.Validation("A mensagem da campanha deve ter no máximo 1000 caracteres.")
            request.targetAudience !in NotificationCampaignTargetAudiences ->
                AdminError.Validation("Escolha um público de teste ou marketing opt-in.")
            request.scheduledAtIso.isNotBlank() && !request.scheduledAtIso.isValidUtcInstantIso() ->
                AdminError.Validation("Indique a data agendada em ISO UTC.")
            request.notes.length > MaxNotificationCampaignNotesLength ->
                AdminError.Validation("As notas devem ter no máximo 500 caracteres.")
            !request.pushEnabled ->
                AdminError.Validation("Apenas rascunhos push são suportados.")
            else -> null
        }
    }

    private fun validate(request: AdminNotificationCampaignDraftArchiveRequest): AdminError.Validation? {
        return when {
            request.campaignId.isBlank() || !request.campaignId.isValidCampaignId() ->
                AdminError.Validation("O identificador da campanha é inválido.")
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

    private fun AdminBlockedSlotUpsertRequest.normalized(): AdminBlockedSlotUpsertRequest = copy(
        blockedSlotId = blockedSlotId.trim(),
        date = date.trim(),
        slotStartIso = slotStartIso.trim(),
        slotEndIso = slotEndIso.trim(),
        reason = reason.normalizeAdminText(),
    )

    private fun AdminBlockedSlotClearRequest.normalized(): AdminBlockedSlotClearRequest = copy(
        blockedSlotId = blockedSlotId.trim(),
    )

    private fun AdminBookingPolicyUpdateRequest.normalized(): AdminBookingPolicyUpdateRequest = copy(
        paymentEligibilityCopy = paymentEligibilityCopy.normalizeAdminText(),
    )

    private fun AdminLoyaltySettingsUpdateRequest.normalized(): AdminLoyaltySettingsUpdateRequest = copy(
        rewardType = rewardType.normalizeRewardType(),
        rewardDescription = rewardDescription.normalizeAdminText(),
    )

    private fun AdminNotificationSettingsUpdateRequest.normalized(): AdminNotificationSettingsUpdateRequest = copy(
        quietHoursStart = quietHoursStart.trim(),
        quietHoursEnd = quietHoursEnd.trim(),
        quietHoursTimeZone = quietHoursTimeZone.trim().ifBlank { DefaultNotificationQuietHoursTimeZone },
        templates = templates
            .map {
                it.copy(
                    key = it.key.trim(),
                    label = it.label.normalizeAdminText(),
                    title = it.title.normalizeAdminText(),
                    body = it.body.normalizeAdminText(),
                )
            }
            .distinctBy { it.key },
    )

    private fun AdminNotificationTestRequest.normalized(): AdminNotificationTestRequest = copy(
        templateKey = templateKey.trim(),
        campaignId = campaignId.trim(),
    )

    private fun AdminNotificationCampaignDraftMutationRequest.normalized():
        AdminNotificationCampaignDraftMutationRequest = copy(
        campaignId = campaignId.trim(),
        title = title.normalizeAdminText(),
        body = body.normalizeAdminText(),
        targetAudience = targetAudience.trim(),
        scheduledAtIso = scheduledAtIso.trim(),
        notes = notes.normalizeAdminText(),
        pushEnabled = pushEnabled,
    )

    private fun AdminNotificationCampaignDraftArchiveRequest.normalized():
        AdminNotificationCampaignDraftArchiveRequest = copy(
        campaignId = campaignId.trim(),
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
private const val MinAvailabilitySlotIntervalMinutes = 5
private const val MaxAvailabilitySlotIntervalMinutes = 240
private const val MaxBlockedSlotReasonLength = 160
private const val MinPendingHoldMinutes = 15
private const val MinPolicyWindowMinutes = 0
private const val MaxPolicyWindowMinutes = 7 * 24 * 60
private const val MaxPaymentEligibilityCopyLength = 500
private const val MinLoyaltyStampsRequired = 1
private const val MaxLoyaltyStampsRequired = 50
private const val MaxLoyaltyRewardDescriptionLength = 200
private const val MinNotificationReminderLeadMinutes = 15
private const val MaxNotificationReminderLeadMinutes = 7 * 24 * 60
private const val MaxNotificationTemplateTitleLength = 120
private const val MaxNotificationTemplateBodyLength = 500
private const val MaxNotificationQuietHoursTimeZoneLength = 80
private const val DefaultNotificationQuietHoursTimeZone = "Europe/Lisbon"
private const val MaxNotificationCampaignTitleLength = 120
private const val MaxNotificationCampaignBodyLength = 1000
private const val MaxNotificationCampaignNotesLength = 500
private val NotificationTemplateKeys = setOf(
    "booking_request",
    "booking_accepted",
    "booking_rejected",
    "booking_expired",
    "booking_cancelled",
    "booking_rescheduled",
    "booking_reminder",
    "review_prompt",
    "loyalty_reward",
    "admin_pending_booking",
)
private val CatalogIdRegex = Regex("^[A-Za-z0-9_-]{1,80}$")
private val BlockedSlotIdRegex = Regex("^[A-Za-z0-9_-]{1,120}$")
private val CampaignIdRegex = Regex("^[A-Za-z0-9_-]{3,80}$")
private val UtcInstantIsoRegex = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z$")
private val AvailabilityTimeRangeRegex = Regex("([0-2]?\\d):([0-5]\\d)\\D+([0-2]?\\d):([0-5]\\d)")
private val AdminTimeRegex = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")
private val NotificationTimeZoneRegex = Regex("^[A-Za-z_]+(/[A-Za-z0-9_+\\-]+)*$")
private val NotificationCampaignTargetAudiences = setOf("test_users", "marketing_opt_in_users")

private fun unauthenticatedError(): AdminError.Unauthenticated {
    return AdminError.Unauthenticated("Inicie sessão para gerir a área administrativa.")
}

private fun roleMismatchError(): AdminError.Permission {
    return AdminError.Permission("A validação administrativa não corresponde à sessão atual.")
}

private fun AdminRole.belongsTo(session: AuthSession): Boolean {
    return uid == session.user.uid
}

private fun String.isValidCatalogId(): Boolean {
    return CatalogIdRegex.matches(this)
}

private fun String.isValidBlockedSlotId(): Boolean {
    return BlockedSlotIdRegex.matches(this) && !contains("/")
}

private fun String.isValidCampaignId(): Boolean {
    return CampaignIdRegex.matches(this) && !contains("/")
}

private fun String.isValidUtcInstantIso(): Boolean {
    return UtcInstantIsoRegex.matches(trim())
}

private fun String.normalizeAdminText(): String {
    return trim().replace(Regex("\\s+"), " ")
}

private fun String.normalizeRewardType(): String {
    return trim()
        .lowercase()
        .replace(Regex("[-\\s]+"), "_")
}

private fun String.rewardValueRangeOrNull(): IntRange? {
    return when (this) {
        "free_wash" -> 1..10
        "discount_amount" -> 1..100000
        "discount_percent" -> 1..100
        else -> null
    }
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

private fun String.isValidAdminTime(): Boolean {
    return AdminTimeRegex.matches(trim())
}

private fun String.isValidNotificationTimeZone(): Boolean {
    val value = trim()
    return value.length in 1..MaxNotificationQuietHoursTimeZoneLength &&
        NotificationTimeZoneRegex.matches(value)
}
