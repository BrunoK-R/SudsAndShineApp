package com.sudsmobile.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class FirebaseAuthRepositoryTest {
    @Test
    fun rejectsInvalidLoginBeforeCallingApi() = runTest {
        val api = RecordingAuthApi()
        val repository = FirebaseAuthRepository(api)

        val result = repository.signIn(email = "not-an-email", password = "secret123")

        assertIs<AuthResult.Failure>(result)
        assertIs<AuthError.Validation>(result.error)
        assertEquals(0, api.signInCalls)
    }

    @Test
    fun registerStoresAuthenticatedSessionWithLocalProfile() = runTest {
        val api = RecordingAuthApi()
        val repository = FirebaseAuthRepository(api)

        val result = repository.register(
            displayName = "  Bruno Ribeiro  ",
            email = "  BRUNO@EXAMPLE.COM  ",
            phoneNumber = "  +351913005855  ",
            password = "secret123",
        )

        val success = assertIs<AuthResult.Success>(result)
        assertEquals("bruno@example.com", api.lastSignUpEmail)
        assertEquals("Bruno Ribeiro", api.lastDisplayName)
        assertEquals("Bruno Ribeiro", success.session.user.displayName)
        assertEquals("+351913005855", success.session.user.phoneNumber)

        val sessionState = assertIs<AuthSessionState.Authenticated>(repository.sessionState.value)
        assertEquals("user-1", sessionState.session.user.uid)
    }

    @Test
    fun signOutClearsAuthenticatedSession() = runTest {
        val repository = FirebaseAuthRepository(RecordingAuthApi())

        repository.signIn(email = "bruno@example.com", password = "secret123")
        assertIs<AuthSessionState.Authenticated>(repository.sessionState.value)

        repository.signOut()

        assertEquals(AuthSessionState.Unauthenticated, repository.sessionState.value)
    }
}

private class RecordingAuthApi : AuthApi {
    var signInCalls: Int = 0
        private set
    var lastSignUpEmail: String? = null
        private set
    var lastDisplayName: String? = null
        private set

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls += 1
        return AuthResult.Success(testSession(email = email, displayName = "Bruno Ribeiro"))
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        lastSignUpEmail = email
        return AuthResult.Success(testSession(email = email, displayName = ""))
    }

    override suspend fun updateProfile(session: AuthSession, displayName: String): AuthResult {
        lastDisplayName = displayName
        return AuthResult.Success(
            session.copy(
                user = session.user.copy(displayName = displayName),
                idToken = "updated-id-token",
            ),
        )
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }
}

private fun testSession(
    email: String,
    displayName: String,
): AuthSession = AuthSession(
    user = AuthUser(
        uid = "user-1",
        email = email,
        displayName = displayName,
        phoneNumber = "",
    ),
    idToken = "id-token",
    refreshToken = "refresh-token",
    expiresInSeconds = 3600,
)
