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
import com.sudsmobile.data.admin.AdminNotificationCampaignDeliverySummary
import com.sudsmobile.data.admin.AdminNotificationCampaignDraftArchiveRequest
import com.sudsmobile.data.admin.AdminNotificationCampaignBroadcastReceipt
import com.sudsmobile.data.admin.AdminNotificationCampaignBroadcastRequest
import com.sudsmobile.data.admin.AdminNotificationCampaignBroadcastResult
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
    fun loadDraftsPreservesReadyCampaignSendMetadata() = runTest {
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
        assertEquals(false, draft.sendBlocked)
        assertEquals("", draft.sendBlockedReason)
        assertEquals(false, draft.deliveryLocked)
        assertEquals("ready", draft.sendState)
        assertEquals("Pronta para envio", draft.sendStateLabel)
    }

    @Test
    fun loadDraftsMapsCampaignDeliveryOutcomes() = runTest {
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = FakeCampaignDraftsAdminRepository(
                loadResult = AdminNotificationCampaignDraftsResult.Success(
                    campaignDraftsConfig(
                        deliverySummary = AdminNotificationCampaignDeliverySummary(
                            totalCount = 12,
                            sentCount = 8,
                            failedCount = 1,
                            suppressedCount = 2,
                            pendingCount = 1,
                        ),
                    ),
                ),
            ),
        )

        viewModel.loadDrafts()
        runCurrent()

        val loaded = assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
        val draft = loaded.drafts.single()
        assertEquals(12, draft.deliveryTotalCount)
        assertEquals(8, draft.deliverySentCount)
        assertEquals(1, draft.deliveryFailedCount)
        assertEquals(2, draft.deliverySuppressedCount)
        assertEquals(1, draft.deliveryPendingCount)
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
    fun loadDraftsIgnoresStaleResponseAfterUserSwitchAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminNotificationCampaignDraftsResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository(loadResultDeferred = deferred)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        authRepository.authenticateAs("uid-2")
        deferred.complete(AdminNotificationCampaignDraftsResult.Failure(AdminError.Permission("denied")))
        runCurrent()

        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
    }

    @Test
    fun loadDraftsIgnoresStaleResponseAfterSameUserTokenRefreshAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminNotificationCampaignDraftsResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository(loadResultDeferred = deferred)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        authRepository.authenticateAs("uid-1", tokenVersion = 2)
        deferred.complete(AdminNotificationCampaignDraftsResult.Failure(AdminError.Permission("denied")))
        runCurrent()

        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
    }

    @Test
    fun refreshReloadsWhenSameUserSessionTokenChanges() = runTest {
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository()
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        repository.loadResult = AdminNotificationCampaignDraftsResult.Failure(AdminError.Permission("denied"))
        authRepository.authenticateAs("uid-1", tokenVersion = 2)
        viewModel.refreshForSession()
        runCurrent()

        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.NotAdmin>(viewModel.uiState.value)
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
        viewModel.updateForm(campaignForm(campaignId = "../bad", targetAudience = "marketing_opt_in_users"))
        viewModel.save()
        runCurrent()

        assertIs<AdminNotificationCampaignDraftMutationState.Error>(viewModel.mutationState.value)
        assertEquals(0, repository.upsertRequests.size)
    }

    @Test
    fun saveSubmitsTitleAndMessageWithGeneratedIdAndCustomerAudience() = runTest {
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
                title = " Oferta verão ",
                body = " Campanha para clientes ",
                campaignId = "",
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminNotificationCampaignDraftMutationState.Success>(viewModel.mutationState.value)
        val request = repository.upsertRequests.single()
        assertEquals("", request.campaignId)
        assertEquals("Oferta verão", request.title)
        assertEquals("Campanha para clientes", request.body)
        assertEquals("marketing_opt_in_users", request.targetAudience)
    }

    @Test
    fun loadSafelyScopesLegacyAllUsersDraftsToMarketingOptIn() = runTest {
        val repository = FakeCampaignDraftsAdminRepository(
            loadResult = AdminNotificationCampaignDraftsResult.Success(
                campaignDraftsConfig(targetAudience = "all_users"),
            ),
        )
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()

        val loaded = assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
        assertEquals("marketing_opt_in_users", loaded.drafts.single().targetAudience)
        assertEquals("Clientes com opt-in marketing", loaded.drafts.single().targetAudienceLabel)
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
    fun saveIgnoresStaleFailureAfterUserSwitchAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminNotificationCampaignDraftMutationResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository(upsertResultDeferred = deferred)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.startCreate()
        viewModel.updateForm(campaignForm())
        viewModel.save()
        runCurrent()
        authRepository.authenticateAs("uid-2")
        deferred.complete(AdminNotificationCampaignDraftMutationResult.Failure(AdminError.Permission("denied")))
        runCurrent()

        assertEquals(1, repository.upsertRequests.size)
        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Idle>(viewModel.mutationState.value)
    }

    @Test
    fun saveIgnoresStaleResponseAfterSameUserTokenRefreshAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminNotificationCampaignDraftMutationResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository(upsertResultDeferred = deferred)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.startCreate()
        viewModel.updateForm(campaignForm())
        viewModel.save()
        runCurrent()
        repository.loadResult = AdminNotificationCampaignDraftsResult.Failure(AdminError.Permission("denied"))
        authRepository.authenticateAs("uid-1", tokenVersion = 2)
        deferred.complete(AdminNotificationCampaignDraftMutationResult.Success(campaignDraftMutationReceipt()))
        runCurrent()

        assertEquals(1, repository.upsertRequests.size)
        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.NotAdmin>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Idle>(viewModel.mutationState.value)
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
    fun archiveIgnoresStaleFailureAfterUserSwitchAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminNotificationCampaignDraftMutationResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository(archiveResultDeferred = deferred)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.archive("summer-test")
        runCurrent()
        authRepository.authenticateAs("uid-2")
        deferred.complete(AdminNotificationCampaignDraftMutationResult.Failure(AdminError.Permission("denied")))
        runCurrent()

        assertEquals(1, repository.archiveRequests.size)
        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Idle>(viewModel.mutationState.value)
    }

    @Test
    fun archiveIgnoresStaleResponseAfterSameUserTokenRefreshAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminNotificationCampaignDraftMutationResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository(archiveResultDeferred = deferred)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.archive("summer-test")
        runCurrent()
        repository.loadResult = AdminNotificationCampaignDraftsResult.Failure(AdminError.Permission("denied"))
        authRepository.authenticateAs("uid-1", tokenVersion = 2)
        deferred.complete(campaignDraftMutationResultSuccess(status = "archived"))
        runCurrent()

        assertEquals(1, repository.archiveRequests.size)
        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.NotAdmin>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Idle>(viewModel.mutationState.value)
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
        assertEquals("Teste de campanha enviado apenas para o administrador atual.", success.message)
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

    @Test
    fun sendTestIgnoresStaleFailureAfterUserSwitchAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminNotificationTestResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository(testResultDeferred = deferred)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.sendTest("summer-test")
        runCurrent()
        authRepository.authenticateAs("uid-2")
        deferred.complete(AdminNotificationTestResult.Failure(AdminError.Permission("denied")))
        runCurrent()

        assertEquals(1, repository.testRequests.size)
        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.Loaded>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Idle>(viewModel.mutationState.value)
    }

    @Test
    fun sendTestIgnoresStaleResponseAfterSameUserTokenRefreshAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminNotificationTestResult>()
        val authRepository = FakeCampaignDraftsAuthRepository(authenticated = true)
        val repository = FakeCampaignDraftsAdminRepository(testResultDeferred = deferred)
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.sendTest("summer-test")
        runCurrent()
        repository.loadResult = AdminNotificationCampaignDraftsResult.Failure(AdminError.Permission("denied"))
        authRepository.authenticateAs("uid-1", tokenVersion = 2)
        deferred.complete(notificationTestSuccess(campaignId = "summer-test"))
        runCurrent()

        assertEquals(1, repository.testRequests.size)
        assertEquals(2, repository.loadCalls)
        assertIs<AdminNotificationCampaignDraftsUiState.NotAdmin>(viewModel.uiState.value)
        assertIs<AdminNotificationCampaignDraftMutationState.Idle>(viewModel.mutationState.value)
    }

    @Test
    fun broadcastSubmitsConfirmedCampaignSend() = runTest {
        val repository = FakeCampaignDraftsAdminRepository()
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.broadcast("summer-test")
        runCurrent()

        val success = assertIs<AdminNotificationCampaignDraftMutationState.Success>(viewModel.mutationState.value)
        assertEquals("Campanha em fila para 3 dispositivos.", success.message)
        assertEquals(
            AdminNotificationCampaignBroadcastRequest("summer-test", confirmBroadcast = true),
            repository.broadcastRequests.single(),
        )
    }

    @Test
    fun broadcastIgnoresLegacyDraftLockMetadata() = runTest {
        val repository = FakeCampaignDraftsAdminRepository(
            loadResult = AdminNotificationCampaignDraftsResult.Success(
                campaignDraftsConfig(sendBlocked = true, sendBlockedReason = "locked"),
            ),
        )
        val viewModel = AdminNotificationCampaignDraftsViewModel(
            authRepository = FakeCampaignDraftsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadDrafts()
        runCurrent()
        viewModel.broadcast("summer-test")
        runCurrent()

        val success = assertIs<AdminNotificationCampaignDraftMutationState.Success>(viewModel.mutationState.value)
        assertEquals("Campanha em fila para 3 dispositivos.", success.message)
        assertEquals(
            AdminNotificationCampaignBroadcastRequest("summer-test", confirmBroadcast = true),
            repository.broadcastRequests.single(),
        )
    }
}

private class FakeCampaignDraftsAdminRepository(
    var loadResult: AdminNotificationCampaignDraftsResult =
        AdminNotificationCampaignDraftsResult.Success(campaignDraftsConfig()),
    var upsertResult: AdminNotificationCampaignDraftMutationResult? = null,
    var archiveResult: AdminNotificationCampaignDraftMutationResult? = null,
    var testResult: AdminNotificationTestResult? = null,
    var broadcastResult: AdminNotificationCampaignBroadcastResult? = null,
    loadResultDeferred: CompletableDeferred<AdminNotificationCampaignDraftsResult>? = null,
    upsertResultDeferred: CompletableDeferred<AdminNotificationCampaignDraftMutationResult>? = null,
    archiveResultDeferred: CompletableDeferred<AdminNotificationCampaignDraftMutationResult>? = null,
    testResultDeferred: CompletableDeferred<AdminNotificationTestResult>? = null,
) : AdminRepository {
    private var pendingLoadResultDeferred = loadResultDeferred
    private var pendingUpsertResultDeferred = upsertResultDeferred
    private var pendingArchiveResultDeferred = archiveResultDeferred
    private var pendingTestResultDeferred = testResultDeferred
    var loadCalls = 0
        private set
    val upsertRequests = mutableListOf<AdminNotificationCampaignDraftMutationRequest>()
    val archiveRequests = mutableListOf<AdminNotificationCampaignDraftArchiveRequest>()
    val testRequests = mutableListOf<AdminNotificationTestRequest>()
    val broadcastRequests = mutableListOf<AdminNotificationCampaignBroadcastRequest>()

    override suspend fun syncMyRole(): AdminRoleResult {
        return AdminRoleResult.Success(AdminRole(uid = "uid-1", email = "admin@example.com", role = "admin"))
    }

    override suspend fun getNotificationCampaignDrafts(): AdminNotificationCampaignDraftsResult {
        loadCalls += 1
        val deferred = pendingLoadResultDeferred
        pendingLoadResultDeferred = null
        return deferred?.await() ?: loadResult
    }

    override suspend fun upsertNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftMutationRequest,
    ): AdminNotificationCampaignDraftMutationResult {
        upsertRequests += request
        val deferred = pendingUpsertResultDeferred
        pendingUpsertResultDeferred = null
        if (deferred != null) return deferred.await()
        return upsertResult ?: campaignDraftMutationResultSuccess(
            campaignId = request.campaignId.ifBlank { "generated-campaign" },
            created = request.campaignId.isBlank(),
            targetAudience = request.targetAudience,
        )
    }

    override suspend fun archiveNotificationCampaignDraft(
        request: AdminNotificationCampaignDraftArchiveRequest,
    ): AdminNotificationCampaignDraftMutationResult {
        archiveRequests += request
        val deferred = pendingArchiveResultDeferred
        pendingArchiveResultDeferred = null
        if (deferred != null) return deferred.await()
        return archiveResult ?: campaignDraftMutationResultSuccess(campaignId = request.campaignId, status = "archived")
    }

    override suspend fun sendNotificationTestToSelf(
        request: AdminNotificationTestRequest,
    ): AdminNotificationTestResult {
        testRequests += request
        val deferred = pendingTestResultDeferred
        pendingTestResultDeferred = null
        return deferred?.await() ?: testResult ?: notificationTestSuccess(campaignId = request.campaignId)
    }

    override suspend fun broadcastNotificationCampaign(
        request: AdminNotificationCampaignBroadcastRequest,
    ): AdminNotificationCampaignBroadcastResult {
        broadcastRequests += request
        return broadcastResult ?: campaignBroadcastSuccess(campaignId = request.campaignId)
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
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) AuthSessionState.Authenticated(authSession()) else AuthSessionState.Unauthenticated,
    )
    override val sessionState = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        val session = authSession()
        mutableSessionState.value = AuthSessionState.Authenticated(session)
        return AuthResult.Success(session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        val session = authSession()
        mutableSessionState.value = AuthSessionState.Authenticated(session)
        return AuthResult.Success(session)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }

    fun authenticateAs(uid: String, tokenVersion: Int = 1) {
        mutableSessionState.value = AuthSessionState.Authenticated(authSession(uid, tokenVersion))
    }

    private fun authSession(uid: String = "uid-1", tokenVersion: Int = 1): AuthSession {
        return AuthSession(
            user = AuthUser(
                uid = uid,
                email = "admin@example.com",
                displayName = "Admin",
                phoneNumber = "",
            ),
            idToken = "id-token-$uid-$tokenVersion",
            refreshToken = "refresh-token-$uid-$tokenVersion",
            expiresInSeconds = 3600,
            issuedAtEpochSeconds = tokenVersion.toLong(),
        )
    }
}

private fun campaignForm(
    campaignId: String = "summer-test",
    title: String = "Oferta verão",
    body: String = "Campanha apenas em rascunho",
    targetAudience: String = "marketing_opt_in_users",
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
            deliveryState = "sent",
            recipientUid = "uid-1",
            message = "queued",
            targetScope = "self",
            testOnly = true,
            targetAudience = "test_users",
            sendBlocked = true,
            sendBlockedReason = "campaign-send-not-implemented",
            deliveryLocked = true,
            sendState = "draft_only",
            tokenCount = 1,
            sentCount = 1,
        ),
    )

private fun campaignDraftMutationResultSuccess(
    campaignId: String = "summer-test",
    status: String = "draft",
    created: Boolean = false,
    targetAudience: String = "test_users",
): AdminNotificationCampaignDraftMutationResult.Success =
    AdminNotificationCampaignDraftMutationResult.Success(
        campaignDraftMutationReceipt(
            campaignId = campaignId,
            status = status,
            created = created,
            targetAudience = targetAudience,
        ),
    )

private fun campaignDraftMutationReceipt(
    campaignId: String = "summer-test",
    status: String = "draft",
    created: Boolean = false,
    targetAudience: String = "test_users",
): AdminNotificationCampaignDraftMutationReceipt =
    AdminNotificationCampaignDraftMutationReceipt(
        campaignId = campaignId,
        status = status,
        created = created,
        targetAudience = targetAudience,
        sendBlocked = status == "archived",
        sendBlockedReason = if (status == "archived") "campaign-send-not-implemented" else "",
        deliveryLocked = status == "archived",
        sendState = if (status == "archived") "archived" else "ready",
    )

private fun campaignBroadcastSuccess(campaignId: String): AdminNotificationCampaignBroadcastResult.Success =
    AdminNotificationCampaignBroadcastResult.Success(
        AdminNotificationCampaignBroadcastReceipt(
            campaignId = campaignId,
            status = "sent",
            targetAudience = "test_users",
            queuedCount = 3,
            sendBlocked = true,
            sendBlockedReason = "campaign-already-sent",
            deliveryLocked = true,
            sendState = "sent",
        ),
    )

private fun campaignDraftsConfig(
    sendBlocked: Boolean = false,
    sendBlockedReason: String = "",
    targetAudience: String = "test_users",
    deliverySummary: AdminNotificationCampaignDeliverySummary = AdminNotificationCampaignDeliverySummary(),
): AdminNotificationCampaignDraftsConfig = AdminNotificationCampaignDraftsConfig(
    source = "firestore",
    campaigns = listOf(
        AdminNotificationCampaignDraft(
            campaignId = "summer-test",
            title = "Oferta verão",
            body = "Campanha apenas em rascunho",
            targetAudience = targetAudience,
            channels = listOf("push"),
            marketingConsentRequired = false,
            status = "draft",
            scheduledAtIso = "",
            notes = "QA",
            sendBlocked = sendBlocked,
            sendBlockedReason = sendBlockedReason,
            deliveryLocked = sendBlocked,
            sendState = if (sendBlocked) "draft_only" else "ready",
            createdAtIso = "2026-06-01T10:00:00.000Z",
            updatedAtIso = "2026-06-01T11:30:00.000Z",
            createdByUid = "admin-cr",
            updatedByUid = "admin-updated-long-id",
            deliverySummary = deliverySummary,
        ),
    ),
)
