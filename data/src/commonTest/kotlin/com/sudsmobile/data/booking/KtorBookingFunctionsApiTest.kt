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
                    "reservationCode": "SS-ABCDEFGH"
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
                    "reservations": [
                      {
                        "id": "reservation-1",
                        "reservationCode": "SS-ABCDEFGH",
                        "serviceId": "premium",
                        "serviceName": "Lavagem Premium",
                        "slotStart": "2026-05-20T09:30:00.000Z",
                        "slotEnd": "2026-05-20T10:15:00.000Z",
                        "status": "pending",
                        "vehicleType": "suv",
                        "vehicleLabel": "BMW 320d",
                        "priceCents": 3400,
                        "upcoming": true,
                        "reviewed": true,
                        "reviewRating": 5,
                        "reviewTags": ["Qualidade", "Rápido"]
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
        assertEquals(true, success.history.reservations.first().upcoming)
        assertEquals(true, success.history.reservations.first().reviewed)
        assertEquals(5, success.history.reservations.first().reviewRating)
        assertEquals(listOf("Qualidade", "Rápido"), success.history.reservations.first().reviewTags)
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
