package com.sudsmobile.data.booking

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.fullPath
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class KtorBookingFunctionsApiTest {
    @Test
    fun mapsCallableAvailabilityResponseToMonth() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "monthTitle": "maio 2026",
                    "leadingEmptyCells": 4,
                    "days": [
                      {
                        "id": "2026-05-20",
                        "dayOfMonth": 20,
                        "dateLabel": "20 mai",
                        "summaryLabel": "20 de maio, 2026",
                        "available": true,
                        "waitlistEligible": true,
                        "slots": [
                          {
                            "time": "10:00",
                            "available": true,
                            "remainingCapacity": 1
                          }
                        ]
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.getAvailability(BookingAvailabilityRequest(serviceDurationMinutes = 45))

        val success = assertIs<BookingAvailabilityResult.Success>(result)
        assertEquals("maio 2026", success.month.monthTitle)
        assertEquals(4, success.month.leadingEmptyCells)
        assertEquals("2026-05-20", success.month.days.first().id)
        assertEquals(true, success.month.days.first().waitlistEligible)
        assertEquals("10:00", success.month.days.first().slots.first().time)
    }

    @Test
    fun mapsCallableAvailabilityValidationError() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "INVALID_ARGUMENT",
                    "message": "serviceDurationMinutes must be valid"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.getAvailability(BookingAvailabilityRequest(serviceDurationMinutes = 0))

        val failure = assertIs<BookingAvailabilityResult.Failure>(result)
        assertIs<BookingAvailabilityError.Validation>(failure.error)
        assertEquals("serviceDurationMinutes must be valid", failure.error.message)
    }

    @Test
    fun mapsCallableSuccessResponseToReceipt() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "reservationId": "reservation-1",
                    "reservationCode": "SS-ABCDEFGH",
                    "status": "pending",
                    "pendingExpiresAt": "2026-05-30T10:00:00.000Z",
                    "loyaltyRewardApplied": true,
                    "loyaltyRewardCode": "SS-FREE-UID1-0001",
                    "priceCents": 0,
                    "discountCents": 3200,
                    "paymentStatus": "covered_by_loyalty",
                    "extras": [
                      {
                        "id": "wax",
                        "name": "Enceramento",
                        "priceCents": 1500
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.createReservation(validRequest(), idToken = null)

        val success = assertIs<BookingCreateResult.Success>(result)
        assertEquals("reservation-1", success.receipt.reservationId)
        assertEquals("SS-ABCDEFGH", success.receipt.reservationCode)
        assertEquals("pending", success.receipt.status)
        assertEquals("2026-05-30T10:00:00.000Z", success.receipt.pendingExpiresAtIso)
        assertEquals(true, success.receipt.loyaltyRewardApplied)
        assertEquals("SS-FREE-UID1-0001", success.receipt.loyaltyRewardCode)
        assertEquals(0, success.receipt.priceCents)
        assertEquals(3200, success.receipt.discountCents)
        assertEquals("covered_by_loyalty", success.receipt.paymentStatus)
        assertEquals("wax", success.receipt.extras.single().id)
        assertEquals("Enceramento", success.receipt.extras.single().name)
        assertEquals(1500, success.receipt.extras.single().priceCents)
    }

    @Test
    fun mapsCallableConflictErrorToConflictState() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "ALREADY_EXISTS",
                    "message": "Selected time slot is unavailable"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.createReservation(validRequest(), idToken = null)

        val failure = assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Conflict>(failure.error)
        assertEquals("Este horário deixou de estar disponível.", failure.error.message)
    }

    @Test
    fun mapsCallableRewardPreconditionErrorToValidationState() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "FAILED_PRECONDITION",
                    "message": "Loyalty reward has already been used"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.createReservation(
            validRequest().copy(loyaltyRewardCode = "SS-FREE-UID1-0001"),
            idToken = "id-token-1",
        )

        val failure = assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Validation>(failure.error)
        assertEquals("Esta recompensa não está disponível ou já foi utilizada.", failure.error.message)
    }

    @Test
    fun sendsAuthorizationHeaderWhenCreatingReservationWithSession() = runTest {
        var authorizationHeader: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "reservationId": "reservation-1",
                    "reservationCode": "SS-ABCDEFGH"
                  }
                }
                """.trimIndent(),
            ) { request ->
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.createReservation(validRequest(), idToken = "id-token-1")

        assertIs<BookingCreateResult.Success>(result)
        assertEquals("Bearer id-token-1", authorizationHeader)
    }

    @Test
    fun mapsMyReservationsResponseToHistory() = runTest {
        var requestedPath: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "loyalty": {
                      "totalWashes": 10,
                      "currentWashes": 10,
                      "targetWashes": 10,
                      "remainingWashes": 0,
                      "progress": 1.0,
                      "rewardReady": true,
                      "completedRewards": 1,
                      "claimedRewards": 0,
                      "availableRewards": 1
                    },
                    "reservations": [
                      {
                        "id": "reservation-1",
                        "reservationCode": "SS-ABCDEFGH",
                        "serviceId": "premium",
                        "serviceName": "Lavagem Premium",
                        "slotStart": "2026-05-20T09:30:00.000Z",
                        "slotEnd": "2026-05-20T10:15:00.000Z",
                        "status": "pending",
                        "paymentStatus": "pending",
                        "vehicleType": "suv",
                        "vehicleLabel": "BMW 320d",
                        "priceCents": 3400,
                        "extras": [
                          {
                            "id": "wax",
                            "name": "Enceramento",
                            "priceCents": 1500
                          }
                        ],
                        "upcoming": true,
                        "reviewed": true,
                        "reviewRating": 5,
                        "reviewTags": ["Qualidade", "Rápido"],
                        "reviewComment": "Ficou impecável.",
                        "createdAt": "2026-05-18T08:00:00.000Z",
                        "updatedAt": "2026-05-19T12:15:00.000Z",
                        "acceptedAt": "2026-05-19T13:00:00.000Z",
                        "pendingExpiresAt": "2026-05-19T08:00:00.000Z",
                        "rejectedAt": "2026-05-19T13:30:00.000Z",
                        "rejectionReason": "Agenda cheia.",
                        "rescheduledAt": "2026-05-19T12:15:00.000Z",
                        "previousSlotStart": "2026-05-20T08:30:00.000Z",
                        "previousSlotEnd": "2026-05-20T09:15:00.000Z",
                        "rescheduleCount": 1
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.getMyReservations("id-token-1")

        val success = assertIs<BookingHistoryResult.Success>(result)
        assertEquals("/test-project/europe-west1/getMyReservations", requestedPath)
        assertEquals("reservation-1", success.history.reservations.first().id)
        assertEquals("BMW 320d", success.history.reservations.first().vehicleLabel)
        assertEquals(3400, success.history.reservations.first().priceCents)
        assertEquals("pending", success.history.reservations.first().paymentStatus)
        assertEquals("wax", success.history.reservations.first().extras.single().id)
        assertEquals("Enceramento", success.history.reservations.first().extras.single().name)
        assertEquals(true, success.history.reservations.first().upcoming)
        assertEquals(true, success.history.reservations.first().reviewed)
        assertEquals(5, success.history.reservations.first().reviewRating)
        assertEquals(listOf("Qualidade", "Rápido"), success.history.reservations.first().reviewTags)
        assertEquals("Ficou impecável.", success.history.reservations.first().reviewComment)
        assertEquals("2026-05-18T08:00:00.000Z", success.history.reservations.first().createdAtIso)
        assertEquals("2026-05-19T12:15:00.000Z", success.history.reservations.first().updatedAtIso)
        assertEquals("2026-05-19T13:00:00.000Z", success.history.reservations.first().acceptedAtIso)
        assertEquals("2026-05-19T08:00:00.000Z", success.history.reservations.first().pendingExpiresAtIso)
        assertEquals("2026-05-19T13:30:00.000Z", success.history.reservations.first().rejectedAtIso)
        assertEquals("Agenda cheia.", success.history.reservations.first().rejectionReason)
        assertEquals("2026-05-19T12:15:00.000Z", success.history.reservations.first().rescheduledAtIso)
        assertEquals("2026-05-20T08:30:00.000Z", success.history.reservations.first().previousSlotStartIso)
        assertEquals("2026-05-20T09:15:00.000Z", success.history.reservations.first().previousSlotEndIso)
        assertEquals(1, success.history.reservations.first().rescheduleCount)
        assertEquals(10, success.history.loyalty?.totalWashes)
        assertEquals(1, success.history.loyalty?.availableRewards)
    }

    @Test
    fun mapsMyLoyaltyResponseToRewardsAndStampHistory() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "totalWashes": 10,
                    "currentWashes": 0,
                    "targetWashes": 10,
                    "remainingWashes": 10,
                    "progress": 0.0,
                    "rewardReady": false,
                    "completedRewards": 1,
                    "claimedRewards": 1,
                    "availableRewards": 0,
                    "rewardType": "discount_percent",
                    "rewardValue": 15,
                    "rewardDescription": "15% de desconto",
                    "stampHistory": [
                      {
                        "id": "reservation-1",
                        "serviceId": "premium",
                        "serviceName": "Lavagem Premium",
                        "slotStart": "2026-05-18T10:00:00.000Z",
                        "slotEnd": "2026-05-18T10:45:00.000Z",
                        "points": 1
                      }
                    ],
                    "redemptions": [
                      {
                        "id": "reward-0001",
                        "rewardCode": "SS-FREE-UID1-0001",
                        "rewardNumber": 1,
                        "status": "issued",
                        "createdAt": "2026-05-20T12:00:00.000Z"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.getMyLoyalty("id-token-1")

        val success = assertIs<BookingLoyaltyResult.Success>(result)
        assertEquals("/test-project/europe-west1/getMyLoyalty", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals(10, success.loyalty.summary.totalWashes)
        assertEquals("discount_percent", success.loyalty.summary.rewardType)
        assertEquals(15, success.loyalty.summary.rewardValue)
        assertEquals("15% de desconto", success.loyalty.summary.rewardDescription)
        assertEquals("reservation-1", success.loyalty.stampHistory.single().id)
        assertEquals("Lavagem Premium", success.loyalty.stampHistory.single().serviceName)
        assertEquals("reward-0001", success.loyalty.redemptions.single().id)
        assertEquals("SS-FREE-UID1-0001", success.loyalty.redemptions.single().rewardCode)
        assertEquals(1, success.loyalty.redemptions.single().rewardNumber)
    }

    @Test
    fun submitsReviewWithAuthorizationHeader() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "reviewId": "review-1",
                    "reservationId": "reservation-1"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.submitReservationReview(validReviewRequest(), idToken = "id-token-1")

        val success = assertIs<BookingReviewResult.Success>(result)
        assertEquals("/test-project/europe-west1/submitReservationReview", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("review-1", success.receipt.reviewId)
        assertEquals("reservation-1", success.receipt.reservationId)
    }

    @Test
    fun mapsReviewPreconditionErrorToNotReviewableState() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "FAILED_PRECONDITION",
                    "message": "Reservation is not ready for review"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.submitReservationReview(validReviewRequest(), idToken = "id-token-1")

        val failure = assertIs<BookingReviewResult.Failure>(result)
        assertIs<BookingReviewError.NotReviewable>(failure.error)
        assertEquals("Reservation is not ready for review", failure.error.message)
    }

    @Test
    fun cancelsReservationWithAuthorizationHeader() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "reservationId": "reservation-1",
                    "status": "cancelled"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.cancelMyReservation(validCancelRequest(), idToken = "id-token-1")

        val success = assertIs<BookingCancelResult.Success>(result)
        assertEquals("/test-project/europe-west1/cancelMyReservation", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("reservation-1", success.receipt.reservationId)
        assertEquals("cancelled", success.receipt.status)
    }

    @Test
    fun mapsCancelPreconditionErrorToNotCancelableState() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "FAILED_PRECONDITION",
                    "message": "Reservation can no longer be cancelled"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.cancelMyReservation(validCancelRequest(), idToken = "id-token-1")

        val failure = assertIs<BookingCancelResult.Failure>(result)
        assertIs<BookingCancelError.NotCancelable>(failure.error)
        assertEquals("Reservation can no longer be cancelled", failure.error.message)
    }

    @Test
    fun reschedulesReservationWithAuthorizationHeader() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "reservationId": "reservation-1",
                    "status": "pending",
                    "slotStart": "2026-05-22T11:00:00.000Z",
                    "slotEnd": "2026-05-22T11:45:00.000Z"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.rescheduleMyReservation(validRescheduleRequest(), idToken = "id-token-1")

        val success = assertIs<BookingRescheduleResult.Success>(result)
        assertEquals("/test-project/europe-west1/rescheduleMyReservation", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("reservation-1", success.receipt.reservationId)
        assertEquals("pending", success.receipt.status)
        assertEquals("2026-05-22T11:00:00.000Z", success.receipt.slotStartIso)
        assertEquals("2026-05-22T11:45:00.000Z", success.receipt.slotEndIso)
    }

    @Test
    fun mapsRescheduleConflictError() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "ALREADY_EXISTS",
                    "message": "Selected time slot is unavailable"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.rescheduleMyReservation(validRescheduleRequest(), idToken = "id-token-1")

        val failure = assertIs<BookingRescheduleResult.Failure>(result)
        assertIs<BookingRescheduleError.Conflict>(failure.error)
        assertEquals("Este horário deixou de estar disponível.", failure.error.message)
    }

    @Test
    fun redeemsLoyaltyRewardWithAuthorizationHeader() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "redemption": {
                      "id": "reward-0001",
                      "rewardCode": "SS-FREE-UID1-0001",
                      "rewardNumber": 1,
                      "status": "issued"
                    },
                    "loyalty": {
                      "totalWashes": 10,
                      "currentWashes": 0,
                      "targetWashes": 10,
                      "remainingWashes": 10,
                      "progress": 0.0,
                      "rewardReady": false,
                      "completedRewards": 1,
                      "claimedRewards": 1,
                      "availableRewards": 0
                    }
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.redeemMyLoyaltyReward(idToken = "id-token-1")

        val success = assertIs<BookingRewardRedemptionResult.Success>(result)
        assertEquals("/test-project/europe-west1/redeemMyLoyaltyReward", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("reward-0001", success.receipt.redemptionId)
        assertEquals("SS-FREE-UID1-0001", success.receipt.rewardCode)
        assertEquals(1, success.receipt.loyalty.claimedRewards)
        assertEquals(0, success.receipt.loyalty.availableRewards)
    }

    @Test
    fun mapsRedeemPreconditionErrorToRewardUnavailableState() = runTest {
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "FAILED_PRECONDITION",
                    "message": "No loyalty reward is available"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.redeemMyLoyaltyReward(idToken = "id-token-1")

        val failure = assertIs<BookingRewardRedemptionResult.Failure>(result)
        assertIs<BookingRewardRedemptionError.NotAvailable>(failure.error)
        assertEquals("Ainda não tem uma recompensa disponível.", failure.error.message)
    }

    @Test
    fun mapsAuthenticatedWaitlistEntries() = runTest {
        var requestedPath = ""
        var authorizationHeader: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "entries": [
                      {
                        "id": "waitlist-1",
                        "date": "2026-08-14",
                        "serviceId": "premium",
                        "serviceName": "Lavagem Premium",
                        "serviceDurationMinutes": 45,
                        "status": "active",
                        "createdAt": "2026-07-22T10:00:00.000Z",
                        "updatedAt": "2026-07-22T10:00:00.000Z"
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.getMyWaitlist(idToken = "id-token-1")

        val success = assertIs<BookingWaitlistListResult.Success>(result)
        assertEquals("/test-project/europe-west1/getMyWaitlist", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("waitlist-1", success.entries.single().id)
        assertEquals("2026-08-14", success.entries.single().dateId)
        assertEquals("Lavagem Premium", success.entries.single().serviceName)
    }

    @Test
    fun mapsWaitlistActivationReceipt() = runTest {
        var requestedPath = ""
        var authorizationHeader: String? = null
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "waitlistId": "waitlist-1",
                    "date": "2026-08-14",
                    "serviceId": "premium",
                    "serviceName": "Lavagem Premium",
                    "serviceDurationMinutes": 45,
                    "status": "active"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.joinMyWaitlist(
            request = BookingWaitlistJoinRequest(
                dateId = "2026-08-14",
                serviceId = "premium",
                serviceName = "Lavagem Premium",
                serviceDurationMinutes = 45,
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<BookingWaitlistActionResult.Success>(result)
        assertEquals("/test-project/europe-west1/joinMyWaitlist", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("waitlist-1", success.receipt.waitlistId)
        assertEquals("active", success.receipt.status)
        assertEquals("2026-08-14", success.receipt.entry?.dateId)
    }

    @Test
    fun mapsWaitlistCancellationReceipt() = runTest {
        var requestedPath = ""
        val api = KtorBookingFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "waitlistId": "waitlist-1",
                    "status": "cancelled"
                  }
                }
                """.trimIndent(),
            ) { request -> requestedPath = request.url.fullPath },
            config = testConfig(),
        )

        val result = api.cancelMyWaitlist(waitlistId = "waitlist-1", idToken = "id-token-1")

        val success = assertIs<BookingWaitlistActionResult.Success>(result)
        assertEquals("/test-project/europe-west1/cancelMyWaitlist", requestedPath)
        assertEquals("waitlist-1", success.receipt.waitlistId)
        assertEquals("cancelled", success.receipt.status)
    }
}

private fun mockClient(
    responseJson: String,
    onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {},
): HttpClient {
    val engine = MockEngine {
        onRequest(it)
        respond(
            content = responseJson,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
    return HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }
    }
}

private fun testConfig(): FirebaseFunctionsConfig = FirebaseFunctionsConfig(
    projectId = "test-project",
    region = "europe-west1",
    useEmulator = true,
    emulatorHost = "127.0.0.1",
)

private fun validRequest(): BookingCreateRequest = BookingCreateRequest(
    customerName = "Bruno Ribeiro",
    customerEmail = "bruno@example.com",
    customerPhone = "+351913005855",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = "2026-05-20T09:30:00.000Z",
    slotEndIso = "2026-05-20T10:15:00.000Z",
    vehicleType = "passageiros",
    gdprConsent = true,
    notes = "",
)

private fun validReviewRequest(): BookingReviewRequest = BookingReviewRequest(
    reservationId = "reservation-1",
    rating = 5,
    tags = listOf("Qualidade", "Rápido"),
    comment = "Ficou impecável.",
)

private fun validCancelRequest(): BookingCancelRequest = BookingCancelRequest(
    reservationId = "reservation-1",
)

private fun validRescheduleRequest(): BookingRescheduleRequest = BookingRescheduleRequest(
    reservationId = "reservation-1",
    slotStartIso = "2026-05-22T11:00:00.000Z",
    slotEndIso = "2026-05-22T11:45:00.000Z",
)
