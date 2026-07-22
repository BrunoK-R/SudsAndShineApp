package com.sudsmobile.data.referral

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

class KtorReferralFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : ReferralFunctionsApi {
    override suspend fun getMyReferral(idToken: String): ReferralProgramResult = callReferral<
        CallableReferralRequest<EmptyReferralPayload>,
        ReferralProgramPayload,
        >(
        url = config.getMyReferralUrl,
        idToken = idToken,
        request = CallableReferralRequest(EmptyReferralPayload()),
    ) { it }

    override suspend fun claimMyReferralCode(code: String, idToken: String): ReferralProgramResult = callReferral<
        CallableReferralRequest<ClaimReferralPayload>,
        ClaimReferralResultPayload,
        >(
        url = config.claimMyReferralCodeUrl,
        idToken = idToken,
        request = CallableReferralRequest(ClaimReferralPayload(code)),
    ) { it.referral }

    private suspend inline fun <reified Request : Any, reified Result : Any> callReferral(
        url: String,
        idToken: String,
        request: Request,
        crossinline program: (Result) -> ReferralProgramPayload,
    ): ReferralProgramResult {
        return try {
            val response = httpClient.post(url) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer $idToken")
                setBody(request)
            }
            val body = response.body<CallableReferralResponse<Result>>()
            when {
                body.error != null -> ReferralProgramResult.Failure(body.error.toReferralError())
                body.result != null -> ReferralProgramResult.Success(program(body.result).toReferralProgram())
                else -> ReferralProgramResult.Failure(
                    ReferralError.Backend("A resposta das indicações veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            ReferralProgramResult.Failure(
                ReferralError.Unavailable("Não foi possível carregar as indicações. Tente novamente."),
            )
        }
    }
}

@Serializable
private data class CallableReferralRequest<T>(val data: T)

@Serializable
private class EmptyReferralPayload

@Serializable
private data class ClaimReferralPayload(val code: String)

@Serializable
private data class CallableReferralResponse<T>(
    val result: T? = null,
    val error: CallableReferralError? = null,
)

@Serializable
private data class CallableReferralError(
    val status: String? = null,
    val message: String? = null,
    val code: String? = null,
)

@Serializable
private data class ClaimReferralResultPayload(
    val ok: Boolean = false,
    val referral: ReferralProgramPayload,
)

@Serializable
private data class ReferralProgramPayload(
    val code: String,
    val shareMessage: String,
    val rewardPoints: Int = 1,
    val attributionDays: Int = 30,
    val canClaimCode: Boolean = true,
    val claimIneligibleReason: String = "",
    val referredBy: ReferralAttributionPayload? = null,
    val stats: ReferralStatsPayload = ReferralStatsPayload(),
    val invitations: List<ReferralInvitationPayload> = emptyList(),
)

@Serializable
private data class ReferralAttributionPayload(
    val code: String,
    val status: String,
    val claimedAt: String = "",
    val qualifiedAt: String? = null,
)

@Serializable
private data class ReferralStatsPayload(
    val claimedCount: Int = 0,
    val qualifiedCount: Int = 0,
    val pendingCount: Int = 0,
    val bonusPointsEarned: Int = 0,
)

@Serializable
private data class ReferralInvitationPayload(
    val id: String,
    val status: String,
    val claimedAt: String = "",
    val qualifiedAt: String? = null,
)

private fun ReferralProgramPayload.toReferralProgram(): ReferralProgram = ReferralProgram(
    code = code.trim(),
    shareMessage = shareMessage.trim(),
    rewardPoints = rewardPoints.coerceIn(1, 50),
    attributionDays = attributionDays.coerceIn(1, 365),
    canClaimCode = canClaimCode,
    claimIneligibleReason = claimIneligibleReason.trim().takeIf(String::isNotBlank),
    referredBy = referredBy?.let {
        ReferralAttribution(
            code = it.code.trim(),
            status = it.status.trim(),
            claimedAtIso = it.claimedAt.trim(),
            qualifiedAtIso = it.qualifiedAt?.trim()?.takeIf(String::isNotBlank),
        )
    },
    stats = ReferralStats(
        claimedCount = stats.claimedCount.coerceAtLeast(0),
        qualifiedCount = stats.qualifiedCount.coerceAtLeast(0),
        pendingCount = stats.pendingCount.coerceAtLeast(0),
        bonusPointsEarned = stats.bonusPointsEarned.coerceAtLeast(0),
    ),
    invitations = invitations.map {
        ReferralInvitation(
            id = it.id.trim(),
            status = it.status.trim(),
            claimedAtIso = it.claimedAt.trim(),
            qualifiedAtIso = it.qualifiedAt?.trim()?.takeIf(String::isNotBlank),
        )
    },
)

private fun CallableReferralError.toReferralError(): ReferralError {
    val normalizedCode = (status ?: code.orEmpty())
        .substringAfterLast('/')
        .trim()
        .lowercase()
        .replace('_', '-')
    val fallbackCopy = message?.trim().orEmpty().ifBlank { "Não foi possível concluir a indicação." }
    return when (normalizedCode) {
        "invalid-argument" -> ReferralError.Validation("O código de indicação é inválido.")
        "unauthenticated" -> ReferralError.Unauthenticated("Inicie sessão para gerir indicações.")
        "permission-denied" -> ReferralError.Permission("Esta conta não pode concluir a indicação.")
        "not-found" -> ReferralError.NotFound("Não encontrámos este código de indicação.")
        "already-exists" -> ReferralError.AlreadyClaimed("Esta conta já tem um código de indicação associado.")
        "failed-precondition" -> ReferralError.NotEligible(
            "Este código já não pode ser associado. Use-o nos primeiros 30 dias e antes da primeira lavagem paga.",
        )
        "unavailable", "deadline-exceeded", "resource-exhausted" -> ReferralError.Unavailable(
            "As indicações estão temporariamente indisponíveis. Tente novamente.",
        )
        else -> ReferralError.Backend(fallbackCopy)
    }
}
