package com.sudsmobile.data.booking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

class KtorBookingFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : BookingFunctionsApi {
    override suspend fun createReservation(request: BookingCreateRequest): BookingCreateResult {
        return try {
            val response = httpClient.post(config.createReservationUrl) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(CallableCreateReservationRequest(CreateReservationPayload.from(request)))
            }
            val body = response.body<CallableCreateReservationResponse>()
            val error = body.error
            when {
                error != null -> BookingCreateResult.Failure(error.toCreateError())
                body.result != null -> BookingCreateResult.Success(
                    BookingReceipt(
                        reservationId = body.result.reservationId,
                        reservationCode = body.result.reservationCode,
                    ),
                )
                else -> BookingCreateResult.Failure(
                    BookingCreateError.Backend("A resposta da marcação veio sem referência."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BookingCreateResult.Failure(
                BookingCreateError.Unavailable(
                    "Não foi possível contactar o serviço de marcações. Tente novamente.",
                ),
            )
        }
    }
}

@Serializable
private data class CallableCreateReservationRequest(
    val data: CreateReservationPayload,
)

@Serializable
private data class CreateReservationPayload(
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val serviceId: String,
    val serviceName: String,
    val slotStart: String,
    val slotEnd: String,
    val vehicleType: String,
    val gdprConsent: Boolean,
    val notes: String,
) {
    companion object {
        fun from(request: BookingCreateRequest): CreateReservationPayload = CreateReservationPayload(
            customerName = request.customerName,
            customerEmail = request.customerEmail,
            customerPhone = request.customerPhone,
            serviceId = request.serviceId,
            serviceName = request.serviceName,
            slotStart = request.slotStartIso,
            slotEnd = request.slotEndIso,
            vehicleType = request.vehicleType,
            gdprConsent = request.gdprConsent,
            notes = request.notes,
        )
    }
}

@Serializable
private data class CallableCreateReservationResponse(
    val result: CreateReservationResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CreateReservationResult(
    val ok: Boolean = false,
    val reservationId: String,
    val reservationCode: String,
)

@Serializable
private data class CallableError(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
) {
    fun toCreateError(): BookingCreateError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível concluir a marcação."
        return when (normalizedCode) {
            "INVALID_ARGUMENT" -> BookingCreateError.Validation(fallbackMessage)
            "ALREADY_EXISTS" -> BookingCreateError.Conflict("Este horário deixou de estar disponível.")
            "PERMISSION_DENIED" -> BookingCreateError.Permission("Não tem permissões para criar esta marcação.")
            "UNAUTHENTICATED" -> BookingCreateError.Unauthenticated("Inicie sessão para continuar.")
            "UNAVAILABLE" -> BookingCreateError.Unavailable("O serviço de marcações está indisponível.")
            else -> BookingCreateError.Backend(fallbackMessage)
        }
    }
}
