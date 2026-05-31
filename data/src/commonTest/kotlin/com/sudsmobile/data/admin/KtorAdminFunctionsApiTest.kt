package com.sudsmobile.data.admin

import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class KtorAdminFunctionsApiTest {
    @Test
    fun mapsSyncRoleResponse() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "uid": "uid-1",
                    "email": "admin@example.com",
                    "role": "admin"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.syncMyRole("id-token-1")

        val success = assertIs<AdminRoleResult.Success>(result)
        assertEquals("/test-project/europe-west1/syncMyRole", requestedPath)
        assertEquals("uid-1", success.role.uid)
        assertTrue(success.role.isAdmin)
    }

    @Test
    fun mapsPendingReservationRequests() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "requests": [
                      {
                        "id": "reservation-1",
                        "reservationCode": "SS-ABCDEFGH",
                        "customerName": "Bruno Ribeiro",
                        "customerEmail": "bruno@example.com",
                        "customerPhone": "913005855",
                        "serviceId": "premium",
                        "serviceName": "Lavagem Premium",
                        "slotStart": "2026-05-30T09:30:00.000Z",
                        "slotEnd": "2026-05-30T10:15:00.000Z",
                        "status": "pending",
                        "paymentStatus": "pending",
                        "vehicleType": "suv",
                        "vehicleLabel": "BMW 320d",
                        "priceCents": 4900,
                        "extras": [
                          {
                            "id": "wax",
                            "name": "Enceramento",
                            "priceCents": 1500
                          }
                        ],
                        "notes": "Portão lateral",
                        "createdAt": "2026-05-29T09:00:00.000Z",
                        "pendingExpiresAt": "2026-05-30T09:00:00.000Z",
                        "loyaltyRewardApplied": true
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

        val result = api.getPendingBookingRequests("id-token-1")

        val success = assertIs<AdminBookingRequestsResult.Success>(result)
        val request = success.requests.single()
        assertEquals("/test-project/europe-west1/getAdminPendingReservations", requestedPath)
        assertEquals("reservation-1", request.id)
        assertEquals("Bruno Ribeiro", request.customerName)
        assertEquals("BMW 320d", request.vehicleLabel)
        assertEquals(4900, request.priceCents)
        assertEquals("wax", request.extras.single().id)
        assertEquals("2026-05-30T09:00:00.000Z", request.pendingExpiresAtIso)
        assertEquals(true, request.loyaltyRewardApplied)
    }

    @Test
    fun mapsAdminBusinessInfoConfiguration() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "phone": "913 005 855",
                    "email": "info@sudsshine.pt",
                    "addressLine1": "Shopping Norte Sul",
                    "addressLine2": "Leiria, Portugal",
                    "mapsUri": "https://maps.example.test",
                    "whatsappUri": "https://wa.me/351913005855",
                    "openingHours": [
                      {
                        "dayLabel": "Segunda a Sexta",
                        "hoursLabel": "09:00 - 19:00",
                        "closed": false
                      }
                    ],
                    "socialLinks": [
                      {
                        "label": "Instagram",
                        "uri": "https://instagram.com/sudsshine"
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

        val result = api.getBusinessInfoConfiguration("id-token-1")

        val success = assertIs<AdminBusinessInfoResult.Success>(result)
        assertEquals("/test-project/europe-west1/getAdminBusinessInfo", requestedPath)
        assertEquals("913 005 855", success.config.phone)
        assertEquals("Segunda a Sexta", success.config.openingHours.single().dayLabel)
        assertEquals("Instagram", success.config.socialLinks.single().label)
    }

    @Test
    fun postsBusinessInfoUpdateWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "phone": "913 005 855",
                    "email": "info@sudsshine.pt",
                    "addressLine1": "Shopping Norte Sul",
                    "addressLine2": "Leiria, Portugal",
                    "mapsUri": "https://maps.example.test",
                    "whatsappUri": "https://wa.me/351913005855",
                    "openingHours": [
                      {
                        "dayLabel": "Segunda a Sexta",
                        "hoursLabel": "09:00 - 19:00",
                        "closed": false
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

        val result = api.updateBusinessInfoConfiguration(
            AdminBusinessInfoUpdateRequest(
                phone = "913 005 855",
                email = "info@sudsshine.pt",
                addressLine1 = "Shopping Norte Sul",
                addressLine2 = "Leiria, Portugal",
                mapsUri = "https://maps.example.test",
                whatsappUri = "https://wa.me/351913005855",
                openingHours = listOf(
                    AdminBusinessOpeningHours(
                        dayLabel = "Segunda a Sexta",
                        hoursLabel = "09:00 - 19:00",
                        closed = false,
                    ),
                ),
                socialLinks = emptyList(),
            ),
            idToken = "id-token-1",
        )

        assertIs<AdminBusinessInfoResult.Success>(result)
        assertEquals("/test-project/europe-west1/updateBusinessInfo", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
    }

    @Test
    fun mapsAdminServiceCatalogConfiguration() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "services": [
                      {
                        "id": "premium",
                        "name": "Lavagem Premium",
                        "description": "Detalhe completo",
                        "durationMinutes": 45,
                        "passengerPriceCents": 3200,
                        "suvPriceCents": 3400,
                        "iconKey": "sparkles",
                        "popular": true,
                        "active": false,
                        "sortOrder": 20
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

        val result = api.getServiceCatalogConfiguration("id-token-1")

        val success = assertIs<AdminServiceCatalogResult.Success>(result)
        val service = success.config.services.single()
        assertEquals("/test-project/europe-west1/getAdminServiceCatalog", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("premium", service.id)
        assertEquals("Lavagem Premium", service.name)
        assertEquals(45, service.durationMinutes)
        assertEquals(3200, service.passengerPriceCents)
        assertEquals(false, service.active)
        assertEquals(20, service.sortOrder)
    }

    @Test
    fun mapsAdminServiceExtrasConfiguration() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "extras": [
                      {
                        "id": "wax",
                        "name": "Enceramento",
                        "description": "Proteção extra",
                        "priceCents": 1500,
                        "iconKey": "shield",
                        "eligibleServiceIds": ["premium", "standard"],
                        "active": false,
                        "sortOrder": 30
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

        val result = api.getServiceExtrasConfiguration("id-token-1")

        val success = assertIs<AdminServiceExtrasResult.Success>(result)
        val extra = success.config.extras.single()
        assertEquals("/test-project/europe-west1/getAdminServiceExtras", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("wax", extra.id)
        assertEquals("Enceramento", extra.name)
        assertEquals(1500, extra.priceCents)
        assertEquals(listOf("premium", "standard"), extra.eligibleServiceIds)
        assertEquals(false, extra.active)
        assertEquals(30, extra.sortOrder)
    }

    @Test
    fun postsRejectDecisionWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "reservationId": "reservation-1",
                    "reservationCode": "SS-ABCDEFGH",
                    "status": "rejected"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.rejectBookingRequest(
            AdminBookingDecisionRequest(
                reservationId = "reservation-1",
                rejectionReason = "Agenda cheia.",
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminBookingDecisionResult.Success>(result)
        assertEquals("/test-project/europe-west1/rejectReservation", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("reservation-1", success.receipt.reservationId)
        assertEquals("rejected", success.receipt.status)
    }

    @Test
    fun mapsPreconditionErrorToConflict() = runTest {
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "FAILED_PRECONDITION",
                    "message": "Reservation request has expired"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.acceptBookingRequest(
            AdminBookingDecisionRequest(reservationId = "reservation-1"),
            idToken = "id-token-1",
        )

        val failure = assertIs<AdminBookingDecisionResult.Failure>(result)
        assertIs<AdminError.Conflict>(failure.error)
        assertEquals("Reservation request has expired", failure.error.message)
    }

    @Test
    fun postsServiceCatalogUpsertWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "serviceId": "premium",
                    "status": "active",
                    "created": false
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.upsertServiceCatalogItem(
            AdminServiceCatalogMutationRequest(
                serviceId = "premium",
                name = "Lavagem Premium",
                description = "Lavagem detalhada",
                durationMinutes = 45,
                passengerPriceCents = 3200,
                suvPriceCents = 3400,
                iconKey = "sparkles",
                popular = true,
                sortOrder = 20,
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminServiceCatalogMutationResult.Success>(result)
        assertEquals("/test-project/europe-west1/upsertServiceCatalogItem", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("premium", success.receipt.serviceId)
        assertEquals("active", success.receipt.status)
        assertEquals(false, success.receipt.created)
    }

    @Test
    fun mapsServiceCatalogArchiveNotFound() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "NOT_FOUND",
                    "message": "Service catalog item not found"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.archiveServiceCatalogItem(
            AdminServiceCatalogArchiveRequest(serviceId = "premium"),
            idToken = "id-token-1",
        )

        val failure = assertIs<AdminServiceCatalogMutationResult.Failure>(result)
        assertEquals("/test-project/europe-west1/archiveServiceCatalogItem", requestedPath)
        assertIs<AdminError.NotFound>(failure.error)
        assertEquals("O item selecionado já não existe.", failure.error.message)
    }

    @Test
    fun postsServiceExtraUpsertWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "extraId": "wax",
                    "status": "active",
                    "created": false
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.upsertServiceExtra(
            AdminServiceExtraMutationRequest(
                extraId = "wax",
                name = "Enceramento",
                description = "Proteção extra",
                priceCents = 1500,
                iconKey = "shield",
                eligibleServiceIds = listOf("premium"),
                active = true,
                sortOrder = 30,
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminServiceExtraMutationResult.Success>(result)
        assertEquals("/test-project/europe-west1/upsertServiceExtra", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("wax", success.receipt.extraId)
        assertEquals("active", success.receipt.status)
        assertEquals(false, success.receipt.created)
    }

    @Test
    fun mapsServiceExtraArchiveNotFound() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "NOT_FOUND",
                    "message": "Service extra not found"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.archiveServiceExtra(
            AdminServiceExtraArchiveRequest(extraId = "wax"),
            idToken = "id-token-1",
        )

        val failure = assertIs<AdminServiceExtraMutationResult.Failure>(result)
        assertEquals("/test-project/europe-west1/archiveServiceExtra", requestedPath)
        assertIs<AdminError.NotFound>(failure.error)
        assertEquals("O item selecionado já não existe.", failure.error.message)
    }
}

private fun mockClient(
    responseJson: String,
    onRequest: (HttpRequestData) -> Unit = {},
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
