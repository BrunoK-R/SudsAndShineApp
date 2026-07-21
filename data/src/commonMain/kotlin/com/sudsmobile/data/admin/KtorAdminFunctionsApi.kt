package com.sudsmobile.data.admin

import com.sudsmobile.data.booking.BookingReservationExtra
import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

class KtorAdminFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : AdminFunctionsApi {
    override suspend fun syncMyRole(idToken: String): AdminRoleResult {
        return try {
            val response = httpClient.post(config.syncMyRoleUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableRoleResponse>()
            val error = body.error
            when {
                error != null -> AdminRoleResult.Failure(error.toAdminError())
                body.result != null -> AdminRoleResult.Success(body.result.toAdminRole())
                else -> AdminRoleResult.Failure(AdminError.Backend("A resposta de acesso veio sem dados."))
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminRoleResult.Failure(
                AdminError.Unavailable("Não foi possível validar o acesso administrativo. Tente novamente."),
            )
        }
    }

    override suspend fun getPendingBookingRequests(idToken: String): AdminBookingRequestsResult {
        return try {
            val response = httpClient.post(config.getAdminPendingReservationsUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallablePendingReservationsResponse>()
            val error = body.error
            when {
                error != null -> AdminBookingRequestsResult.Failure(error.toAdminError())
                body.result != null -> AdminBookingRequestsResult.Success(
                    body.result.requests.map { it.toAdminBookingRequest() },
                )
                else -> AdminBookingRequestsResult.Failure(
                    AdminError.Backend("A resposta dos pedidos veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminBookingRequestsResult.Failure(
                AdminError.Unavailable("Não foi possível carregar os pedidos de marcação. Tente novamente."),
            )
        }
    }

    override suspend fun getAcceptedBookingRequests(idToken: String): AdminBookingRequestsResult {
        return try {
            val response = httpClient.post(config.getAdminAcceptedReservationsUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallablePendingReservationsResponse>()
            val error = body.error
            when {
                error != null -> AdminBookingRequestsResult.Failure(error.toAdminError())
                body.result != null -> AdminBookingRequestsResult.Success(
                    body.result.requests.map { it.toAdminBookingRequest() },
                )
                else -> AdminBookingRequestsResult.Failure(
                    AdminError.Backend("A resposta das marcações aceites veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminBookingRequestsResult.Failure(
                AdminError.Unavailable("Não foi possível carregar marcações aceites. Tente novamente."),
            )
        }
    }

    override suspend fun getCompletableBookingRequests(idToken: String): AdminBookingRequestsResult {
        return getAcceptedBookingRequests(idToken)
    }

    override suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult = postDecision(
        url = config.acceptReservationUrl,
        payload = DecisionPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível aceitar a marcação. Tente novamente.",
    )

    override suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult = postDecision(
        url = config.rejectReservationUrl,
        payload = DecisionPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível rejeitar a marcação. Tente novamente.",
    )

    override suspend fun startBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult = postDecision(
        url = config.startReservationUrl,
        payload = DecisionPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível iniciar a lavagem. Tente novamente.",
    )

    override suspend fun completeBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult = postDecision(
        url = config.completeReservationUrl,
        payload = DecisionPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível concluir a marcação. Tente novamente.",
    )

    override suspend fun getBusinessInfoConfiguration(idToken: String): AdminBusinessInfoResult {
        return try {
            val response = httpClient.post(config.getAdminBusinessInfoUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableBusinessInfoResponse>()
            val error = body.error
            when {
                error != null -> AdminBusinessInfoResult.Failure(error.toAdminError())
                body.result != null -> AdminBusinessInfoResult.Success(body.result.toAdminBusinessInfoConfig())
                else -> AdminBusinessInfoResult.Failure(
                    AdminError.Backend("A resposta da configuração veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminBusinessInfoResult.Failure(
                AdminError.Unavailable("Não foi possível carregar a configuração do negócio. Tente novamente."),
            )
        }
    }

    override suspend fun getAvailabilityConfiguration(idToken: String): AdminAvailabilityResult {
        return try {
            val response = httpClient.post(config.getAdminAvailabilityConfigurationUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableAvailabilityResponse>()
            val error = body.error
            when {
                error != null -> AdminAvailabilityResult.Failure(error.toAdminError())
                body.result != null -> AdminAvailabilityResult.Success(body.result.toAdminAvailabilityConfig())
                else -> AdminAvailabilityResult.Failure(
                    AdminError.Backend("A resposta da disponibilidade veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminAvailabilityResult.Failure(
                AdminError.Unavailable("Não foi possível carregar a disponibilidade. Tente novamente."),
            )
        }
    }

    override suspend fun getBookingPolicyConfiguration(idToken: String): AdminBookingPolicyResult {
        return try {
            val response = httpClient.post(config.getAdminBookingPolicyUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableBookingPolicyResponse>()
            val error = body.error
            when {
                error != null -> AdminBookingPolicyResult.Failure(error.toAdminError())
                body.result != null -> AdminBookingPolicyResult.Success(body.result.toAdminBookingPolicyConfig())
                else -> AdminBookingPolicyResult.Failure(
                    AdminError.Backend("A resposta da política veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminBookingPolicyResult.Failure(
                AdminError.Unavailable("Não foi possível carregar a política de marcações. Tente novamente."),
            )
        }
    }

    override suspend fun getLoyaltySettingsConfiguration(idToken: String): AdminLoyaltySettingsResult {
        return try {
            val response = httpClient.post(config.getAdminLoyaltySettingsUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableLoyaltySettingsResponse>()
            val error = body.error
            when {
                error != null -> AdminLoyaltySettingsResult.Failure(error.toAdminError())
                body.result != null -> AdminLoyaltySettingsResult.Success(body.result.toAdminLoyaltySettingsConfig())
                else -> AdminLoyaltySettingsResult.Failure(
                    AdminError.Backend("A resposta da fidelização veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminLoyaltySettingsResult.Failure(
                AdminError.Unavailable("Não foi possível carregar a fidelização. Tente novamente."),
            )
        }
    }

    override suspend fun getNotificationSettingsConfiguration(idToken: String): AdminNotificationSettingsResult {
        return try {
            val response = httpClient.post(config.getAdminNotificationSettingsUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableNotificationSettingsResponse>()
            val error = body.error
            when {
                error != null -> AdminNotificationSettingsResult.Failure(error.toAdminError())
                body.result != null -> AdminNotificationSettingsResult.Success(
                    body.result.toAdminNotificationSettingsConfig(),
                )
                else -> AdminNotificationSettingsResult.Failure(
                    AdminError.Backend("A resposta das notificações veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminNotificationSettingsResult.Failure(
                AdminError.Unavailable("Não foi possível carregar as notificações. Tente novamente."),
            )
        }
    }

    override suspend fun getServiceCatalogConfiguration(idToken: String): AdminServiceCatalogResult {
        return try {
            val response = httpClient.post(config.getAdminServiceCatalogUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableAdminServiceCatalogResponse>()
            val error = body.error
            when {
                error != null -> AdminServiceCatalogResult.Failure(error.toAdminError())
                body.result != null -> AdminServiceCatalogResult.Success(body.result.toAdminServiceCatalogConfig())
                else -> AdminServiceCatalogResult.Failure(
                    AdminError.Backend("A resposta do catálogo veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminServiceCatalogResult.Failure(
                AdminError.Unavailable("Não foi possível carregar a configuração do catálogo. Tente novamente."),
            )
        }
    }

    override suspend fun getServiceExtrasConfiguration(idToken: String): AdminServiceExtrasResult {
        return try {
            val response = httpClient.post(config.getAdminServiceExtrasUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableAdminServiceExtrasResponse>()
            val error = body.error
            when {
                error != null -> AdminServiceExtrasResult.Failure(error.toAdminError())
                body.result != null -> AdminServiceExtrasResult.Success(body.result.toAdminServiceExtrasConfig())
                else -> AdminServiceExtrasResult.Failure(
                    AdminError.Backend("A resposta dos extras veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminServiceExtrasResult.Failure(
                AdminError.Unavailable("Não foi possível carregar a configuração dos extras. Tente novamente."),
            )
        }
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
        idToken: String,
    ): AdminBusinessInfoResult {
        return try {
            val response = httpClient.post(config.updateBusinessInfoUrl) {
                callableHeaders(idToken)
                setBody(CallableBusinessInfoUpdateRequest(BusinessInfoPayload.from(request)))
            }
            val body = response.body<CallableBusinessInfoResponse>()
            val error = body.error
            when {
                error != null -> AdminBusinessInfoResult.Failure(error.toAdminError())
                body.result != null -> AdminBusinessInfoResult.Success(body.result.toAdminBusinessInfoConfig())
                else -> AdminBusinessInfoResult.Failure(
                    AdminError.Backend("A resposta da configuração veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminBusinessInfoResult.Failure(
                AdminError.Unavailable("Não foi possível guardar a configuração do negócio. Tente novamente."),
            )
        }
    }

    override suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
        idToken: String,
    ): AdminAvailabilityResult {
        return try {
            val response = httpClient.post(config.updateAvailabilityConfigurationUrl) {
                callableHeaders(idToken)
                setBody(CallableAvailabilityUpdateRequest(AvailabilityPayload.from(request)))
            }
            val body = response.body<CallableAvailabilityResponse>()
            val error = body.error
            when {
                error != null -> AdminAvailabilityResult.Failure(error.toAdminError())
                body.result != null -> AdminAvailabilityResult.Success(body.result.toAdminAvailabilityConfig())
                else -> AdminAvailabilityResult.Failure(
                    AdminError.Backend("A resposta da disponibilidade veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminAvailabilityResult.Failure(
                AdminError.Unavailable("Não foi possível guardar a disponibilidade. Tente novamente."),
            )
        }
    }

    override suspend fun updateBookingPolicyConfiguration(
        request: AdminBookingPolicyUpdateRequest,
        idToken: String,
    ): AdminBookingPolicyResult {
        return try {
            val response = httpClient.post(config.updateBookingPolicyUrl) {
                callableHeaders(idToken)
                setBody(CallableBookingPolicyUpdateRequest(BookingPolicyPayload.from(request)))
            }
            val body = response.body<CallableBookingPolicyResponse>()
            val error = body.error
            when {
                error != null -> AdminBookingPolicyResult.Failure(error.toAdminError())
                body.result != null -> AdminBookingPolicyResult.Success(body.result.toAdminBookingPolicyConfig())
                else -> AdminBookingPolicyResult.Failure(
                    AdminError.Backend("A resposta da política veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminBookingPolicyResult.Failure(
                AdminError.Unavailable("Não foi possível guardar a política de marcações. Tente novamente."),
            )
        }
    }

    override suspend fun updateLoyaltySettingsConfiguration(
        request: AdminLoyaltySettingsUpdateRequest,
        idToken: String,
    ): AdminLoyaltySettingsResult {
        return try {
            val response = httpClient.post(config.updateLoyaltySettingsUrl) {
                callableHeaders(idToken)
                setBody(CallableLoyaltySettingsUpdateRequest(LoyaltySettingsPayload.from(request)))
            }
            val body = response.body<CallableLoyaltySettingsResponse>()
            val error = body.error
            when {
                error != null -> AdminLoyaltySettingsResult.Failure(error.toAdminError())
                body.result != null -> AdminLoyaltySettingsResult.Success(body.result.toAdminLoyaltySettingsConfig())
                else -> AdminLoyaltySettingsResult.Failure(
                    AdminError.Backend("A resposta da fidelização veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminLoyaltySettingsResult.Failure(
                AdminError.Unavailable("Não foi possível guardar a fidelização. Tente novamente."),
            )
        }
    }

    override suspend fun updateNotificationSettingsConfiguration(
        request: AdminNotificationSettingsUpdateRequest,
        idToken: String,
    ): AdminNotificationSettingsResult {
        return try {
            val response = httpClient.post(config.updateNotificationSettingsUrl) {
                callableHeaders(idToken)
                setBody(CallableNotificationSettingsUpdateRequest(NotificationSettingsPayload.from(request)))
            }
            val body = response.body<CallableNotificationSettingsResponse>()
            val error = body.error
            when {
                error != null -> AdminNotificationSettingsResult.Failure(error.toAdminError())
                body.result != null -> AdminNotificationSettingsResult.Success(
                    body.result.toAdminNotificationSettingsConfig(),
                )
                else -> AdminNotificationSettingsResult.Failure(
                    AdminError.Backend("A resposta das notificações veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminNotificationSettingsResult.Failure(
                AdminError.Unavailable("Não foi possível guardar as notificações. Tente novamente."),
            )
        }
    }

    override suspend fun sendNotificationTestToSelf(
        request: AdminNotificationTestRequest,
        idToken: String,
    ): AdminNotificationTestResult {
        return try {
            val response = httpClient.post(config.sendAdminNotificationTestUrl) {
                callableHeaders(idToken)
                setBody(CallableNotificationTestRequest(NotificationTestPayload.from(request)))
            }
            val body = response.body<CallableNotificationTestResponse>()
            val error = body.error
            when {
                error != null -> AdminNotificationTestResult.Failure(error.toAdminError())
                body.result != null -> AdminNotificationTestResult.Success(body.result.toAdminNotificationTestReceipt())
                else -> AdminNotificationTestResult.Failure(
                    AdminError.Backend("A resposta do teste de notificação veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminNotificationTestResult.Failure(
                AdminError.Unavailable("Não foi possível enviar o teste de notificação. Tente novamente."),
            )
        }
    }

    override suspend fun getNotificationCampaignDrafts(idToken: String): AdminNotificationCampaignDraftsResult {
        return try {
            val response = httpClient.post(config.getAdminNotificationCampaignDraftsUrl) {
                callableHeaders(idToken)
                setBody(CallableEmptyRequest(data = emptyMap()))
            }
            val body = response.body<CallableNotificationCampaignDraftsResponse>()
            val error = body.error
            when {
                error != null -> AdminNotificationCampaignDraftsResult.Failure(error.toAdminError())
                body.result != null -> AdminNotificationCampaignDraftsResult.Success(
                    body.result.toAdminNotificationCampaignDraftsConfig(),
                )
                else -> AdminNotificationCampaignDraftsResult.Failure(
                    AdminError.Backend("A resposta dos rascunhos veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminNotificationCampaignDraftsResult.Failure(
                AdminError.Unavailable("Não foi possível carregar rascunhos de campanha. Tente novamente."),
            )
        }
    }

    override suspend fun upsertNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftMutationRequest,
        idToken: String,
    ): AdminNotificationCampaignDraftMutationResult = postNotificationCampaignDraftMutation(
        url = config.upsertAdminNotificationCampaignDraftUrl,
        payload = NotificationCampaignDraftUpsertPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível guardar o rascunho da campanha. Tente novamente.",
    )

    override suspend fun archiveNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftArchiveRequest,
        idToken: String,
    ): AdminNotificationCampaignDraftMutationResult = postNotificationCampaignDraftMutation(
        url = config.archiveAdminNotificationCampaignDraftUrl,
        payload = NotificationCampaignDraftArchivePayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível arquivar o rascunho da campanha. Tente novamente.",
    )

    override suspend fun broadcastNotificationCampaign(
        request: AdminNotificationCampaignBroadcastRequest,
        idToken: String,
    ): AdminNotificationCampaignBroadcastResult {
        return try {
            val response = httpClient.post(config.broadcastAdminNotificationCampaignUrl) {
                callableHeaders(idToken)
                setBody(
                    CallableNotificationCampaignBroadcastRequest(
                        NotificationCampaignBroadcastPayload.from(request),
                    ),
                )
            }
            val body = response.body<CallableNotificationCampaignBroadcastResponse>()
            val error = body.error
            when {
                error != null -> AdminNotificationCampaignBroadcastResult.Failure(error.toAdminError())
                body.result != null -> AdminNotificationCampaignBroadcastResult.Success(body.result.toReceipt())
                else -> AdminNotificationCampaignBroadcastResult.Failure(
                    AdminError.Backend("A resposta do envio veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminNotificationCampaignBroadcastResult.Failure(
                AdminError.Unavailable("Não foi possível enviar a campanha. Tente novamente."),
            )
        }
    }

    override suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
        idToken: String,
    ): AdminCapacityOverrideMutationResult = postCapacityOverrideMutation(
        url = config.upsertCapacityOverrideUrl,
        payload = CapacityOverrideUpsertPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível guardar a exceção de capacidade. Tente novamente.",
    )

    override suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
        idToken: String,
    ): AdminCapacityOverrideMutationResult = postCapacityOverrideMutation(
        url = config.clearCapacityOverrideUrl,
        payload = CapacityOverrideClearPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível limpar a exceção de capacidade. Tente novamente.",
    )

    override suspend fun upsertBlockedSlot(
        request: AdminBlockedSlotUpsertRequest,
        idToken: String,
    ): AdminBlockedSlotMutationResult = postBlockedSlotMutation(
        url = config.upsertBlockedSlotUrl,
        payload = BlockedSlotUpsertPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível guardar o bloqueio de horário. Tente novamente.",
    )

    override suspend fun clearBlockedSlot(
        request: AdminBlockedSlotClearRequest,
        idToken: String,
    ): AdminBlockedSlotMutationResult = postBlockedSlotMutation(
        url = config.clearBlockedSlotUrl,
        payload = BlockedSlotClearPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível limpar o bloqueio de horário. Tente novamente.",
    )

    override suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult = postServiceCatalogMutation(
        url = config.upsertServiceCatalogItemUrl,
        payload = ServiceCatalogUpsertPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível guardar o serviço. Tente novamente.",
    )

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult = postServiceCatalogMutation(
        url = config.archiveServiceCatalogItemUrl,
        payload = ServiceCatalogArchivePayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível arquivar o serviço. Tente novamente.",
    )

    override suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
        idToken: String,
    ): AdminServiceExtraMutationResult = postServiceExtraMutation(
        url = config.upsertServiceExtraUrl,
        payload = ServiceExtraUpsertPayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível guardar o extra. Tente novamente.",
    )

    override suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
        idToken: String,
    ): AdminServiceExtraMutationResult = postServiceExtraMutation(
        url = config.archiveServiceExtraUrl,
        payload = ServiceExtraArchivePayload.from(request),
        idToken = idToken,
        unavailableMessage = "Não foi possível arquivar o extra. Tente novamente.",
    )

    private suspend fun postDecision(
        url: String,
        payload: DecisionPayload,
        idToken: String,
        unavailableMessage: String,
    ): AdminBookingDecisionResult {
        return try {
            val response = httpClient.post(url) {
                callableHeaders(idToken)
                setBody(CallableDecisionRequest(payload))
            }
            val body = response.body<CallableDecisionResponse>()
            val error = body.error
            when {
                error != null -> AdminBookingDecisionResult.Failure(error.toAdminError())
                body.result != null -> AdminBookingDecisionResult.Success(body.result.toDecisionReceipt())
                else -> AdminBookingDecisionResult.Failure(
                    AdminError.Backend("A resposta da decisão veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminBookingDecisionResult.Failure(AdminError.Unavailable(unavailableMessage))
        }
    }

    private suspend fun postServiceCatalogMutation(
        url: String,
        payload: ServiceCatalogMutationPayload,
        idToken: String,
        unavailableMessage: String,
    ): AdminServiceCatalogMutationResult {
        return try {
            val response = httpClient.post(url) {
                callableHeaders(idToken)
                setBody(CallableServiceCatalogMutationRequest(payload))
            }
            val body = response.body<CallableServiceCatalogMutationResponse>()
            val error = body.error
            when {
                error != null -> AdminServiceCatalogMutationResult.Failure(error.toAdminError())
                body.result != null -> AdminServiceCatalogMutationResult.Success(body.result.toReceipt())
                else -> AdminServiceCatalogMutationResult.Failure(
                    AdminError.Backend("A resposta do catálogo veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminServiceCatalogMutationResult.Failure(AdminError.Unavailable(unavailableMessage))
        }
    }

    private suspend fun postCapacityOverrideMutation(
        url: String,
        payload: CapacityOverridePayload,
        idToken: String,
        unavailableMessage: String,
    ): AdminCapacityOverrideMutationResult {
        return try {
            val response = httpClient.post(url) {
                callableHeaders(idToken)
                setBody(CallableCapacityOverrideMutationRequest(payload))
            }
            val body = response.body<CallableCapacityOverrideMutationResponse>()
            val error = body.error
            when {
                error != null -> AdminCapacityOverrideMutationResult.Failure(error.toAdminError())
                body.result != null -> AdminCapacityOverrideMutationResult.Success(body.result.toReceipt())
                else -> AdminCapacityOverrideMutationResult.Failure(
                    AdminError.Backend("A resposta da capacidade veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminCapacityOverrideMutationResult.Failure(AdminError.Unavailable(unavailableMessage))
        }
    }

    private suspend fun postBlockedSlotMutation(
        url: String,
        payload: BlockedSlotPayload,
        idToken: String,
        unavailableMessage: String,
    ): AdminBlockedSlotMutationResult {
        return try {
            val response = httpClient.post(url) {
                callableHeaders(idToken)
                setBody(CallableBlockedSlotMutationRequest(payload))
            }
            val body = response.body<CallableBlockedSlotMutationResponse>()
            val error = body.error
            when {
                error != null -> AdminBlockedSlotMutationResult.Failure(error.toAdminError())
                body.result != null -> AdminBlockedSlotMutationResult.Success(body.result.toReceipt())
                else -> AdminBlockedSlotMutationResult.Failure(
                    AdminError.Backend("A resposta do bloqueio veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminBlockedSlotMutationResult.Failure(AdminError.Unavailable(unavailableMessage))
        }
    }

    private suspend fun postNotificationCampaignDraftMutation(
        url: String,
        payload: NotificationCampaignDraftMutationPayload,
        idToken: String,
        unavailableMessage: String,
    ): AdminNotificationCampaignDraftMutationResult {
        return try {
            val response = httpClient.post(url) {
                callableHeaders(idToken)
                setBody(CallableNotificationCampaignDraftMutationRequest(payload))
            }
            val body = response.body<CallableNotificationCampaignDraftMutationResponse>()
            val error = body.error
            when {
                error != null -> AdminNotificationCampaignDraftMutationResult.Failure(error.toAdminError())
                body.result != null -> AdminNotificationCampaignDraftMutationResult.Success(body.result.toReceipt())
                else -> AdminNotificationCampaignDraftMutationResult.Failure(
                    AdminError.Backend("A resposta da campanha veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminNotificationCampaignDraftMutationResult.Failure(AdminError.Unavailable(unavailableMessage))
        }
    }

    private suspend fun postServiceExtraMutation(
        url: String,
        payload: ServiceExtraMutationPayload,
        idToken: String,
        unavailableMessage: String,
    ): AdminServiceExtraMutationResult {
        return try {
            val response = httpClient.post(url) {
                callableHeaders(idToken)
                setBody(CallableServiceExtraMutationRequest(payload))
            }
            val body = response.body<CallableServiceExtraMutationResponse>()
            val error = body.error
            when {
                error != null -> AdminServiceExtraMutationResult.Failure(error.toAdminError())
                body.result != null -> AdminServiceExtraMutationResult.Success(body.result.toReceipt())
                else -> AdminServiceExtraMutationResult.Failure(
                    AdminError.Backend("A resposta dos extras veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AdminServiceExtraMutationResult.Failure(AdminError.Unavailable(unavailableMessage))
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.callableHeaders(idToken: String) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        header(HttpHeaders.Authorization, "Bearer $idToken")
    }
}

@Serializable
private data class CallableEmptyRequest(
    val data: Map<String, String>,
)

@Serializable
private data class CallableDecisionRequest(
    val data: DecisionPayload,
)

@Serializable
private data class CallableBusinessInfoUpdateRequest(
    val data: BusinessInfoPayload,
)

@Serializable
private data class CallableAvailabilityUpdateRequest(
    val data: AvailabilityPayload,
)

@Serializable
private data class CallableBookingPolicyUpdateRequest(
    val data: BookingPolicyPayload,
)

@Serializable
private data class CallableLoyaltySettingsUpdateRequest(
    val data: LoyaltySettingsPayload,
)

@Serializable
private data class CallableNotificationSettingsUpdateRequest(
    val data: NotificationSettingsPayload,
)

@Serializable
private data class CallableNotificationTestRequest(
    val data: NotificationTestPayload,
)

@Serializable
private data class CallableNotificationCampaignDraftMutationRequest(
    val data: NotificationCampaignDraftMutationPayload,
)

@Serializable
private data class CallableNotificationCampaignBroadcastRequest(
    val data: NotificationCampaignBroadcastPayload,
)

@Serializable
private data class CallableCapacityOverrideMutationRequest(
    val data: CapacityOverridePayload,
)

@Serializable
private data class CallableBlockedSlotMutationRequest(
    val data: BlockedSlotPayload,
)

@Serializable
private data class CallableServiceCatalogMutationRequest(
    val data: ServiceCatalogMutationPayload,
)

@Serializable
private data class CallableServiceExtraMutationRequest(
    val data: ServiceExtraMutationPayload,
)

@Serializable
private data class DecisionPayload(
    val reservationId: String,
    val rejectionReason: String = "",
) {
    companion object {
        fun from(request: AdminBookingDecisionRequest): DecisionPayload = DecisionPayload(
            reservationId = request.reservationId,
            rejectionReason = request.rejectionReason,
        )
    }
}

@Serializable
private data class BusinessInfoPayload(
    val phone: String,
    val email: String,
    val addressLine1: String,
    val addressLine2: String,
    val mapsUri: String,
    val whatsappUri: String,
    val openingHours: List<BusinessOpeningHoursPayload>,
    val socialLinks: List<BusinessSocialLinkPayload> = emptyList(),
) {
    companion object {
        fun from(request: AdminBusinessInfoUpdateRequest): BusinessInfoPayload = BusinessInfoPayload(
            phone = request.phone,
            email = request.email,
            addressLine1 = request.addressLine1,
            addressLine2 = request.addressLine2,
            mapsUri = request.mapsUri,
            whatsappUri = request.whatsappUri,
            openingHours = request.openingHours.map { BusinessOpeningHoursPayload.from(it) },
            socialLinks = request.socialLinks.map { BusinessSocialLinkPayload.from(it) },
        )
    }
}

@Serializable
private data class AvailabilityPayload(
    val defaultMaxBookingsPerSlot: Int,
    val defaultSlotIntervalMinutes: Int,
    val openingHours: List<BusinessOpeningHoursPayload>,
) {
    companion object {
        fun from(request: AdminAvailabilityUpdateRequest): AvailabilityPayload = AvailabilityPayload(
            defaultMaxBookingsPerSlot = request.defaultMaxBookingsPerSlot,
            defaultSlotIntervalMinutes = request.defaultSlotIntervalMinutes,
            openingHours = request.openingHours.map { BusinessOpeningHoursPayload.from(it) },
        )
    }
}

@Serializable
private data class BookingPolicyPayload(
    val pendingHoldMinutes: Int,
    val cancellationWindowMinutes: Int,
    val rescheduleWindowMinutes: Int,
    val paymentEligibilityCopy: String,
) {
    companion object {
        fun from(request: AdminBookingPolicyUpdateRequest): BookingPolicyPayload = BookingPolicyPayload(
            pendingHoldMinutes = request.pendingHoldMinutes,
            cancellationWindowMinutes = request.cancellationWindowMinutes,
            rescheduleWindowMinutes = request.rescheduleWindowMinutes,
            paymentEligibilityCopy = request.paymentEligibilityCopy,
        )
    }
}

@Serializable
private data class LoyaltySettingsPayload(
    val stampsRequired: Int,
    val rewardType: String,
    val rewardValue: Int,
    val rewardDescription: String,
) {
    companion object {
        fun from(request: AdminLoyaltySettingsUpdateRequest): LoyaltySettingsPayload = LoyaltySettingsPayload(
            stampsRequired = request.stampsRequired,
            rewardType = request.rewardType,
            rewardValue = request.rewardValue,
            rewardDescription = request.rewardDescription,
        )
    }
}

@Serializable
private data class NotificationSettingsPayload(
    val bookingStatusEnabled: Boolean,
    val appointmentReminderEnabled: Boolean,
    val loyaltyEnabled: Boolean,
    val adminPendingAlertEnabled: Boolean,
    val marketingEnabled: Boolean,
    val reminderLeadMinutes: Int,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val quietHoursTimeZone: String,
    val templates: List<NotificationTemplatePayload>,
) {
    companion object {
        fun from(request: AdminNotificationSettingsUpdateRequest): NotificationSettingsPayload =
            NotificationSettingsPayload(
                bookingStatusEnabled = request.bookingStatusEnabled,
                appointmentReminderEnabled = request.appointmentReminderEnabled,
                loyaltyEnabled = request.loyaltyEnabled,
                adminPendingAlertEnabled = request.adminPendingAlertEnabled,
                marketingEnabled = request.marketingEnabled,
                reminderLeadMinutes = request.reminderLeadMinutes,
                quietHoursStart = request.quietHoursStart,
                quietHoursEnd = request.quietHoursEnd,
                quietHoursTimeZone = request.quietHoursTimeZone,
                templates = request.templates.map { NotificationTemplatePayload.from(it) },
            )
    }
}

@Serializable
private data class NotificationTemplatePayload(
    val key: String,
    val label: String = "",
    val enabled: Boolean = true,
    val title: String = "",
    val body: String = "",
) {
    companion object {
        fun from(template: AdminNotificationTemplateConfig): NotificationTemplatePayload {
            return NotificationTemplatePayload(
                key = template.key,
                label = template.label,
                enabled = template.enabled,
                title = template.title,
                body = template.body,
            )
        }
    }

    fun toAdminNotificationTemplate(): AdminNotificationTemplateConfig = AdminNotificationTemplateConfig(
        key = key.trim(),
        label = label.trim(),
        enabled = enabled,
        title = title.trim(),
        body = body.trim(),
    )
}

@Serializable
private data class NotificationTestPayload(
    val templateKey: String,
    val campaignId: String = "",
) {
    companion object {
        fun from(request: AdminNotificationTestRequest): NotificationTestPayload = NotificationTestPayload(
            templateKey = request.templateKey,
            campaignId = request.campaignId,
        )
    }
}

@Serializable
private sealed interface NotificationCampaignDraftMutationPayload

@Serializable
private data class NotificationCampaignDraftUpsertPayload(
    val campaignId: String = "",
    val title: String,
    val body: String,
    val targetAudience: String,
    val scheduledAtIso: String = "",
    val notes: String = "",
    val pushEnabled: Boolean = true,
) : NotificationCampaignDraftMutationPayload {
    companion object {
        fun from(request: AdminNotificationCampaignDraftMutationRequest): NotificationCampaignDraftUpsertPayload {
            return NotificationCampaignDraftUpsertPayload(
                campaignId = request.campaignId,
                title = request.title,
                body = request.body,
                targetAudience = request.targetAudience,
                scheduledAtIso = request.scheduledAtIso,
                notes = request.notes,
                pushEnabled = request.pushEnabled,
            )
        }
    }
}

@Serializable
private data class NotificationCampaignDraftArchivePayload(
    val campaignId: String,
) : NotificationCampaignDraftMutationPayload {
    companion object {
        fun from(request: AdminNotificationCampaignDraftArchiveRequest): NotificationCampaignDraftArchivePayload {
            return NotificationCampaignDraftArchivePayload(campaignId = request.campaignId)
        }
    }
}

@Serializable
private data class NotificationCampaignBroadcastPayload(
    val campaignId: String,
    val confirmBroadcast: Boolean = true,
) {
    companion object {
        fun from(request: AdminNotificationCampaignBroadcastRequest): NotificationCampaignBroadcastPayload {
            return NotificationCampaignBroadcastPayload(
                campaignId = request.campaignId,
                confirmBroadcast = request.confirmBroadcast,
            )
        }
    }
}

@Serializable
private sealed interface CapacityOverridePayload

@Serializable
private data class CapacityOverrideUpsertPayload(
    val date: String,
    val maxBookingsPerSlot: Int,
) : CapacityOverridePayload {
    companion object {
        fun from(request: AdminCapacityOverrideUpsertRequest): CapacityOverrideUpsertPayload {
            return CapacityOverrideUpsertPayload(
                date = request.date,
                maxBookingsPerSlot = request.maxBookingsPerSlot,
            )
        }
    }
}

@Serializable
private data class CapacityOverrideClearPayload(
    val date: String,
) : CapacityOverridePayload {
    companion object {
        fun from(request: AdminCapacityOverrideClearRequest): CapacityOverrideClearPayload {
            return CapacityOverrideClearPayload(date = request.date)
        }
    }
}

@Serializable
private sealed interface BlockedSlotPayload

@Serializable
private data class BlockedSlotUpsertPayload(
    val blockedSlotId: String = "",
    val date: String,
    val slotStart: String,
    val slotEnd: String,
    val reason: String = "",
) : BlockedSlotPayload {
    companion object {
        fun from(request: AdminBlockedSlotUpsertRequest): BlockedSlotUpsertPayload {
            return BlockedSlotUpsertPayload(
                blockedSlotId = request.blockedSlotId,
                date = request.date,
                slotStart = request.slotStartIso,
                slotEnd = request.slotEndIso,
                reason = request.reason,
            )
        }
    }
}

@Serializable
private data class BlockedSlotClearPayload(
    val blockedSlotId: String,
) : BlockedSlotPayload {
    companion object {
        fun from(request: AdminBlockedSlotClearRequest): BlockedSlotClearPayload {
            return BlockedSlotClearPayload(blockedSlotId = request.blockedSlotId)
        }
    }
}

@Serializable
private data class BusinessOpeningHoursPayload(
    val dayLabel: String,
    val hoursLabel: String,
    val closed: Boolean = false,
) {
    companion object {
        fun from(hours: AdminBusinessOpeningHours): BusinessOpeningHoursPayload = BusinessOpeningHoursPayload(
            dayLabel = hours.dayLabel,
            hoursLabel = hours.hoursLabel,
            closed = hours.closed,
        )
    }

    fun toAdminOpeningHours(): AdminBusinessOpeningHours = AdminBusinessOpeningHours(
        dayLabel = dayLabel.trim(),
        hoursLabel = hoursLabel.trim(),
        closed = closed,
    )
}

@Serializable
private data class BusinessSocialLinkPayload(
    val label: String,
    val uri: String,
) {
    companion object {
        fun from(link: AdminBusinessSocialLink): BusinessSocialLinkPayload = BusinessSocialLinkPayload(
            label = link.label,
            uri = link.uri,
        )
    }

    fun toAdminSocialLink(): AdminBusinessSocialLink = AdminBusinessSocialLink(
        label = label.trim(),
        uri = uri.trim(),
    )
}

@Serializable
private sealed interface ServiceCatalogMutationPayload

@Serializable
private data class ServiceCatalogUpsertPayload(
    val serviceId: String = "",
    val name: String,
    val description: String = "",
    val durationMinutes: Int,
    val passengerPriceCents: Int,
    val suvPriceCents: Int,
    val iconKey: String = "car",
    val popular: Boolean = false,
    val active: Boolean = true,
    val sortOrder: Int = 999,
) : ServiceCatalogMutationPayload {
    companion object {
        fun from(request: AdminServiceCatalogMutationRequest): ServiceCatalogUpsertPayload {
            return ServiceCatalogUpsertPayload(
                serviceId = request.serviceId,
                name = request.name,
                description = request.description,
                durationMinutes = request.durationMinutes,
                passengerPriceCents = request.passengerPriceCents,
                suvPriceCents = request.suvPriceCents,
                iconKey = request.iconKey,
                popular = request.popular,
                active = request.active,
                sortOrder = request.sortOrder,
            )
        }
    }
}

@Serializable
private data class ServiceCatalogArchivePayload(
    val serviceId: String,
) : ServiceCatalogMutationPayload {
    companion object {
        fun from(request: AdminServiceCatalogArchiveRequest): ServiceCatalogArchivePayload {
            return ServiceCatalogArchivePayload(serviceId = request.serviceId)
        }
    }
}

@Serializable
private sealed interface ServiceExtraMutationPayload

@Serializable
private data class ServiceExtraUpsertPayload(
    val extraId: String = "",
    val name: String,
    val description: String = "",
    val priceCents: Int,
    val iconKey: String = "auto_awesome",
    val eligibleServiceIds: List<String> = emptyList(),
    val active: Boolean = true,
    val sortOrder: Int = 999,
) : ServiceExtraMutationPayload {
    companion object {
        fun from(request: AdminServiceExtraMutationRequest): ServiceExtraUpsertPayload {
            return ServiceExtraUpsertPayload(
                extraId = request.extraId,
                name = request.name,
                description = request.description,
                priceCents = request.priceCents,
                iconKey = request.iconKey,
                eligibleServiceIds = request.eligibleServiceIds,
                active = request.active,
                sortOrder = request.sortOrder,
            )
        }
    }
}

@Serializable
private data class ServiceExtraArchivePayload(
    val extraId: String,
) : ServiceExtraMutationPayload {
    companion object {
        fun from(request: AdminServiceExtraArchiveRequest): ServiceExtraArchivePayload {
            return ServiceExtraArchivePayload(extraId = request.extraId)
        }
    }
}

@Serializable
private data class CallableRoleResponse(
    val result: RolePayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallablePendingReservationsResponse(
    val result: PendingReservationsPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableDecisionResponse(
    val result: DecisionResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableBusinessInfoResponse(
    val result: BusinessInfoResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableAvailabilityResponse(
    val result: AvailabilityResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableBookingPolicyResponse(
    val result: BookingPolicyResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableLoyaltySettingsResponse(
    val result: LoyaltySettingsResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableNotificationSettingsResponse(
    val result: NotificationSettingsResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableNotificationTestResponse(
    val result: NotificationTestResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableNotificationCampaignDraftsResponse(
    val result: NotificationCampaignDraftsPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableNotificationCampaignDraftMutationResponse(
    val result: NotificationCampaignDraftMutationResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableNotificationCampaignBroadcastResponse(
    val result: NotificationCampaignBroadcastResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableCapacityOverrideMutationResponse(
    val result: CapacityOverrideMutationResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableBlockedSlotMutationResponse(
    val result: BlockedSlotMutationResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableServiceCatalogMutationResponse(
    val result: ServiceCatalogMutationResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableServiceExtraMutationResponse(
    val result: ServiceExtraMutationResultPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableAdminServiceCatalogResponse(
    val result: AdminServiceCatalogPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableAdminServiceExtrasResponse(
    val result: AdminServiceExtrasPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class RolePayload(
    val uid: String,
    val email: String = "",
    val role: String,
) {
    fun toAdminRole(): AdminRole = AdminRole(
        uid = uid.trim(),
        email = email.trim(),
        role = role.trim(),
    )
}

@Serializable
private data class PendingReservationsPayload(
    val requests: List<AdminBookingRequestPayload> = emptyList(),
)

@Serializable
private data class AdminBookingRequestPayload(
    val id: String,
    val reservationCode: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val slotStart: String,
    val slotEnd: String,
    val status: String = "pending",
    val paymentStatus: String = "pending",
    val vehicleType: String = "passageiros",
    val vehicleLabel: String = "",
    val priceCents: Int? = null,
    val extras: List<AdminReservationExtraPayload> = emptyList(),
    val notes: String = "",
    val createdAt: String = "",
    val pendingExpiresAt: String? = null,
    val loyaltyRewardApplied: Boolean = false,
    val canStart: Boolean = false,
    val canComplete: Boolean = false,
    val acceptedAt: String? = null,
    val acceptedByUid: String = "",
    val startedAt: String? = null,
    val startedByUid: String = "",
    val rejectedAt: String? = null,
    val rejectedByUid: String = "",
    val completedAt: String? = null,
    val completedByUid: String = "",
) {
    fun toAdminBookingRequest(): AdminBookingRequest = AdminBookingRequest(
        id = id,
        reservationCode = reservationCode,
        customerName = customerName,
        customerEmail = customerEmail,
        customerPhone = customerPhone,
        serviceId = serviceId,
        serviceName = serviceName,
        slotStartIso = slotStart,
        slotEndIso = slotEnd,
        status = status,
        paymentStatus = paymentStatus,
        vehicleType = vehicleType,
        vehicleLabel = vehicleLabel,
        priceCents = priceCents,
        extras = extras.map { it.toReservationExtra() },
        notes = notes,
        createdAtIso = createdAt,
        pendingExpiresAtIso = pendingExpiresAt,
        loyaltyRewardApplied = loyaltyRewardApplied,
        canStart = canStart,
        canComplete = canComplete,
        acceptedAtIso = acceptedAt,
        acceptedByUid = acceptedByUid.trim(),
        startedAtIso = startedAt,
        startedByUid = startedByUid.trim(),
        rejectedAtIso = rejectedAt,
        rejectedByUid = rejectedByUid.trim(),
        completedAtIso = completedAt,
        completedByUid = completedByUid.trim(),
    )
}

@Serializable
private data class AdminReservationExtraPayload(
    val id: String,
    val name: String,
    val priceCents: Int,
) {
    fun toReservationExtra(): BookingReservationExtra = BookingReservationExtra(
        id = id,
        name = name,
        priceCents = priceCents.coerceAtLeast(0),
    )
}

@Serializable
private data class DecisionResultPayload(
    val reservationId: String,
    val reservationCode: String = "",
    val status: String,
) {
    fun toDecisionReceipt(): AdminBookingDecisionReceipt = AdminBookingDecisionReceipt(
        reservationId = reservationId,
        reservationCode = reservationCode,
        status = status,
    )
}

@Serializable
private data class ServiceCatalogMutationResultPayload(
    val serviceId: String,
    val status: String = "",
    val created: Boolean = false,
) {
    fun toReceipt(): AdminServiceCatalogMutationReceipt = AdminServiceCatalogMutationReceipt(
        serviceId = serviceId,
        status = status,
        created = created,
    )
}

@Serializable
private data class ServiceExtraMutationResultPayload(
    val extraId: String,
    val status: String = "",
    val created: Boolean = false,
) {
    fun toReceipt(): AdminServiceExtraMutationReceipt = AdminServiceExtraMutationReceipt(
        extraId = extraId,
        status = status,
        created = created,
    )
}

@Serializable
private data class CapacityOverrideMutationResultPayload(
    val date: String,
    val status: String = "",
    val maxBookingsPerSlot: Int? = null,
) {
    fun toReceipt(): AdminCapacityOverrideMutationReceipt = AdminCapacityOverrideMutationReceipt(
        date = date.trim(),
        status = status.trim(),
        maxBookingsPerSlot = maxBookingsPerSlot?.coerceIn(0, 20),
    )
}

@Serializable
private data class BlockedSlotMutationResultPayload(
    val blockedSlotId: String,
    val status: String = "",
    val date: String = "",
) {
    fun toReceipt(): AdminBlockedSlotMutationReceipt = AdminBlockedSlotMutationReceipt(
        blockedSlotId = blockedSlotId.trim(),
        status = status.trim(),
        date = date.trim(),
    )
}

@Serializable
private data class AdminServiceCatalogPayload(
    val services: List<AdminServiceCatalogItemPayload> = emptyList(),
) {
    fun toAdminServiceCatalogConfig(): AdminServiceCatalogConfig = AdminServiceCatalogConfig(
        services = services.map { it.toAdminServiceCatalogItem() },
    )
}

@Serializable
private data class AdminServiceExtrasPayload(
    val extras: List<AdminServiceExtraItemPayload> = emptyList(),
) {
    fun toAdminServiceExtrasConfig(): AdminServiceExtrasConfig = AdminServiceExtrasConfig(
        extras = extras.map { it.toAdminServiceExtraItem() },
    )
}

@Serializable
private data class AdminServiceCatalogItemPayload(
    val id: String,
    val name: String,
    val description: String = "",
    val durationMinutes: Int = 30,
    val passengerPriceCents: Int = 0,
    val suvPriceCents: Int = passengerPriceCents,
    val iconKey: String = "car",
    val popular: Boolean = false,
    val active: Boolean = true,
    val sortOrder: Int = 999,
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
    val archivedAtIso: String = "",
    val createdByUid: String = "",
    val updatedByUid: String = "",
    val archivedByUid: String = "",
) {
    fun toAdminServiceCatalogItem(): AdminServiceCatalogItem = AdminServiceCatalogItem(
        id = id.trim(),
        name = name.trim(),
        description = description.trim(),
        durationMinutes = durationMinutes.coerceIn(5, 480),
        passengerPriceCents = passengerPriceCents.coerceIn(0, 100000),
        suvPriceCents = suvPriceCents.coerceIn(0, 100000),
        iconKey = iconKey.trim().ifBlank { "car" },
        popular = popular,
        active = active,
        sortOrder = sortOrder.coerceIn(0, 9999),
        createdAtIso = createdAtIso.trim(),
        updatedAtIso = updatedAtIso.trim(),
        archivedAtIso = archivedAtIso.trim(),
        createdByUid = createdByUid.trim(),
        updatedByUid = updatedByUid.trim(),
        archivedByUid = archivedByUid.trim(),
    )
}

@Serializable
private data class AdminServiceExtraItemPayload(
    val id: String,
    val name: String,
    val description: String = "",
    val priceCents: Int = 0,
    val iconKey: String = "auto_awesome",
    val eligibleServiceIds: List<String> = emptyList(),
    val active: Boolean = true,
    val sortOrder: Int = 999,
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
    val archivedAtIso: String = "",
    val createdByUid: String = "",
    val updatedByUid: String = "",
    val archivedByUid: String = "",
) {
    fun toAdminServiceExtraItem(): AdminServiceExtraItem = AdminServiceExtraItem(
        id = id.trim(),
        name = name.trim(),
        description = description.trim(),
        priceCents = priceCents.coerceIn(0, 100000),
        iconKey = iconKey.trim().ifBlank { "auto_awesome" },
        eligibleServiceIds = eligibleServiceIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct(),
        active = active,
        sortOrder = sortOrder.coerceIn(0, 9999),
        createdAtIso = createdAtIso.trim(),
        updatedAtIso = updatedAtIso.trim(),
        archivedAtIso = archivedAtIso.trim(),
        createdByUid = createdByUid.trim(),
        updatedByUid = updatedByUid.trim(),
        archivedByUid = archivedByUid.trim(),
    )
}

@Serializable
private data class BusinessInfoResultPayload(
    val phone: String = "",
    val email: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val mapsUri: String = "",
    val whatsappUri: String = "",
    val openingHours: List<BusinessOpeningHoursPayload> = emptyList(),
    val socialLinks: List<BusinessSocialLinkPayload> = emptyList(),
    val source: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
) {
    fun toAdminBusinessInfoConfig(): AdminBusinessInfoConfig = AdminBusinessInfoConfig(
        phone = phone.trim(),
        email = email.trim(),
        addressLine1 = addressLine1.trim(),
        addressLine2 = addressLine2.trim(),
        mapsUri = mapsUri.trim(),
        whatsappUri = whatsappUri.trim(),
        openingHours = openingHours.map { it.toAdminOpeningHours() },
        socialLinks = socialLinks.map { it.toAdminSocialLink() },
        source = source.trim(),
        updatedAtIso = updatedAtIso.trim(),
        updatedByUid = updatedByUid.trim(),
    )
}

@Serializable
private data class AvailabilityResultPayload(
    val defaultMaxBookingsPerSlot: Int = 2,
    val defaultSlotIntervalMinutes: Int = 30,
    val openingHours: List<BusinessOpeningHoursPayload> = emptyList(),
    val capacityOverrides: List<CapacityOverrideItemPayload> = emptyList(),
    val blockedSlots: List<BlockedSlotItemPayload> = emptyList(),
) {
    fun toAdminAvailabilityConfig(): AdminAvailabilityConfig = AdminAvailabilityConfig(
        defaultMaxBookingsPerSlot = defaultMaxBookingsPerSlot.coerceIn(0, 20),
        defaultSlotIntervalMinutes = defaultSlotIntervalMinutes.coerceIn(5, 240),
        openingHours = openingHours.map { it.toAdminOpeningHours() },
        capacityOverrides = capacityOverrides.map { it.toAdminCapacityOverrideItem() },
        blockedSlots = blockedSlots.mapNotNull { it.toAdminBlockedSlotItemOrNull() },
    )
}

@Serializable
private data class BookingPolicyResultPayload(
    val pendingHoldMinutes: Int = 1440,
    val cancellationWindowMinutes: Int = 0,
    val rescheduleWindowMinutes: Int = 0,
    val paymentEligibilityCopy: String = "",
    val source: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
) {
    fun toAdminBookingPolicyConfig(): AdminBookingPolicyConfig = AdminBookingPolicyConfig(
        pendingHoldMinutes = pendingHoldMinutes.coerceIn(15, 10080),
        cancellationWindowMinutes = cancellationWindowMinutes.coerceIn(0, 10080),
        rescheduleWindowMinutes = rescheduleWindowMinutes.coerceIn(0, 10080),
        paymentEligibilityCopy = paymentEligibilityCopy.trim().ifBlank {
            "Pagamento confirmado no local após validação da marcação."
        },
        source = source.trim(),
        updatedAtIso = updatedAtIso.trim(),
        updatedByUid = updatedByUid.trim(),
    )
}

@Serializable
private data class LoyaltySettingsResultPayload(
    val stampsRequired: Int = 10,
    val rewardType: String = "free_wash",
    val rewardValue: Int = 1,
    val rewardDescription: String = "",
    val source: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
) {
    fun toAdminLoyaltySettingsConfig(): AdminLoyaltySettingsConfig = AdminLoyaltySettingsConfig(
        stampsRequired = stampsRequired.coerceIn(1, 50),
        rewardType = rewardType.trim().ifBlank { "free_wash" },
        rewardValue = rewardValue.coerceAtLeast(1),
        rewardDescription = rewardDescription.trim().ifBlank { "1 lavagem grátis" },
        source = source.trim(),
        updatedAtIso = updatedAtIso.trim(),
        updatedByUid = updatedByUid.trim(),
    )
}

@Serializable
private data class NotificationSettingsResultPayload(
    val bookingStatusEnabled: Boolean = true,
    val appointmentReminderEnabled: Boolean = true,
    val loyaltyEnabled: Boolean = true,
    val adminPendingAlertEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 120,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00",
    val quietHoursTimeZone: String = "Europe/Lisbon",
    val templates: List<NotificationTemplatePayload> = emptyList(),
    val source: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
) {
    fun toAdminNotificationSettingsConfig(): AdminNotificationSettingsConfig = AdminNotificationSettingsConfig(
        bookingStatusEnabled = bookingStatusEnabled,
        appointmentReminderEnabled = appointmentReminderEnabled,
        loyaltyEnabled = loyaltyEnabled,
        adminPendingAlertEnabled = adminPendingAlertEnabled,
        marketingEnabled = marketingEnabled,
        reminderLeadMinutes = reminderLeadMinutes.coerceIn(15, 10080),
        quietHoursStart = quietHoursStart.trim().ifBlank { "22:00" },
        quietHoursEnd = quietHoursEnd.trim().ifBlank { "08:00" },
        quietHoursTimeZone = quietHoursTimeZone.trim().ifBlank { "Europe/Lisbon" },
        templates = templates.map { it.toAdminNotificationTemplate() },
        source = source.trim(),
        updatedAtIso = updatedAtIso.trim(),
        updatedByUid = updatedByUid.trim(),
    )
}

@Serializable
private data class NotificationTestResultPayload(
    val notificationId: String = "",
    val templateKey: String = "",
    val campaignId: String = "",
    val deliveryState: String = "",
    val recipientUid: String = "",
    val targetScope: String = "",
    val testOnly: Boolean = false,
    val targetAudience: String = "",
    val marketingConsentRequired: Boolean = false,
    val sendBlocked: Boolean = false,
    val sendBlockedReason: String = "",
    val deliveryLocked: Boolean = false,
    val sendState: String = "",
    val tokenCount: Int = 0,
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val invalidatedCount: Int = 0,
    val message: String = "",
) {
    fun toAdminNotificationTestReceipt(): AdminNotificationTestReceipt {
        val normalizedCampaignId = campaignId.trim()
        val isCampaignReceipt = normalizedCampaignId.isNotBlank()
        val normalizedAudience = if (isCampaignReceipt) {
            targetAudience.toSafeCampaignAudience()
        } else {
            targetAudience.trim()
        }
        val normalizedSendBlockedReason = if (isCampaignReceipt) {
            sendBlockedReason.trim().ifBlank { NotificationCampaignDraftSendBlockedReason }
        } else {
            sendBlockedReason.trim()
        }
        val normalizedSendState = if (isCampaignReceipt) {
            sendState.trim().ifBlank { NotificationCampaignDraftSendState }
        } else {
            sendState.trim()
        }
        return AdminNotificationTestReceipt(
            notificationId = notificationId.trim(),
            templateKey = templateKey.trim(),
            campaignId = normalizedCampaignId,
            deliveryState = deliveryState.trim(),
            recipientUid = recipientUid.trim(),
            targetScope = targetScope.trim(),
            testOnly = testOnly,
            message = message.trim(),
            targetAudience = normalizedAudience,
            marketingConsentRequired = marketingConsentRequired ||
                (isCampaignReceipt && normalizedAudience != "test_users"),
            sendBlocked = if (isCampaignReceipt) true else sendBlocked,
            sendBlockedReason = normalizedSendBlockedReason,
            deliveryLocked = if (isCampaignReceipt) true else deliveryLocked,
            sendState = normalizedSendState,
            tokenCount = tokenCount.coerceAtLeast(0),
            sentCount = sentCount.coerceAtLeast(0),
            failedCount = failedCount.coerceAtLeast(0),
            invalidatedCount = invalidatedCount.coerceAtLeast(0),
        )
    }
}

@Serializable
private data class NotificationCampaignDraftsPayload(
    val source: String = "",
    val campaigns: List<NotificationCampaignDraftPayload> = emptyList(),
) {
    fun toAdminNotificationCampaignDraftsConfig(): AdminNotificationCampaignDraftsConfig {
        return AdminNotificationCampaignDraftsConfig(
            source = source.trim().ifBlank { "empty" },
            campaigns = campaigns.mapNotNull { it.toAdminNotificationCampaignDraftOrNull() },
        )
    }
}

private const val NotificationCampaignDraftSendBlockedReason = "campaign-send-not-implemented"
private const val NotificationCampaignDraftSendState = "ready"

private fun String.toSafeCampaignAudience(): String = when (trim()) {
    "test_users" -> "test_users"
    "marketing_opt_in_users", "all_users", "" -> "marketing_opt_in_users"
    else -> "test_users"
}

@Serializable
private data class NotificationCampaignDraftPayload(
    val campaignId: String = "",
    val title: String = "",
    val body: String = "",
    val targetAudience: String = "marketing_opt_in_users",
    val channels: List<String> = emptyList(),
    val marketingConsentRequired: Boolean = false,
    val status: String = "draft",
    val scheduledAtIso: String = "",
    val notes: String = "",
    val sendBlocked: Boolean = true,
    val sendBlockedReason: String = "",
    val deliveryLocked: Boolean = true,
    val sendState: String = "",
    val createdAtIso: String = "",
    val updatedAtIso: String = "",
    val archivedAtIso: String = "",
    val createdByUid: String = "",
    val updatedByUid: String = "",
    val archivedByUid: String = "",
    val sentAtIso: String = "",
    val sentByUid: String = "",
    val queuedCount: Int = 0,
) {
    fun toAdminNotificationCampaignDraftOrNull(): AdminNotificationCampaignDraft? {
        val id = campaignId.trim()
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (id.isBlank() || cleanTitle.isBlank() || cleanBody.isBlank()) return null
        val normalizedAudience = targetAudience.toSafeCampaignAudience()
        val normalizedStatus = status.trim().ifBlank { "draft" }
        val normalizedSendBlockedReason = sendBlockedReason.trim()
            .ifBlank {
                when (normalizedStatus) {
                    "archived" -> NotificationCampaignDraftSendBlockedReason
                    "sent" -> "campaign-already-sent"
                    else -> ""
                }
            }
        val normalizedSendState = sendState.trim().ifBlank {
            when (normalizedStatus) {
                "archived" -> "archived"
                "sent" -> "sent"
                else -> NotificationCampaignDraftSendState
            }
        }
        return AdminNotificationCampaignDraft(
            campaignId = id,
            title = cleanTitle,
            body = cleanBody,
            targetAudience = normalizedAudience,
            channels = channels.map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("push") },
            marketingConsentRequired = marketingConsentRequired || normalizedAudience != "test_users",
            status = normalizedStatus,
            scheduledAtIso = scheduledAtIso.trim(),
            notes = notes.trim(),
            sendBlocked = normalizedStatus == "sent" || normalizedStatus == "archived",
            sendBlockedReason = if (normalizedStatus == "sent" || normalizedStatus == "archived") {
                normalizedSendBlockedReason
            } else {
                ""
            },
            deliveryLocked = normalizedStatus == "sent" || normalizedStatus == "archived",
            sendState = normalizedSendState,
            createdAtIso = createdAtIso.trim(),
            updatedAtIso = updatedAtIso.trim(),
            archivedAtIso = archivedAtIso.trim(),
            createdByUid = createdByUid.trim(),
            updatedByUid = updatedByUid.trim(),
            archivedByUid = archivedByUid.trim(),
            sentAtIso = sentAtIso.trim(),
            sentByUid = sentByUid.trim(),
            queuedCount = queuedCount.coerceAtLeast(0),
        )
    }
}

@Serializable
private data class NotificationCampaignDraftMutationResultPayload(
    val campaignId: String = "",
    val status: String = "",
    val created: Boolean = false,
    val targetAudience: String = "",
    val sendBlocked: Boolean = true,
    val sendBlockedReason: String = "",
    val deliveryLocked: Boolean = true,
    val sendState: String = "",
) {
    fun toReceipt(): AdminNotificationCampaignDraftMutationReceipt {
        val normalizedStatus = status.trim().ifBlank { "draft" }
        val normalizedSendBlockedReason = sendBlockedReason.trim()
            .ifBlank {
                when (normalizedStatus) {
                    "archived" -> NotificationCampaignDraftSendBlockedReason
                    "sent" -> "campaign-already-sent"
                    else -> ""
                }
            }
        val normalizedSendState = sendState.trim().ifBlank {
            when (normalizedStatus) {
                "archived" -> "archived"
                "sent" -> "sent"
                else -> NotificationCampaignDraftSendState
            }
        }
        val locked = normalizedStatus == "sent" || normalizedStatus == "archived"
        return AdminNotificationCampaignDraftMutationReceipt(
            campaignId = campaignId.trim(),
            status = normalizedStatus,
            created = created,
            targetAudience = targetAudience.toSafeCampaignAudience(),
            sendBlocked = locked,
            sendBlockedReason = if (locked) normalizedSendBlockedReason else "",
            deliveryLocked = locked,
            sendState = normalizedSendState,
        )
    }
}

@Serializable
private data class NotificationCampaignBroadcastResultPayload(
    val campaignId: String = "",
    val status: String = "",
    val targetAudience: String = "",
    val queuedCount: Int = 0,
    val skippedCount: Int = 0,
    val sentByUid: String = "",
    val sendBlocked: Boolean = true,
    val sendBlockedReason: String = "",
    val deliveryLocked: Boolean = true,
    val sendState: String = "sent",
) {
    fun toReceipt(): AdminNotificationCampaignBroadcastReceipt {
        return AdminNotificationCampaignBroadcastReceipt(
            campaignId = campaignId.trim(),
            status = status.trim(),
            targetAudience = targetAudience.toSafeCampaignAudience(),
            queuedCount = queuedCount.coerceAtLeast(0),
            skippedCount = skippedCount.coerceAtLeast(0),
            sentByUid = sentByUid.trim(),
            sendBlocked = sendBlocked,
            sendBlockedReason = sendBlockedReason.trim().ifBlank { "campaign-already-sent" },
            deliveryLocked = deliveryLocked,
            sendState = sendState.trim().ifBlank { "sent" },
        )
    }
}

@Serializable
private data class CapacityOverrideItemPayload(
    val date: String,
    val maxBookingsPerSlot: Int = 0,
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
) {
    fun toAdminCapacityOverrideItem(): AdminCapacityOverrideItem = AdminCapacityOverrideItem(
        date = date.trim(),
        maxBookingsPerSlot = maxBookingsPerSlot.coerceIn(0, 20),
        updatedAtIso = updatedAtIso.trim(),
        updatedByUid = updatedByUid.trim(),
    )
}

@Serializable
private data class BlockedSlotItemPayload(
    val blockedSlotId: String = "",
    val id: String = "",
    val date: String = "",
    val slotStart: String = "",
    val slotEnd: String = "",
    val reason: String = "",
    val updatedAtIso: String = "",
    val updatedByUid: String = "",
) {
    fun toAdminBlockedSlotItemOrNull(): AdminBlockedSlotItem? {
        val normalizedId = blockedSlotId.trim().ifBlank { id.trim() }
        val normalizedDate = date.trim()
        val start = slotStart.trim()
        val end = slotEnd.trim()
        if (normalizedId.isBlank() || normalizedDate.isBlank() || start.isBlank() || end.isBlank()) return null
        return AdminBlockedSlotItem(
            blockedSlotId = normalizedId,
            date = normalizedDate,
            slotStartIso = start,
            slotEndIso = end,
            reason = reason.trim().ifBlank { "Bloqueio administrativo" },
            updatedAtIso = updatedAtIso.trim(),
            updatedByUid = updatedByUid.trim(),
        )
    }
}

@Serializable
private data class CallableError(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
) {
    fun toAdminError(): AdminError {
        val normalizedCode = (status ?: code).orEmpty()
            .lowercase()
            .replace("-", "_")
        val fallbackMessage = message ?: "Não foi possível concluir a operação administrativa."
        return when (normalizedCode) {
            "invalid_argument" -> AdminError.Validation(fallbackMessage)
            "permission_denied" -> AdminError.Permission("Não tem permissões administrativas.")
            "unauthenticated" -> AdminError.Unauthenticated("Inicie sessão para gerir a área administrativa.")
            "not_found" -> AdminError.NotFound("O item selecionado já não existe.")
            "failed_precondition", "already_exists", "aborted" -> AdminError.Conflict(fallbackMessage)
            "unavailable" -> AdminError.Unavailable("O serviço administrativo está indisponível.")
            else -> AdminError.Backend(fallbackMessage)
        }
    }
}
