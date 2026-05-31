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
            "unauthenticated" -> AdminError.Unauthenticated("Inicie sessão para gerir marcações.")
            "not_found" -> AdminError.NotFound("A marcação selecionada já não existe.")
            "failed_precondition", "already_exists", "aborted" -> AdminError.Conflict(fallbackMessage)
            "unavailable" -> AdminError.Unavailable("O serviço administrativo está indisponível.")
            else -> AdminError.Backend(fallbackMessage)
        }
    }
}
