package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminAvailabilityConfig
import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingRequestsResult
import com.sudsmobile.data.admin.AdminBusinessInfoResult
import com.sudsmobile.data.admin.AdminBusinessInfoUpdateRequest
import com.sudsmobile.data.admin.AdminBusinessOpeningHours
import com.sudsmobile.data.admin.AdminCapacityOverrideClearRequest
import com.sudsmobile.data.admin.AdminCapacityOverrideItem
import com.sudsmobile.data.admin.AdminCapacityOverrideMutationReceipt
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AdminAvailabilityViewModelTest {
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
        val repository = FakeAvailabilityAdminRepository()
        val viewModel = AdminAvailabilityViewModel(
            authRepository = FakeAvailabilityAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminAvailabilityUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadConfigurationMapsPermissionFailureToNotAdmin() = runTest {
        val viewModel = AdminAvailabilityViewModel(
            authRepository = FakeAvailabilityAuthRepository(authenticated = true),
            adminRepository = FakeAvailabilityAdminRepository(
                loadResult = AdminAvailabilityResult.Failure(AdminError.Permission("denied")),
            ),
        )

        viewModel.loadConfiguration()
        runCurrent()

        assertIs<AdminAvailabilityUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun loadConfigurationIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminAvailabilityResult>()
        val authRepository = FakeAvailabilityAuthRepository(authenticated = true)
        val repository = FakeAvailabilityAdminRepository(loadResultDeferred = deferred)
        val viewModel = AdminAvailabilityViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminAvailabilityResult.Success(adminAvailabilityConfig()))
        runCurrent()

        assertIs<AdminAvailabilityUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveValidatesBeforeRepositoryCall() = runTest {
        val repository = FakeAvailabilityAdminRepository()
        val viewModel = AdminAvailabilityViewModel(
            authRepository = FakeAvailabilityAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.updateForm(
            AdminAvailabilityForm(
                defaultMaxBookingsPerSlot = "abc",
                openingHoursText = "Segunda | 09:00 - 19:00",
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminAvailabilitySaveState.Error>(viewModel.saveState.value)
        assertEquals(0, repository.updateRequests.size)
    }

    @Test
    fun saveSubmitsParsedAvailabilityConfiguration() = runTest {
        val repository = FakeAvailabilityAdminRepository()
        val viewModel = AdminAvailabilityViewModel(
            authRepository = FakeAvailabilityAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.updateForm(
            AdminAvailabilityForm(
                defaultMaxBookingsPerSlot = "4",
                openingHoursText = "Segunda a Sexta | 09:00 - 13:00, 14:00 - 19:00\nDomingo | Encerrado | fechado",
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<AdminAvailabilitySaveState.Success>(viewModel.saveState.value)
        val request = repository.updateRequests.single()
        assertEquals(4, request.defaultMaxBookingsPerSlot)
        assertEquals("Segunda a Sexta", request.openingHours.first().dayLabel)
        assertEquals(true, request.openingHours.last().closed)
    }

    @Test
    fun saveStopsWhenSessionChangesBeforeRepositoryCall() = runTest {
        val authRepository = FakeAvailabilityAuthRepository(authenticated = true)
        val repository = FakeAvailabilityAdminRepository()
        val viewModel = AdminAvailabilityViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.updateRequests.size)
        assertIs<AdminAvailabilityUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveCapacityOverrideSubmitsParsedDateCapacityAndReloads() = runTest {
        val repository = FakeAvailabilityAdminRepository()
        val viewModel = AdminAvailabilityViewModel(
            authRepository = FakeAvailabilityAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        val loaded = assertIs<AdminAvailabilityUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(
            loaded.form.copy(
                overrideDate = "2026-06-10",
                overrideMaxBookingsPerSlot = "0",
            ),
        )
        viewModel.saveCapacityOverride()
        runCurrent()

        val request = repository.upsertCapacityOverrideRequests.single()
        assertEquals("2026-06-10", request.date)
        assertEquals(0, request.maxBookingsPerSlot)
        assertIs<AdminAvailabilitySaveState.Success>(viewModel.saveState.value)
        assertEquals(2, repository.loadCalls)
    }

    @Test
    fun clearCapacityOverrideSubmitsDateAndReloads() = runTest {
        val repository = FakeAvailabilityAdminRepository()
        val viewModel = AdminAvailabilityViewModel(
            authRepository = FakeAvailabilityAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadConfiguration()
        runCurrent()
        viewModel.clearCapacityOverride("2026-06-10")
        runCurrent()

        assertEquals("2026-06-10", repository.clearCapacityOverrideRequests.single().date)
        assertIs<AdminAvailabilitySaveState.Success>(viewModel.saveState.value)
        assertEquals(2, repository.loadCalls)
    }
}

private class FakeAvailabilityAdminRepository(
    var loadResult: AdminAvailabilityResult = AdminAvailabilityResult.Success(adminAvailabilityConfig()),
    var updateResult: AdminAvailabilityResult = AdminAvailabilityResult.Success(adminAvailabilityConfig()),
    private val loadResultDeferred: CompletableDeferred<AdminAvailabilityResult>? = null,
) : AdminRepository {
    var loadCalls = 0
        private set
    val updateRequests = mutableListOf<AdminAvailabilityUpdateRequest>()
    val upsertCapacityOverrideRequests = mutableListOf<AdminCapacityOverrideUpsertRequest>()
    val clearCapacityOverrideRequests = mutableListOf<AdminCapacityOverrideClearRequest>()

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

    override suspend fun updateAvailabilityConfiguration(
        request: AdminAvailabilityUpdateRequest,
    ): AdminAvailabilityResult {
        updateRequests += request
        return updateResult
    }

    override suspend fun upsertCapacityOverride(
        request: AdminCapacityOverrideUpsertRequest,
    ): AdminCapacityOverrideMutationResult {
        upsertCapacityOverrideRequests += request
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
    ): AdminCapacityOverrideMutationResult {
        clearCapacityOverrideRequests += request
        return AdminCapacityOverrideMutationResult.Success(
            AdminCapacityOverrideMutationReceipt(
                date = request.date,
                status = "cleared",
            ),
        )
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

private class FakeAvailabilityAuthRepository(authenticated: Boolean) : AuthRepository {
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

private fun adminAvailabilityConfig(
    defaultMaxBookingsPerSlot: Int = 2,
): AdminAvailabilityConfig = AdminAvailabilityConfig(
    defaultMaxBookingsPerSlot = defaultMaxBookingsPerSlot,
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
)
