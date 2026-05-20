package com.sudsmobile.data.booking

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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

        val result = api.createReservation(validRequest())

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

        val result = api.createReservation(validRequest())

        val failure = assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Conflict>(failure.error)
        assertEquals("Este horário deixou de estar disponível.", failure.error.message)
    }
}

private fun mockClient(responseJson: String): HttpClient {
    val engine = MockEngine {
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
