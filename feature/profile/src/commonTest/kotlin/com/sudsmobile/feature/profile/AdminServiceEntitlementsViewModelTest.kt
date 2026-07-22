package com.sudsmobile.feature.profile

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.catalog.ServiceCatalog
import com.sudsmobile.data.catalog.ServiceCatalogRepository
import com.sudsmobile.data.catalog.ServiceCatalogResult
import com.sudsmobile.data.catalog.ServiceCatalogService
import com.sudsmobile.data.entitlement.AdminEntitlementCustomer
import com.sudsmobile.data.entitlement.AdminServiceEntitlementList
import com.sudsmobile.data.entitlement.AdminServiceEntitlementListResult
import com.sudsmobile.data.entitlement.AdjustServiceEntitlementUsageRequest
import com.sudsmobile.data.entitlement.IssueServiceEntitlementRequest
import com.sudsmobile.data.entitlement.RevokeServiceEntitlementRequest
import com.sudsmobile.data.entitlement.ServiceEntitlement
import com.sudsmobile.data.entitlement.ServiceEntitlementListResult
import com.sudsmobile.data.entitlement.ServiceEntitlementMutationResult
import com.sudsmobile.data.entitlement.ServiceEntitlementRepository
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
class AdminServiceEntitlementsViewModelTest {
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
    fun refreshLoadsCatalogAndPreselectsActiveServices() = runTest {
        val viewModel = adminEntitlementsViewModel()

        viewModel.refreshForSession()
        runCurrent()

        val loaded = assertIs<AdminServiceEntitlementsUiState.Loaded>(viewModel.uiState.value)
        assertEquals(setOf("standard"), loaded.form.selectedServiceIds)
        assertEquals("Lavagem Standard", loaded.services.single().name)
    }

    @Test
    fun findCustomerThenIssueSendsParsedStaffSale() = runTest {
        val repository = AdminEntitlementRepository()
        val viewModel = adminEntitlementsViewModel(repository = repository)
        viewModel.refreshForSession()
        runCurrent()
        val initial = assertIs<AdminServiceEntitlementsUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(initial.form.copy(customerEmail = "client@example.com"))
        viewModel.findCustomer()
        runCurrent()
        val found = assertIs<AdminServiceEntitlementsUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(found.form.copy(amountPaidEuros = "49,90"))

        viewModel.issue()
        runCurrent()

        val request = repository.issueRequests.single()
        assertEquals(4990, request.amountPaidCents)
        assertEquals("client@example.com", request.customerEmail)
        assertEquals(listOf("standard"), request.eligibleServiceIds)
        assertIs<AdminServiceEntitlementActionState.Success>(viewModel.actionState.value)
    }

    @Test
    fun customerLookupResponseIsIgnoredAfterSignOut() = runTest {
        val deferred = CompletableDeferred<AdminServiceEntitlementListResult>()
        val auth = AdminEntitlementAuthRepository()
        val viewModel = adminEntitlementsViewModel(auth, AdminEntitlementRepository(adminLookup = deferred))
        viewModel.refreshForSession()
        runCurrent()
        val loaded = assertIs<AdminServiceEntitlementsUiState.Loaded>(viewModel.uiState.value)
        viewModel.updateForm(loaded.form.copy(customerEmail = "client@example.com"))
        viewModel.findCustomer()
        runCurrent()

        auth.signOut()
        deferred.complete(adminEntitlementList())
        runCurrent()

        assertIs<AdminServiceEntitlementsUiState.Unauthenticated>(viewModel.uiState.value)
        assertIs<AdminServiceEntitlementActionState.Idle>(viewModel.actionState.value)
    }
}

private fun adminEntitlementsViewModel(
    auth: AdminEntitlementAuthRepository = AdminEntitlementAuthRepository(),
    repository: AdminEntitlementRepository = AdminEntitlementRepository(),
) = AdminServiceEntitlementsViewModel(auth, AdminEntitlementCatalogRepository(), repository)

private class AdminEntitlementCatalogRepository : ServiceCatalogRepository {
    override suspend fun getServiceCatalog(): ServiceCatalogResult = ServiceCatalogResult.Success(
        ServiceCatalog(
            listOf(
                ServiceCatalogService(
                    id = "standard",
                    name = "Lavagem Standard",
                    description = "Exterior e interior",
                    durationMinutes = 60,
                    passengerPriceCents = 2500,
                    suvPriceCents = 3000,
                    iconKey = "car",
                    popular = true,
                ),
            ),
        ),
    )
}

private class AdminEntitlementRepository(
    private val adminLookup: CompletableDeferred<AdminServiceEntitlementListResult>? = null,
) : ServiceEntitlementRepository {
    val issueRequests = mutableListOf<IssueServiceEntitlementRequest>()

    override suspend fun getMyEntitlements(): ServiceEntitlementListResult = error("Not used")
    override suspend fun getAdminEntitlements(customerEmail: String): AdminServiceEntitlementListResult =
        adminLookup?.await() ?: adminEntitlementList()

    override suspend fun issueEntitlement(request: IssueServiceEntitlementRequest): ServiceEntitlementMutationResult {
        issueRequests += request
        return ServiceEntitlementMutationResult.Success(adminEntitlement())
    }

    override suspend fun adjustUsage(request: AdjustServiceEntitlementUsageRequest): ServiceEntitlementMutationResult =
        error("Not used")
    override suspend fun revokeEntitlement(request: RevokeServiceEntitlementRequest): ServiceEntitlementMutationResult =
        error("Not used")
}

private class AdminEntitlementAuthRepository : AuthRepository {
    private val state = MutableStateFlow<AuthSessionState>(
        AuthSessionState.Authenticated(
            AuthSession(AuthUser("admin-1", "admin@example.com", "Admin", ""), "token", "refresh", 3600),
        ),
    )
    override val sessionState: StateFlow<AuthSessionState> = state
    override suspend fun currentSession(): AuthSession? = (state.value as? AuthSessionState.Authenticated)?.session
    override suspend fun signIn(email: String, password: String): AuthResult = error("Not used")
    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult = error("Not used")
    override suspend fun sendPasswordReset(email: String): AuthActionResult = error("Not used")
    override fun signOut() {
        state.value = AuthSessionState.Unauthenticated
    }
}

private fun adminEntitlementList() = AdminServiceEntitlementListResult.Success(
    AdminServiceEntitlementList(
        customer = AdminEntitlementCustomer("client-1", "client@example.com", "Client"),
        entitlements = emptyList(),
        purchaseMode = "staff_issued",
        onlinePurchaseAvailable = false,
    ),
)

private fun adminEntitlement() = ServiceEntitlement(
    id = "issue-1",
    code = "SS-PLAN-ABC123",
    kind = "package",
    name = "Pacote 5 lavagens",
    status = "active",
    totalUses = 5,
    usedUses = 0,
    remainingUses = 5,
    eligibleServiceIds = listOf("standard"),
    eligibleServiceNames = listOf("Lavagem Standard"),
    validFromIso = "2026-07-22T00:00:00.000Z",
    validUntilIso = "2027-01-18T00:00:00.000Z",
    amountPaidCents = 4990,
    purchaseMode = "staff_issued",
    onlinePurchaseAvailable = false,
    createdAtIso = "2026-07-22T00:00:00.000Z",
    updatedAtIso = "2026-07-22T00:00:00.000Z",
    lastUsedAtIso = "",
    lastReservationCode = "",
)
