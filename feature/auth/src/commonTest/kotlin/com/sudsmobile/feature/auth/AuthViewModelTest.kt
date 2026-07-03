package com.sudsmobile.feature.auth

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthError
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
class AuthViewModelTest {
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
    fun googleSignInPublishesAuthenticatedState() = runTest {
        val authRepository = FakeProviderAuthRepository()
        val viewModel = AuthViewModel(authRepository)

        viewModel.signInWithGoogleIdToken("google-id-token")
        runCurrent()

        val authenticated = assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
        assertEquals("google-id-token", authRepository.lastGoogleIdToken)
        assertEquals("bruno@gmail.com", authenticated.user.email)
    }

    @Test
    fun googleSignInFailurePublishesErrorState() = runTest {
        val viewModel = AuthViewModel(
            FakeProviderAuthRepository(
                googleResult = AuthResult.Failure(
                    AuthError.Permission("Este método de autenticação não está ativo."),
                ),
            ),
        )

        viewModel.signInWithGoogleIdToken("google-id-token")
        runCurrent()

        val error = assertIs<AuthUiState.Error>(viewModel.uiState.value)
        assertEquals("Este método de autenticação não está ativo.", error.message)
        assertEquals(false, error.retryable)
    }

    @Test
    fun googleSignInIsIgnoredWhileLoading() = runTest {
        val authRepository = SuspendedProviderAuthRepository()
        val viewModel = AuthViewModel(authRepository)

        viewModel.signInWithGoogleIdToken("first-token")
        viewModel.signInWithGoogleIdToken("second-token")
        runCurrent()

        assertIs<AuthUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, authRepository.googleSignInCalls)
        assertEquals("first-token", authRepository.lastGoogleIdToken)

        authRepository.complete(AuthResult.Success(providerSession()))
        runCurrent()

        assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
    }

    @Test
    fun authenticatedSessionRestorePublishesAuthenticatedState() = runTest {
        val viewModel = AuthViewModel(
            FakeProviderAuthRepository(
                initialState = AuthSessionState.Authenticated(providerSession()),
            ),
        )
        runCurrent()

        val authenticated = assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
        assertEquals("uid-1", authenticated.user.uid)
    }

    @Test
    fun unauthenticatedSessionReturnsToIdleState() = runTest {
        val authRepository = FakeProviderAuthRepository(
            initialState = AuthSessionState.Authenticated(providerSession()),
        )
        val viewModel = AuthViewModel(authRepository)
        runCurrent()

        assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)

        authRepository.signOut()
        runCurrent()

        assertIs<AuthUiState.Idle>(viewModel.uiState.value)
    }

    @Test
    fun googlePlatformErrorPublishesRetryableErrorWhenNotLoading() = runTest {
        val viewModel = AuthViewModel(FakeProviderAuthRepository())

        viewModel.showGoogleSignInError("Não foi possível iniciar sessão com Google.")

        val error = assertIs<AuthUiState.Error>(viewModel.uiState.value)
        assertEquals("Não foi possível iniciar sessão com Google.", error.message)
        assertEquals(true, error.retryable)
    }
}

private open class FakeProviderAuthRepository(
    initialState: AuthSessionState = AuthSessionState.Unauthenticated,
    private val googleResult: AuthResult = AuthResult.Success(providerSession()),
) : AuthRepository {
    protected val mutableSessionState = MutableStateFlow(initialState)
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState
    var googleSignInCalls: Int = 0
        protected set
    var lastGoogleIdToken: String? = null
        protected set

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        error("Email/password sign-in is not used by provider auth tests.")
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        googleSignInCalls += 1
        lastGoogleIdToken = idToken
        if (googleResult is AuthResult.Success) {
            mutableSessionState.value = AuthSessionState.Authenticated(googleResult.session)
        }
        return googleResult
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        error("Registration is not used by provider auth tests.")
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        error("Password reset is not used by provider auth tests.")
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private class SuspendedProviderAuthRepository : FakeProviderAuthRepository() {
    private val googleResult = CompletableDeferred<AuthResult>()

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        googleSignInCalls += 1
        lastGoogleIdToken = idToken
        return googleResult.await().also { result ->
            if (result is AuthResult.Success) {
                mutableSessionState.value = AuthSessionState.Authenticated(result.session)
            }
        }
    }

    fun complete(result: AuthResult) {
        googleResult.complete(result)
    }
}

private fun providerSession(
    uid: String = "uid-1",
    email: String = "bruno@gmail.com",
    displayName: String = "Bruno Ribeiro",
): AuthSession = AuthSession(
    user = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        phoneNumber = "",
    ),
    idToken = "id-token",
    refreshToken = "refresh-token",
    expiresInSeconds = 3600L,
    issuedAtEpochSeconds = 1_700_000_000L,
)
