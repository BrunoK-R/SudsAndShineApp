package com.sudsmobile.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun registerRejectsInvalidPhoneBeforeCallingApi() = runTest {
        val api = RecordingAuthApi()
        val repository = FirebaseAuthRepository(api)

        val result = repository.register(
            displayName = "Bruno Ribeiro",
            email = "bruno@example.com",
            phoneNumber = "abc",
            password = "secret123",
        )

        assertIs<AuthResult.Failure>(result)
        assertIs<AuthError.Validation>(result.error)
        assertEquals(0, api.signUpCalls)
    }

    @Test
    fun signOutClearsAuthenticatedSession() = runTest {
        val store = RecordingAuthSessionStore()
        val repository = FirebaseAuthRepository(
            api = RecordingAuthApi(),
            sessionStore = store,
            restoreOnStart = false,
        )

        repository.signIn(email = "bruno@example.com", password = "secret123")
        assertIs<AuthSessionState.Authenticated>(repository.sessionState.value)

        repository.signOut()

        assertEquals(AuthSessionState.Unauthenticated, repository.sessionState.value)
        assertNull(store.savedSession)
    }

    @Test
    fun signInPersistsIssuedSession() = runTest {
        val store = RecordingAuthSessionStore()
        val repository = FirebaseAuthRepository(
            api = RecordingAuthApi(),
            sessionStore = store,
            nowEpochSeconds = { 1_000L },
            restoreOnStart = false,
        )

        val result = repository.signIn(email = "bruno@example.com", password = "secret123")

        val success = assertIs<AuthResult.Success>(result)
        assertEquals(1_000L, success.session.issuedAtEpochSeconds)
        assertEquals("id-token", store.savedSession?.idToken)
        assertEquals(1_000L, store.savedSession?.issuedAtEpochSeconds)
    }

    @Test
    fun googleSignInPersistsIssuedSession() = runTest {
        val store = RecordingAuthSessionStore()
        val api = RecordingAuthApi()
        val repository = FirebaseAuthRepository(
            api = api,
            sessionStore = store,
            nowEpochSeconds = { 1_000L },
            restoreOnStart = false,
        )

        val result = repository.signInWithGoogleIdToken(" google-id-token ")

        val success = assertIs<AuthResult.Success>(result)
        assertEquals("google-id-token", api.lastGoogleIdToken)
        assertEquals(1_000L, success.session.issuedAtEpochSeconds)
        assertEquals("id-token", store.savedSession?.idToken)
    }

    @Test
    fun restoresPersistedSessionWithRefreshedToken() = runTest {
        val store = RecordingAuthSessionStore(savedSession = testSession(
            email = "bruno@example.com",
            displayName = "Bruno Ribeiro",
        ))
        val api = RecordingAuthApi()
        val repository = FirebaseAuthRepository(
            api = api,
            sessionStore = store,
            nowEpochSeconds = { 2_000L },
            repositoryScope = this,
        )

        advanceUntilIdle()

        val sessionState = assertIs<AuthSessionState.Authenticated>(repository.sessionState.value)
        assertEquals("refresh-token", api.lastRefreshToken)
        assertEquals("refreshed-id-token", sessionState.session.idToken)
        assertEquals("Bruno Ribeiro", sessionState.session.user.displayName)
        assertEquals(2_000L, sessionState.session.issuedAtEpochSeconds)
        assertEquals("refreshed-id-token", store.savedSession?.idToken)
    }

    @Test
    fun currentSessionRefreshesExpiredToken() = runTest {
        var now = 1_000L
        val store = RecordingAuthSessionStore()
        val api = RecordingAuthApi()
        val repository = FirebaseAuthRepository(
            api = api,
            sessionStore = store,
            nowEpochSeconds = { now },
            restoreOnStart = false,
        )
        repository.signIn(email = "bruno@example.com", password = "secret123")
        now = 4_600L

        val session = repository.currentSession()

        assertEquals("refresh-token", api.lastRefreshToken)
        assertEquals("refreshed-id-token", session?.idToken)
        assertEquals("refreshed-id-token", store.savedSession?.idToken)
    }

    @Test
    fun refreshCurrentSessionRefreshesFreshTokenForClaimChanges() = runTest {
        val store = RecordingAuthSessionStore()
        val api = RecordingAuthApi()
        val repository = FirebaseAuthRepository(
            api = api,
            sessionStore = store,
            nowEpochSeconds = { 1_000L },
            restoreOnStart = false,
        )
        repository.signIn(email = "bruno@example.com", password = "secret123")

        val result = repository.refreshCurrentSession()

        val success = assertIs<AuthResult.Success>(result)
        assertEquals("refresh-token", api.lastRefreshToken)
        assertEquals("refreshed-id-token", success.session.idToken)
        assertEquals("refreshed-id-token", store.savedSession?.idToken)
        val sessionState = assertIs<AuthSessionState.Authenticated>(repository.sessionState.value)
        assertEquals("refreshed-id-token", sessionState.session.idToken)
    }

    @Test
    fun inFlightCurrentSessionRefreshDoesNotReauthenticateAfterSignOut() = runTest {
        var now = 1_000L
        val store = RecordingAuthSessionStore()
        val refreshResult = CompletableDeferred<AuthResult>()
        val api = RecordingAuthApi(refreshResult = refreshResult)
        val repository = FirebaseAuthRepository(
            api = api,
            sessionStore = store,
            nowEpochSeconds = { now },
            restoreOnStart = false,
        )
        repository.signIn(email = "bruno@example.com", password = "secret123")
        now = 4_600L

        val session = async { repository.currentSession() }
        assertEquals("refresh-token", api.refreshStarted.await())

        repository.signOut()
        refreshResult.complete(
            AuthResult.Success(
                testSession(
                    email = "bruno@example.com",
                    displayName = "Bruno Ribeiro",
                    idToken = "late-id-token",
                    refreshToken = "late-refresh-token",
                ),
            ),
        )

        assertNull(session.await())
        assertEquals(AuthSessionState.Unauthenticated, repository.sessionState.value)
        assertNull(store.savedSession)
    }

    @Test
    fun inFlightForcedRefreshDoesNotReauthenticateAfterSignOut() = runTest {
        val store = RecordingAuthSessionStore()
        val refreshResult = CompletableDeferred<AuthResult>()
        val api = RecordingAuthApi(refreshResult = refreshResult)
        val repository = FirebaseAuthRepository(
            api = api,
            sessionStore = store,
            nowEpochSeconds = { 1_000L },
            restoreOnStart = false,
        )
        repository.signIn(email = "bruno@example.com", password = "secret123")

        val refresh = async { repository.refreshCurrentSession() }
        assertEquals("refresh-token", api.refreshStarted.await())

        repository.signOut()
        refreshResult.complete(
            AuthResult.Success(
                testSession(
                    email = "bruno@example.com",
                    displayName = "Bruno Ribeiro",
                    idToken = "late-id-token",
                    refreshToken = "late-refresh-token",
                ),
            ),
        )

        val failure = assertIs<AuthResult.Failure>(refresh.await())
        assertIs<AuthError.InvalidCredentials>(failure.error)
        assertEquals(AuthSessionState.Unauthenticated, repository.sessionState.value)
        assertNull(store.savedSession)
    }

    @Test
    fun inFlightCurrentSessionRefreshDoesNotReplaceNewSignIn() = runTest {
        var now = 1_000L
        val store = RecordingAuthSessionStore()
        val refreshResult = CompletableDeferred<AuthResult>()
        val api = RecordingAuthApi(
            refreshResult = refreshResult,
            signInSession = { email ->
                if (email == "ana@example.com") {
                    testSession(
                        uid = "user-2",
                        email = email,
                        displayName = "Ana Silva",
                        idToken = "ana-id-token",
                        refreshToken = "ana-refresh-token",
                    )
                } else {
                    testSession(email = email, displayName = "Bruno Ribeiro")
                }
            },
        )
        val repository = FirebaseAuthRepository(
            api = api,
            sessionStore = store,
            nowEpochSeconds = { now },
            restoreOnStart = false,
        )
        repository.signIn(email = "bruno@example.com", password = "secret123")
        now = 4_600L

        val staleSession = async { repository.currentSession() }
        assertEquals("refresh-token", api.refreshStarted.await())

        repository.signIn(email = "ana@example.com", password = "secret123")
        refreshResult.complete(
            AuthResult.Success(
                testSession(
                    email = "bruno@example.com",
                    displayName = "Bruno Ribeiro",
                    idToken = "late-id-token",
                    refreshToken = "late-refresh-token",
                ),
            ),
        )

        assertNull(staleSession.await())
        val sessionState = assertIs<AuthSessionState.Authenticated>(repository.sessionState.value)
        assertEquals("user-2", sessionState.session.user.uid)
        assertEquals("ana-id-token", sessionState.session.idToken)
        assertEquals("ana-id-token", store.savedSession?.idToken)
    }
}

private class RecordingAuthApi(
    private val refreshResult: CompletableDeferred<AuthResult>? = null,
    private val signInSession: (String) -> AuthSession = { email ->
        testSession(email = email, displayName = "Bruno Ribeiro")
    },
) : AuthApi {
    var signInCalls: Int = 0
        private set
    var signUpCalls: Int = 0
        private set
    var lastSignUpEmail: String? = null
        private set
    var lastDisplayName: String? = null
        private set
    var lastGoogleIdToken: String? = null
        private set
    var lastRefreshToken: String? = null
        private set
    val refreshStarted = CompletableDeferred<String>()

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls += 1
        return AuthResult.Success(signInSession(email))
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        lastGoogleIdToken = idToken
        return AuthResult.Success(signInSession("bruno@gmail.com"))
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        signUpCalls += 1
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

    override suspend fun refreshSession(refreshToken: String): AuthResult {
        lastRefreshToken = refreshToken
        refreshStarted.complete(refreshToken)
        if (refreshResult != null) return refreshResult.await()
        return AuthResult.Success(
            AuthSession(
                user = AuthUser(
                    uid = "user-1",
                    email = "",
                    displayName = "",
                    phoneNumber = "",
                ),
                idToken = "refreshed-id-token",
                refreshToken = "refreshed-refresh-token",
                expiresInSeconds = 3600,
            ),
        )
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }
}

private class RecordingAuthSessionStore(
    var savedSession: AuthSession? = null,
) : AuthSessionStore {
    override fun readSession(): AuthSession? = savedSession

    override fun writeSession(session: AuthSession) {
        savedSession = session
    }

    override fun clearSession() {
        savedSession = null
    }
}

private fun testSession(
    uid: String = "user-1",
    email: String,
    displayName: String,
    idToken: String = "id-token",
    refreshToken: String = "refresh-token",
    expiresInSeconds: Long = 3600,
): AuthSession = AuthSession(
    user = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        phoneNumber = "",
    ),
    idToken = idToken,
    refreshToken = refreshToken,
    expiresInSeconds = expiresInSeconds,
)
