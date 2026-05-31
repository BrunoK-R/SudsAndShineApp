package com.sudsmobile.feature.auth

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.profile.UserProfile
import com.sudsmobile.data.profile.UserProfileError
import com.sudsmobile.data.profile.UserProfileMutationResult
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.profile.UserProfileResult
import com.sudsmobile.data.profile.UserProfileSaveRequest
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
    fun registerPersistsProfileBeforePublishingAuthenticatedUiState() = runTest {
        val profileRepository = FakeRegistrationProfileRepository(
            mutationResult = UserProfileMutationResult.Success(
                registrationProfile(displayName = "Bruno Ribeiro", phoneNumber = "913 005 855"),
            ),
        )
        val viewModel = authViewModel(profileRepository = profileRepository)

        viewModel.register(
            displayName = "  Bruno Ribeiro  ",
            email = "  BRUNO@EXAMPLE.COM  ",
            phoneNumber = "  913 005 855  ",
            password = "secret123",
            acceptsTerms = true,
        )
        runCurrent()

        val authenticated = assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
        assertEquals("Bruno Ribeiro", authenticated.user.displayName)
        assertEquals("913 005 855", authenticated.user.phoneNumber)
        assertEquals("Bruno Ribeiro", profileRepository.lastRequest?.displayName)
        assertEquals("913 005 855", profileRepository.lastRequest?.phoneNumber)
        assertEquals(false, profileRepository.lastRequest?.marketingOptIn)
        assertEquals(false, profileRepository.lastRequest?.appointmentReminderOptIn)
        assertEquals(1, profileRepository.updateCalls)
    }

    @Test
    fun registerProfileFailureCanRetryAndThenAuthenticate() = runTest {
        val profileRepository = FakeRegistrationProfileRepository(
            mutationResult = UserProfileMutationResult.Failure(
                UserProfileError.Unavailable("Não foi possível guardar os dados pessoais."),
            ),
        )
        val viewModel = authViewModel(profileRepository = profileRepository)

        viewModel.register(
            displayName = "Bruno Ribeiro",
            email = "bruno@example.com",
            phoneNumber = "913005855",
            password = "secret123",
            acceptsTerms = true,
        )
        runCurrent()

        val error = assertIs<AuthUiState.RegistrationProfileError>(viewModel.uiState.value)
        assertEquals("Não foi possível guardar os dados pessoais.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(1, profileRepository.updateCalls)

        profileRepository.mutationResult = UserProfileMutationResult.Success(
            registrationProfile(displayName = "Bruno Ribeiro", phoneNumber = "913005855"),
        )
        viewModel.retryRegistrationProfileSave()
        runCurrent()

        assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
        assertEquals(2, profileRepository.updateCalls)
    }

    @Test
    fun registerProfileFailureCanContinueWithAuthenticatedSession() = runTest {
        val viewModel = authViewModel(
            profileRepository = FakeRegistrationProfileRepository(
                mutationResult = UserProfileMutationResult.Failure(
                    UserProfileError.Backend("Perfil indisponível."),
                ),
            ),
        )

        viewModel.register(
            displayName = "Bruno Ribeiro",
            email = "bruno@example.com",
            phoneNumber = "913005855",
            password = "secret123",
            acceptsTerms = true,
        )
        runCurrent()

        assertIs<AuthUiState.RegistrationProfileError>(viewModel.uiState.value)

        viewModel.continueAfterRegistrationProfileError()

        val authenticated = assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
        assertEquals("Bruno Ribeiro", authenticated.user.displayName)
    }

    @Test
    fun registerProfileFailureClearsRetryWhenSessionSwitches() = runTest {
        val authRepository = FakeRegistrationAuthRepository()
        val profileRepository = FakeRegistrationProfileRepository(
            mutationResult = UserProfileMutationResult.Failure(
                UserProfileError.Backend("Perfil indisponível."),
            ),
        )
        val viewModel = authViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )

        viewModel.register(
            displayName = "Bruno Ribeiro",
            email = "bruno@example.com",
            phoneNumber = "913005855",
            password = "secret123",
            acceptsTerms = true,
        )
        runCurrent()

        assertIs<AuthUiState.RegistrationProfileError>(viewModel.uiState.value)

        authRepository.replaceSession(
            registrationSession(
                uid = "uid-2",
                email = "maria@example.com",
                displayName = "Maria Sousa",
                phoneNumber = "914000000",
            ),
        )
        runCurrent()

        val authenticated = assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
        assertEquals("uid-2", authenticated.user.uid)

        viewModel.retryRegistrationProfileSave()
        runCurrent()

        assertEquals(1, profileRepository.updateCalls)
    }

    @Test
    fun registerTermsValidationDoesNotCreateAccountOrProfile() = runTest {
        val authRepository = FakeRegistrationAuthRepository()
        val profileRepository = FakeRegistrationProfileRepository(
            mutationResult = UserProfileMutationResult.Success(registrationProfile()),
        )
        val viewModel = authViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )

        viewModel.register(
            displayName = "Bruno Ribeiro",
            email = "bruno@example.com",
            phoneNumber = "913005855",
            password = "secret123",
            acceptsTerms = false,
        )
        runCurrent()

        assertIs<AuthUiState.Error>(viewModel.uiState.value)
        assertEquals(0, authRepository.registerCalls)
        assertEquals(0, profileRepository.updateCalls)
    }

    @Test
    fun googleSignInPublishesAuthenticatedState() = runTest {
        val authRepository = FakeRegistrationAuthRepository()
        val viewModel = authViewModel(authRepository = authRepository)

        viewModel.signInWithGoogleIdToken("google-id-token")
        runCurrent()

        val authenticated = assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
        assertEquals("google-id-token", authRepository.lastGoogleIdToken)
        assertEquals("bruno@gmail.com", authenticated.user.email)
    }

    @Test
    fun registerProfileSaveSkipsRepositoryWhenSessionChangesBeforeSync() = runTest {
        val authRepository = FakeRegistrationAuthRepository(
            sessionStateAfterRegister = AuthSessionState.Unauthenticated,
        )
        val profileRepository = FakeRegistrationProfileRepository(
            mutationResult = UserProfileMutationResult.Success(registrationProfile()),
        )
        val viewModel = authViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )

        viewModel.register(
            displayName = "Bruno Ribeiro",
            email = "bruno@example.com",
            phoneNumber = "913005855",
            password = "secret123",
            acceptsTerms = true,
        )
        runCurrent()

        assertIs<AuthUiState.Idle>(viewModel.uiState.value)
        assertEquals(0, profileRepository.updateCalls)
    }

    @Test
    fun registerProfileSaveSuccessDoesNotAuthenticateAfterSignOut() = runTest {
        val authRepository = FakeRegistrationAuthRepository()
        val profileRepository = SuspendedRegistrationProfileRepository()
        val viewModel = authViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )

        viewModel.register(
            displayName = "Bruno Ribeiro",
            email = "bruno@example.com",
            phoneNumber = "913005855",
            password = "secret123",
            acceptsTerms = true,
        )
        runCurrent()

        assertEquals(1, profileRepository.updateCalls)

        authRepository.signOut()
        runCurrent()
        profileRepository.complete(
            UserProfileMutationResult.Success(
                registrationProfile(displayName = "Bruno Ribeiro", phoneNumber = "913005855"),
            ),
        )
        runCurrent()

        assertIs<AuthUiState.Idle>(viewModel.uiState.value)

        viewModel.retryRegistrationProfileSave()
        runCurrent()

        assertEquals(1, profileRepository.updateCalls)
    }

    @Test
    fun registerProfileSaveSuccessKeepsCurrentSessionAfterUserSwitch() = runTest {
        val authRepository = FakeRegistrationAuthRepository()
        val profileRepository = SuspendedRegistrationProfileRepository()
        val viewModel = authViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )

        viewModel.register(
            displayName = "Bruno Ribeiro",
            email = "bruno@example.com",
            phoneNumber = "913005855",
            password = "secret123",
            acceptsTerms = true,
        )
        runCurrent()

        val switchedSession = registrationSession(
            uid = "uid-2",
            email = "maria@example.com",
            displayName = "Maria Sousa",
            phoneNumber = "914000000",
        )
        authRepository.replaceSession(switchedSession)
        runCurrent()
        profileRepository.complete(
            UserProfileMutationResult.Success(
                registrationProfile(displayName = "Bruno Ribeiro", phoneNumber = "913005855"),
            ),
        )
        runCurrent()

        val authenticated = assertIs<AuthUiState.Authenticated>(viewModel.uiState.value)
        assertEquals("uid-2", authenticated.user.uid)
        assertEquals("Maria Sousa", authenticated.user.displayName)
    }
}

private fun authViewModel(
    authRepository: AuthRepository = FakeRegistrationAuthRepository(),
    profileRepository: UserProfileRepository = FakeRegistrationProfileRepository(
        mutationResult = UserProfileMutationResult.Success(registrationProfile()),
    ),
): AuthViewModel = AuthViewModel(
    authRepository = authRepository,
    profileRepository = profileRepository,
)

private class FakeRegistrationAuthRepository(
    private val sessionStateAfterRegister: AuthSessionState? = null,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Unauthenticated)
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState
    var registerCalls: Int = 0
        private set
    var lastGoogleIdToken: String? = null
        private set

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        val session = registrationSession(
            email = email.trim().lowercase(),
            displayName = "Bruno Ribeiro",
            phoneNumber = "913005855",
        )
        mutableSessionState.value = AuthSessionState.Authenticated(session)
        return AuthResult.Success(session)
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        lastGoogleIdToken = idToken
        val session = registrationSession(
            email = "bruno@gmail.com",
            displayName = "Bruno Ribeiro",
            phoneNumber = "",
        )
        mutableSessionState.value = AuthSessionState.Authenticated(session)
        return AuthResult.Success(session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        registerCalls += 1
        val session = registrationSession(
            email = email.trim().lowercase(),
            displayName = displayName.trim(),
            phoneNumber = phoneNumber.trim(),
        )
        mutableSessionState.value = sessionStateAfterRegister ?: AuthSessionState.Authenticated(session)
        return AuthResult.Success(session)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }

    fun replaceSession(session: AuthSession) {
        mutableSessionState.value = AuthSessionState.Authenticated(session)
    }
}

private class FakeRegistrationProfileRepository(
    var mutationResult: UserProfileMutationResult,
) : UserProfileRepository {
    var updateCalls: Int = 0
        private set
    var lastRequest: UserProfileSaveRequest? = null
        private set

    override suspend fun getMyProfile(): UserProfileResult {
        error("Not used")
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        updateCalls += 1
        lastRequest = request
        return mutationResult
    }
}

private class SuspendedRegistrationProfileRepository : UserProfileRepository {
    private val pendingResult = CompletableDeferred<UserProfileMutationResult>()
    var updateCalls: Int = 0
        private set

    override suspend fun getMyProfile(): UserProfileResult {
        error("Not used")
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        updateCalls += 1
        return pendingResult.await()
    }

    fun complete(result: UserProfileMutationResult) {
        pendingResult.complete(result)
    }
}

private fun registrationSession(
    uid: String = "uid-1",
    email: String,
    displayName: String,
    phoneNumber: String,
): AuthSession = AuthSession(
    user = AuthUser(
        uid = uid,
        email = email,
        displayName = displayName,
        phoneNumber = phoneNumber,
    ),
    idToken = "id-token-1",
    refreshToken = "refresh-token-1",
    expiresInSeconds = 3600,
)

private fun registrationProfile(
    displayName: String = "Bruno Ribeiro",
    phoneNumber: String = "913005855",
): UserProfile = UserProfile(
    uid = "uid-1",
    email = "bruno@example.com",
    displayName = displayName,
    phoneNumber = phoneNumber,
    marketingOptIn = false,
    appointmentReminderOptIn = false,
)
