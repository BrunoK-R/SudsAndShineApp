package com.sudsmobile.data.entitlement

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

class FirebaseServiceEntitlementRepositoryTest {
    @Test
    fun requiresAuthenticationAndValidatesAdminLookupEmail() = runTest {
        val api = FakeEntitlementApi()
        val unauthenticated = FirebaseServiceEntitlementRepository(api, EntitlementAuthRepository(false))
        assertIs<ServiceEntitlementListResult.Failure>(unauthenticated.getMyEntitlements())
        assertEquals(0, api.myCalls)

        val authenticated = FirebaseServiceEntitlementRepository(api, EntitlementAuthRepository(true))
        val invalid = assertIs<AdminServiceEntitlementListResult.Failure>(
            authenticated.getAdminEntitlements("not-an-email"),
        )
        assertIs<ServiceEntitlementError.Validation>(invalid.error)
        assertEquals(0, api.adminCalls)
    }

    @Test
    fun forwardsAuthenticatedCustomerPlanLookup() = runTest {
        val api = FakeEntitlementApi()
        val repository = FirebaseServiceEntitlementRepository(api, EntitlementAuthRepository(true))

        assertIs<ServiceEntitlementListResult.Success>(repository.getMyEntitlements())

        assertEquals(1, api.myCalls)
        assertEquals("id-token", api.lastToken)
    }
}

private class FakeEntitlementApi : ServiceEntitlementFunctionsApi {
    var myCalls = 0
    var adminCalls = 0
    var lastToken = ""

    override suspend fun getMyEntitlements(idToken: String): ServiceEntitlementListResult {
        myCalls += 1
        lastToken = idToken
        return ServiceEntitlementListResult.Success(ServiceEntitlementList(emptyList(), "staff_issued", false))
    }
    override suspend fun getAdminEntitlements(customerEmail: String, idToken: String): AdminServiceEntitlementListResult {
        adminCalls += 1
        return AdminServiceEntitlementListResult.Failure(ServiceEntitlementError.NotFound("missing"))
    }
    override suspend fun issueEntitlement(
        request: IssueServiceEntitlementRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult = error("Not used")
    override suspend fun adjustUsage(
        request: AdjustServiceEntitlementUsageRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult = error("Not used")
    override suspend fun revokeEntitlement(
        request: RevokeServiceEntitlementRequest,
        idToken: String,
    ): ServiceEntitlementMutationResult = error("Not used")
}

private class EntitlementAuthRepository(authenticated: Boolean) : AuthRepository {
    private val state = MutableStateFlow<AuthSessionState>(
        if (authenticated) AuthSessionState.Authenticated(
            AuthSession(AuthUser("uid", "client@example.com", "Client", ""), "id-token", "refresh", 3600),
        ) else {
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
