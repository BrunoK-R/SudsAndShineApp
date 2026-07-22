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

    override suspend fun createReservation(request: BookingCreateRequest, idToken: String?): BookingCreateResult {
        return try {
            val response = httpClient.post(config.createReservationUrl) {
                callableHeaders(idToken)
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
                        status = body.result.status,
                        pendingExpiresAtIso = body.result.pendingExpiresAt,
                        loyaltyRewardApplied = body.result.loyaltyRewardApplied,
                        loyaltyRewardCode = body.result.loyaltyRewardCode,
                        priceCents = body.result.priceCents,
                        discountCents = body.result.discountCents,
                        extras = body.result.extras.map { it.toReservationExtra() },
                        paymentStatus = body.result.paymentStatus,
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

    override suspend fun getMyReservations(idToken: String): BookingHistoryResult {
        return try {
            val response = httpClient.post(config.getMyReservationsUrl) {
                callableHeaders(idToken)
                setBody(CallableMyReservationsRequest(data = emptyMap()))
            }
            val body = response.body<CallableMyReservationsResponse>()
            val error = body.error
            when {
                error != null -> BookingHistoryResult.Failure(error.toHistoryError())
                body.result != null -> BookingHistoryResult.Success(body.result.toHistory())
                else -> BookingHistoryResult.Failure(
                    BookingHistoryError.Backend("A resposta das marcações veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BookingHistoryResult.Failure(
                BookingHistoryError.Unavailable(
                    "Não foi possível carregar as suas marcações. Tente novamente.",
                ),
            )
        }
    }

    override suspend fun getMyLoyalty(idToken: String): BookingLoyaltyResult {
        return try {
            val response = httpClient.post(config.getMyLoyaltyUrl) {
                callableHeaders(idToken)
                setBody(CallableMyLoyaltyRequest(data = emptyMap()))
            }
            val body = response.body<CallableMyLoyaltyResponse>()
            val error = body.error
            when {
                error != null -> BookingLoyaltyResult.Failure(error.toLoyaltyError())
                body.result != null -> BookingLoyaltyResult.Success(body.result.toLoyalty())
                else -> BookingLoyaltyResult.Failure(
                    BookingLoyaltyError.Backend("A resposta das recompensas veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BookingLoyaltyResult.Failure(
                BookingLoyaltyError.Unavailable(
                    "Não foi possível carregar as suas recompensas. Tente novamente.",
                ),
            )
        }
    }

    override suspend fun submitReservationReview(
        request: BookingReviewRequest,
        idToken: String,
    ): BookingReviewResult {
        return try {
            val response = httpClient.post(config.submitReservationReviewUrl) {
                callableHeaders(idToken)
                setBody(CallableReviewRequest(ReviewPayload.from(request)))
            }
            val body = response.body<CallableReviewResponse>()
            val error = body.error
            when {
                error != null -> BookingReviewResult.Failure(error.toReviewError())
                body.result != null -> BookingReviewResult.Success(
                    BookingReviewReceipt(
                        reviewId = body.result.reviewId,
                        reservationId = body.result.reservationId,
                    ),
                )
                else -> BookingReviewResult.Failure(
                    BookingReviewError.Backend("A resposta da avaliação veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BookingReviewResult.Failure(
                BookingReviewError.Unavailable(
                    "Não foi possível enviar a avaliação. Tente novamente.",
                ),
            )
        }
    }

    override suspend fun cancelMyReservation(
        request: BookingCancelRequest,
        idToken: String,
    ): BookingCancelResult {
        return try {
            val response = httpClient.post(config.cancelMyReservationUrl) {
                callableHeaders(idToken)
                setBody(CallableCancelRequest(CancelPayload.from(request)))
            }
            val body = response.body<CallableCancelResponse>()
            val error = body.error
            when {
                error != null -> BookingCancelResult.Failure(error.toCancelError())
                body.result != null -> BookingCancelResult.Success(
                    BookingCancelReceipt(
                        reservationId = body.result.reservationId,
                        status = body.result.status,
                    ),
                )
                else -> BookingCancelResult.Failure(
                    BookingCancelError.Backend("A resposta do cancelamento veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BookingCancelResult.Failure(
                BookingCancelError.Unavailable(
                    "Não foi possível cancelar a marcação. Tente novamente.",
                ),
            )
        }
    }

    override suspend fun rescheduleMyReservation(
        request: BookingRescheduleRequest,
        idToken: String,
    ): BookingRescheduleResult {
        return try {
            val response = httpClient.post(config.rescheduleMyReservationUrl) {
                callableHeaders(idToken)
                setBody(CallableRescheduleRequest(ReschedulePayload.from(request)))
            }
            val body = response.body<CallableRescheduleResponse>()
            val error = body.error
            when {
                error != null -> BookingRescheduleResult.Failure(error.toRescheduleError())
                body.result != null -> BookingRescheduleResult.Success(
                    BookingRescheduleReceipt(
                        reservationId = body.result.reservationId,
                        status = body.result.status,
                        slotStartIso = body.result.slotStart,
                        slotEndIso = body.result.slotEnd,
                    ),
                )
                else -> BookingRescheduleResult.Failure(
                    BookingRescheduleError.Backend("A resposta da remarcação veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BookingRescheduleResult.Failure(
                BookingRescheduleError.Unavailable(
                    "Não foi possível remarcar. Tente novamente.",
                ),
            )
        }
    }

    override suspend fun redeemMyLoyaltyReward(idToken: String): BookingRewardRedemptionResult {
        return try {
            val response = httpClient.post(config.redeemMyLoyaltyRewardUrl) {
                callableHeaders(idToken)
                setBody(CallableRedeemRewardRequest(data = emptyMap()))
            }
            val body = response.body<CallableRedeemRewardResponse>()
            val error = body.error
            val receipt = body.result?.toReceiptOrNull()
            when {
                error != null -> BookingRewardRedemptionResult.Failure(error.toRewardRedemptionError())
                receipt != null -> BookingRewardRedemptionResult.Success(receipt)
                else -> BookingRewardRedemptionResult.Failure(
                    BookingRewardRedemptionError.Backend("A resposta da recompensa veio sem confirmação."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            BookingRewardRedemptionResult.Failure(
                BookingRewardRedemptionError.Unavailable(
                    "Não foi possível resgatar a recompensa. Tente novamente.",
                ),
            )
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.callableHeaders(idToken: String? = null) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        if (!idToken.isNullOrBlank()) {
            header(HttpHeaders.Authorization, "Bearer $idToken")
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
private data class CallableMyReservationsRequest(
    val data: Map<String, String>,
)

@Serializable
private data class CallableMyLoyaltyRequest(
    val data: Map<String, String>,
)

@Serializable
private data class CallableReviewRequest(
    val data: ReviewPayload,
)

@Serializable
private data class CallableCancelRequest(
    val data: CancelPayload,
)

@Serializable
private data class CallableRescheduleRequest(
    val data: ReschedulePayload,
)

@Serializable
private data class CallableRedeemRewardRequest(
    val data: Map<String, String>,
)

@Serializable
private data class GetAvailabilityPayload(
    val anchorDate: String? = null,
    val serviceDurationMinutes: Int,
    val slotIntervalMinutes: Int? = null,
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
    val userVehicleId: String? = null,
    val vehicleLabel: String? = null,
    val loyaltyRewardCode: String? = null,
    val extraIds: List<String> = emptyList(),
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
            userVehicleId = request.userVehicleId,
            vehicleLabel = request.vehicleLabel,
            loyaltyRewardCode = request.loyaltyRewardCode,
            extraIds = request.extraIds,
        )
    }
}

@Serializable
private data class ReviewPayload(
    val reservationId: String,
    val rating: Int,
    val tags: List<String>,
    val comment: String,
) {
    companion object {
        fun from(request: BookingReviewRequest): ReviewPayload = ReviewPayload(
            reservationId = request.reservationId,
            rating = request.rating,
            tags = request.tags,
            comment = request.comment,
        )
    }
}

@Serializable
private data class CancelPayload(
    val reservationId: String,
) {
    companion object {
        fun from(request: BookingCancelRequest): CancelPayload = CancelPayload(
            reservationId = request.reservationId,
        )
    }
}

@Serializable
private data class ReschedulePayload(
    val reservationId: String,
    val slotStart: String,
    val slotEnd: String,
) {
    companion object {
        fun from(request: BookingRescheduleRequest): ReschedulePayload = ReschedulePayload(
            reservationId = request.reservationId,
            slotStart = request.slotStartIso,
            slotEnd = request.slotEndIso,
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
private data class CallableMyReservationsResponse(
    val result: MyReservationsResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableMyLoyaltyResponse(
    val result: LoyaltyPayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableReviewResponse(
    val result: ReviewResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableCancelResponse(
    val result: CancelResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableRescheduleResponse(
    val result: RescheduleResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableRedeemRewardResponse(
    val result: RedeemRewardResult? = null,
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
    val status: String = "",
    val pendingExpiresAt: String? = null,
    val loyaltyRewardApplied: Boolean = false,
    val loyaltyRewardCode: String? = null,
    val priceCents: Int? = null,
    val discountCents: Int? = null,
    val extras: List<ReservationExtraPayload> = emptyList(),
    val paymentStatus: String = "",
)

@Serializable
private data class ReservationExtraPayload(
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
private data class ReviewResult(
    val ok: Boolean = false,
    val reviewId: String,
    val reservationId: String,
)

@Serializable
private data class CancelResult(
    val ok: Boolean = false,
    val reservationId: String,
    val status: String = "cancelled",
)

@Serializable
private data class RescheduleResult(
    val ok: Boolean = false,
    val reservationId: String,
    val status: String = "pending",
    val slotStart: String,
    val slotEnd: String,
)

@Serializable
private data class RedeemRewardResult(
    val ok: Boolean = false,
    val redemption: LoyaltyRedemptionPayload? = null,
    val loyalty: LoyaltyPayload? = null,
) {
    fun toReceiptOrNull(): BookingRewardRedemptionReceipt? {
        val redemption = redemption ?: return null
        val loyalty = loyalty ?: return null
        val rewardNumber = redemption.rewardNumber ?: return null
        return BookingRewardRedemptionReceipt(
            redemptionId = redemption.id,
            rewardCode = redemption.rewardCode,
            rewardNumber = rewardNumber,
            status = redemption.status,
            loyalty = loyalty.toSummary(),
        )
    }
}

@Serializable
private data class LoyaltyRedemptionPayload(
    val id: String,
    val rewardCode: String,
    val rewardNumber: Int? = null,
    val status: String = "issued",
    val createdAt: String = "",
) {
    fun toRedemption(): BookingLoyaltyRedemption = BookingLoyaltyRedemption(
        id = id,
        rewardCode = rewardCode,
        rewardNumber = rewardNumber?.takeIf { it > 0 },
        status = status.ifBlank { "issued" },
        createdAtIso = createdAt,
    )
}

@Serializable
private data class LoyaltyStampPayload(
    val id: String,
    val serviceId: String = "",
    val serviceName: String = "",
    val slotStart: String = "",
    val slotEnd: String = "",
    val points: Int = 1,
) {
    fun toStamp(): BookingLoyaltyStamp = BookingLoyaltyStamp(
        id = id,
        serviceId = serviceId,
        serviceName = serviceName,
        slotStartIso = slotStart,
        slotEndIso = slotEnd,
        points = points.coerceAtLeast(0),
    )
}

@Serializable
private data class LoyaltyPayload(
    val totalWashes: Int,
    val currentWashes: Int,
    val targetWashes: Int,
    val remainingWashes: Int,
    val progress: Float,
    val rewardReady: Boolean,
    val completedRewards: Int,
    val claimedRewards: Int,
    val availableRewards: Int,
    val rewardType: String = "free_wash",
    val rewardValue: Int = 1,
    val rewardDescription: String = "",
    val stampHistory: List<LoyaltyStampPayload> = emptyList(),
    val redemptions: List<LoyaltyRedemptionPayload> = emptyList(),
) {
    fun toSummary(): BookingLoyaltySummary = BookingLoyaltySummary(
        totalWashes = totalWashes.coerceAtLeast(0),
        currentWashes = currentWashes.coerceAtLeast(0),
        targetWashes = targetWashes.coerceAtLeast(1),
        remainingWashes = remainingWashes.coerceAtLeast(0),
        progress = progress.coerceIn(0f, 1f),
        rewardReady = rewardReady,
        completedRewards = completedRewards.coerceAtLeast(0),
        claimedRewards = claimedRewards.coerceAtLeast(0),
        availableRewards = availableRewards.coerceAtLeast(0),
        rewardType = rewardType.trim().ifBlank { "free_wash" },
        rewardValue = rewardValue.coerceAtLeast(1),
        rewardDescription = rewardDescription.trim().ifBlank { "1 lavagem grátis" },
    )

    fun toLoyalty(): BookingLoyalty = BookingLoyalty(
        summary = toSummary(),
        stampHistory = stampHistory.map { it.toStamp() },
        redemptions = redemptions.map { it.toRedemption() },
    )
}

@Serializable
private data class MyReservationsResult(
    val reservations: List<MyReservationItem>,
    val loyalty: LoyaltyPayload? = null,
) {
    fun toHistory(): BookingHistory = BookingHistory(
        reservations = reservations.map { it.toReservation() },
        loyalty = loyalty?.toSummary(),
    )
}

@Serializable
private data class MyReservationItem(
    val id: String,
    val reservationCode: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val slotStart: String,
    val slotEnd: String,
    val status: String = "pending",
    val paymentStatus: String = "",
    val vehicleType: String = "passageiros",
    val vehicleLabel: String? = null,
    val priceCents: Int? = null,
    val upcoming: Boolean = true,
    val reviewed: Boolean = false,
    val reviewRating: Int? = null,
    val reviewTags: List<String> = emptyList(),
    val reviewComment: String = "",
    val extras: List<ReservationExtraPayload> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val cancelledAt: String? = null,
    val rejectedAt: String? = null,
    val rejectionReason: String = "",
    val acceptedAt: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val paymentConfirmedAt: String? = null,
    val pendingExpiresAt: String? = null,
    val rescheduledAt: String? = null,
    val previousSlotStart: String? = null,
    val previousSlotEnd: String? = null,
    val rescheduleCount: Int = 0,
    val loyaltyRewardApplied: Boolean = false,
    val loyaltyRewardCode: String = "",
    val loyaltyRewardDescription: String = "",
    val loyaltyStampGranted: Boolean? = null,
) {
    fun toReservation(): BookingHistoryReservation = BookingHistoryReservation(
        id = id,
        reservationCode = reservationCode,
        serviceId = serviceId,
        serviceName = serviceName,
        slotStartIso = slotStart,
        slotEndIso = slotEnd,
        status = status,
        paymentStatus = paymentStatus,
        vehicleType = vehicleType,
        vehicleLabel = vehicleLabel,
        priceCents = priceCents,
        upcoming = upcoming,
        reviewed = reviewed,
        reviewRating = reviewRating,
        reviewTags = reviewTags,
        reviewComment = reviewComment,
        extras = extras.map { it.toReservationExtra() },
        createdAtIso = createdAt,
        updatedAtIso = updatedAt,
        cancelledAtIso = cancelledAt,
        rejectedAtIso = rejectedAt,
        rejectionReason = rejectionReason,
        acceptedAtIso = acceptedAt,
        startedAtIso = startedAt,
        completedAtIso = completedAt,
        paymentConfirmedAtIso = paymentConfirmedAt,
        pendingExpiresAtIso = pendingExpiresAt,
        rescheduledAtIso = rescheduledAt,
        previousSlotStartIso = previousSlotStart,
        previousSlotEndIso = previousSlotEnd,
        rescheduleCount = rescheduleCount.coerceAtLeast(0),
        loyaltyRewardApplied = loyaltyRewardApplied,
        loyaltyRewardCode = loyaltyRewardCode,
        loyaltyRewardDescription = loyaltyRewardDescription,
        loyaltyStampGranted = loyaltyStampGranted,
    )
}

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
            "FAILED_PRECONDITION", "NOT_FOUND" ->
                BookingCreateError.Validation("Esta recompensa não está disponível ou já foi utilizada.")
            "PERMISSION_DENIED" -> BookingCreateError.Permission("Não tem permissões para criar esta marcação.")
            "UNAUTHENTICATED" -> BookingCreateError.Unauthenticated("Inicie sessão para continuar.")
            "UNAVAILABLE" -> BookingCreateError.Unavailable("O serviço de marcações está indisponível.")
            else -> BookingCreateError.Backend(fallbackMessage)
        }
    }

    fun toHistoryError(): BookingHistoryError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível carregar as suas marcações."
        return when (normalizedCode) {
            "PERMISSION_DENIED" -> BookingHistoryError.Permission("Não tem permissões para consultar estas marcações.")
            "UNAUTHENTICATED" -> BookingHistoryError.Unauthenticated("Inicie sessão para ver as suas marcações.")
            "UNAVAILABLE" -> BookingHistoryError.Unavailable("O serviço de marcações está indisponível.")
            else -> BookingHistoryError.Backend(fallbackMessage)
        }
    }

    fun toLoyaltyError(): BookingLoyaltyError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível carregar as suas recompensas."
        return when (normalizedCode) {
            "PERMISSION_DENIED" -> BookingLoyaltyError.Permission("Não tem permissões para consultar recompensas.")
            "UNAUTHENTICATED" -> BookingLoyaltyError.Unauthenticated("Inicie sessão para ver as suas recompensas.")
            "UNAVAILABLE" -> BookingLoyaltyError.Unavailable("O serviço de recompensas está indisponível.")
            else -> BookingLoyaltyError.Backend(fallbackMessage)
        }
    }

    fun toReviewError(): BookingReviewError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível enviar a avaliação."
        return when (normalizedCode) {
            "INVALID_ARGUMENT" -> BookingReviewError.Validation(fallbackMessage)
            "PERMISSION_DENIED" -> BookingReviewError.Permission("Esta marcação não pertence à sessão atual.")
            "UNAUTHENTICATED" -> BookingReviewError.Unauthenticated("Inicie sessão para avaliar esta marcação.")
            "NOT_FOUND" -> BookingReviewError.NotFound("A marcação selecionada já não existe.")
            "FAILED_PRECONDITION" -> BookingReviewError.NotReviewable(fallbackMessage)
            "UNAVAILABLE" -> BookingReviewError.Unavailable("O serviço de avaliações está indisponível.")
            else -> BookingReviewError.Backend(fallbackMessage)
        }
    }

    fun toCancelError(): BookingCancelError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível cancelar a marcação."
        return when (normalizedCode) {
            "INVALID_ARGUMENT" -> BookingCancelError.Validation(fallbackMessage)
            "PERMISSION_DENIED" -> BookingCancelError.Permission("Esta marcação não pertence à sessão atual.")
            "UNAUTHENTICATED" -> BookingCancelError.Unauthenticated("Inicie sessão para cancelar esta marcação.")
            "NOT_FOUND" -> BookingCancelError.NotFound("A marcação selecionada já não existe.")
            "FAILED_PRECONDITION" -> BookingCancelError.NotCancelable(fallbackMessage)
            "UNAVAILABLE" -> BookingCancelError.Unavailable("O serviço de cancelamentos está indisponível.")
            else -> BookingCancelError.Backend(fallbackMessage)
        }
    }

    fun toRescheduleError(): BookingRescheduleError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível remarcar."
        return when (normalizedCode) {
            "INVALID_ARGUMENT" -> BookingRescheduleError.Validation(fallbackMessage)
            "ALREADY_EXISTS" -> BookingRescheduleError.Conflict("Este horário deixou de estar disponível.")
            "PERMISSION_DENIED" -> BookingRescheduleError.Permission("Esta marcação não pertence à sessão atual.")
            "UNAUTHENTICATED" -> BookingRescheduleError.Unauthenticated("Inicie sessão para remarcar esta marcação.")
            "NOT_FOUND" -> BookingRescheduleError.NotFound("A marcação selecionada já não existe.")
            "FAILED_PRECONDITION" -> BookingRescheduleError.NotReschedulable(fallbackMessage)
            "UNAVAILABLE" -> BookingRescheduleError.Unavailable("O serviço de remarcações está indisponível.")
            else -> BookingRescheduleError.Backend(fallbackMessage)
        }
    }

    fun toRewardRedemptionError(): BookingRewardRedemptionError {
        val normalizedCode = status ?: code
        val fallbackMessage = message ?: "Não foi possível resgatar a recompensa."
        return when (normalizedCode) {
            "PERMISSION_DENIED" ->
                BookingRewardRedemptionError.Permission("Esta recompensa não pertence à sessão atual.")
            "UNAUTHENTICATED" ->
                BookingRewardRedemptionError.Unauthenticated("Inicie sessão para resgatar recompensas.")
            "FAILED_PRECONDITION" ->
                BookingRewardRedemptionError.NotAvailable("Ainda não tem uma recompensa disponível.")
            "ALREADY_EXISTS" ->
                BookingRewardRedemptionError.AlreadyClaimed("Esta recompensa já foi resgatada.")
            "UNAVAILABLE" ->
                BookingRewardRedemptionError.Unavailable("O serviço de recompensas está indisponível.")
            else -> BookingRewardRedemptionError.Backend(fallbackMessage)
        }
    }
}
