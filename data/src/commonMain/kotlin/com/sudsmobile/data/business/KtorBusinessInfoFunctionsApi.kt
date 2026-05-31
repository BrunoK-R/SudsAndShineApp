package com.sudsmobile.data.business

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

class KtorBusinessInfoFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : BusinessInfoFunctionsApi {
    override suspend fun getBusinessInfo(): BusinessInfoResult {
        return try {
            val response = httpClient.post(config.getBusinessInfoUrl) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(CallableBusinessInfoRequest(data = emptyMap()))
            }
            val body = response.body<CallableBusinessInfoResponse>()
            val error = body.error
            when {
                error != null -> BusinessInfoResult.Failure(error.toBusinessInfoError())
                body.result != null -> BusinessInfoResult.Success(body.result.toBusinessInfo())
                else -> BusinessInfoResult.Failure(
                    BusinessInfoError.Backend("A resposta dos contactos veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BusinessInfoResult.Failure(
                BusinessInfoError.Unavailable(
                    "Não foi possível carregar os contactos. Tente novamente.",
                ),
            )
        }
    }
}

@Serializable
private data class CallableBusinessInfoRequest(
    val data: Map<String, String>,
)

@Serializable
private data class CallableBusinessInfoResponse(
    val result: GetBusinessInfoResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class GetBusinessInfoResult(
    val phone: String = "",
    val phoneUri: String = "",
    val email: String = "",
    val emailUri: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val mapsUri: String = "",
    val whatsappUri: String = "",
    val openingHours: List<GetOpeningHours> = emptyList(),
    val faq: List<GetFaq> = emptyList(),
    val stats: List<GetStat> = emptyList(),
    val socialLinks: List<GetSocialLink> = emptyList(),
) {
    fun toBusinessInfo(): BusinessInfo {
        val fallback = DefaultBusinessInfo
        return BusinessInfo(
            phone = phone.ifBlank { fallback.phone },
            phoneUri = phoneUri.ifBlank { fallback.phoneUri },
            email = email.ifBlank { fallback.email },
            emailUri = emailUri.ifBlank { fallback.emailUri },
            addressLine1 = addressLine1.ifBlank { fallback.addressLine1 },
            addressLine2 = addressLine2.ifBlank { fallback.addressLine2 },
            mapsUri = mapsUri.ifBlank { fallback.mapsUri },
            whatsappUri = whatsappUri.ifBlank { fallback.whatsappUri },
            openingHours = openingHours.mapNotNull { it.toOpeningHoursOrNull() }
                .ifEmpty { fallback.openingHours },
            faq = faq.mapNotNull { it.toFaqOrNull() }
                .ifEmpty { fallback.faq },
            stats = stats.mapNotNull { it.toStatOrNull() }
                .ifEmpty { fallback.stats },
            socialLinks = socialLinks.mapNotNull { it.toSocialLinkOrNull() },
        )
    }
}

@Serializable
private data class GetOpeningHours(
    val dayLabel: String = "",
    val hoursLabel: String = "",
    val closed: Boolean = false,
) {
    fun toOpeningHoursOrNull(): BusinessOpeningHours? {
        if (dayLabel.isBlank() || hoursLabel.isBlank()) return null
        return BusinessOpeningHours(
            dayLabel = dayLabel.trim(),
            hoursLabel = hoursLabel.trim(),
            closed = closed,
        )
    }
}

@Serializable
private data class GetFaq(
    val question: String = "",
    val answer: String = "",
) {
    fun toFaqOrNull(): BusinessFaq? {
        if (question.isBlank() || answer.isBlank()) return null
        return BusinessFaq(
            question = question.trim(),
            answer = answer.trim(),
        )
    }
}

@Serializable
private data class GetStat(
    val value: String = "",
    val label: String = "",
) {
    fun toStatOrNull(): BusinessStat? {
        if (value.isBlank() || label.isBlank()) return null
        return BusinessStat(
            value = value.trim(),
            label = label.trim(),
        )
    }
}

@Serializable
internal data class GetSocialLink(
    val label: String = "",
    val uri: String = "",
) {
    fun toSocialLinkOrNull(): BusinessSocialLink? {
        if (label.isBlank() || uri.isBlank()) return null
        return BusinessSocialLink(
            label = label.trim(),
            uri = uri.trim(),
        )
    }
}

@Serializable
private data class CallableError(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
) {
    fun toBusinessInfoError(): BusinessInfoError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível carregar os contactos."
        return when (normalizedCode) {
            "PERMISSION_DENIED" -> BusinessInfoError.Permission("Não tem permissões para consultar contactos.")
            "UNAUTHENTICATED" -> BusinessInfoError.Unauthenticated("Inicie sessão para consultar contactos.")
            "UNAVAILABLE" -> BusinessInfoError.Unavailable("O serviço de contactos está indisponível.")
            else -> BusinessInfoError.Backend(fallbackMessage)
        }
    }
}
