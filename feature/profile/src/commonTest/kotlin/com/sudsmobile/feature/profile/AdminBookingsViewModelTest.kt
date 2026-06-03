package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminAvailabilityResult
import com.sudsmobile.data.admin.AdminAvailabilityUpdateRequest
import com.sudsmobile.data.admin.AdminBookingDecisionReceipt
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingRequest
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
import com.sudsmobile.data.booking.BookingReservationExtra
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
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
class AdminBookingsViewModelTest {
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
    fun adminAccessSyncShowsAdminForAdminRole() = runTest {
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Success(adminRole(role = "admin")),
        )
        val viewModel = AdminAccessViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        val admin = assertIs<AdminAccessUiState.Admin>(viewModel.uiState.value)
        assertEquals("admin@example.com", admin.email)
        assertEquals("Administrador", admin.roleLabel)
        assertEquals(1, repository.syncRoleCalls)
    }

    @Test
    fun adminAccessSyncHidesNormalUsers() = runTest {
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Success(adminRole(role = "customer")),
        )
        val viewModel = AdminAccessViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun adminAccessSyncHidesMismatchedRolePayloads() = runTest {
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Success(adminRole(uid = "uid-2", role = "admin")),
        )
        val viewModel = AdminAccessViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun adminAccessRetryResyncsAfterBackendError() = runTest {
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Failure(AdminError.Unavailable("Rede indisponível.")),
        )
        val viewModel = AdminAccessViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        val error = assertIs<AdminAccessUiState.Error>(viewModel.uiState.value)
        assertEquals("Rede indisponível.", error.message)
        assertEquals(true, error.retryable)

        repository.roleResult = AdminRoleResult.Success(adminRole(role = "admin"))
        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.Admin>(viewModel.uiState.value)
        assertEquals(2, repository.syncRoleCalls)
    }

    @Test
    fun adminAccessLoadsAfterRestoreCompletes() = runTest {
        val authRepository = FakeAdminAuthRepository(
            authenticated = false,
            initialState = AuthSessionState.Restoring,
        )
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Success(adminRole(role = "admin")),
        )
        val viewModel = AdminAccessViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.Loading>(viewModel.uiState.value)

        authRepository.authenticate()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.Admin>(viewModel.uiState.value)
        assertEquals(1, repository.syncRoleCalls)
    }

    @Test
    fun adminAccessResyncsWhenSameUserSessionTokenChanges() = runTest {
        val authRepository = FakeAdminAuthRepository(authenticated = true)
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Success(adminRole(role = "admin")),
        )
        val viewModel = AdminAccessViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.Admin>(viewModel.uiState.value)
        assertEquals(1, repository.syncRoleCalls)

        repository.roleResult = AdminRoleResult.Success(adminRole(role = "customer"))
        authRepository.authenticate(tokenVersion = 2)
        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.NotAdmin>(viewModel.uiState.value)
        assertEquals(2, repository.syncRoleCalls)
    }

    @Test
    fun adminAccessStartsNewSyncAndIgnoresStaleResultWhenSameUserSessionTokenChanges() = runTest {
        val firstRoleResult = CompletableDeferred<AdminRoleResult>()
        val authRepository = FakeAdminAuthRepository(authenticated = true)
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Success(adminRole(role = "customer")),
            roleResultDeferred = firstRoleResult,
        )
        val viewModel = AdminAccessViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, repository.syncRoleCalls)

        authRepository.authenticate(tokenVersion = 2)
        viewModel.refreshForSession()
        runCurrent()

        assertEquals(2, repository.syncRoleCalls)
        assertIs<AdminAccessUiState.NotAdmin>(viewModel.uiState.value)

        firstRoleResult.complete(AdminRoleResult.Success(adminRole(role = "admin")))
        runCurrent()

        assertEquals(2, repository.syncRoleCalls)
        assertIs<AdminAccessUiState.NotAdmin>(viewModel.uiState.value)
    }

    @Test
    fun adminAccessStoresRefreshedSessionAfterRoleSync() = runTest {
        val authRepository = FakeAdminAuthRepository(authenticated = true)
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Success(adminRole(role = "admin")),
            onSyncRole = {
                authRepository.authenticate(tokenVersion = 2)
            },
        )
        val viewModel = AdminAccessViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.Admin>(viewModel.uiState.value)
        assertEquals(1, repository.syncRoleCalls)
    }

    @Test
    fun adminAccessForceRefreshResyncsLoadedAdminRole() = runTest {
        val repository = FakeAdminRepository(
            roleResult = AdminRoleResult.Success(adminRole(role = "admin")),
        )
        val viewModel = AdminAccessViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminAccessUiState.Admin>(viewModel.uiState.value)
        assertEquals(1, repository.syncRoleCalls)

        repository.roleResult = AdminRoleResult.Success(adminRole(role = "customer"))
        viewModel.refreshForSession(force = true)
        runCurrent()

        assertIs<AdminAccessUiState.NotAdmin>(viewModel.uiState.value)
        assertEquals(2, repository.syncRoleCalls)
    }

    @Test
    fun loadRequestsRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(listOf(adminBookingRequest())),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = false),
            adminRepository = repository,
        )

        viewModel.loadRequests()
        runCurrent()

        assertIs<AdminBookingsUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.pendingRequestCalls)
    }

    @Test
    fun loadRequestsBuildsPendingRequestCards() = runTest {
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(
                listOf(
                    adminBookingRequest(
                        id = "reservation-1",
                        customerName = "Ana Silva",
                        extras = listOf(BookingReservationExtra(id = "wax", name = "Cera", priceCents = 700)),
                    ),
                ),
            ),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadRequests()
        runCurrent()

        val loaded = assertIs<AdminBookingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals("reservation-1", loaded.pendingRequests.single().id)
        assertEquals("Ana Silva", loaded.pendingRequests.single().customerName)
        assertEquals("Pendente", loaded.pendingRequests.single().statusLabel)
        assertEquals("Cera", loaded.pendingRequests.single().extras.single().name)
        assertEquals("7,00 €", loaded.pendingRequests.single().extras.single().price)
    }

    @Test
    fun loadRequestsBuildsCompletableRequestCards() = runTest {
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(emptyList()),
            completableRequestsResult = AdminBookingRequestsResult.Success(
                listOf(
                    adminBookingRequest(
                        id = "reservation-2",
                        status = "confirmed",
                        acceptedAtIso = "2026-05-29T10:15:00.000Z",
                        acceptedByUid = "admin-uid",
                    ),
                ),
            ),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.loadRequests()
        runCurrent()

        val loaded = assertIs<AdminBookingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals(emptyList(), loaded.pendingRequests)
        assertEquals("reservation-2", loaded.completableRequests.single().id)
        assertEquals("Confirmada", loaded.completableRequests.single().statusLabel)
        assertEquals("Pronta a concluir", loaded.completableRequests.single().statusDetail)
        assertEquals(
            "Aceite em 29 de maio, 2026 às 10:15 por admin-uid",
            loaded.completableRequests.single().auditLabels.single(),
        )
    }

    @Test
    fun refreshForSessionReloadsWhenSameUserSessionTokenChanges() = runTest {
        val authRepository = FakeAdminAuthRepository(authenticated = true)
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(listOf(adminBookingRequest())),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadRequests()
        runCurrent()

        assertIs<AdminBookingsUiState.Loaded>(viewModel.uiState.value)

        repository.requestsResult = AdminBookingRequestsResult.Success(emptyList())
        authRepository.authenticate(tokenVersion = 2)
        viewModel.refreshForSession()
        runCurrent()

        assertEquals(2, repository.pendingRequestCalls)
        assertIs<AdminBookingsUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun loadRequestsIgnoresResultAfterSameUserSessionTokenChanges() = runTest {
        val pendingResult = CompletableDeferred<AdminBookingRequestsResult>()
        val authRepository = FakeAdminAuthRepository(authenticated = true)
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(emptyList()),
            completableRequestsResult = AdminBookingRequestsResult.Success(emptyList()),
            requestsResultDeferred = pendingResult,
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadRequests()
        runCurrent()

        authRepository.authenticate(tokenVersion = 2)
        pendingResult.complete(
            AdminBookingRequestsResult.Success(listOf(adminBookingRequest(id = "stale-reservation"))),
        )
        runCurrent()
        runCurrent()

        assertEquals(2, repository.pendingRequestCalls)
        assertIs<AdminBookingsUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun acceptRequestSendsDecisionAndReloadsRequests() = runTest {
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(emptyList()),
            acceptResult = AdminBookingDecisionResult.Success(decisionReceipt(status = "confirmed")),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
            bookingChangeNotifier = MutableBookingChangeNotifier(),
        )

        viewModel.acceptRequest(" reservation-1 ")
        runCurrent()

        assertEquals("reservation-1", repository.acceptRequests.single().reservationId)
        assertIs<AdminBookingDecisionUiState.Success>(viewModel.decisionState.value)
        assertIs<AdminBookingsUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun rejectRequestSendsOptionalReason() = runTest {
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(emptyList()),
            rejectResult = AdminBookingDecisionResult.Success(decisionReceipt(status = "rejected")),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
            bookingChangeNotifier = MutableBookingChangeNotifier(),
        )

        viewModel.rejectRequest("reservation-1", "Sem vaga operacional")
        runCurrent()

        assertEquals("reservation-1", repository.rejectRequests.single().reservationId)
        assertEquals("Sem vaga operacional", repository.rejectRequests.single().rejectionReason)
        assertIs<AdminBookingDecisionUiState.Success>(viewModel.decisionState.value)
    }

    @Test
    fun completeRequestSendsDecisionAndReloadsRequests() = runTest {
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(emptyList()),
            completableRequestsResult = AdminBookingRequestsResult.Success(emptyList()),
            completeResult = AdminBookingDecisionResult.Success(decisionReceipt(status = "completed")),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
            bookingChangeNotifier = MutableBookingChangeNotifier(),
        )

        viewModel.completeRequest(" reservation-1 ")
        runCurrent()

        assertEquals("reservation-1", repository.completeRequests.single().reservationId)
        val success = assertIs<AdminBookingDecisionUiState.Success>(viewModel.decisionState.value)
        assertEquals("Marcação concluída.", success.message)
        assertIs<AdminBookingsUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun completeRequestIgnoresResultAfterSignOut() = runTest {
        val completionResult = CompletableDeferred<AdminBookingDecisionResult>()
        val authRepository = FakeAdminAuthRepository(authenticated = true)
        val repository = FakeAdminRepository(
            completeResultDeferred = completionResult,
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.completeRequest("reservation-1")
        runCurrent()

        authRepository.signOut()
        completionResult.complete(AdminBookingDecisionResult.Success(decisionReceipt(status = "completed")))
        runCurrent()

        assertEquals(1, repository.completeRequests.size)
        assertIs<AdminBookingDecisionUiState.Idle>(viewModel.decisionState.value)
        assertIs<AdminBookingsUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun completeRequestIgnoresResultAfterSameUserSessionTokenChanges() = runTest {
        val completionResult = CompletableDeferred<AdminBookingDecisionResult>()
        val authRepository = FakeAdminAuthRepository(authenticated = true)
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(emptyList()),
            completableRequestsResult = AdminBookingRequestsResult.Success(emptyList()),
            completeResultDeferred = completionResult,
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.completeRequest("reservation-1")
        runCurrent()

        authRepository.authenticate(tokenVersion = 2)
        completionResult.complete(AdminBookingDecisionResult.Success(decisionReceipt(status = "completed")))
        runCurrent()
        runCurrent()

        assertEquals(1, repository.completeRequests.size)
        assertEquals(1, repository.pendingRequestCalls)
        assertIs<AdminBookingDecisionUiState.Idle>(viewModel.decisionState.value)
        assertIs<AdminBookingsUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun conflictDecisionErrorIsRetryableForAdminRefresh() = runTest {
        val repository = FakeAdminRepository(
            acceptResult = AdminBookingDecisionResult.Failure(
                AdminError.Conflict("A marcação já expirou."),
            ),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = FakeAdminAuthRepository(authenticated = true),
            adminRepository = repository,
        )

        viewModel.acceptRequest("reservation-1")
        runCurrent()

        val error = assertIs<AdminBookingDecisionUiState.Error>(viewModel.decisionState.value)
        assertEquals("A marcação já expirou.", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun sessionChangeClearsLoadedRequests() = runTest {
        val authRepository = FakeAdminAuthRepository(authenticated = true)
        val repository = FakeAdminRepository(
            requestsResult = AdminBookingRequestsResult.Success(listOf(adminBookingRequest())),
        )
        val viewModel = AdminBookingsViewModel(
            authRepository = authRepository,
            adminRepository = repository,
        )

        viewModel.loadRequests()
        runCurrent()
        assertIs<AdminBookingsUiState.Loaded>(viewModel.uiState.value)

        authRepository.signOut()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<AdminBookingsUiState.Unauthenticated>(viewModel.uiState.value)
    }
}

private class FakeAdminRepository(
    var roleResult: AdminRoleResult = AdminRoleResult.Success(adminRole(role = "admin")),
    var requestsResult: AdminBookingRequestsResult = AdminBookingRequestsResult.Success(emptyList()),
    var completableRequestsResult: AdminBookingRequestsResult = AdminBookingRequestsResult.Success(emptyList()),
    var acceptResult: AdminBookingDecisionResult = AdminBookingDecisionResult.Success(
        decisionReceipt(status = "confirmed"),
    ),
    var rejectResult: AdminBookingDecisionResult = AdminBookingDecisionResult.Success(
        decisionReceipt(status = "rejected"),
    ),
    var completeResult: AdminBookingDecisionResult = AdminBookingDecisionResult.Success(
        decisionReceipt(status = "completed"),
    ),
    private var roleResultDeferred: CompletableDeferred<AdminRoleResult>? = null,
    private var requestsResultDeferred: CompletableDeferred<AdminBookingRequestsResult>? = null,
    private val completeResultDeferred: CompletableDeferred<AdminBookingDecisionResult>? = null,
    private val onSyncRole: (() -> Unit)? = null,
) : AdminRepository {
    var syncRoleCalls = 0
        private set
    var pendingRequestCalls = 0
        private set
    val acceptRequests = mutableListOf<AdminBookingDecisionRequest>()
    val rejectRequests = mutableListOf<AdminBookingDecisionRequest>()
    val completeRequests = mutableListOf<AdminBookingDecisionRequest>()

    override suspend fun syncMyRole(): AdminRoleResult {
        syncRoleCalls += 1
        onSyncRole?.invoke()
        val deferred = roleResultDeferred
        if (deferred != null) {
            roleResultDeferred = null
            return deferred.await()
        }
        return roleResult
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult {
        pendingRequestCalls += 1
        val deferred = requestsResultDeferred
        if (deferred != null) {
            requestsResultDeferred = null
            return deferred.await()
        }
        return requestsResult
    }

    override suspend fun getCompletableBookingRequests(): AdminBookingRequestsResult {
        return completableRequestsResult
    }

    override suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        acceptRequests += request
        return acceptResult
    }

    override suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        rejectRequests += request
        return rejectResult
    }

    override suspend fun completeBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        completeRequests += request
        return completeResultDeferred?.await() ?: completeResult
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

private class FakeAdminAuthRepository(
    authenticated: Boolean,
    initialState: AuthSessionState? = null,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        initialState ?: if (authenticated) adminAuthenticatedSession() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    fun authenticate(uid: String = "uid-1", tokenVersion: Int = 1) {
        mutableSessionState.value = adminAuthenticatedSession(uid = uid, tokenVersion = tokenVersion)
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        authenticate()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        authenticate()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private fun adminAuthenticatedSession(
    uid: String = "uid-1",
    tokenVersion: Int = 1,
): AuthSessionState.Authenticated {
    return AuthSessionState.Authenticated(
        AuthSession(
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
        ),
    )
}

private fun adminRole(uid: String = "uid-1", role: String): AdminRole = AdminRole(
    uid = uid,
    email = "admin@example.com",
    role = role,
)

private fun decisionReceipt(status: String): AdminBookingDecisionReceipt = AdminBookingDecisionReceipt(
    reservationId = "reservation-1",
    reservationCode = "SS-0001",
    status = status,
)

private fun adminBookingRequest(
    id: String = "reservation-1",
    customerName: String = "Bruno Ribeiro",
    status: String = "pending",
    extras: List<BookingReservationExtra> = emptyList(),
    acceptedAtIso: String? = null,
    acceptedByUid: String = "",
): AdminBookingRequest = AdminBookingRequest(
    id = id,
    reservationCode = "SS-0001",
    customerName = customerName,
    customerEmail = "cliente@example.com",
    customerPhone = "+351 900 000 000",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = "2026-05-30T10:00:00.000Z",
    slotEndIso = "2026-05-30T11:00:00.000Z",
    status = status,
    paymentStatus = "pending",
    vehicleType = "passageiros",
    vehicleLabel = "BMW 320d",
    priceCents = 3200,
    extras = extras,
    notes = "Atenção às jantes.",
    createdAtIso = "2026-05-29T08:00:00.000Z",
    pendingExpiresAtIso = "2026-05-30T08:00:00.000Z",
    loyaltyRewardApplied = false,
    acceptedAtIso = acceptedAtIso,
    acceptedByUid = acceptedByUid,
)
