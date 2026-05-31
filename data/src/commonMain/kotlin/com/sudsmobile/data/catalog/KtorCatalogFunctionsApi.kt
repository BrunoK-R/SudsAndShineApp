package com.sudsmobile.data.catalog

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

class KtorCatalogFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : CatalogFunctionsApi {
    override suspend fun getServiceCatalog(): ServiceCatalogResult {
        return try {
            val response = httpClient.post(config.getServiceCatalogUrl) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(CallableServiceCatalogRequest(data = emptyMap()))
            }
            val body = response.body<CallableServiceCatalogResponse>()
            val error = body.error
            when {
                error != null -> ServiceCatalogResult.Failure(error.toCatalogError())
                body.result != null -> ServiceCatalogResult.Success(body.result.toCatalog())
                else -> ServiceCatalogResult.Failure(
                    ServiceCatalogError.Backend("A resposta dos serviços veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            ServiceCatalogResult.Failure(
                ServiceCatalogError.Unavailable(
                    "Não foi possível carregar os serviços. Tente novamente.",
                ),
            )
        }
    }
}

@Serializable
private data class CallableServiceCatalogRequest(
    val data: Map<String, String>,
)

@Serializable
private data class CallableServiceCatalogResponse(
    val result: GetServiceCatalogResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class GetServiceCatalogResult(
    val services: List<GetServiceCatalogService>,
    val extras: List<GetServiceCatalogExtra> = emptyList(),
) {
    fun toCatalog(): ServiceCatalog = ServiceCatalog(
        services = services.map { it.toService() },
        extras = extras.map { it.toExtra() },
    )
}

@Serializable
private data class GetServiceCatalogService(
    val id: String,
    val name: String,
    val description: String = "",
    val durationMinutes: Int,
    val passengerPriceCents: Int,
    val suvPriceCents: Int,
    val iconKey: String = "car",
    val popular: Boolean = false,
) {
    fun toService(): ServiceCatalogService = ServiceCatalogService(
        id = id,
        name = name,
        description = description,
        durationMinutes = durationMinutes.coerceIn(5, 480),
        passengerPriceCents = passengerPriceCents.coerceAtLeast(0),
        suvPriceCents = suvPriceCents.coerceAtLeast(0),
        iconKey = iconKey.ifBlank { "car" },
        popular = popular,
    )
}

@Serializable
private data class GetServiceCatalogExtra(
    val id: String,
    val name: String,
    val description: String = "",
    val priceCents: Int,
    val iconKey: String = "auto_awesome",
    val eligibleServiceIds: List<String> = emptyList(),
) {
    fun toExtra(): ServiceCatalogExtra = ServiceCatalogExtra(
        id = id,
        name = name,
        description = description,
        priceCents = priceCents.coerceAtLeast(0),
        iconKey = iconKey.ifBlank { "auto_awesome" },
        eligibleServiceIds = eligibleServiceIds
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct(),
    )
}

@Serializable
private data class CallableError(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
) {
    fun toCatalogError(): ServiceCatalogError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível carregar os serviços."
        return when (normalizedCode) {
            "PERMISSION_DENIED" -> ServiceCatalogError.Permission("Não tem permissões para consultar serviços.")
            "UNAUTHENTICATED" -> ServiceCatalogError.Unauthenticated("Inicie sessão para consultar serviços.")
            "UNAVAILABLE" -> ServiceCatalogError.Unavailable("O serviço de catálogo está indisponível.")
            else -> ServiceCatalogError.Backend(fallbackMessage)
        }
    }
}
