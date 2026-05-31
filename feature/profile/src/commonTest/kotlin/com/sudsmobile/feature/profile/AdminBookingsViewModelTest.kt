package com.sudsmobile.feature.profile

import com.sudsmobile.data.admin.AdminBookingDecisionReceipt
import com.sudsmobile.data.admin.AdminBookingDecisionRequest
import com.sudsmobile.data.admin.AdminBookingDecisionResult
import com.sudsmobile.data.admin.AdminBookingRequest
import com.sudsmobile.data.admin.AdminBookingRequestsResult
import com.sudsmobile.data.admin.AdminBusinessInfoResult
import com.sudsmobile.data.admin.AdminBusinessInfoUpdateRequest
import com.sudsmobile.data.admin.AdminError
import com.sudsmobile.data.admin.AdminRepository
import com.sudsmobile.data.admin.AdminRole
import com.sudsmobile.data.admin.AdminRoleResult
import com.sudsmobile.data.admin.AdminServiceCatalogArchiveRequest
import com.sudsmobile.data.admin.AdminServiceCatalogMutationRequest
import com.sudsmobile.data.admin.AdminServiceCatalogMutationResult
import com.sudsmobile.data.admin.AdminServiceCatalogResult
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

        assertIs<AdminAccessUiState.Admin>(viewModel.uiState.value)
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
        assertEquals("reservation-1", loaded.requests.single().id)
        assertEquals("Ana Silva", loaded.requests.single().customerName)
        assertEquals("Cera", loaded.requests.single().extras.single().name)
        assertEquals("7,00 €", loaded.requests.single().extras.single().price)
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
    var acceptResult: AdminBookingDecisionResult = AdminBookingDecisionResult.Success(
        decisionReceipt(status = "confirmed"),
    ),
    var rejectResult: AdminBookingDecisionResult = AdminBookingDecisionResult.Success(
        decisionReceipt(status = "rejected"),
    ),
) : AdminRepository {
    var syncRoleCalls = 0
        private set
    var pendingRequestCalls = 0
        private set
    val acceptRequests = mutableListOf<AdminBookingDecisionRequest>()
    val rejectRequests = mutableListOf<AdminBookingDecisionRequest>()

    override suspend fun syncMyRole(): AdminRoleResult {
        syncRoleCalls += 1
        return roleResult
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult {
        pendingRequestCalls += 1
        return requestsResult
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

    override suspend fun getBusinessInfoConfiguration(): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getServiceCatalogConfiguration(): AdminServiceCatalogResult {
        return AdminServiceCatalogResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
    ): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
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

    fun authenticate(uid: String = "uid-1") {
        mutableSessionState.value = adminAuthenticatedSession(uid)
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

private fun adminAuthenticatedSession(uid: String = "uid-1"): AuthSessionState.Authenticated {
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

private fun adminRole(role: String): AdminRole = AdminRole(
    uid = "uid-1",
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
    extras: List<BookingReservationExtra> = emptyList(),
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
    status = "pending",
    paymentStatus = "pending",
    vehicleType = "passageiros",
    vehicleLabel = "BMW 320d",
    priceCents = 3200,
    extras = extras,
    notes = "Atenção às jantes.",
    createdAtIso = "2026-05-29T08:00:00.000Z",
    pendingExpiresAtIso = "2026-05-30T08:00:00.000Z",
    loyaltyRewardApplied = false,
)
