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
    val updateAvailabilityRequests = mutableListOf<AdminAvailabilityUpdateRequest>()
    val updateAvailabilityIdTokens = mutableListOf<String>()
    val updateBookingPolicyRequests = mutableListOf<AdminBookingPolicyUpdateRequest>()
    val updateBookingPolicyIdTokens = mutableListOf<String>()
    val updateLoyaltySettingsRequests = mutableListOf<AdminLoyaltySettingsUpdateRequest>()
    val updateLoyaltySettingsIdTokens = mutableListOf<String>()
    val upsertCapacityOverrideRequests = mutableListOf<AdminCapacityOverrideUpsertRequest>()
    val upsertCapacityOverrideIdTokens = mutableListOf<String>()
    val clearCapacityOverrideRequests = mutableListOf<AdminCapacityOverrideClearRequest>()
    val clearCapacityOverrideIdTokens = mutableListOf<String>()

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
