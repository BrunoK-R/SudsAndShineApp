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
    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        return try {
            val response = httpClient.post(config.getAvailabilityUrl) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(CallableGetAvailabilityRequest(GetAvailabilityPayload.from(request)))
            }
            val body = response.body<CallableGetAvailabilityResponse>()
            val error = body.error
            when {
                error != null -> BookingAvailabilityResult.Failure(error.toAvailabilityError())
                body.result != null -> BookingAvailabilityResult.Success(body.result.toAvailabilityMonth())
                else -> BookingAvailabilityResult.Failure(
                    BookingAvailabilityError.Backend("A resposta de disponibilidade veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BookingAvailabilityResult.Failure(
                BookingAvailabilityError.Unavailable(
                    "Não foi possível carregar os horários disponíveis. Tente novamente.",
                ),
            )
        }
    }

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
private data class CallableGetAvailabilityRequest(
    val data: GetAvailabilityPayload,
)

@Serializable
private data class CallableCreateReservationRequest(
    val data: CreateReservationPayload,
)

@Serializable
private data class GetAvailabilityPayload(
    val anchorDate: String? = null,
    val serviceDurationMinutes: Int,
    val slotIntervalMinutes: Int,
) {
    companion object {
        fun from(request: BookingAvailabilityRequest): GetAvailabilityPayload = GetAvailabilityPayload(
            anchorDate = request.anchorDate,
            serviceDurationMinutes = request.serviceDurationMinutes,
            slotIntervalMinutes = request.slotIntervalMinutes,
        )
    }
}

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
private data class CallableGetAvailabilityResponse(
    val result: GetAvailabilityResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableCreateReservationResponse(
    val result: CreateReservationResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class GetAvailabilityResult(
    val monthTitle: String,
    val leadingEmptyCells: Int,
    val days: List<GetAvailabilityDay>,
) {
    fun toAvailabilityMonth(): BookingAvailabilityMonth = BookingAvailabilityMonth(
        monthTitle = monthTitle,
        leadingEmptyCells = leadingEmptyCells.coerceIn(0, 6),
        days = days.map { it.toAvailabilityDay() },
    )
}

@Serializable
private data class GetAvailabilityDay(
    val id: String,
    val dayOfMonth: Int,
    val dateLabel: String,
    val summaryLabel: String,
    val available: Boolean,
    val slots: List<GetAvailabilitySlot>,
) {
    fun toAvailabilityDay(): BookingAvailabilityDay = BookingAvailabilityDay(
        id = id,
        dayOfMonth = dayOfMonth,
        dateLabel = dateLabel,
        summaryLabel = summaryLabel,
        available = available,
        slots = slots.map { it.toAvailabilitySlot() },
    )
}

@Serializable
private data class GetAvailabilitySlot(
    val time: String,
    val available: Boolean,
    val remainingCapacity: Int,
) {
    fun toAvailabilitySlot(): BookingAvailabilitySlot = BookingAvailabilitySlot(
        time = time,
        available = available,
        remainingCapacity = remainingCapacity,
    )
}

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
    fun toAvailabilityError(): BookingAvailabilityError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível carregar os horários disponíveis."
        return when (normalizedCode) {
            "INVALID_ARGUMENT" -> BookingAvailabilityError.Validation(fallbackMessage)
            "PERMISSION_DENIED" -> BookingAvailabilityError.Permission("Não tem permissões para consultar horários.")
            "UNAUTHENTICATED" -> BookingAvailabilityError.Unauthenticated("Inicie sessão para consultar horários.")
            "UNAVAILABLE" -> BookingAvailabilityError.Unavailable("O serviço de horários está indisponível.")
            else -> BookingAvailabilityError.Backend(fallbackMessage)
        }
    }

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
