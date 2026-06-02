package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingPolicyResult
import com.sudsmobile.data.admin.AdminBookingRequestsResult
import com.sudsmobile.data.admin.AdminBusinessInfoResult
import com.sudsmobile.data.admin.AdminBusinessInfoUpdateRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideClearRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideMutationResult
import com.sudsmobile.data.admin.AdminCapacityOverrideUpsertRequest
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminNotificationCampaignDraft
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftArchiveRequest
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftMutationReceipt
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftMutationRequest
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftMutationResult
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftsConfig
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftsResult
import com.sudsmobile.data.admin.AdminNotificationTestReceipt
import com.sudsmobile.data.admin.AdminNotificationTestRequest
import com.sudsmobile.data.admin.AdminNotificationTestResult
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.admin.AdminRole
import com.sudsmobile.data.admin.AdminRoleResult
import com.sudsmobile.data.admin.AdminServiceCatalogArchiveRequest
import com.sudsmobile.data.admin.AdminServiceCatalogMutationRequest
import com.sudsmobile.data.admin.AdminServiceCatalogMutationResult
import com.sudsmobile.data.admin.AdminServiceCatalogResult
import com.sudsmobile.data.admin.AdminServiceExtraArchiveRequest
import com.sudsmobile.data.admin.AdminServiceExtraMutationRequest
import com.sudsmobile.data.admin.AdminServiceExtraMutationResult
import com.sudsmobile.data.admin.AdminServiceExtrasResult
import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AdminNotificationCampaignDraftsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDraftsRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeCampaignDraftsAdminRepository()
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()

        assertIs<AdminNotificationCampaignDraftsUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadDraftsMapsPermissionFailureToNotAdmin() = runTest {
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = FakeCampaignDraftsAdminRepository(
                loadResult = AdminNotificationCampaignDraftsResult.Failure(AdminError.Permission("denied")),
            ),
        )

        viewModel.loadDrafts()
        runCurrent()

        assertIs<AdminNotificationCampaignDraftsUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun loadDraftsMapsAuditMetadataForAdminCards() = runTest {
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = FakeCampaignDraftsAdminRepository(),
        )

        viewModel.loadDrafts()
        runCurrent()

        val loaded = assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
        val draft = loaded.drafts.single()
        assertEquals("Criado 2026-06-01 10:00 UTC por admin-cr", draft.createdAuditLabel)
        assertEquals("Atualizado 2026-06-01 11:30 UTC por admin-up...", draft.updatedAuditLabel)
        assertEquals("", draft.archivedAuditLabel)
    }

    @Test
    fun loadDraftsShowsCampaignDraftsAsBlockedWhenMetadataIsUnsafe() = runTest {
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = FakeCampaignDraftsAdminRepository(
                loadResult = AdminNotificationCampaignDraftsResult.Success(
                    campaignDraftsConfig(sendBlocked = false, sendBlockedReason = ""),
                ),
            ),
        )

        viewModel.loadDrafts()
        runCurrent()

        val loaded = assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
        val draft = loaded.drafts.single()
        assertEquals(true, draft.sendBlocked)
        assertEquals("campaign-send-not-implemented", draft.sendBlockedReason)
    }

    @Test
    fun loadDraftsIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminNotificationCampaignDraftsResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = FakeCampaignDraftsAdminRepository(loadResultDeferred = deferred),
        )

        viewModel.loadDrafts()
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminNotificationCampaignDraftsResult.Success(campaignDraftsConfig()))
        runCurrent()

        assertIs<AdminNotificationCampaignDraftsUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveValidatesBeforeRepositoryCall() = runTest {
        val repository = FakeCampaignDraftsAdminRepository()
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.startCreate()
        viewModel.updateForm(campaignForm(campaignId = "../bad", targetAudience = "all_users"))
        viewModel.save()
        runCurrent()

        assertIs<AdminNotificationCampaignDraftMutationState.Error>(viewModel.mutationState.value)
        assertEquals(0, repository.upsertRequests.size)
    }

    @Test
    fun saveSubmitsParsedDraft() = runTest {
        val repository = FakeCampaignDraftsAdminRepository()
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.startCreate()
        viewModel.updateForm(
            campaignForm(
                campaignId = " summer-test ",
                title = " Oferta verão ",
                body = " Campanha apenas em rascunho ",
                targetAudience = "marketing_opt_in_users",
                scheduledAtIso = "2026-06-10T10:00:00.000Z",
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminNotificationCampaignDraftMutationState.Success>(viewModel.mutationState.value)
        val request = repository.upsertRequests.single()
        assertEquals("summer-test", request.campaignId)
        assertEquals("Oferta verão", request.title)
        assertEquals("Campanha apenas em rascunho", request.body)
        assertEquals("marketing_opt_in_users", request.targetAudience)
    }

    @Test
    fun saveStopsWhenSessionChangesBeforeRepositoryCall() = runTest {
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository()
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.startCreate()
        viewModel.updateForm(campaignForm())
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.upsertRequests.size)
        assertIs<AdminNotificationCampaignDraftsUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun archiveMapsPermissionFailureToNotAdmin() = runTest {
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = FakeCampaignDraftsAdminRepository(
                archiveResult = AdminNotificationCampaignDraftMutationResult.Failure(AdminError.Permission("denied")),
            ),
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.archive("summer-test")
        runCurrent()

        assertIs<AdminNotificationCampaignDraftsUiState.NotAdmin>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Error>(viewModel.mutationState.value)
    }

    @Test
    fun sendTestSubmitsCampaignDraftToSelf() = runTest {
        val repository = FakeCampaignDraftsAdminRepository()
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.sendTest("summer-test")
        runCurrent()

        val success = assertIs<AdminNotificationCampaignDraftMutationState.Success>(viewModel.mutationState.value)
        assertEquals("Teste de campanha em fila apenas para o administrador atual.", success.message)
        assertEquals("summer-test", repository.testRequests.single().campaignId)
    }

    @Test
    fun sendTestRejectsCampaignReceiptWithoutExplicitCurrentAdminSelfScope() = runTest {
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = FakeCampaignDraftsAdminRepository(
                testResult = AdminNotificationTestResult.Success(
                    AdminNotificationTestReceipt(
                        notificationId = "unsafe-notification",
                        templateKey = "campaign_draft",
                        campaignId = "summer-test",
                        deliveryState = "queued",
                        recipientUid = "uid-1",
                        message = "queued",
                    ),
                ),
            ),
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.sendTest("summer-test")
        runCurrent()

        val error = assertIs<AdminNotificationCampaignDraftMutationState.Error>(viewModel.mutationState.value)
        assertEquals(UnsafeAdminNotificationTestReceiptMessage, error.message)
        assertEquals(false, error.retryable)
    }

    @Test
    fun sendTestMapsPermissionFailureToNotAdmin() = runTest {
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = FakeCampaignDraftsAdminRepository(
                testResult = AdminNotificationTestResult.Failure(AdminError.Permission("denied")),
            ),
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.sendTest("summer-test")
        runCurrent()

        assertIs<AdminNotificationCampaignDraftsUiState.NotAdmin>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Error>(viewModel.mutationState.value)
    }

    @Test
    fun sendTestIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminNotificationTestResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = FakeCampaignDraftsAdminRepository(testResultDeferred = deferred),
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.sendTest("summer-test")
        runCurrent()
        authRepository.signOut()
        deferred.complete(notificationTestSuccess(campaignId = "summer-test"))
        runCurrent()

        assertIs<AdminNotificationCampaignDraftsUiState.Unauthenticated>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Idle>(viewModel.mutationState.value)
    }
}

private class FakeCampaignDraftsAdminRepository(
    var loadResult: AdminNotificationCampaignDraftsResult =
        AdminNotificationCampaignDraftsResult.Success(campaignDraftsConfig()),
    var upsertResult: AdminNotificationCampaignDraftMutationResult? = null,
    var archiveResult: AdminNotificationCampaignDraftMutationResult? = null,
    var testResult: AdminNotificationTestResult? = null,
    private val loadResultDeferred: CompletableDeferred<AdminNotificationCampaignDraftsResult>? = null,
    private val testResultDeferred: CompletableDeferred<AdminNotificationTestResult>? = null,
) : AdminRepository {
    var loadCalls = 0
        private set
    val upsertRequests = mutableListOf<AdminNotificationCampaignDraftMutationRequest>()
    val archiveRequests = mutableListOf<AdminNotificationCampaignDraftArchiveRequest>()
    val testRequests = mutableListOf<AdminNotificationTestRequest>()

    override suspend fun syncMyRole(): AdminRoleResult {
        return AdminRoleResult.Success(AdminRole(uid = "uid-1", email = "admin@example.com", role = "admin"))
    }

    override suspend fun getNotificationCampaignDrafts(): AdminNotificationCampaignDraftsResult {
        loadCalls += 1
        return loadResultDeferred?.await() ?: loadResult
    }

    override suspend fun upsertNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftMutationRequest,
    ): AdminNotificationCampaignDraftMutationResult {
        upsertRequests += request
        return upsertResult ?: AdminNotificationCampaignDraftMutationResult.Success(
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
    ): AdminNotificationCampaignDraftMutationResult {
        archiveRequests += request
        return archiveResult ?: AdminNotificationCampaignDraftMutationResult.Success(
            AdminNotificationCampaignDraftMutationReceipt(campaignId = request.campaignId, status = "archived"),
        )
    }

    override suspend fun sendNotificationTestToSelf(
        request: AdminNotificationTestRequest,
    ): AdminNotificationTestResult {
        testRequests += request
        return testResultDeferred?.await() ?: testResult ?: notificationTestSuccess(campaignId = request.campaignId)
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult =
        AdminBookingRequestsResult.Failure(AdminError.Backend("unused"))

    override suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult =
        AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))

    override suspend fun getAvailabilityConfiguration(): AdminAvailabilityResult =
        AdminAvailabilityResult.Failure(AdminError.Backend("unused"))

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult =
        AdminServiceCatalogResult.Failure(AdminError.Backend("unused"))

    override suspend fun getServiceExtrasConfiguration(): AdminServiceExtrasResult =
        AdminServiceExtrasResult.Failure(AdminError.Backend("unused"))

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult = AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))

    override suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
    ): AdminAvailabilityResult = AdminAvailabilityResult.Failure(AdminError.Backend("unused"))

    override suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
    ): AdminCapacityOverrideMutationResult = AdminCapacityOverrideMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
    ): AdminCapacityOverrideMutationResult = AdminCapacityOverrideMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun acceptBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult =
        AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))

    override suspend fun rejectBookingRequest(request: AdminBookingDecisionRequest): AdminBookingDecisionResult =
        AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))

    override suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
    ): AdminServiceCatalogMutationResult = AdminServiceCatalogMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
    ): AdminServiceCatalogMutationResult = AdminServiceCatalogMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
    ): AdminServiceExtraMutationResult = AdminServiceExtraMutationResult.Failure(AdminError.Backend("unused"))

    override suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
    ): AdminServiceExtraMutationResult = AdminServiceExtraMutationResult.Failure(AdminError.Backend("unused"))
}

private class FakeCampaignDraftsAuthRepository(authenticated: Boolean) : AuthRepository {
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
    override val sessionState = mutableSessionState

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

private fun campaignForm(
    campaignId: String = "summer-test",
    title: String = "Oferta verão",
    body: String = "Campanha apenas em rascunho",
    targetAudience: String = "test_users",
    scheduledAtIso: String = "",
): AdminNotificationCampaignDraftForm = AdminNotificationCampaignDraftForm(
    campaignId = campaignId,
    title = title,
    body = body,
    targetAudience = targetAudience,
    scheduledAtIso = scheduledAtIso,
)

private fun notificationTestSuccess(campaignId: String): AdminNotificationTestResult.Success =
    AdminNotificationTestResult.Success(
        AdminNotificationTestReceipt(
            notificationId = "test-notification-1",
            templateKey = "campaign_draft",
            campaignId = campaignId,
            deliveryState = "queued",
            recipientUid = "uid-1",
            message = "queued",
            targetScope = "self",
            testOnly = true,
        ),
    )

private fun campaignDraftsConfig(
    sendBlocked: Boolean = true,
    sendBlockedReason: String = "campaign-send-not-implemented",
): AdminNotificationCampaignDraftsConfig = AdminNotificationCampaignDraftsConfig(
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
            scheduledAtIso = "",
            notes = "QA",
            sendBlocked = sendBlocked,
            sendBlockedReason = sendBlockedReason,
            createdAtIso = "2026-06-01T10:00:00.000Z",
            updatedAtIso = "2026-06-01T11:30:00.000Z",
            createdByUid = "admin-cr",
            updatedByUid = "admin-updated-long-id",
        ),
    ),
)
