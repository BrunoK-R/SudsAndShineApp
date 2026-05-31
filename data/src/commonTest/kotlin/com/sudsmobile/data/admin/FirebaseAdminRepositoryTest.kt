package com.sudsmobile.data.admin

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

class FirebaseAdminRepositoryTest {
    @Test
    fun upsertServiceCatalogItemNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertServiceCatalogItem(
            AdminServiceCatalogMutationRequest(
                serviceId = " premium ",
                name = " Lavagem   Premium ",
                description = " Detalhe   completo ",
                durationMinutes = 45,
                passengerPriceCents = 3200,
                suvPriceCents = 3400,
                iconKey = "",
                popular = true,
                sortOrder = 20,
            ),
        )

        val success = assertIs<AdminServiceCatalogMutationResult.Success>(result)
        assertEquals("premium", success.receipt.serviceId)
        assertEquals("id-token-1", api.upsertIdTokens.single())
        val request = api.upsertRequests.single()
        assertEquals("premium", request.serviceId)
        assertEquals("Lavagem Premium", request.name)
        assertEquals("Detalhe completo", request.description)
        assertEquals("car", request.iconKey)
    }

    @Test
    fun upsertServiceCatalogItemReturnsValidationBeforeApiCall() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.upsertServiceCatalogItem(
            AdminServiceCatalogMutationRequest(
                serviceId = "services/premium",
                name = "Premium",
                durationMinutes = 45,
                passengerPriceCents = 3200,
                suvPriceCents = 3400,
            ),
        )

        val failure = assertIs<AdminServiceCatalogMutationResult.Failure>(result)
        assertIs<AdminError.Validation>(failure.error)
        assertEquals(0, api.upsertRequests.size)
    }

    @Test
    fun upsertServiceCatalogItemRequiresAuthenticatedSession() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = false),
        )

        val result = repository.upsertServiceCatalogItem(
            AdminServiceCatalogMutationRequest(
                name = "Premium",
                durationMinutes = 45,
                passengerPriceCents = 3200,
                suvPriceCents = 3400,
            ),
        )

        val failure = assertIs<AdminServiceCatalogMutationResult.Failure>(result)
        assertIs<AdminError.Unauthenticated>(failure.error)
        assertEquals(0, api.upsertRequests.size)
    }

    @Test
    fun archiveServiceCatalogItemNormalizesRequestAndUsesCurrentToken() = runTest {
        val api = FakeAdminFunctionsApi()
        val repository = FirebaseAdminRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
        )

        val result = repository.archiveServiceCatalogItem(
            AdminServiceCatalogArchiveRequest(serviceId = " premium "),
        )

        val success = assertIs<AdminServiceCatalogMutationResult.Success>(result)
        assertEquals("premium", success.receipt.serviceId)
        assertEquals("id-token-1", api.archiveIdTokens.single())
        assertEquals("premium", api.archiveRequests.single().serviceId)
    }
}

private class FakeAdminFunctionsApi : AdminFunctionsApi {
    val upsertRequests = mutableListOf<AdminServiceCatalogMutationRequest>()
    val upsertIdTokens = mutableListOf<String>()
    val archiveRequests = mutableListOf<AdminServiceCatalogArchiveRequest>()
    val archiveIdTokens = mutableListOf<String>()

    override suspend fun syncMyRole(idToken: String): AdminRoleResult {
        return AdminRoleResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getPendingBookingRequests(idToken: String): AdminBookingRequestsResult {
        return AdminBookingRequestsResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult {
        return AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
        idToken: String,
    ): AdminBookingDecisionResult {
        return AdminBookingDecisionResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun getBusinessInfoConfiguration(idToken: String): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun updateBusinessInfoConfiguration(
        request: AdminBusinessInfoUpdateRequest,
        idToken: String,
    ): AdminBusinessInfoResult {
        return AdminBusinessInfoResult.Failure(AdminError.Backend("unused"))
    }

    override suspend fun upsertServiceCatalogItem(
        request: AdminServiceCatalogMutationRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult {
        upsertRequests += request
        upsertIdTokens += idToken
        return AdminServiceCatalogMutationResult.Success(
            AdminServiceCatalogMutationReceipt(
                serviceId = request.serviceId.ifBlank { "generated-service" },
                status = if (request.active) "active" else "inactive",
            ),
        )
    }

    override suspend fun archiveServiceCatalogItem(
        request: AdminServiceCatalogArchiveRequest,
        idToken: String,
    ): AdminServiceCatalogMutationResult {
        archiveRequests += request
        archiveIdTokens += idToken
        return AdminServiceCatalogMutationResult.Success(
            AdminServiceCatalogMutationReceipt(
                serviceId = request.serviceId,
                status = "archived",
            ),
        )
    }
}

private class FakeAuthRepository(authenticated: Boolean) : AuthRepository {
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

    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

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

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}
