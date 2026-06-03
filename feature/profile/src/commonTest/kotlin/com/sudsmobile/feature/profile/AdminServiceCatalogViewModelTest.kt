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
import com.sudsmobile.data.admin.AdminServiceCatalogConfig
import com.sudsmobile.data.admin.AdminServiceCatalogItem
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
class AdminServiceCatalogViewModelTest {
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
    fun loadCatalogRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeServiceCatalogAdminRepository()
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = FakeServiceCatalogAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()

        assertIs<AdminServiceCatalogUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadCatalogMapsPermissionFailureToNotAdmin() = runTest {
        val repository = FakeServiceCatalogAdminRepository(
            loadResult = AdminServiceCatalogResult.Failure(AdminError.Permission("denied")),
        )
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = FakeServiceCatalogAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()

        assertIs<AdminServiceCatalogUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun loadCatalogIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminServiceCatalogResult>()
        val authRepository = FakeServiceCatalogAuthRepository(authenticated = true)
        val repository = FakeServiceCatalogAdminRepository(loadResultDeferred = deferred)
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminServiceCatalogResult.Success(adminServiceCatalogConfig()))
        runCurrent()

        assertIs<AdminServiceCatalogUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun loadCatalogBuildsLoadedServiceRows() = runTest {
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = FakeServiceCatalogAuthRepository(authenticated = true),
            adminRepository = FakeServiceCatalogAdminRepository(
                loadResult = AdminServiceCatalogResult.Success(
                    adminServiceCatalogConfig(
                        services = listOf(
                            adminServiceCatalogItem(
                                active = false,
                                passengerPriceCents = 3200,
                                updatedAtIso = "2026-06-01T11:30:00.000Z",
                                updatedByUid = "admin-service-catalog-updater",
                            ),
                        ),
                    ),
                ),
            ),
        )

        viewModel.loadCatalog()
        runCurrent()

        val loaded = assertIs<AdminServiceCatalogUiState.Loaded>(viewModel.uiState.value)
        val service = loaded.services.single()
        assertEquals("premium", service.id)
        assertEquals("32,00 €", service.passengerPriceLabel)
        assertEquals(false, service.active)
        assertEquals(
            listOf("Atualizado 2026-06-01 11:30 UTC por admin-se..."),
            service.auditLabels,
        )
    }

    @Test
    fun saveSendsServiceUpdateAndReloadsCatalog() = runTest {
        val repository = FakeServiceCatalogAdminRepository(
            loadResults = ArrayDeque(
                listOf(
                    AdminServiceCatalogResult.Success(adminServiceCatalogConfig()),
                    AdminServiceCatalogResult.Success(
                        adminServiceCatalogConfig(
                            services = listOf(adminServiceCatalogItem(name = "Lavagem Deluxe")),
                        ),
                    ),
                ),
            ),
            upsertResult = AdminServiceCatalogMutationResult.Success(
                AdminServiceCatalogMutationReceiptForTest.receipt(serviceId = "premium"),
            ),
        )
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = FakeServiceCatalogAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()
        viewModel.editService("premium")
        val loaded = assertIs<AdminServiceCatalogUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(
            loaded.form!!.copy(
                name = " Lavagem   Deluxe ",
                durationMinutes = "50",
                passengerPrice = "33,50",
                suvPrice = "35.00",
                active = true,
                sortOrder = "25",
            ),
        )
        viewModel.save()
        runCurrent()

        val request = repository.upsertRequests.single()
        assertEquals("premium", request.serviceId)
        assertEquals(" Lavagem   Deluxe ", request.name)
        assertEquals(50, request.durationMinutes)
        assertEquals(3350, request.passengerPriceCents)
        assertEquals(3500, request.suvPriceCents)
        assertEquals(25, request.sortOrder)
        assertIs<AdminServiceCatalogMutationState.Success>(viewModel.mutationState.value)
        val reloaded = assertIs<AdminServiceCatalogUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Lavagem Deluxe", reloaded.services.single().name)
    }

    @Test
    fun saveValidationErrorDoesNotCallRepository() = runTest {
        val repository = FakeServiceCatalogAdminRepository()
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = FakeServiceCatalogAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()
        viewModel.editService("premium")
        val loaded = assertIs<AdminServiceCatalogUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(loaded.form!!.copy(passengerPrice = "abc"))
        viewModel.save()
        runCurrent()

        assertEquals(0, repository.upsertRequests.size)
        assertIs<AdminServiceCatalogMutationState.Error>(viewModel.mutationState.value)
    }

    @Test
    fun saveRechecksSessionBeforeRepositoryCall() = runTest {
        val authRepository = FakeServiceCatalogAuthRepository(authenticated = true)
        val repository = FakeServiceCatalogAdminRepository()
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()
        viewModel.editService("premium")
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.upsertRequests.size)
        assertIs<AdminServiceCatalogUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveIgnoresStaleFailureAfterUserSwitchAndReloadsCurrentSession() = runTest {
        val deferred = CompletableDeferred<AdminServiceCatalogMutationResult>()
        val authRepository = FakeServiceCatalogAuthRepository(authenticated = true)
        val repository = FakeServiceCatalogAdminRepository(
            upsertResultDeferred = deferred,
            loadResults = ArrayDeque(
                listOf(
                    AdminServiceCatalogResult.Success(adminServiceCatalogConfig()),
                    AdminServiceCatalogResult.Success(
                        adminServiceCatalogConfig(
                            services = listOf(adminServiceCatalogItem(id = "standard", name = "Lavagem Standard")),
                        ),
                    ),
                ),
            ),
        )
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()
        viewModel.editService("premium")
        viewModel.save()
        runCurrent()
        authRepository.switchTo("uid-2")
        deferred.complete(AdminServiceCatalogMutationResult.Failure(AdminError.Permission("old admin denied")))
        runCurrent()

        assertEquals("premium", repository.upsertRequests.single().serviceId)
        assertEquals(2, repository.loadCalls)
        assertIs<AdminServiceCatalogMutationState.Idle>(viewModel.mutationState.value)
        val loaded = assertIs<AdminServiceCatalogUiState.Loaded>(viewModel.uiState.value)
        assertEquals("standard", loaded.services.single().id)
    }

    @Test
    fun archiveSendsArchiveRequestAndReloadsCatalog() = runTest {
        val repository = FakeServiceCatalogAdminRepository(
            loadResults = ArrayDeque(
                listOf(
                    AdminServiceCatalogResult.Success(adminServiceCatalogConfig()),
                    AdminServiceCatalogResult.Success(adminServiceCatalogConfig(services = emptyList())),
                ),
            ),
            archiveResult = AdminServiceCatalogMutationResult.Success(
                AdminServiceCatalogMutationReceiptForTest.receipt(serviceId = "premium", status = "archived"),
            ),
        )
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = FakeServiceCatalogAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()
        viewModel.archive(" premium ")
        runCurrent()

        assertEquals("premium", repository.archiveRequests.single().serviceId)
        assertIs<AdminServiceCatalogMutationState.Success>(viewModel.mutationState.value)
        assertIs<AdminServiceCatalogUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun archiveIgnoresStaleFailureAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminServiceCatalogMutationResult>()
        val authRepository = FakeServiceCatalogAuthRepository(authenticated = true)
        val repository = FakeServiceCatalogAdminRepository(archiveResultDeferred = deferred)
        val viewModel = AdminServiceCatalogViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadCatalog()
        runCurrent()
        viewModel.archive("premium")
        runCurrent()
        authRepository.signOut()
        deferred.complete(AdminServiceCatalogMutationResult.Failure(AdminError.Permission("old admin denied")))
        runCurrent()

        assertEquals("premium", repository.archiveRequests.single().serviceId)
        assertIs<AdminServiceCatalogMutationState.Idle>(viewModel.mutationState.value)
        assertIs<AdminServiceCatalogUiState.Unauthenticated>(viewModel.uiState.value)
    }
}

private class FakeServiceCatalogAdminRepository(
    var loadResult: AdminServiceCatalogResult = AdminServiceCatalogResult.Success(adminServiceCatalogConfig()),
    var upsertResult: AdminServiceCatalogMutationResult = AdminServiceCatalogMutationResult.Success(
        AdminServiceCatalogMutationReceiptForTest.receipt(serviceId = "premium"),
    ),
    var archiveResult: AdminServiceCatalogMutationResult = AdminServiceCatalogMutationResult.Success(
        AdminServiceCatalogMutationReceiptForTest.receipt(serviceId = "premium", status = "archived"),
    ),
    private val loadResultDeferred: CompletableDeferred<AdminServiceCatalogResult>? = null,
    private val loadResults: ArrayDeque<AdminServiceCatalogResult>? = null,
    private val upsertResultDeferred: CompletableDeferred<AdminServiceCatalogMutationResult>? = null,
    private val archiveResultDeferred: CompletableDeferred<AdminServiceCatalogMutationResult>? = null,
) : AdminRepository {
    var loadCalls = 0
        private set
    val upsertRequests = mutableListOf<AdminServiceCatalogMutationRequest>()
    val archiveRequests = mutableListOf<AdminServiceCatalogArchiveRequest>()

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
        loadCalls += 1
        return loadResultDeferred?.await() ?: loadResults?.removeFirstOrNull() ?: loadResult
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
        upsertRequests += request
        return upsertResultDeferred?.await() ?: upsertResult
    }

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
    ): AdminServiceCatalogMutationResult {
        archiveRequests += request
        return archiveResultDeferred?.await() ?: archiveResult
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

private class FakeServiceCatalogAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) serviceCatalogAuthenticatedSession() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }

    fun switchTo(uid: String) {
        mutableSessionState.value = serviceCatalogAuthenticatedSession(uid)
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        mutableSessionState.value = serviceCatalogAuthenticatedSession()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        mutableSessionState.value = serviceCatalogAuthenticatedSession()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }
}

private fun serviceCatalogAuthenticatedSession(uid: String = "uid-1"): AuthSessionState.Authenticated {
    return AuthSessionState.Authenticated(
        AuthSession(
            user = AuthUser(
                uid = uid,
                email = "admin@example.com",
                displayName = "Admin",
                phoneNumber = "",
            ),
            idToken = "id-token-$uid",
            refreshToken = "refresh-token-$uid",
            expiresInSeconds = 3600,
        ),
    )
}

private object AdminServiceCatalogMutationReceiptForTest {
    fun receipt(
        serviceId: String,
        status: String = "active",
    ): com.sudsmobile.data.admin.AdminServiceCatalogMutationReceipt {
        return com.sudsmobile.data.admin.AdminServiceCatalogMutationReceipt(
            serviceId = serviceId,
            status = status,
        )
    }
}

private fun adminServiceCatalogConfig(
    services: List<AdminServiceCatalogItem> = listOf(adminServiceCatalogItem()),
): AdminServiceCatalogConfig = AdminServiceCatalogConfig(services = services)

private fun adminServiceCatalogItem(
    id: String = "premium",
    name: String = "Lavagem Premium",
    active: Boolean = true,
    passengerPriceCents: Int = 3200,
    updatedAtIso: String = "",
    updatedByUid: String = "",
): AdminServiceCatalogItem = AdminServiceCatalogItem(
    id = id,
    name = name,
    description = "Lavagem detalhada",
    durationMinutes = 45,
    passengerPriceCents = passengerPriceCents,
    suvPriceCents = 3400,
    iconKey = "sparkles",
    popular = true,
    active = active,
    sortOrder = 20,
    updatedAtIso = updatedAtIso,
    updatedByUid = updatedByUid,
)
