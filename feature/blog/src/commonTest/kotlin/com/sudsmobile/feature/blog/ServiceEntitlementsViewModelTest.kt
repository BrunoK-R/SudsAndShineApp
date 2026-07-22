package com.sudsmobile.feature.blog

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.entitlement.AdminServiceEntitlementListResult
import com.sudsmobile.data.entitlement.AdjustServiceEntitlementUsageRequest
import com.sudsmobile.data.entitlement.IssueServiceEntitlementRequest
import com.sudsmobile.data.entitlement.RevokeServiceEntitlementRequest
import com.sudsmobile.data.entitlement.ServiceEntitlement
import com.sudsmobile.data.entitlement.ServiceEntitlementList
import com.sudsmobile.data.entitlement.ServiceEntitlementListResult
import com.sudsmobile.data.entitlement.ServiceEntitlementMutationResult
import com.sudsmobile.data.entitlement.ServiceEntitlementRepository
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
class ServiceEntitlementsViewModelTest {
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
    fun loadRequiresAuthenticationBeforeRepositoryCall() = runTest {
        val repository = CustomerEntitlementRepository()
        val viewModel = ServiceEntitlementsViewModel(CustomerEntitlementAuthRepository(false), repository)

        viewModel.load()
        runCurrent()

        assertIs<ServiceEntitlementsUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadMapsStaffIssuedPlanForTheCustomer() = runTest {
        val repository = CustomerEntitlementRepository()
        val viewModel = ServiceEntitlementsViewModel(CustomerEntitlementAuthRepository(true), repository)

        viewModel.load()
        runCurrent()

        val loaded = assertIs<ServiceEntitlementsUiState.Loaded>(viewModel.uiState.value)
        val plan = loaded.entitlements.single()
        assertEquals("Pacote", plan.kindLabel)
        assertEquals("Ativo", plan.statusLabel)
        assertEquals(3, plan.remainingUses)
        assertEquals("Lavagem Standard", plan.eligibleServicesLabel)
        assertEquals("2026-12-31", plan.validUntilLabel)
    }
}

private class CustomerEntitlementRepository : ServiceEntitlementRepository {
    var loadCalls = 0

    override suspend fun getMyEntitlements(): ServiceEntitlementListResult {
        loadCalls += 1
        return ServiceEntitlementListResult.Success(
            ServiceEntitlementList(listOf(customerEntitlement()), "staff_issued", false),
        )
    }

    override suspend fun getAdminEntitlements(customerEmail: String): AdminServiceEntitlementListResult = error("Not used")
    override suspend fun issueEntitlement(request: IssueServiceEntitlementRequest): ServiceEntitlementMutationResult =
        error("Not used")
    override suspend fun adjustUsage(request: AdjustServiceEntitlementUsageRequest): ServiceEntitlementMutationResult =
        error("Not used")
    override suspend fun revokeEntitlement(request: RevokeServiceEntitlementRequest): ServiceEntitlementMutationResult =
        error("Not used")
}

private class CustomerEntitlementAuthRepository(authenticated: Boolean) : AuthRepository {
    private val state = MutableStateFlow<AuthSessionState>(
        if (authenticated) {
            AuthSessionState.Authenticated(
                AuthSession(AuthUser("uid-1", "client@example.com", "Client", ""), "token", "refresh", 3600),
            )
        } else {
            AuthSessionState.Unauthenticated
        },
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

private fun customerEntitlement() = ServiceEntitlement(
    id = "issue-1",
    code = "SS-PLAN-ABC123",
    kind = "package",
    name = "Pacote 5 lavagens",
    status = "active",
    totalUses = 5,
    usedUses = 2,
    remainingUses = 3,
    eligibleServiceIds = listOf("standard"),
    eligibleServiceNames = listOf("Lavagem Standard"),
    validFromIso = "2026-07-01T00:00:00.000Z",
    validUntilIso = "2026-12-31T00:00:00.000Z",
    amountPaidCents = 10000,
    purchaseMode = "staff_issued",
    onlinePurchaseAvailable = false,
    createdAtIso = "2026-07-01T00:00:00.000Z",
    updatedAtIso = "2026-07-02T00:00:00.000Z",
    lastUsedAtIso = "",
    lastReservationCode = "",
)
