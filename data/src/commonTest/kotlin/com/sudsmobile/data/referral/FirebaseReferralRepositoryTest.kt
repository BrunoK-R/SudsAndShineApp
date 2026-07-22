package com.sudsmobile.data.referral

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

class FirebaseReferralRepositoryTest {
    @Test
    fun requiresAuthenticationBeforeLoadingOrClaiming() = runTest {
        val api = RecordingReferralFunctionsApi()
        val repository = FirebaseReferralRepository(api, ReferralFakeAuthRepository(authenticated = false))

        assertIs<ReferralError.Unauthenticated>(
            assertIs<ReferralProgramResult.Failure>(repository.getMyReferral()).error,
        )
        assertIs<ReferralError.Unauthenticated>(
            assertIs<ReferralProgramResult.Failure>(repository.claimReferralCode("SUDS-ABCD123456")).error,
        )
        assertEquals(0, api.calls)
    }

    @Test
    fun validatesAndNormalizesCodeBeforeApiCall() = runTest {
        val api = RecordingReferralFunctionsApi()
        val repository = FirebaseReferralRepository(api, ReferralFakeAuthRepository(authenticated = true))

        val invalid = assertIs<ReferralProgramResult.Failure>(repository.claimReferralCode("wrong"))
        assertIs<ReferralError.Validation>(invalid.error)
        assertEquals(0, api.calls)

        repository.claimReferralCode(" suds-abcd123456 ")
        assertEquals(1, api.calls)
        assertEquals("SUDS-ABCD123456", api.lastCode)
        assertEquals("id-token-1", api.lastIdToken)
    }
}

private class RecordingReferralFunctionsApi : ReferralFunctionsApi {
    var calls = 0
    var lastCode: String? = null
    var lastIdToken: String? = null

    override suspend fun getMyReferral(idToken: String): ReferralProgramResult {
        calls += 1
        lastIdToken = idToken
        return ReferralProgramResult.Success(referralProgram())
    }

    override suspend fun claimMyReferralCode(code: String, idToken: String): ReferralProgramResult {
        calls += 1
        lastCode = code
        lastIdToken = idToken
        return ReferralProgramResult.Success(referralProgram())
    }
}

private class ReferralFakeAuthRepository(authenticated: Boolean) : AuthRepository {
    override val sessionState: StateFlow<AuthSessionState> = MutableStateFlow(
        if (authenticated) {
            AuthSessionState.Authenticated(
                AuthSession(
                    user = AuthUser("uid-1", "bruno@example.com", "Bruno", ""),
                    idToken = "id-token-1",
                    refreshToken = "refresh-token-1",
                    expiresInSeconds = 3600,
                ),
            )
        } else {
            AuthSessionState.Unauthenticated
        },
    )

    override suspend fun currentSession(): AuthSession? =
        (sessionState.value as? AuthSessionState.Authenticated)?.session

    override suspend fun signIn(email: String, password: String): AuthResult = error("Not used")
    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult = error("Not used")
    override suspend fun sendPasswordReset(email: String): AuthActionResult = error("Not used")
    override fun signOut() = Unit
}

private fun referralProgram(): ReferralProgram = ReferralProgram(
    code = "SUDS-AABBCCDDEE",
    shareMessage = "Convite",
    rewardPoints = 1,
    attributionDays = 30,
    referredBy = null,
    stats = ReferralStats(0, 0, 0, 0),
    invitations = emptyList(),
)
