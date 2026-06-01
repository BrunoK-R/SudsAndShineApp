package com.sudsmobile.data.admin

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

class FirebaseAdminRepositoryTest {
    @Test
    fun upsertServiceCatalogItemNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertServiceCatalogItem(
            AdminServiceCatalogMutationRequest(
                serviceId = " premium ",
                name = " Lavagem   Premium ",
                description = " Detalhe   completo ",
                durationMinutes = 45,
                passengerPriceCents = 3200,
                suvPriceCents = 3400,
                iconKey = "",
                popular = true,
                sortOrder = 20,
            ),
        )

        val success = assertIs<AdminServiceCatalogMutationResult.Success>(result)
        assertEquals("premium", success.receipt.serviceId)
        assertEquals("id-token-1", api.upsertIdTokens.single())
        val request = api.upsertRequests.single()
        assertEquals("premium", request.serviceId)
        assertEquals("Lavagem Premium", request.name)
        assertEquals("Detalhe completo", request.description)
        assertEquals("car", request.iconKey)
    }

    @Test
    fun upsertServiceCatalogItemReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertServiceCatalogItem(
            AdminServiceCatalogMutationRequest(
                serviceId = "services/premium",
                name = "Premium",
                durationMinutes = 45,
                passengerPriceCents = 3200,
                suvPriceCents = 3400,
            ),
        )

        val failure = assertIs<AdminServiceCatalogMutationResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.upsertRequests.size)
    }

    @Test
    fun upsertServiceCatalogItemRequiresAuthenticatedSession() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = false),
        )

        val result = repository.upsertServiceCatalogItem(
            AdminServiceCatalogMutationRequest(
                name = "Premium",
                durationMinutes = 45,
                passengerPriceCents = 3200,
                suvPriceCents = 3400,
            ),
        )

        val failure = assertIs<AdminServiceCatalogMutationResult.Failure>(result)
        assertIs<AdminError.Unauthenticated>(failure.error)
        assertEquals(0, api.upsertRequests.size)
    }

    @Test
    fun archiveServiceCatalogItemNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.archiveServiceCatalogItem(
            AdminServiceCatalogArchiveRequest(serviceId = " premium "),
        )

        val success = assertIs<AdminServiceCatalogMutationResult.Success>(result)
        assertEquals("premium", success.receipt.serviceId)
        assertEquals("id-token-1", api.archiveIdTokens.single())
        assertEquals("premium", api.archiveRequests.single().serviceId)
    }

    @Test
    fun getServiceCatalogConfigurationRequiresAuthenticatedSession() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = false),
        )

        val result = repository.getServiceCatalogConfiguration()

        val failure = assertIs<AdminServiceCatalogResult.Failure>(result)
        assertIs<AdminError.Unauthenticated>(failure.error)
        assertEquals(0, api.catalogIdTokens.size)
    }

    @Test
    fun getServiceCatalogConfigurationUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.getServiceCatalogConfiguration()

        val success = assertIs<AdminServiceCatalogResult.Success>(result)
        assertEquals("premium", success.config.services.single().id)
        assertEquals("id-token-1", api.catalogIdTokens.single())
    }

    @Test
    fun upsertServiceExtraNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertServiceExtra(
            AdminServiceExtraMutationRequest(
                extraId = " wax ",
                name = " Enceramento   Premium ",
                description = " Proteção   extra ",
                priceCents = 1500,
                iconKey = "",
                eligibleServiceIds = listOf(" premium ", "standard", "premium"),
                active = true,
                sortOrder = 30,
            ),
        )

        val success = assertIs<AdminServiceExtraMutationResult.Success>(result)
        assertEquals("wax", success.receipt.extraId)
        assertEquals("id-token-1", api.upsertExtraIdTokens.single())
        val request = api.upsertExtraRequests.single()
        assertEquals("wax", request.extraId)
        assertEquals("Enceramento Premium", request.name)
        assertEquals("Proteção extra", request.description)
        assertEquals("auto_awesome", request.iconKey)
        assertEquals(listOf("premium", "standard"), request.eligibleServiceIds)
    }

    @Test
    fun upsertServiceExtraReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertServiceExtra(
            AdminServiceExtraMutationRequest(
                extraId = "service_extras/wax",
                name = "Wax",
                priceCents = 1500,
            ),
        )

        val failure = assertIs<AdminServiceExtraMutationResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.upsertExtraRequests.size)
    }

    @Test
    fun archiveServiceExtraNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.archiveServiceExtra(
            AdminServiceExtraArchiveRequest(extraId = " wax "),
        )

        val success = assertIs<AdminServiceExtraMutationResult.Success>(result)
        assertEquals("wax", success.receipt.extraId)
        assertEquals("id-token-1", api.archiveExtraIdTokens.single())
        assertEquals("wax", api.archiveExtraRequests.single().extraId)
    }

    @Test
    fun getServiceExtrasConfigurationUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.getServiceExtrasConfiguration()

        val success = assertIs<AdminServiceExtrasResult.Success>(result)
        assertEquals("wax", success.config.extras.single().id)
        assertEquals("id-token-1", api.extrasIdTokens.single())
    }

    @Test
    fun updateAvailabilityConfigurationNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.updateAvailabilityConfiguration(
            AdminAvailabilityUpdateRequest(
                defaultMaxBookingsPerSlot = 4,
                openingHours = listOf(
                    AdminBusinessOpeningHours(
                        dayLabel = " Segunda   a Sexta ",
                        hoursLabel = " 09:00 - 13:00, 14:00 - 19:00 ",
                        closed = false,
                    ),
                    AdminBusinessOpeningHours(
                        dayLabel = " Domingo ",
                        hoursLabel = " Encerrado ",
                        closed = true,
                    ),
                ),
            ),
        )

        val success = assertIs<AdminAvailabilityResult.Success>(result)
        assertEquals(4, success.config.defaultMaxBookingsPerSlot)
        assertEquals("id-token-1", api.updateAvailabilityIdTokens.single())
        assertEquals("Segunda a Sexta", api.updateAvailabilityRequests.single().openingHours.first().dayLabel)
    }

    @Test
    fun updateAvailabilityConfigurationReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.updateAvailabilityConfiguration(
            AdminAvailabilityUpdateRequest(
                defaultMaxBookingsPerSlot = 21,
                openingHours = listOf(
                    AdminBusinessOpeningHours("Segunda", "09:00 - 19:00", closed = false),
                ),
            ),
        )

        val failure = assertIs<AdminAvailabilityResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.updateAvailabilityRequests.size)
    }

    @Test
    fun getAvailabilityConfigurationUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.getAvailabilityConfiguration()

        val success = assertIs<AdminAvailabilityResult.Success>(result)
        assertEquals(2, success.config.defaultMaxBookingsPerSlot)
        assertEquals("id-token-1", api.availabilityIdTokens.single())
    }

    @Test
    fun getBookingPolicyConfigurationUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.getBookingPolicyConfiguration()

        val success = assertIs<AdminBookingPolicyResult.Success>(result)
        assertEquals(1440, success.config.pendingHoldMinutes)
        assertEquals("id-token-1", api.bookingPolicyIdTokens.single())
    }

    @Test
    fun updateBookingPolicyConfigurationNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.updateBookingPolicyConfiguration(
            AdminBookingPolicyUpdateRequest(
                pendingHoldMinutes = 240,
                cancellationWindowMinutes = 120,
                rescheduleWindowMinutes = 60,
                paymentEligibilityCopy = "  Pagamento   no local  ",
            ),
        )

        val success = assertIs<AdminBookingPolicyResult.Success>(result)
        assertEquals(240, success.config.pendingHoldMinutes)
        assertEquals("id-token-1", api.updateBookingPolicyIdTokens.single())
        assertEquals("Pagamento no local", api.updateBookingPolicyRequests.single().paymentEligibilityCopy)
    }

    @Test
    fun updateBookingPolicyConfigurationReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.updateBookingPolicyConfiguration(
            AdminBookingPolicyUpdateRequest(
                pendingHoldMinutes = 5,
                cancellationWindowMinutes = 120,
                rescheduleWindowMinutes = 60,
                paymentEligibilityCopy = "Pagamento no local",
            ),
        )

        val failure = assertIs<AdminBookingPolicyResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.updateBookingPolicyRequests.size)
    }

    @Test
    fun loadsLoyaltySettingsWithCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.getLoyaltySettingsConfiguration()

        val success = assertIs<AdminLoyaltySettingsResult.Success>(result)
        assertEquals(10, success.config.stampsRequired)
        assertEquals("id-token-1", api.loyaltySettingsIdTokens.single())
    }

    @Test
    fun updateLoyaltySettingsNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.updateLoyaltySettingsConfiguration(
            AdminLoyaltySettingsUpdateRequest(
                stampsRequired = 8,
                rewardType = "discount percent",
                rewardValue = 15,
                rewardDescription = "  15%   de desconto  ",
            ),
        )

        val success = assertIs<AdminLoyaltySettingsResult.Success>(result)
        assertEquals(8, success.config.stampsRequired)
        assertEquals("id-token-1", api.updateLoyaltySettingsIdTokens.single())
        assertEquals("discount_percent", api.updateLoyaltySettingsRequests.single().rewardType)
        assertEquals("15% de desconto", api.updateLoyaltySettingsRequests.single().rewardDescription)
    }

    @Test
    fun updateLoyaltySettingsReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.updateLoyaltySettingsConfiguration(
            AdminLoyaltySettingsUpdateRequest(
                stampsRequired = 0,
                rewardType = "free_wash",
                rewardValue = 1,
                rewardDescription = "1 lavagem grátis",
            ),
        )

        val failure = assertIs<AdminLoyaltySettingsResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.updateLoyaltySettingsRequests.size)
    }

    @Test
    fun loadsNotificationSettingsWithCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.getNotificationSettingsConfiguration()

        val success = assertIs<AdminNotificationSettingsResult.Success>(result)
        assertEquals(120, success.config.reminderLeadMinutes)
        assertEquals("booking_request", success.config.templates.first().key)
        assertEquals("id-token-1", api.notificationSettingsIdTokens.single())
    }

    @Test
    fun updateNotificationSettingsNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.updateNotificationSettingsConfiguration(
            adminNotificationSettingsRequest(
                reminderLeadMinutes = 60,
                templates = adminNotificationTemplates().map {
                    if (it.key == "booking_request") it.copy(title = "  Pedido   recebido  ") else it
                },
            ),
        )

        val success = assertIs<AdminNotificationSettingsResult.Success>(result)
        assertEquals(60, success.config.reminderLeadMinutes)
        assertEquals("id-token-1", api.updateNotificationSettingsIdTokens.single())
        assertEquals(
            "Pedido recebido",
            api.updateNotificationSettingsRequests.single().templates.first { it.key == "booking_request" }.title,
        )
    }

    @Test
    fun updateNotificationSettingsReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.updateNotificationSettingsConfiguration(
            adminNotificationSettingsRequest(reminderLeadMinutes = 5),
        )

        val failure = assertIs<AdminNotificationSettingsResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.updateNotificationSettingsRequests.size)
    }

    @Test
    fun sendNotificationTestNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.sendNotificationTestToSelf(
            AdminNotificationTestRequest(templateKey = " booking_request "),
        )

        val success = assertIs<AdminNotificationTestResult.Success>(result)
        assertEquals("test-notification-1", success.receipt.notificationId)
        assertEquals("id-token-1", api.notificationTestIdTokens.single())
        assertEquals("booking_request", api.notificationTestRequests.single().templateKey)
    }

    @Test
    fun sendNotificationTestReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.sendNotificationTestToSelf(
            AdminNotificationTestRequest(templateKey = "../booking_request"),
        )

        val failure = assertIs<AdminNotificationTestResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.notificationTestRequests.size)
    }

    @Test
    fun getNotificationCampaignDraftsUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.getNotificationCampaignDrafts()

        val success = assertIs<AdminNotificationCampaignDraftsResult.Success>(result)
        assertEquals("summer-test", success.config.campaigns.single().campaignId)
        assertEquals("id-token-1", api.notificationCampaignDraftsIdTokens.single())
    }

    @Test
    fun upsertNotificationCampaignDraftNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertNotificationCampaignDraft(
            AdminNotificationCampaignDraftMutationRequest(
                campaignId = " summer-test ",
                title = "  Oferta   verão ",
                body = "  Campanha   apenas   em rascunho ",
                targetAudience = "test_users",
                scheduledAtIso = " 2026-06-10T10:00:00.000Z ",
                notes = "  QA   interno ",
            ),
        )

        val success = assertIs<AdminNotificationCampaignDraftMutationResult.Success>(result)
        val request = api.upsertNotificationCampaignDraftRequests.single()
        assertEquals("summer-test", success.receipt.campaignId)
        assertEquals("id-token-1", api.upsertNotificationCampaignDraftIdTokens.single())
        assertEquals("summer-test", request.campaignId)
        assertEquals("Oferta verão", request.title)
        assertEquals("Campanha apenas em rascunho", request.body)
        assertEquals("QA interno", request.notes)
    }

    @Test
    fun upsertNotificationCampaignDraftReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertNotificationCampaignDraft(
            AdminNotificationCampaignDraftMutationRequest(
                campaignId = "../summer-test",
                title = "Oferta verão",
                body = "Campanha apenas em rascunho",
                targetAudience = "all_users",
            ),
        )

        val failure = assertIs<AdminNotificationCampaignDraftMutationResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.upsertNotificationCampaignDraftRequests.size)
    }

    @Test
    fun archiveNotificationCampaignDraftNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.archiveNotificationCampaignDraft(
            AdminNotificationCampaignDraftArchiveRequest(campaignId = " summer-test "),
        )

        val success = assertIs<AdminNotificationCampaignDraftMutationResult.Success>(result)
        assertEquals("summer-test", success.receipt.campaignId)
        assertEquals("id-token-1", api.archiveNotificationCampaignDraftIdTokens.single())
        assertEquals("summer-test", api.archiveNotificationCampaignDraftRequests.single().campaignId)
    }

    @Test
    fun upsertCapacityOverrideNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertCapacityOverride(
            AdminCapacityOverrideUpsertRequest(
                date = " 2026-06-10 ",
                maxBookingsPerSlot = 0,
            ),
        )

        val success = assertIs<AdminCapacityOverrideMutationResult.Success>(result)
        assertEquals("2026-06-10", success.receipt.date)
        assertEquals("id-token-1", api.upsertCapacityOverrideIdTokens.single())
        assertEquals("2026-06-10", api.upsertCapacityOverrideRequests.single().date)
    }

    @Test
    fun upsertCapacityOverrideReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertCapacityOverride(
            AdminCapacityOverrideUpsertRequest(
                date = "2026-02-31",
                maxBookingsPerSlot = 2,
            ),
        )

        val failure = assertIs<AdminCapacityOverrideMutationResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.upsertCapacityOverrideRequests.size)
    }

    @Test
    fun clearCapacityOverrideRequiresAuthenticatedSession() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = false),
        )

        val result = repository.clearCapacityOverride(
            AdminCapacityOverrideClearRequest(date = "2026-06-10"),
        )

        val failure = assertIs<AdminCapacityOverrideMutationResult.Failure>(result)
        assertIs<AdminError.Unauthenticated>(failure.error)
        assertEquals(0, api.clearCapacityOverrideRequests.size)
    }

    @Test
    fun upsertBlockedSlotNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertBlockedSlot(
            AdminBlockedSlotUpsertRequest(
                blockedSlotId = " block-1 ",
                date = " 2026-06-10 ",
                slotStartIso = " 2026-06-10T09:00:00.000Z ",
                slotEndIso = " 2026-06-10T10:30:00.000Z ",
                reason = " Reunião   equipa ",
            ),
        )

        val success = assertIs<AdminBlockedSlotMutationResult.Success>(result)
        assertEquals("block-1", success.receipt.blockedSlotId)
        assertEquals("id-token-1", api.upsertBlockedSlotIdTokens.single())
        val request = api.upsertBlockedSlotRequests.single()
        assertEquals("block-1", request.blockedSlotId)
        assertEquals("2026-06-10T09:00:00.000Z", request.slotStartIso)
        assertEquals("Reunião equipa", request.reason)
    }

    @Test
    fun upsertBlockedSlotReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertBlockedSlot(
            AdminBlockedSlotUpsertRequest(
                blockedSlotId = "bad/id",
                date = "2026-06-10",
                slotStartIso = "2026-06-10T11:00:00.000Z",
                slotEndIso = "2026-06-10T10:00:00.000Z",
            ),
        )

        val failure = assertIs<AdminBlockedSlotMutationResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.upsertBlockedSlotRequests.size)
    }

    @Test
    fun clearBlockedSlotRequiresAuthenticatedSession() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = false),
        )

        val result = repository.clearBlockedSlot(
            AdminBlockedSlotClearRequest(blockedSlotId = "block-1"),
        )

        val failure = assertIs<AdminBlockedSlotMutationResult.Failure>(result)
        assertIs<AdminError.Unauthenticated>(failure.error)
        assertEquals(0, api.clearBlockedSlotRequests.size)
    }
}

private class FakeAdminFunctionsApi : AdminFunctionsApi {
    val upsertRequests = mutableListOf<AdminServiceCatalogMutationRequest>()
    val upsertIdTokens = mutableListOf<String>()
    val archiveRequests = mutableListOf<AdminServiceCatalogArchiveRequest>()
    val archiveIdTokens = mutableListOf<String>()
    val catalogIdTokens = mutableListOf<String>()
    val extrasIdTokens = mutableListOf<String>()
    val upsertExtraRequests = mutableListOf<AdminServiceExtraMutationRequest>()
    val upsertExtraIdTokens = mutableListOf<String>()
    val archiveExtraRequests = mutableListOf<AdminServiceExtraArchiveRequest>()
    val archiveExtraIdTokens = mutableListOf<String>()
    val availabilityIdTokens = mutableListOf<String>()
    val bookingPolicyIdTokens = mutableListOf<String>()
    val loyaltySettingsIdTokens = mutableListOf<String>()
    val notificationSettingsIdTokens = mutableListOf<String>()
    val updateAvailabilityRequests = mutableListOf<AdminAvailabilityUpdateRequest>()
    val updateAvailabilityIdTokens = mutableListOf<String>()
    val updateBookingPolicyRequests = mutableListOf<AdminBookingPolicyUpdateRequest>()
    val updateBookingPolicyIdTokens = mutableListOf<String>()
    val updateLoyaltySettingsRequests = mutableListOf<AdminLoyaltySettingsUpdateRequest>()
    val updateLoyaltySettingsIdTokens = mutableListOf<String>()
    val updateNotificationSettingsRequests = mutableListOf<AdminNotificationSettingsUpdateRequest>()
    val updateNotificationSettingsIdTokens = mutableListOf<String>()
    val notificationTestRequests = mutableListOf<AdminNotificationTestRequest>()
    val notificationTestIdTokens = mutableListOf<String>()
    val notificationCampaignDraftsIdTokens = mutableListOf<String>()
    val upsertNotificationCampaignDraftRequests = mutableListOf<AdminNotificationCampaignDraftMutationRequest>()
    val upsertNotificationCampaignDraftIdTokens = mutableListOf<String>()
    val archiveNotificationCampaignDraftRequests = mutableListOf<AdminNotificationCampaignDraftArchiveRequest>()
    val archiveNotificationCampaignDraftIdTokens = mutableListOf<String>()
    val upsertCapacityOverrideRequests = mutableListOf<AdminCapacityOverrideUpsertRequest>()
    val upsertCapacityOverrideIdTokens = mutableListOf<String>()
    val clearCapacityOverrideRequests = mutableListOf<AdminCapacityOverrideClearRequest>()
    val clearCapacityOverrideIdTokens = mutableListOf<String>()
    val upsertBlockedSlotRequests = mutableListOf<AdminBlockedSlotUpsertRequest>()
    val upsertBlockedSlotIdTokens = mutableListOf<String>()
    val clearBlockedSlotRequests = mutableListOf<AdminBlockedSlotClearRequest>()
    val clearBlockedSlotIdTokens = mutableListOf<String>()

    override suspend fun syncMyRole(idToken: String): AdminRoleResult {
        return AdminRoleResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getPendingBookingRequests(idToken: String): AdminBookingRequestsResult {
        return AdminBookingRequestsResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult {
        return AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult {
        return AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getBusinessInfoConfiguration(idToken: String): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getAvailabilityConfiguration(idToken: String): AdminAvailabilityResult {
        availabilityIdTokens += idToken
        return AdminAvailabilityResult.Success(
            AdminAvailabilityConfig(
                defaultMaxBookingsPerSlot = 2,
                openingHours = listOf(
                    AdminBusinessOpeningHours(
                        dayLabel = "Segunda a Sexta",
                        hoursLabel = "09:00 - 19:00",
                        closed = false,
                    ),
                ),
                capacityOverrides = listOf(
                    AdminCapacityOverrideItem(
                        date = "2026-06-10",
                        maxBookingsPerSlot = 0,
                    ),
                ),
                blockedSlots = listOf(
                    AdminBlockedSlotItem(
                        blockedSlotId = "block-1",
                        date = "2026-06-10",
                        slotStartIso = "2026-06-10T09:00:00.000Z",
                        slotEndIso = "2026-06-10T10:00:00.000Z",
                        reason = "Manutenção",
                    ),
                ),
            ),
        )
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
        idToken: String,
    ): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getBookingPolicyConfiguration(idToken: String): AdminBookingPolicyResult {
        bookingPolicyIdTokens += idToken
        return AdminBookingPolicyResult.Success(
            AdminBookingPolicyConfig(
                pendingHoldMinutes = 1440,
                cancellationWindowMinutes = 0,
                rescheduleWindowMinutes = 0,
                paymentEligibilityCopy = "Pagamento no local",
            ),
        )
    }

    override suspend fun updateBookingPolicyConfiguration(
        request: AdminBookingPolicyUpdateRequest,
        idToken: String,
    ): AdminBookingPolicyResult {
        updateBookingPolicyRequests += request
        updateBookingPolicyIdTokens += idToken
        return AdminBookingPolicyResult.Success(
            AdminBookingPolicyConfig(
                pendingHoldMinutes = request.pendingHoldMinutes,
                cancellationWindowMinutes = request.cancellationWindowMinutes,
                rescheduleWindowMinutes = request.rescheduleWindowMinutes,
                paymentEligibilityCopy = request.paymentEligibilityCopy,
            ),
        )
    }

    override suspend fun getLoyaltySettingsConfiguration(idToken: String): AdminLoyaltySettingsResult {
        loyaltySettingsIdTokens += idToken
        return AdminLoyaltySettingsResult.Success(
            AdminLoyaltySettingsConfig(
                stampsRequired = 10,
                rewardType = "free_wash",
                rewardValue = 1,
                rewardDescription = "1 lavagem grátis",
            ),
        )
    }

    override suspend fun updateLoyaltySettingsConfiguration(
        request: AdminLoyaltySettingsUpdateRequest,
        idToken: String,
    ): AdminLoyaltySettingsResult {
        updateLoyaltySettingsRequests += request
        updateLoyaltySettingsIdTokens += idToken
        return AdminLoyaltySettingsResult.Success(
            AdminLoyaltySettingsConfig(
                stampsRequired = request.stampsRequired,
                rewardType = request.rewardType,
                rewardValue = request.rewardValue,
                rewardDescription = request.rewardDescription,
            ),
        )
    }

    override suspend fun getNotificationSettingsConfiguration(idToken: String): AdminNotificationSettingsResult {
        notificationSettingsIdTokens += idToken
        return AdminNotificationSettingsResult.Success(adminNotificationSettingsConfig())
    }

    override suspend fun updateNotificationSettingsConfiguration(
        request: AdminNotificationSettingsUpdateRequest,
        idToken: String,
    ): AdminNotificationSettingsResult {
        updateNotificationSettingsRequests += request
        updateNotificationSettingsIdTokens += idToken
        return AdminNotificationSettingsResult.Success(
            AdminNotificationSettingsConfig(
                bookingStatusEnabled = request.bookingStatusEnabled,
                appointmentReminderEnabled = request.appointmentReminderEnabled,
                loyaltyEnabled = request.loyaltyEnabled,
                adminPendingAlertEnabled = request.adminPendingAlertEnabled,
                marketingEnabled = request.marketingEnabled,
                reminderLeadMinutes = request.reminderLeadMinutes,
                quietHoursStart = request.quietHoursStart,
                quietHoursEnd = request.quietHoursEnd,
                templates = request.templates,
            ),
        )
    }

    override suspend fun sendNotificationTestToSelf(
        request: AdminNotificationTestRequest,
        idToken: String,
    ): AdminNotificationTestResult {
        notificationTestRequests += request
        notificationTestIdTokens += idToken
        return AdminNotificationTestResult.Success(
            AdminNotificationTestReceipt(
                notificationId = "test-notification-1",
                templateKey = request.templateKey,
                deliveryState = "queued",
                recipientUid = "admin-1",
                message = "queued",
            ),
        )
    }

    override suspend fun getNotificationCampaignDrafts(idToken: String): AdminNotificationCampaignDraftsResult {
        notificationCampaignDraftsIdTokens += idToken
        return AdminNotificationCampaignDraftsResult.Success(
            AdminNotificationCampaignDraftsConfig(
                source = "firestore",
                campaigns = listOf(
                    AdminNotificationCampaignDraft(
                        campaignId = "summer-test",
                        title = "Oferta verão",
                        body = "Campanha apenas em rascunho",
                        targetAudience = "test_users",
                        channels = listOf("push"),
                        marketingConsentRequired = false,
                        status = "draft",
                        scheduledAtIso = "2026-06-10T10:00:00.000Z",
                        notes = "QA",
                        sendBlocked = true,
                        sendBlockedReason = "campaign-send-not-implemented",
                    ),
                ),
            ),
        )
    }

    override suspend fun upsertNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftMutationRequest,
        idToken: String,
    ): AdminNotificationCampaignDraftMutationResult {
        upsertNotificationCampaignDraftRequests += request
        upsertNotificationCampaignDraftIdTokens += idToken
        return AdminNotificationCampaignDraftMutationResult.Success(
            AdminNotificationCampaignDraftMutationReceipt(
                campaignId = request.campaignId.ifBlank { "generated-campaign" },
                status = "draft",
                created = request.campaignId.isBlank(),
                targetAudience = request.targetAudience,
                sendBlocked = true,
                sendBlockedReason = "campaign-send-not-implemented",
            ),
        )
    }

    override suspend fun archiveNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftArchiveRequest,
        idToken: String,
    ): AdminNotificationCampaignDraftMutationResult {
        archiveNotificationCampaignDraftRequests += request
        archiveNotificationCampaignDraftIdTokens += idToken
        return AdminNotificationCampaignDraftMutationResult.Success(
            AdminNotificationCampaignDraftMutationReceipt(
                campaignId = request.campaignId,
                status = "archived",
            ),
        )
    }

    override suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
        idToken: String,
    ): AdminAvailabilityResult {
        updateAvailabilityRequests += request
        updateAvailabilityIdTokens += idToken
        return AdminAvailabilityResult.Success(
            AdminAvailabilityConfig(
                defaultMaxBookingsPerSlot = request.defaultMaxBookingsPerSlot,
                openingHours = request.openingHours,
            ),
        )
    }

    override suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
        idToken: String,
    ): AdminCapacityOverrideMutationResult {
        upsertCapacityOverrideRequests += request
        upsertCapacityOverrideIdTokens += idToken
        return AdminCapacityOverrideMutationResult.Success(
            AdminCapacityOverrideMutationReceipt(
                date = request.date,
                status = "updated",
                maxBookingsPerSlot = request.maxBookingsPerSlot,
            ),
        )
    }

    override suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
        idToken: String,
    ): AdminCapacityOverrideMutationResult {
        clearCapacityOverrideRequests += request
        clearCapacityOverrideIdTokens += idToken
        return AdminCapacityOverrideMutationResult.Success(
            AdminCapacityOverrideMutationReceipt(
                date = request.date,
                status = "cleared",
            ),
        )
    }

    override suspend fun upsertBlockedSlot(
        request: AdminBlockedSlotUpsertRequest,
        idToken: String,
    ): AdminBlockedSlotMutationResult {
        upsertBlockedSlotRequests += request
        upsertBlockedSlotIdTokens += idToken
        return AdminBlockedSlotMutationResult.Success(
            AdminBlockedSlotMutationReceipt(
                blockedSlotId = request.blockedSlotId.ifBlank { "generated-block" },
                date = request.date,
                status = "updated",
            ),
        )
    }

    override suspend fun clearBlockedSlot(
        request: AdminBlockedSlotClearRequest,
        idToken: String,
    ): AdminBlockedSlotMutationResult {
        clearBlockedSlotRequests += request
        clearBlockedSlotIdTokens += idToken
        return AdminBlockedSlotMutationResult.Success(
            AdminBlockedSlotMutationReceipt(
                blockedSlotId = request.blockedSlotId,
                status = "cleared",
            ),
        )
    }

    override suspend fun getServiceCatalogConfiguration(idToken: String): AdminServiceCatalogResult {
        catalogIdTokens += idToken
        return AdminServiceCatalogResult.Success(
            AdminServiceCatalogConfig(
                services = listOf(
                    AdminServiceCatalogItem(
                        id = "premium",
                        name = "Lavagem Premium",
                        description = "Detalhe",
                        durationMinutes = 45,
                        passengerPriceCents = 3200,
                        suvPriceCents = 3400,
                        iconKey = "sparkles",
                        popular = true,
                        active = true,
                        sortOrder = 20,
                    ),
                ),
            ),
        )
    }

    override suspend fun getServiceExtrasConfiguration(idToken: String): AdminServiceExtrasResult {
        extrasIdTokens += idToken
        return AdminServiceExtrasResult.Success(
            AdminServiceExtrasConfig(
                extras = listOf(
                    AdminServiceExtraItem(
                        id = "wax",
                        name = "Enceramento",
                        description = "Proteção",
                        priceCents = 1500,
                        iconKey = "shield",
                        eligibleServiceIds = listOf("premium"),
                        active = true,
                        sortOrder = 30,
                    ),
                ),
            ),
        )
    }

    override suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult {
        upsertRequests += request
        upsertIdTokens += idToken
        return AdminServiceCatalogMutationResult.Success(
            AdminServiceCatalogMutationReceipt(
                serviceId = request.serviceId.ifBlank { "generated-service" },
                status = if (request.active) "active" else "inactive",
            ),
        )
    }

    override suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
        idToken: String,
    ): AdminServiceExtraMutationResult {
        upsertExtraRequests += request
        upsertExtraIdTokens += idToken
        return AdminServiceExtraMutationResult.Success(
            AdminServiceExtraMutationReceipt(
                extraId = request.extraId.ifBlank { "generated-extra" },
                status = if (request.active) "active" else "inactive",
            ),
        )
    }

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult {
        archiveRequests += request
        archiveIdTokens += idToken
        return AdminServiceCatalogMutationResult.Success(
            AdminServiceCatalogMutationReceipt(
                serviceId = request.serviceId,
                status = "archived",
            ),
        )
    }

    override suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
        idToken: String,
    ): AdminServiceExtraMutationResult {
        archiveExtraRequests += request
        archiveExtraIdTokens += idToken
        return AdminServiceExtraMutationResult.Success(
            AdminServiceExtraMutationReceipt(
                extraId = request.extraId,
                status = "archived",
            ),
        )
    }
}

private class FakeAuthRepository(authenticated: Boolean) : AuthRepository {
    private val authSession = AuthSession(
        user = AuthUser(
            uid = "uid-1",
            email = "admin@example.com",
            displayName = "Admin",
            phoneNumber = "",
        ),
        idToken = "id-token-1",
        refreshToken = "refresh-token-1",
        expiresInSeconds = 3600,
    )
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) AuthSessionState.Authenticated(authSession) else AuthSessionState.Unauthenticated,
    )

    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        mutableSessionState.value = AuthSessionState.Authenticated(authSession)
        return AuthResult.Success(authSession)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        mutableSessionState.value = AuthSessionState.Authenticated(authSession)
        return AuthResult.Success(authSession)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private fun adminNotificationSettingsConfig(): AdminNotificationSettingsConfig = AdminNotificationSettingsConfig(
    bookingStatusEnabled = true,
    appointmentReminderEnabled = true,
    loyaltyEnabled = true,
    adminPendingAlertEnabled = true,
    marketingEnabled = false,
    reminderLeadMinutes = 120,
    quietHoursStart = "22:00",
    quietHoursEnd = "08:00",
    templates = adminNotificationTemplates(),
)

private fun adminNotificationSettingsRequest(
    bookingStatusEnabled: Boolean = true,
    appointmentReminderEnabled: Boolean = true,
    loyaltyEnabled: Boolean = true,
    adminPendingAlertEnabled: Boolean = true,
    marketingEnabled: Boolean = false,
    reminderLeadMinutes: Int = 120,
    quietHoursStart: String = "22:00",
    quietHoursEnd: String = "08:00",
    templates: List<AdminNotificationTemplateConfig> = adminNotificationTemplates(),
): AdminNotificationSettingsUpdateRequest = AdminNotificationSettingsUpdateRequest(
    bookingStatusEnabled = bookingStatusEnabled,
    appointmentReminderEnabled = appointmentReminderEnabled,
    loyaltyEnabled = loyaltyEnabled,
    adminPendingAlertEnabled = adminPendingAlertEnabled,
    marketingEnabled = marketingEnabled,
    reminderLeadMinutes = reminderLeadMinutes,
    quietHoursStart = quietHoursStart,
    quietHoursEnd = quietHoursEnd,
    templates = templates,
)

private fun adminNotificationTemplates(): List<AdminNotificationTemplateConfig> = listOf(
    AdminNotificationTemplateConfig(
        key = "booking_request",
        label = "Pedido recebido",
        enabled = true,
        title = "Pedido de marcação recebido",
        body = "Recebemos o seu pedido de marcação.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_accepted",
        label = "Marcação aceite",
        enabled = true,
        title = "Marcação confirmada",
        body = "A sua marcação foi aceite.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_rejected",
        label = "Marcação rejeitada",
        enabled = true,
        title = "Marcação rejeitada",
        body = "Não foi possível aceitar a marcação.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_expired",
        label = "Pedido expirado",
        enabled = true,
        title = "Pedido expirado",
        body = "O pedido expirou antes da confirmação.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_cancelled",
        label = "Marcação cancelada",
        enabled = true,
        title = "Marcação cancelada",
        body = "A marcação foi cancelada.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_rescheduled",
        label = "Marcação remarcada",
        enabled = true,
        title = "Marcação remarcada",
        body = "A marcação foi remarcada.",
    ),
    AdminNotificationTemplateConfig(
        key = "booking_reminder",
        label = "Lembrete de marcação",
        enabled = true,
        title = "Lembrete",
        body = "Tem uma lavagem marcada em breve.",
    ),
    AdminNotificationTemplateConfig(
        key = "review_prompt",
        label = "Pedido de avaliação",
        enabled = true,
        title = "Avalie a lavagem",
        body = "Diga-nos como correu o serviço.",
    ),
    AdminNotificationTemplateConfig(
        key = "admin_pending_booking",
        label = "Alerta admin de pedido",
        enabled = true,
        title = "Novo pedido de marcação",
        body = "{{customerName}} pediu {{serviceName}} para {{slotStart}}.",
    ),
)
