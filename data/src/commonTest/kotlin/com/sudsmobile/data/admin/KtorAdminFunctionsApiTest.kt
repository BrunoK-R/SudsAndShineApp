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
                    "uid": " uid-1 ",
                    "email": " admin@example.com ",
                    "role": " admin "
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
        assertEquals("admin@example.com", success.role.email)
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
    fun mapsAcceptedReservationRequests() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "requests": [
                      {
                        "id": "reservation-2",
                        "reservationCode": "SS-DONE",
                        "customerName": "Ana Silva",
                        "serviceId": "premium",
                        "serviceName": "Lavagem Premium",
                        "slotStart": "2026-05-30T09:30:00.000Z",
                        "slotEnd": "2026-05-30T10:15:00.000Z",
                        "status": "confirmed",
                        "paymentStatus": "paid",
                        "vehicleType": "passageiros",
                        "priceCents": 3200,
                        "createdAt": "2026-05-29T09:00:00.000Z",
                        "canComplete": true,
                        "acceptedAt": "2026-05-29T10:15:00.000Z",
                        "acceptedByUid": " admin-uid "
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

        val result = api.getAcceptedBookingRequests("id-token-1")

        val success = assertIs<AdminBookingRequestsResult.Success>(result)
        val request = success.requests.single()
        assertEquals("/test-project/europe-west1/getAdminAcceptedReservations", requestedPath)
        assertEquals("reservation-2", request.id)
        assertEquals("confirmed", request.status)
        assertEquals("paid", request.paymentStatus)
        assertEquals(true, request.canComplete)
        assertEquals("2026-05-29T10:15:00.000Z", request.acceptedAtIso)
        assertEquals("admin-uid", request.acceptedByUid)
    }

    @Test
    fun postsCompleteReservationDecision() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "reservationId": "reservation-2",
                    "reservationCode": "SS-DONE",
                    "status": "completed"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.completeBookingRequest(
            AdminBookingDecisionRequest(reservationId = "reservation-2"),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminBookingDecisionResult.Success>(result)
        assertEquals("/test-project/europe-west1/completeReservation", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("completed", success.receipt.status)
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
                    "source": "firestore",
                    "updatedAtIso": "2026-06-01T10:15:00.000Z",
                    "updatedByUid": " admin-business ",
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
        assertEquals("firestore", success.config.source)
        assertEquals("2026-06-01T10:15:00.000Z", success.config.updatedAtIso)
        assertEquals("admin-business", success.config.updatedByUid)
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
    fun mapsAdminAvailabilityConfiguration() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "defaultMaxBookingsPerSlot": 3,
                    "defaultSlotIntervalMinutes": 20,
                    "openingHours": [
                      {
                        "dayLabel": "Segunda a Sexta",
                        "hoursLabel": "09:00 - 13:00, 14:00 - 19:00",
                        "closed": false
                      }
                    ],
                    "capacityOverrides": [
                      {
                        "date": "2026-06-10",
                        "maxBookingsPerSlot": 0,
                        "updatedAtIso": "2026-06-01T10:15:00.000Z",
                        "updatedByUid": "admin-capacity"
                      }
                    ],
                    "blockedSlots": [
                      {
                        "blockedSlotId": "block-1",
                        "date": "2026-06-10",
                        "slotStart": "2026-06-10T09:00:00.000Z",
                        "slotEnd": "2026-06-10T10:00:00.000Z",
                        "reason": "Manutenção",
                        "updatedAtIso": "2026-06-01T11:45:00.000Z",
                        "updatedByUid": "admin-blocked"
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

        val result = api.getAvailabilityConfiguration("id-token-1")

        val success = assertIs<AdminAvailabilityResult.Success>(result)
        assertEquals("/test-project/europe-west1/getAdminAvailabilityConfiguration", requestedPath)
        assertEquals(3, success.config.defaultMaxBookingsPerSlot)
        assertEquals(20, success.config.defaultSlotIntervalMinutes)
        assertEquals("Segunda a Sexta", success.config.openingHours.single().dayLabel)
        assertEquals("2026-06-10", success.config.capacityOverrides.single().date)
        assertEquals(0, success.config.capacityOverrides.single().maxBookingsPerSlot)
        assertEquals("2026-06-01T10:15:00.000Z", success.config.capacityOverrides.single().updatedAtIso)
        assertEquals("admin-capacity", success.config.capacityOverrides.single().updatedByUid)
        assertEquals("block-1", success.config.blockedSlots.single().blockedSlotId)
        assertEquals("2026-06-10T09:00:00.000Z", success.config.blockedSlots.single().slotStartIso)
        assertEquals("2026-06-01T11:45:00.000Z", success.config.blockedSlots.single().updatedAtIso)
        assertEquals("admin-blocked", success.config.blockedSlots.single().updatedByUid)
    }

    @Test
    fun mapsAdminBookingPolicyConfiguration() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "pendingHoldMinutes": 240,
                    "cancellationWindowMinutes": 120,
                    "rescheduleWindowMinutes": 60,
                    "paymentEligibilityCopy": "Pagamento no local",
                    "source": " firestore ",
                    "updatedAtIso": " 2026-06-01T10:15:00.000Z ",
                    "updatedByUid": " admin-policy "
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.getBookingPolicyConfiguration("id-token-1")

        val success = assertIs<AdminBookingPolicyResult.Success>(result)
        assertEquals("/test-project/europe-west1/getAdminBookingPolicy", requestedPath)
        assertEquals(240, success.config.pendingHoldMinutes)
        assertEquals(120, success.config.cancellationWindowMinutes)
        assertEquals("Pagamento no local", success.config.paymentEligibilityCopy)
        assertEquals("firestore", success.config.source)
        assertEquals("2026-06-01T10:15:00.000Z", success.config.updatedAtIso)
        assertEquals("admin-policy", success.config.updatedByUid)
    }

    @Test
    fun postsBookingPolicyUpdateWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "pendingHoldMinutes": 240,
                    "cancellationWindowMinutes": 120,
                    "rescheduleWindowMinutes": 60,
                    "paymentEligibilityCopy": "Pagamento no local",
                    "updatedAtIso": "2026-06-01T11:30:00.000Z",
                    "updatedByUid": "admin-save"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.updateBookingPolicyConfiguration(
            AdminBookingPolicyUpdateRequest(
                pendingHoldMinutes = 240,
                cancellationWindowMinutes = 120,
                rescheduleWindowMinutes = 60,
                paymentEligibilityCopy = "Pagamento no local",
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminBookingPolicyResult.Success>(result)
        assertEquals("/test-project/europe-west1/updateBookingPolicy", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals(60, success.config.rescheduleWindowMinutes)
        assertEquals("2026-06-01T11:30:00.000Z", success.config.updatedAtIso)
        assertEquals("admin-save", success.config.updatedByUid)
    }

    @Test
    fun mapsAdminLoyaltySettingsConfiguration() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "stampsRequired": 8,
                    "rewardType": "discount_percent",
                    "rewardValue": 15,
                    "rewardDescription": "15% de desconto",
                    "source": "firestore",
                    "updatedAtIso": "2026-06-01T10:15:00.000Z",
                    "updatedByUid": " admin-loyalty "
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.getLoyaltySettingsConfiguration("id-token-1")

        val success = assertIs<AdminLoyaltySettingsResult.Success>(result)
        assertEquals("/test-project/europe-west1/getAdminLoyaltySettings", requestedPath)
        assertEquals(8, success.config.stampsRequired)
        assertEquals("discount_percent", success.config.rewardType)
        assertEquals(15, success.config.rewardValue)
        assertEquals("15% de desconto", success.config.rewardDescription)
        assertEquals("firestore", success.config.source)
        assertEquals("2026-06-01T10:15:00.000Z", success.config.updatedAtIso)
        assertEquals("admin-loyalty", success.config.updatedByUid)
    }

    @Test
    fun postsLoyaltySettingsUpdateWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "stampsRequired": 8,
                    "rewardType": "free_wash",
                    "rewardValue": 1,
                    "rewardDescription": "1 lavagem grátis",
                    "source": "firestore",
                    "updatedAtIso": "2026-06-01T10:16:00.000Z",
                    "updatedByUid": "admin-save"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.updateLoyaltySettingsConfiguration(
            AdminLoyaltySettingsUpdateRequest(
                stampsRequired = 8,
                rewardType = "free_wash",
                rewardValue = 1,
                rewardDescription = "1 lavagem grátis",
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminLoyaltySettingsResult.Success>(result)
        assertEquals("/test-project/europe-west1/updateLoyaltySettings", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals(8, success.config.stampsRequired)
        assertEquals("2026-06-01T10:16:00.000Z", success.config.updatedAtIso)
        assertEquals("admin-save", success.config.updatedByUid)
    }

    @Test
    fun mapsAdminNotificationSettingsConfiguration() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "bookingStatusEnabled": true,
                    "appointmentReminderEnabled": true,
                    "loyaltyEnabled": true,
                    "adminPendingAlertEnabled": true,
                    "marketingEnabled": false,
                    "reminderLeadMinutes": 60,
                    "quietHoursStart": "23:00",
                    "quietHoursEnd": "06:00",
                    "quietHoursTimeZone": "Atlantic/Azores",
                    "source": "firestore",
                    "updatedAtIso": "2026-06-01T10:15:00.000Z",
                    "updatedByUid": "admin-updated",
                    "templates": [
                      {
                        "key": "booking_rejected",
                        "label": "Marcação rejeitada",
                        "enabled": false,
                        "title": "Rejeitada",
                        "body": "Escolha outro horário"
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

        val result = api.getNotificationSettingsConfiguration("id-token-1")

        val success = assertIs<AdminNotificationSettingsResult.Success>(result)
        assertEquals("/test-project/europe-west1/getAdminNotificationSettings", requestedPath)
        assertEquals(60, success.config.reminderLeadMinutes)
        assertEquals(false, success.config.templates.single().enabled)
        assertEquals("23:00", success.config.quietHoursStart)
        assertEquals("Atlantic/Azores", success.config.quietHoursTimeZone)
        assertEquals("firestore", success.config.source)
        assertEquals("2026-06-01T10:15:00.000Z", success.config.updatedAtIso)
        assertEquals("admin-updated", success.config.updatedByUid)
    }

    @Test
    fun postsNotificationSettingsUpdateWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "bookingStatusEnabled": true,
                    "appointmentReminderEnabled": true,
                    "loyaltyEnabled": true,
                    "adminPendingAlertEnabled": false,
                    "marketingEnabled": false,
                    "reminderLeadMinutes": 60,
                    "quietHoursStart": "23:00",
                    "quietHoursEnd": "06:00",
                    "quietHoursTimeZone": "Europe/Madrid",
                    "source": "firestore",
                    "updatedAtIso": "2026-06-01T10:16:00.000Z",
                    "updatedByUid": "admin-updated",
                    "templates": [
                      {
                        "key": "booking_request",
                        "label": "Pedido recebido",
                        "enabled": true,
                        "title": "Pedido",
                        "body": "Recebido"
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

        val result = api.updateNotificationSettingsConfiguration(
            AdminNotificationSettingsUpdateRequest(
                bookingStatusEnabled = true,
                appointmentReminderEnabled = true,
                loyaltyEnabled = true,
                adminPendingAlertEnabled = false,
                marketingEnabled = false,
                reminderLeadMinutes = 60,
                quietHoursStart = "23:00",
                quietHoursEnd = "06:00",
                quietHoursTimeZone = "Europe/Madrid",
                templates = listOf(
                    AdminNotificationTemplateConfig(
                        key = "booking_request",
                        label = "Pedido recebido",
                        enabled = true,
                        title = "Pedido",
                        body = "Recebido",
                    ),
                ),
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminNotificationSettingsResult.Success>(result)
        assertEquals("/test-project/europe-west1/updateNotificationSettings", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals(false, success.config.adminPendingAlertEnabled)
        assertEquals("2026-06-01T10:16:00.000Z", success.config.updatedAtIso)
        assertEquals("admin-updated", success.config.updatedByUid)
        assertEquals("Europe/Madrid", success.config.quietHoursTimeZone)
    }

    @Test
    fun postsNotificationTestSendWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "notificationId": "test-notification-1",
                    "templateKey": "booking_request",
                    "deliveryState": "queued",
                    "recipientUid": "admin-1",
                    "targetScope": "self",
                    "testOnly": true,
                    "message": "queued"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.sendNotificationTestToSelf(
            AdminNotificationTestRequest(templateKey = "booking_request"),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminNotificationTestResult.Success>(result)
        assertEquals("/test-project/europe-west1/sendAdminNotificationTest", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("test-notification-1", success.receipt.notificationId)
        assertEquals("queued", success.receipt.deliveryState)
        assertEquals("self", success.receipt.targetScope)
        assertEquals(true, success.receipt.testOnly)
    }

    @Test
    fun mapsNotificationCampaignTestReceiptSafetyMetadata() = runTest {
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "notificationId": "test-notification-1",
                    "templateKey": "campaign_draft",
                    "campaignId": "summer-test",
                    "deliveryState": "queued",
                    "recipientUid": "admin-1",
                    "targetScope": "self",
                    "testOnly": true,
                    "targetAudience": "marketing_opt_in_users",
                    "marketingConsentRequired": true,
                    "sendBlocked": true,
                    "sendBlockedReason": "campaign-send-not-implemented",
                    "deliveryLocked": true,
                    "sendState": "draft_only",
                    "message": "queued"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.sendNotificationTestToSelf(
            AdminNotificationTestRequest(campaignId = "summer-test"),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminNotificationTestResult.Success>(result)
        assertEquals("summer-test", success.receipt.campaignId)
        assertEquals("marketing_opt_in_users", success.receipt.targetAudience)
        assertEquals(true, success.receipt.marketingConsentRequired)
        assertEquals(true, success.receipt.sendBlocked)
        assertEquals("campaign-send-not-implemented", success.receipt.sendBlockedReason)
        assertEquals(true, success.receipt.deliveryLocked)
        assertEquals("draft_only", success.receipt.sendState)
    }

    @Test
    fun mapsNotificationCampaignTestReceiptAsBlockedWhenCallableOmitsSendMetadata() = runTest {
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "notificationId": "test-notification-1",
                    "templateKey": "campaign_draft",
                    "campaignId": "summer-test",
                    "deliveryState": "queued",
                    "recipientUid": "admin-1",
                    "targetScope": "self",
                    "testOnly": true,
                    "sendBlocked": false,
                    "sendBlockedReason": "",
                    "deliveryLocked": false,
                    "sendState": "",
                    "message": "queued"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.sendNotificationTestToSelf(
            AdminNotificationTestRequest(campaignId = "summer-test"),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminNotificationTestResult.Success>(result)
        assertEquals(true, success.receipt.sendBlocked)
        assertEquals("campaign-send-not-implemented", success.receipt.sendBlockedReason)
        assertEquals(true, success.receipt.deliveryLocked)
        assertEquals("draft_only", success.receipt.sendState)
    }

    @Test
    fun mapsAdminNotificationCampaignDrafts() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "source": "firestore",
                    "campaigns": [
                      {
                        "campaignId": "summer-test",
                        "title": "Oferta verão",
                        "body": "Campanha apenas em rascunho",
                        "targetAudience": "test_users",
                        "channels": ["push"],
                        "marketingConsentRequired": false,
                        "status": "draft",
                        "scheduledAtIso": "2026-06-10T10:00:00.000Z",
                        "notes": "QA",
                        "sendBlocked": true,
                        "sendBlockedReason": "campaign-send-not-implemented",
                        "deliveryLocked": true,
                        "sendState": "draft_only",
                        "createdAtIso": "2026-06-01T10:00:00.000Z",
                        "updatedAtIso": "2026-06-01T11:00:00.000Z",
                        "createdByUid": "admin-created",
                        "updatedByUid": "admin-updated"
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

        val result = api.getNotificationCampaignDrafts("id-token-1")

        val success = assertIs<AdminNotificationCampaignDraftsResult.Success>(result)
        val draft = success.config.campaigns.single()
        assertEquals("/test-project/europe-west1/getAdminNotificationCampaignDrafts", requestedPath)
        assertEquals("firestore", success.config.source)
        assertEquals("summer-test", draft.campaignId)
        assertEquals("push", draft.channels.single())
        assertEquals(true, draft.sendBlocked)
        assertEquals("campaign-send-not-implemented", draft.sendBlockedReason)
        assertEquals(true, draft.deliveryLocked)
        assertEquals("draft_only", draft.sendState)
        assertEquals("2026-06-01T10:00:00.000Z", draft.createdAtIso)
        assertEquals("2026-06-01T11:00:00.000Z", draft.updatedAtIso)
        assertEquals("admin-created", draft.createdByUid)
        assertEquals("admin-updated", draft.updatedByUid)
    }

    @Test
    fun mapsAdminNotificationCampaignDraftsAsBlockedWhenCallableReturnsUnsafeSendMetadata() = runTest {
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "campaigns": [
                      {
                        "campaignId": "summer-test",
                        "title": "Oferta verão",
                        "body": "Campanha apenas em rascunho",
                        "sendBlocked": false,
                        "sendBlockedReason": "",
                        "deliveryLocked": false,
                        "sendState": ""
                      }
                    ]
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.getNotificationCampaignDrafts("id-token-1")

        val success = assertIs<AdminNotificationCampaignDraftsResult.Success>(result)
        val draft = success.config.campaigns.single()
        assertEquals(true, draft.sendBlocked)
        assertEquals("campaign-send-not-implemented", draft.sendBlockedReason)
        assertEquals(true, draft.deliveryLocked)
        assertEquals("draft_only", draft.sendState)
    }

    @Test
    fun postsNotificationCampaignDraftUpsertWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "created": true,
                    "campaignId": "summer-test",
                    "status": "draft",
                    "targetAudience": "test_users",
                    "sendBlocked": true,
                    "sendBlockedReason": "campaign-send-not-implemented",
                    "deliveryLocked": true,
                    "sendState": "draft_only"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.upsertNotificationCampaignDraft(
            AdminNotificationCampaignDraftMutationRequest(
                campaignId = "summer-test",
                title = "Oferta verão",
                body = "Campanha apenas em rascunho",
                targetAudience = "test_users",
                scheduledAtIso = "2026-06-10T10:00:00.000Z",
                notes = "QA",
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminNotificationCampaignDraftMutationResult.Success>(result)
        assertEquals("/test-project/europe-west1/upsertAdminNotificationCampaignDraft", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("summer-test", success.receipt.campaignId)
        assertEquals(true, success.receipt.created)
        assertEquals(true, success.receipt.sendBlocked)
        assertEquals(true, success.receipt.deliveryLocked)
        assertEquals("draft_only", success.receipt.sendState)
    }

    @Test
    fun mapsNotificationCampaignDraftMutationReceiptAsBlockedWhenCallableReturnsUnsafeSendMetadata() = runTest {
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "campaignId": "summer-test",
                    "status": "draft",
                    "sendBlocked": false,
                    "sendBlockedReason": "",
                    "deliveryLocked": false,
                    "sendState": ""
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.upsertNotificationCampaignDraft(
            AdminNotificationCampaignDraftMutationRequest(
                campaignId = "summer-test",
                title = "Oferta verão",
                body = "Campanha apenas em rascunho",
                targetAudience = "test_users",
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminNotificationCampaignDraftMutationResult.Success>(result)
        assertEquals(true, success.receipt.sendBlocked)
        assertEquals("campaign-send-not-implemented", success.receipt.sendBlockedReason)
        assertEquals(true, success.receipt.deliveryLocked)
        assertEquals("draft_only", success.receipt.sendState)
    }

    @Test
    fun mapsNotificationCampaignArchiveNotFound() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "NOT_FOUND",
                    "message": "Notification campaign draft not found"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.archiveNotificationCampaignDraft(
            AdminNotificationCampaignDraftArchiveRequest(campaignId = "summer-test"),
            idToken = "id-token-1",
        )

        val failure = assertIs<AdminNotificationCampaignDraftMutationResult.Failure>(result)
        assertEquals("/test-project/europe-west1/archiveAdminNotificationCampaignDraft", requestedPath)
        assertIs<AdminError.NotFound>(failure.error)
    }

    @Test
    fun postsAvailabilityUpdateWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "defaultMaxBookingsPerSlot": 4,
                    "defaultSlotIntervalMinutes": 20,
                    "openingHours": [
                      {
                        "dayLabel": "Segunda a Sexta",
                        "hoursLabel": "09:00 - 13:00, 14:00 - 19:00",
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

        val result = api.updateAvailabilityConfiguration(
            AdminAvailabilityUpdateRequest(
                defaultMaxBookingsPerSlot = 4,
                defaultSlotIntervalMinutes = 20,
                openingHours = listOf(
                    AdminBusinessOpeningHours(
                        dayLabel = "Segunda a Sexta",
                        hoursLabel = "09:00 - 13:00, 14:00 - 19:00",
                        closed = false,
                    ),
                ),
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminAvailabilityResult.Success>(result)
        assertEquals("/test-project/europe-west1/updateAvailabilityConfiguration", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals(4, success.config.defaultMaxBookingsPerSlot)
        assertEquals(20, success.config.defaultSlotIntervalMinutes)
    }

    @Test
    fun postsCapacityOverrideMutationWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "date": "2026-06-10",
                    "maxBookingsPerSlot": 0,
                    "status": "updated"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.upsertCapacityOverride(
            AdminCapacityOverrideUpsertRequest(
                date = "2026-06-10",
                maxBookingsPerSlot = 0,
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminCapacityOverrideMutationResult.Success>(result)
        assertEquals("/test-project/europe-west1/upsertCapacityOverride", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("2026-06-10", success.receipt.date)
        assertEquals(0, success.receipt.maxBookingsPerSlot)
    }

    @Test
    fun postsCapacityOverrideClearWithAuthorization() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "date": "2026-06-10",
                    "status": "cleared"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.clearCapacityOverride(
            AdminCapacityOverrideClearRequest(date = "2026-06-10"),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminCapacityOverrideMutationResult.Success>(result)
        assertEquals("/test-project/europe-west1/clearCapacityOverride", requestedPath)
        assertEquals("cleared", success.receipt.status)
    }

    @Test
    fun postsBlockedSlotMutationWithAuthorization() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "blockedSlotId": "block-1",
                    "date": "2026-06-10",
                    "status": "updated"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.upsertBlockedSlot(
            AdminBlockedSlotUpsertRequest(
                blockedSlotId = "block-1",
                date = "2026-06-10",
                slotStartIso = "2026-06-10T09:00:00.000Z",
                slotEndIso = "2026-06-10T10:00:00.000Z",
                reason = "Manutenção",
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminBlockedSlotMutationResult.Success>(result)
        assertEquals("/test-project/europe-west1/upsertBlockedSlot", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("block-1", success.receipt.blockedSlotId)
        assertEquals("2026-06-10", success.receipt.date)
    }

    @Test
    fun postsBlockedSlotClearWithAuthorization() = runTest {
        var requestedPath: String? = null
        val api = KtorAdminFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "blockedSlotId": "block-1",
                    "status": "cleared"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.clearBlockedSlot(
            AdminBlockedSlotClearRequest(blockedSlotId = "block-1"),
            idToken = "id-token-1",
        )

        val success = assertIs<AdminBlockedSlotMutationResult.Success>(result)
        assertEquals("/test-project/europe-west1/clearBlockedSlot", requestedPath)
        assertEquals("cleared", success.receipt.status)
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
                        "sortOrder": 20,
                        "createdAtIso": "2026-06-01T10:00:00.000Z",
                        "updatedAtIso": "2026-06-01T11:30:00.000Z",
                        "archivedAtIso": "2026-06-01T12:45:00.000Z",
                        "createdByUid": " admin-create ",
                        "updatedByUid": " admin-update ",
                        "archivedByUid": " admin-archive "
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
        assertEquals("2026-06-01T10:00:00.000Z", service.createdAtIso)
        assertEquals("2026-06-01T11:30:00.000Z", service.updatedAtIso)
        assertEquals("2026-06-01T12:45:00.000Z", service.archivedAtIso)
        assertEquals("admin-create", service.createdByUid)
        assertEquals("admin-update", service.updatedByUid)
        assertEquals("admin-archive", service.archivedByUid)
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
                        "sortOrder": 30,
                        "createdAtIso": "2026-06-01T10:00:00.000Z",
                        "updatedAtIso": "2026-06-01T11:30:00.000Z",
                        "archivedAtIso": "2026-06-01T12:45:00.000Z",
                        "createdByUid": " admin-create ",
                        "updatedByUid": " admin-update ",
                        "archivedByUid": " admin-archive "
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
        assertEquals("2026-06-01T10:00:00.000Z", extra.createdAtIso)
        assertEquals("2026-06-01T11:30:00.000Z", extra.updatedAtIso)
        assertEquals("2026-06-01T12:45:00.000Z", extra.archivedAtIso)
        assertEquals("admin-create", extra.createdByUid)
        assertEquals("admin-update", extra.updatedByUid)
        assertEquals("admin-archive", extra.archivedByUid)
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
