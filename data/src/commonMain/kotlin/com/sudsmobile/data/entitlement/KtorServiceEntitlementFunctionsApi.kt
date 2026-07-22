package com.sudsmobile.data.entitlement

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

class KtorServiceEntitlementFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : ServiceEntitlementFunctionsApi {
    override suspend fun getMyEntitlements(idToken: String): ServiceEntitlementListResult =
        call<EmptyPayload, EntitlementListPayload, ServiceEntitlementListResult>(
            url = config.getMyServiceEntitlementsUrl,
            idToken = idToken,
            payload = EmptyPayload(),
            mapSuccess = { ServiceEntitlementListResult.Success(it.toModel()) },
            mapFailure = { ServiceEntitlementListResult.Failure(it) },
        )

    override suspend fun getAdminEntitlements(
        customerEmail: String,
        idToken: String,
    ): AdminServiceEntitlementListResult = call<AdminLookupPayload, AdminEntitlementListPayload, AdminServiceEntitlementListResult>(
        url = config.getAdminServiceEntitlementsUrl,
        idToken = idToken,
        payload = AdminLookupPayload(customerEmail),
        mapSuccess = { AdminServiceEntitlementListResult.Success(it.toModel()) },
        mapFailure = { AdminServiceEntitlementListResult.Failure(it) },
    )

    override suspend fun issueEntitlement(
        request: IssueServiceEntitlementRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult = call<IssuePayload, MutationPayload, ServiceEntitlementMutationResult>(
        url = config.issueAdminServiceEntitlementUrl,
        idToken = idToken,
        payload = request.toPayload(),
        mapSuccess = { ServiceEntitlementMutationResult.Success(it.entitlement.toModel()) },
        mapFailure = { ServiceEntitlementMutationResult.Failure(it) },
    )

    override suspend fun adjustUsage(
        request: AdjustServiceEntitlementUsageRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult = call<AdjustUsagePayload, MutationPayload, ServiceEntitlementMutationResult>(
        url = config.adjustAdminServiceEntitlementUsageUrl,
        idToken = idToken,
        payload = request.toPayload(),
        mapSuccess = { ServiceEntitlementMutationResult.Success(it.entitlement.toModel()) },
        mapFailure = { ServiceEntitlementMutationResult.Failure(it) },
    )

    override suspend fun revokeEntitlement(
        request: RevokeServiceEntitlementRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult = call<RevokePayload, MutationPayload, ServiceEntitlementMutationResult>(
        url = config.revokeAdminServiceEntitlementUrl,
        idToken = idToken,
        payload = request.toPayload(),
        mapSuccess = { ServiceEntitlementMutationResult.Success(it.entitlement.toModel()) },
        mapFailure = { ServiceEntitlementMutationResult.Failure(it) },
    )

    private suspend inline fun <reified Payload : Any, reified Result : Any, Output> call(
        url: String,
        idToken: String,
        payload: Payload,
        crossinline mapSuccess: (Result) -> Output,
        crossinline mapFailure: (ServiceEntitlementError) -> Output,
    ): Output = try {
        val response = httpClient.post(url) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $idToken")
            setBody(CallableRequest(payload))
        }
        val body = response.body<CallableResponse<Result>>()
        when {
            body.error != null -> mapFailure(body.error.toError())
            body.result != null -> mapSuccess(body.result)
            else -> mapFailure(ServiceEntitlementError.Backend("A resposta dos planos veio sem dados."))
        }
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: Throwable) {
        mapFailure(ServiceEntitlementError.Unavailable("Não foi possível contactar o serviço de planos."))
    }
}

@Serializable
private data class CallableRequest<T>(val data: T)

@Serializable
private data class CallableResponse<T>(val result: T? = null, val error: CallableError? = null)

@Serializable
private data class CallableError(val status: String? = null, val code: String? = null, val message: String? = null)

@Serializable
private class EmptyPayload

@Serializable
private data class AdminLookupPayload(val customerEmail: String)

@Serializable
private data class IssuePayload(
    val operationId: String,
    val customerEmail: String,
    val kind: String,
    val name: String,
    val totalUses: Int,
    val validDays: Int,
    val amountPaidCents: Int,
    val eligibleServiceIds: List<String>,
    val staffNote: String,
)

@Serializable
private data class AdjustUsagePayload(
    val operationId: String,
    val customerEmail: String,
    val entitlementId: String,
    val deltaUses: Int,
    val reservationCode: String,
    val staffNote: String,
)

@Serializable
private data class RevokePayload(
    val operationId: String,
    val customerEmail: String,
    val entitlementId: String,
    val reason: String,
)

@Serializable
private data class EntitlementListPayload(
    val entitlements: List<EntitlementPayload> = emptyList(),
    val purchaseMode: String = "staff_issued",
    val onlinePurchaseAvailable: Boolean = false,
)

@Serializable
private data class AdminEntitlementListPayload(
    val customer: CustomerPayload = CustomerPayload(),
    val entitlements: List<EntitlementPayload> = emptyList(),
    val purchaseMode: String = "staff_issued",
    val onlinePurchaseAvailable: Boolean = false,
)

@Serializable
private data class MutationPayload(val ok: Boolean = false, val entitlement: EntitlementPayload)

@Serializable
private data class CustomerPayload(val uid: String = "", val email: String = "", val displayName: String = "")

@Serializable
private data class EntitlementPayload(
    val id: String = "",
    val code: String = "",
    val kind: String = "package",
    val name: String = "",
    val status: String = "active",
    val totalUses: Int = 0,
    val usedUses: Int = 0,
    val remainingUses: Int = 0,
    val eligibleServiceIds: List<String> = emptyList(),
    val eligibleServiceNames: List<String> = emptyList(),
    val validFrom: String = "",
    val validUntil: String = "",
    val amountPaidCents: Int = 0,
    val purchaseMode: String = "staff_issued",
    val onlinePurchaseAvailable: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val lastUsedAt: String = "",
    val lastReservationCode: String = "",
)

private fun IssueServiceEntitlementRequest.toPayload() = IssuePayload(
    operationId, customerEmail, kind, name, totalUses, validDays, amountPaidCents, eligibleServiceIds, staffNote,
)

private fun AdjustServiceEntitlementUsageRequest.toPayload() = AdjustUsagePayload(
    operationId, customerEmail, entitlementId, deltaUses, reservationCode, staffNote,
)

private fun RevokeServiceEntitlementRequest.toPayload() = RevokePayload(
    operationId, customerEmail, entitlementId, reason,
)

private fun EntitlementListPayload.toModel() = ServiceEntitlementList(
    entitlements = entitlements.map(EntitlementPayload::toModel),
    purchaseMode = purchaseMode.trim(),
    onlinePurchaseAvailable = onlinePurchaseAvailable,
)

private fun AdminEntitlementListPayload.toModel() = AdminServiceEntitlementList(
    customer = AdminEntitlementCustomer(customer.uid.trim(), customer.email.trim(), customer.displayName.trim()),
    entitlements = entitlements.map(EntitlementPayload::toModel),
    purchaseMode = purchaseMode.trim(),
    onlinePurchaseAvailable = onlinePurchaseAvailable,
)

private fun EntitlementPayload.toModel() = ServiceEntitlement(
    id = id.trim(),
    code = code.trim(),
    kind = kind.trim(),
    name = name.trim(),
    status = status.trim(),
    totalUses = totalUses.coerceAtLeast(0),
    usedUses = usedUses.coerceAtLeast(0),
    remainingUses = remainingUses.coerceAtLeast(0),
    eligibleServiceIds = eligibleServiceIds.map(String::trim).filter(String::isNotBlank),
    eligibleServiceNames = eligibleServiceNames.map(String::trim).filter(String::isNotBlank),
    validFromIso = validFrom.trim(),
    validUntilIso = validUntil.trim(),
    amountPaidCents = amountPaidCents.coerceAtLeast(0),
    purchaseMode = purchaseMode.trim(),
    onlinePurchaseAvailable = onlinePurchaseAvailable,
    createdAtIso = createdAt.trim(),
    updatedAtIso = updatedAt.trim(),
    lastUsedAtIso = lastUsedAt.trim(),
    lastReservationCode = lastReservationCode.trim(),
)

private fun CallableError.toError(): ServiceEntitlementError {
    val normalized = (status ?: code.orEmpty()).substringAfterLast('/').lowercase().replace('_', '-')
    return when (normalized) {
        "invalid-argument" -> ServiceEntitlementError.Validation(message.orEmpty().ifBlank { "Dados do plano inválidos." })
        "unauthenticated" -> ServiceEntitlementError.Unauthenticated("Inicie sessão para gerir planos.")
        "permission-denied" -> ServiceEntitlementError.Permission("Esta conta não pode gerir planos.")
        "not-found" -> ServiceEntitlementError.NotFound("Não encontrámos a conta ou o plano indicado.")
        "failed-precondition", "already-exists" -> ServiceEntitlementError.NotEligible(
            message.orEmpty().ifBlank { "O plano não está disponível para esta operação." },
        )
        "unavailable", "deadline-exceeded", "resource-exhausted" -> ServiceEntitlementError.Unavailable(
            "O serviço de planos está temporariamente indisponível.",
        )
        else -> ServiceEntitlementError.Backend(message.orEmpty().ifBlank { "Não foi possível gerir o plano." })
    }
}
