package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingPolicyResult
import com.sudsmobile.data.admin.AdminBookingPolicyUpdateRequest
import com.sudsmobile.data.admin.AdminBookingRequestsResult
import com.sudsmobile.data.admin.AdminBusinessInfoResult
import com.sudsmobile.data.admin.AdminBusinessInfoUpdateRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideClearRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideMutationResult
import com.sudsmobile.data.admin.AdminCapacityOverrideUpsertRequest
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminLoyaltySettingsConfig
import com.sudsmobile.data.admin.AdminLoyaltySettingsResult
import com.sudsmobile.data.admin.AdminLoyaltySettingsUpdateRequest
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
class AdminLoyaltySettingsViewModelTest {
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
        val repository = FakeLoyaltySettingsAdminRepository()
        val viewModel = AdminLoyaltySettingsViewModel(
            authRepository = FakeLoyaltySettingsAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminLoyaltySettingsUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadConfigurationMapsPermissionFailureToNotAdmin() = runTest {
        val viewModel = AdminLoyaltySettingsViewModel(
            authRepository = FakeLoyaltySettingsAuthRepository(authenticated = true),
            adminRepository = FakeLoyaltySettingsAdminRepository(
                loadResult = AdminLoyaltySettingsResult.Failure(AdminError.Permission("denied")),
            ),
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminLoyaltySettingsUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun loadConfigurationMapsAuditMetadata() = runTest {
        val viewModel = AdminLoyaltySettingsViewModel(
            authRepository = FakeLoyaltySettingsAuthRepository(authenticated = true),
            adminRepository = FakeLoyaltySettingsAdminRepository(
                loadResult = AdminLoyaltySettingsResult.Success(
                    adminLoyaltySettingsConfig(
                        updatedAtIso = "2026-06-01T10:15:00.000Z",
                        updatedByUid = "admin-loyalty-updater",
                    ),
                ),
            ),
        )

        viewModel.loadConfiguration()
        runCurrent()

        val loaded = assertIs<AdminLoyaltySettingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Atualizado 2026-06-01 10:15 UTC por admin-lo...", loaded.form.updatedAuditLabel)
    }

    @Test
    fun loadConfigurationIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminLoyaltySettingsResult>()
        val authRepository = FakeLoyaltySettingsAuthRepository(authenticated = true)
        val viewModel = AdminLoyaltySettingsViewModel(
            authRepository = authRepository,
            adminRepository = FakeLoyaltySettingsAdminRepository(loadResultDeferred = deferred),
        )

        viewModel.loadConfiguration()
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminLoyaltySettingsResult.Success(adminLoyaltySettingsConfig()))
        runCurrent()

        assertIs<AdminLoyaltySettingsUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveValidatesBeforeRepositoryCall() = runTest {
        val repository = FakeLoyaltySettingsAdminRepository()
        val viewModel = AdminLoyaltySettingsViewModel(
            authRepository = FakeLoyaltySettingsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.updateForm(
            AdminLoyaltySettingsForm(
                stampsRequired = "0",
                rewardType = "discount_percent",
                rewardValue = "15",
                rewardDescription = "15% de desconto",
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminLoyaltySettingsSaveState.Error>(viewModel.saveState.value)
        assertEquals(0, repository.updateRequests.size)
    }

    @Test
    fun saveSubmitsParsedLoyaltySettings() = runTest {
        val repository = FakeLoyaltySettingsAdminRepository()
        val viewModel = AdminLoyaltySettingsViewModel(
            authRepository = FakeLoyaltySettingsAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.updateForm(
            AdminLoyaltySettingsForm(
                stampsRequired = "8",
                rewardType = "discount percent",
                rewardValue = "15",
                rewardDescription = "  15%   de desconto  ",
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminLoyaltySettingsSaveState.Success>(viewModel.saveState.value)
        val request = repository.updateRequests.single()
        assertEquals(8, request.stampsRequired)
        assertEquals("discount_percent", request.rewardType)
        assertEquals(15, request.rewardValue)
        assertEquals("15% de desconto", request.rewardDescription)
        val loaded = assertIs<AdminLoyaltySettingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Atualizado 2026-06-01 10:15 UTC por uid-1", loaded.form.updatedAuditLabel)
    }

    @Test
    fun saveStopsWhenSessionChangesBeforeRepositoryCall() = runTest {
        val authRepository = FakeLoyaltySettingsAuthRepository(authenticated = true)
        val repository = FakeLoyaltySettingsAdminRepository()
        val viewModel = AdminLoyaltySettingsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.updateRequests.size)
        assertIs<AdminLoyaltySettingsUiState.Unauthenticated>(viewModel.uiState.value)
    }
}

private class FakeLoyaltySettingsAdminRepository(
    var loadResult: AdminLoyaltySettingsResult = AdminLoyaltySettingsResult.Success(adminLoyaltySettingsConfig()),
    var updateResult: AdminLoyaltySettingsResult? = null,
    private val loadResultDeferred: CompletableDeferred<AdminLoyaltySettingsResult>? = null,
) : AdminRepository {
    var loadCalls = 0
        private set
    val updateRequests = mutableListOf<AdminLoyaltySettingsUpdateRequest>()

    override suspend fun syncMyRole(): AdminRoleResult {
        return AdminRoleResult.Success(AdminRole(uid = "uid-1", email = "admin@example.com", role = "admin"))
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult =
        AdminBookingRequestsResult.Failure(AdminError.Backend("unused"))

    override suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult =
        AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))

    override suspend fun getAvailabilityConfiguration(): AdminAvailabilityResult =
        AdminAvailabilityResult.Failure(AdminError.Backend("unused"))

    override suspend fun getBookingPolicyConfiguration(): AdminBookingPolicyResult =
        AdminBookingPolicyResult.Failure(AdminError.Backend("unused"))

    override suspend fun getLoyaltySettingsConfiguration(): AdminLoyaltySettingsResult {
        loadCalls += 1
        return loadResultDeferred?.await() ?: loadResult
    }

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult =
        AdminServiceCatalogResult.Failure(AdminError.Backend("unused"))

    override suspend fun getServiceExtrasConfiguration(): AdminServiceExtrasResult =
        AdminServiceExtrasResult.Failure(AdminError.Backend("unused"))

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult = AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))

    override suspend fun updateBookingPolicyConfiguration(
        request: AdminBookingPolicyUpdateRequest,
    ): AdminBookingPolicyResult = AdminBookingPolicyResult.Failure(AdminError.Backend("unused"))

    override suspend fun updateLoyaltySettingsConfiguration(
        request: AdminLoyaltySettingsUpdateRequest,
    ): AdminLoyaltySettingsResult {
        updateRequests += request
        return updateResult ?: AdminLoyaltySettingsResult.Success(
            AdminLoyaltySettingsConfig(
                stampsRequired = request.stampsRequired,
                rewardType = request.rewardType,
                rewardValue = request.rewardValue,
                rewardDescription = request.rewardDescription,
                updatedAtIso = "2026-06-01T10:15:00.000Z",
                updatedByUid = "uid-1",
            ),
        )
    }

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

private class FakeLoyaltySettingsAuthRepository(authenticated: Boolean) : AuthRepository {
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

    override suspend fun sendPasswordReset(email: String): AuthActionResult = AuthActionResult.Success

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private fun adminLoyaltySettingsConfig(
    updatedAtIso: String = "",
    updatedByUid: String = "",
): AdminLoyaltySettingsConfig = AdminLoyaltySettingsConfig(
    stampsRequired = 10,
    rewardType = "free_wash",
    rewardValue = 1,
    rewardDescription = "1 lavagem grátis",
    updatedAtIso = updatedAtIso,
    updatedByUid = updatedByUid,
)
