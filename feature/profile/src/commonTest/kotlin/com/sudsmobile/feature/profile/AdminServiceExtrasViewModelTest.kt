package com.sudsmobile.feature.profile

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
import com.sudsmobile.data.admin.AdminServiceCatalogResult
import com.sudsmobile.data.admin.AdminServiceCatalogMutationRequest
import com.sudsmobile.data.admin.AdminServiceCatalogMutationResult
import com.sudsmobile.data.admin.AdminServiceExtraArchiveRequest
import com.sudsmobile.data.admin.AdminServiceExtraItem
import com.sudsmobile.data.admin.AdminServiceExtraMutationReceipt
import com.sudsmobile.data.admin.AdminServiceExtraMutationRequest
import com.sudsmobile.data.admin.AdminServiceExtraMutationResult
import com.sudsmobile.data.admin.AdminServiceExtrasConfig
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
class AdminServiceExtrasViewModelTest {
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
    fun loadExtrasRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeServiceExtrasAdminRepository()
        val viewModel = AdminServiceExtrasViewModel(
            authRepository = FakeServiceExtrasAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadExtras()
        runCurrent()

        assertIs<AdminServiceExtrasUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadExtrasMapsPermissionFailureToNotAdmin() = runTest {
        val viewModel = AdminServiceExtrasViewModel(
            authRepository = FakeServiceExtrasAuthRepository(authenticated = true),
            adminRepository = FakeServiceExtrasAdminRepository(
                loadResult = AdminServiceExtrasResult.Failure(AdminError.Permission("denied")),
            ),
        )

        viewModel.loadExtras()
        runCurrent()

        assertIs<AdminServiceExtrasUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun loadExtrasIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminServiceExtrasResult>()
        val authRepository = FakeServiceExtrasAuthRepository(authenticated = true)
        val viewModel = AdminServiceExtrasViewModel(
            authRepository = authRepository,
            adminRepository = FakeServiceExtrasAdminRepository(loadResultDeferred = deferred),
        )

        viewModel.loadExtras()
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminServiceExtrasResult.Success(adminServiceExtrasConfig()))
        runCurrent()

        assertIs<AdminServiceExtrasUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun loadExtrasBuildsLoadedRows() = runTest {
        val viewModel = AdminServiceExtrasViewModel(
            authRepository = FakeServiceExtrasAuthRepository(authenticated = true),
            adminRepository = FakeServiceExtrasAdminRepository(
                loadResult = AdminServiceExtrasResult.Success(
                    adminServiceExtrasConfig(
                        extras = listOf(
                            adminServiceExtraItem(
                                active = false,
                                priceCents = 1500,
                                updatedAtIso = "2026-06-01T11:30:00.000Z",
                                updatedByUid = "admin-service-extra-updater",
                            ),
                        ),
                    ),
                ),
            ),
        )

        viewModel.loadExtras()
        runCurrent()

        val loaded = assertIs<AdminServiceExtrasUiState.Loaded>(viewModel.uiState.value)
        val extra = loaded.extras.single()
        assertEquals("wax", extra.id)
        assertEquals("15,00 €", extra.priceLabel)
        assertEquals(false, extra.active)
        assertEquals(
            listOf("Atualizado 2026-06-01 11:30 UTC por admin-se..."),
            extra.auditLabels,
        )
    }

    @Test
    fun saveSendsExtraUpdateAndReloadsExtras() = runTest {
        val repository = FakeServiceExtrasAdminRepository(
            loadResults = ArrayDeque(
                listOf(
                    AdminServiceExtrasResult.Success(adminServiceExtrasConfig()),
                    AdminServiceExtrasResult.Success(
                        adminServiceExtrasConfig(extras = listOf(adminServiceExtraItem(name = "Cera Deluxe"))),
                    ),
                ),
            ),
        )
        val viewModel = AdminServiceExtrasViewModel(
            authRepository = FakeServiceExtrasAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadExtras()
        runCurrent()
        viewModel.editExtra("wax")
        val loaded = assertIs<AdminServiceExtrasUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(
            loaded.form!!.copy(
                name = " Cera   Deluxe ",
                price = "17,50",
                eligibleServiceIds = " premium, standard, premium ",
                sortOrder = "35",
            ),
        )
        viewModel.save()
        runCurrent()

        val request = repository.upsertRequests.single()
        assertEquals("wax", request.extraId)
        assertEquals(" Cera   Deluxe ", request.name)
        assertEquals(1750, request.priceCents)
        assertEquals(listOf("premium", "standard"), request.eligibleServiceIds)
        assertEquals(35, request.sortOrder)
        assertIs<AdminServiceExtrasMutationState.Success>(viewModel.mutationState.value)
        val reloaded = assertIs<AdminServiceExtrasUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Cera Deluxe", reloaded.extras.single().name)
    }

    @Test
    fun saveValidationErrorDoesNotCallRepository() = runTest {
        val repository = FakeServiceExtrasAdminRepository()
        val viewModel = AdminServiceExtrasViewModel(
            authRepository = FakeServiceExtrasAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadExtras()
        runCurrent()
        viewModel.editExtra("wax")
        val loaded = assertIs<AdminServiceExtrasUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(loaded.form!!.copy(price = "abc"))
        viewModel.save()
        runCurrent()

        assertEquals(0, repository.upsertRequests.size)
        assertIs<AdminServiceExtrasMutationState.Error>(viewModel.mutationState.value)
    }

    @Test
    fun saveRechecksSessionBeforeRepositoryCall() = runTest {
        val authRepository = FakeServiceExtrasAuthRepository(authenticated = true)
        val repository = FakeServiceExtrasAdminRepository()
        val viewModel = AdminServiceExtrasViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadExtras()
        runCurrent()
        viewModel.editExtra("wax")
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.upsertRequests.size)
        assertIs<AdminServiceExtrasUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun archiveSendsArchiveRequestAndReloadsExtras() = runTest {
        val repository = FakeServiceExtrasAdminRepository(
            loadResults = ArrayDeque(
                listOf(
                    AdminServiceExtrasResult.Success(adminServiceExtrasConfig()),
                    AdminServiceExtrasResult.Success(adminServiceExtrasConfig(extras = emptyList())),
                ),
            ),
        )
        val viewModel = AdminServiceExtrasViewModel(
            authRepository = FakeServiceExtrasAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadExtras()
        runCurrent()
        viewModel.archive(" wax ")
        runCurrent()

        assertEquals("wax", repository.archiveRequests.single().extraId)
        assertIs<AdminServiceExtrasMutationState.Success>(viewModel.mutationState.value)
        assertIs<AdminServiceExtrasUiState.Empty>(viewModel.uiState.value)
    }
}

private class FakeServiceExtrasAdminRepository(
    var loadResult: AdminServiceExtrasResult = AdminServiceExtrasResult.Success(adminServiceExtrasConfig()),
    var upsertResult: AdminServiceExtraMutationResult = AdminServiceExtraMutationResult.Success(
        AdminServiceExtraMutationReceipt(extraId = "wax", status = "active"),
    ),
    var archiveResult: AdminServiceExtraMutationResult = AdminServiceExtraMutationResult.Success(
        AdminServiceExtraMutationReceipt(extraId = "wax", status = "archived"),
    ),
    private val loadResultDeferred: CompletableDeferred<AdminServiceExtrasResult>? = null,
    private val loadResults: ArrayDeque<AdminServiceExtrasResult>? = null,
) : AdminRepository {
    var loadCalls = 0
        private set
    val upsertRequests = mutableListOf<AdminServiceExtraMutationRequest>()
    val archiveRequests = mutableListOf<AdminServiceExtraArchiveRequest>()

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

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult {
        return AdminServiceCatalogResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getServiceExtrasConfiguration(): AdminServiceExtrasResult {
        loadCalls += 1
        return loadResultDeferred?.await() ?: loadResults?.removeFirstOrNull() ?: loadResult
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
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
        upsertRequests += request
        return upsertResult
    }

    override suspend fun archiveServiceExtra(
        request: AdminServiceExtraArchiveRequest,
    ): AdminServiceExtraMutationResult {
        archiveRequests += request
        return archiveResult
    }
}

private class FakeServiceExtrasAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) serviceExtrasAuthenticatedSession() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        mutableSessionState.value = serviceExtrasAuthenticatedSession()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        mutableSessionState.value = serviceExtrasAuthenticatedSession()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }
}

private fun serviceExtrasAuthenticatedSession(): AuthSessionState.Authenticated {
    return AuthSessionState.Authenticated(
        AuthSession(
            user = AuthUser(
                uid = "uid-1",
                email = "admin@example.com",
                displayName = "Admin",
                phoneNumber = "",
            ),
            idToken = "id-token-uid-1",
            refreshToken = "refresh-token-uid-1",
            expiresInSeconds = 3600,
        ),
    )
}

private fun adminServiceExtrasConfig(
    extras: List<AdminServiceExtraItem> = listOf(adminServiceExtraItem()),
): AdminServiceExtrasConfig = AdminServiceExtrasConfig(extras = extras)

private fun adminServiceExtraItem(
    id: String = "wax",
    name: String = "Enceramento",
    active: Boolean = true,
    priceCents: Int = 1500,
    updatedAtIso: String = "",
    updatedByUid: String = "",
): AdminServiceExtraItem = AdminServiceExtraItem(
    id = id,
    name = name,
    description = "Proteção extra",
    priceCents = priceCents,
    iconKey = "shield",
    eligibleServiceIds = listOf("premium"),
    active = active,
    sortOrder = 30,
    updatedAtIso = updatedAtIso,
    updatedByUid = updatedByUid,
)
