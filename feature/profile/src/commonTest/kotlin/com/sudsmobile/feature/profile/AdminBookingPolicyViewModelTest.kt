package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminBookingPolicyConfig
import com.sudsmobile.data.admin.AdminBookingPolicyResult
import com.sudsmobile.data.admin.AdminBookingPolicyUpdateRequest
import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingRequestsResult
import com.sudsmobile.data.admin.AdminBusinessInfoResult
import com.sudsmobile.data.admin.AdminBusinessInfoUpdateRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideClearRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideMutationResult
import com.sudsmobile.data.admin.AdminCapacityOverrideUpsertRequest
import com.sudsmobile.data.admin.AdminError
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
class AdminBookingPolicyViewModelTest {
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
    fun loadConfigurationRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeBookingPolicyAdminRepository()
        val viewModel = AdminBookingPolicyViewModel(
            authRepository = FakeBookingPolicyAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminBookingPolicyUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadConfigurationMapsPermissionFailureToNotAdmin() = runTest {
        val viewModel = AdminBookingPolicyViewModel(
            authRepository = FakeBookingPolicyAuthRepository(authenticated = true),
            adminRepository = FakeBookingPolicyAdminRepository(
                loadResult = AdminBookingPolicyResult.Failure(AdminError.Permission("denied")),
            ),
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminBookingPolicyUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun loadConfigurationFormatsAuditLabel() = runTest {
        val viewModel = AdminBookingPolicyViewModel(
            authRepository = FakeBookingPolicyAuthRepository(authenticated = true),
            adminRepository = FakeBookingPolicyAdminRepository(
                loadResult = AdminBookingPolicyResult.Success(
                    adminBookingPolicyConfig(
                        updatedAtIso = "2026-06-01T10:15:00.000Z",
                        updatedByUid = "administrator-user-1",
                    ),
                ),
            ),
        )

        viewModel.loadConfiguration()
        runCurrent()

        val loaded = assertIs<AdminBookingPolicyUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Atualizado 2026-06-01 10:15 UTC por administ...", loaded.form.updatedAuditLabel)
    }

    @Test
    fun loadConfigurationIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminBookingPolicyResult>()
        val authRepository = FakeBookingPolicyAuthRepository(authenticated = true)
        val repository = FakeBookingPolicyAdminRepository(loadResultDeferred = deferred)
        val viewModel = AdminBookingPolicyViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminBookingPolicyResult.Success(adminBookingPolicyConfig()))
        runCurrent()

        assertIs<AdminBookingPolicyUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveValidatesBeforeRepositoryCall() = runTest {
        val repository = FakeBookingPolicyAdminRepository()
        val viewModel = AdminBookingPolicyViewModel(
            authRepository = FakeBookingPolicyAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.updateForm(
            AdminBookingPolicyForm(
                pendingHoldMinutes = "5",
                cancellationWindowMinutes = "120",
                rescheduleWindowMinutes = "60",
                paymentEligibilityCopy = "Pagamento no local",
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminBookingPolicySaveState.Error>(viewModel.saveState.value)
        assertEquals(0, repository.updateRequests.size)
    }

    @Test
    fun saveSubmitsParsedBookingPolicy() = runTest {
        val repository = FakeBookingPolicyAdminRepository()
        val viewModel = AdminBookingPolicyViewModel(
            authRepository = FakeBookingPolicyAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.updateForm(
            AdminBookingPolicyForm(
                pendingHoldMinutes = "240",
                cancellationWindowMinutes = "120",
                rescheduleWindowMinutes = "60",
                paymentEligibilityCopy = "  Pagamento   no local  ",
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminBookingPolicySaveState.Success>(viewModel.saveState.value)
        val request = repository.updateRequests.single()
        assertEquals(240, request.pendingHoldMinutes)
        assertEquals(120, request.cancellationWindowMinutes)
        assertEquals(60, request.rescheduleWindowMinutes)
        assertEquals("Pagamento no local", request.paymentEligibilityCopy)
    }

    @Test
    fun saveStopsWhenSessionChangesBeforeRepositoryCall() = runTest {
        val authRepository = FakeBookingPolicyAuthRepository(authenticated = true)
        val repository = FakeBookingPolicyAdminRepository()
        val viewModel = AdminBookingPolicyViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.updateRequests.size)
        assertIs<AdminBookingPolicyUiState.Unauthenticated>(viewModel.uiState.value)
    }
}

private class FakeBookingPolicyAdminRepository(
    var loadResult: AdminBookingPolicyResult = AdminBookingPolicyResult.Success(adminBookingPolicyConfig()),
    var updateResult: AdminBookingPolicyResult? = null,
    private val loadResultDeferred: CompletableDeferred<AdminBookingPolicyResult>? = null,
) : AdminRepository {
    var loadCalls = 0
        private set
    val updateRequests = mutableListOf<AdminBookingPolicyUpdateRequest>()

    override suspend fun syncMyRole(): AdminRoleResult {
        return AdminRoleResult.Success(AdminRole(uid = "uid-1", email = "admin@example.com", role = "admin"))
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult {
        return AdminBookingRequestsResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getAvailabilityConfiguration(): AdminAvailabilityResult {
        return AdminAvailabilityResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getBookingPolicyConfiguration(): AdminBookingPolicyResult {
        loadCalls += 1
        return loadResultDeferred?.await() ?: loadResult
    }

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult {
        return AdminServiceCatalogResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getServiceExtrasConfiguration(): AdminServiceExtrasResult {
        return AdminServiceExtrasResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun updateBookingPolicyConfiguration(
        request: AdminBookingPolicyUpdateRequest,
    ): AdminBookingPolicyResult {
        updateRequests += request
        return updateResult ?: AdminBookingPolicyResult.Success(
            AdminBookingPolicyConfig(
                pendingHoldMinutes = request.pendingHoldMinutes,
                cancellationWindowMinutes = request.cancellationWindowMinutes,
                rescheduleWindowMinutes = request.rescheduleWindowMinutes,
                paymentEligibilityCopy = request.paymentEligibilityCopy,
            ),
        )
    }

    override suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
    ): AdminAvailabilityResult {
        return AdminAvailabilityResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
    ): AdminCapacityOverrideMutationResult {
        return AdminCapacityOverrideMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun clearCapacityOverride(
        request: AdminCapacityOverrideClearRequest,
    ): AdminCapacityOverrideMutationResult {
        return AdminCapacityOverrideMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        return AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        return AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
    ): AdminServiceCatalogMutationResult {
        return AdminServiceCatalogMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
    ): AdminServiceCatalogMutationResult {
        return AdminServiceCatalogMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun upsertServiceExtra(
        request: AdminServiceExtraMutationRequest,
    ): AdminServiceExtraMutationResult {
        return AdminServiceExtraMutationResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
    ): AdminServiceExtraMutationResult {
        return AdminServiceExtraMutationResult.Failure(AdminError.Backend("unused"))
    }
}

private class FakeBookingPolicyAuthRepository(authenticated: Boolean) : AuthRepository {
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
    override val sessionState: MutableStateFlow<AuthSessionState> = MutableStateFlow(
        if (authenticated) AuthSessionState.Authenticated(authSession) else AuthSessionState.Unauthenticated,
    )

    override suspend fun currentSession(): AuthSession? {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override fun signOut() {
        sessionState.value = AuthSessionState.Unauthenticated
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        error("unused")
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        error("unused")
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        error("unused")
    }
}

private fun adminBookingPolicyConfig(
    updatedAtIso: String = "",
    updatedByUid: String = "",
): AdminBookingPolicyConfig = AdminBookingPolicyConfig(
    pendingHoldMinutes = 1440,
    cancellationWindowMinutes = 0,
    rescheduleWindowMinutes = 0,
    paymentEligibilityCopy = "Pagamento no local",
    updatedAtIso = updatedAtIso,
    updatedByUid = updatedByUid,
)
