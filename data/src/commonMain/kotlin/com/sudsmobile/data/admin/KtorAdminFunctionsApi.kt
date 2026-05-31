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
private data class CallableCapacityOverrideMutationRequest(
    val data: CapacityOverridePayload,
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
    val openingHours: List<BusinessOpeningHoursPayload>,
) {
    companion object {
        fun from(request: AdminAvailabilityUpdateRequest): AvailabilityPayload = AvailabilityPayload(
            defaultMaxBookingsPerSlot = request.defaultMaxBookingsPerSlot,
            openingHours = request.openingHours.map { BusinessOpeningHoursPayload.from(it) },
        )
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
private data class CallableCapacityOverrideMutationResponse(
    val result: CapacityOverrideMutationResultPayload? = null,
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
        uid = uid,
        email = email,
        role = role,
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
    )
}

@Serializable
private data class AvailabilityResultPayload(
    val defaultMaxBookingsPerSlot: Int = 2,
    val openingHours: List<BusinessOpeningHoursPayload> = emptyList(),
    val capacityOverrides: List<CapacityOverrideItemPayload> = emptyList(),
) {
    fun toAdminAvailabilityConfig(): AdminAvailabilityConfig = AdminAvailabilityConfig(
        defaultMaxBookingsPerSlot = defaultMaxBookingsPerSlot.coerceIn(0, 20),
        openingHours = openingHours.map { it.toAdminOpeningHours() },
        capacityOverrides = capacityOverrides.map { it.toAdminCapacityOverrideItem() },
    )
}

@Serializable
private data class CapacityOverrideItemPayload(
    val date: String,
    val maxBookingsPerSlot: Int = 0,
) {
    fun toAdminCapacityOverrideItem(): AdminCapacityOverrideItem = AdminCapacityOverrideItem(
        date = date.trim(),
        maxBookingsPerSlot = maxBookingsPerSlot.coerceIn(0, 20),
    )
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
