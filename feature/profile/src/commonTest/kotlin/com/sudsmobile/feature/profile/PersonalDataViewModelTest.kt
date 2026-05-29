package com.sudsmobile.feature.profile

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthError
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
class PersonalDataViewModelTest {
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
    fun loadProfileRequiresAuthenticatedSession() = runTest {
        val repository = FakePersonalDataRepository(
            profileResult = UserProfileResult.Success(personalDataProfile()),
        )
        val viewModel = PersonalDataViewModel(
            authRepository = FakePersonalDataAuthRepository(authenticated = false),
            profileRepository = repository,
        )

        viewModel.loadProfile()
        runCurrent()

        assertIs<PersonalDataUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun refreshForSessionWaitsWhileSessionIsRestoring() = runTest {
        val repository = FakePersonalDataRepository(
            profileResult = UserProfileResult.Success(personalDataProfile()),
        )
        val viewModel = PersonalDataViewModel(
            authRepository = FakePersonalDataAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
            profileRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<PersonalDataUiState.Loading>(viewModel.uiState.value)
        assertIs<PersonalDataSaveUiState.Idle>(viewModel.saveState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun refreshForSessionMapsRestoreFailureWithoutProfileCall() = runTest {
        val repository = FakePersonalDataRepository(
            profileResult = UserProfileResult.Success(personalDataProfile()),
        )
        val viewModel = PersonalDataViewModel(
            authRepository = FakePersonalDataAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.RestoreFailed(AuthError.Unavailable("Sessão indisponível.")),
            ),
            profileRepository = repository,
        )

        viewModel.refreshForSession()
        runCurrent()

        val error = assertIs<PersonalDataUiState.Error>(viewModel.uiState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadProfileMapsBackendProfileToForm() = runTest {
        val viewModel = PersonalDataViewModel(
            authRepository = FakePersonalDataAuthRepository(authenticated = true),
            profileRepository = FakePersonalDataRepository(
                profileResult = UserProfileResult.Success(
                    personalDataProfile(
                        displayName = "Bruno Ribeiro",
                        marketingOptIn = true,
                        appointmentReminderOptIn = true,
                    ),
                ),
            ),
        )

        viewModel.loadProfile()
        runCurrent()

        val loaded = assertIs<PersonalDataUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Bruno Ribeiro", loaded.form.displayName)
        assertEquals("bruno@example.com", loaded.form.email)
        assertEquals(true, loaded.form.marketingOptIn)
        assertEquals(true, loaded.form.appointmentReminderOptIn)
    }

    @Test
    fun loadProfileKeepsLatestSessionProfileWhenEarlierLoadCompletesLast() = runTest {
        val authRepository = FakePersonalDataAuthRepository(authenticated = true)
        val repository = QueuedPersonalDataRepository()
        val viewModel = PersonalDataViewModel(
            authRepository = authRepository,
            profileRepository = repository,
        )

        viewModel.loadProfile()
        runCurrent()

        assertIs<PersonalDataUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, repository.loadCalls)

        authRepository.authenticate(uid = "uid-2")
        viewModel.refreshForSession()
        runCurrent()

        assertEquals(2, repository.loadCalls)

        repository.secondLoad.complete(
            UserProfileResult.Success(
                personalDataProfile(
                    uid = "uid-2",
                    email = "second@example.com",
                    displayName = "Segundo Cliente",
                ),
            ),
        )
        runCurrent()

        val loaded = assertIs<PersonalDataUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Segundo Cliente", loaded.form.displayName)
        assertEquals("second@example.com", loaded.form.email)

        repository.firstLoad.complete(
            UserProfileResult.Success(
                personalDataProfile(
                    uid = "uid-1",
                    email = "first@example.com",
                    displayName = "Primeiro Cliente",
                ),
            ),
        )
        runCurrent()

        val latest = assertIs<PersonalDataUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Segundo Cliente", latest.form.displayName)
        assertEquals("second@example.com", latest.form.email)
    }

    @Test
    fun saveProfileValidatesPhoneBeforeCallingRepository() = runTest {
        val repository = FakePersonalDataRepository(
            profileResult = UserProfileResult.Success(personalDataProfile()),
        )
        val viewModel = PersonalDataViewModel(
            authRepository = FakePersonalDataAuthRepository(authenticated = true),
            profileRepository = repository,
        )

        viewModel.saveProfile(
            PersonalDataFormUi(
                displayName = "Bruno Ribeiro",
                email = "bruno@example.com",
                phoneNumber = "abc",
                marketingOptIn = false,
            ),
        )
        runCurrent()

        assertIs<PersonalDataSaveUiState.ValidationError>(viewModel.saveState.value)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun saveProfileDoesNotCallRepositoryWhenSessionChangesBeforeCoroutineRuns() = runTest {
        val authRepository = FakePersonalDataAuthRepository(authenticated = true)
        val repository = FakePersonalDataRepository(
            profileResult = UserProfileResult.Success(personalDataProfile()),
        )
        val viewModel = PersonalDataViewModel(
            authRepository = authRepository,
            profileRepository = repository,
        )

        viewModel.saveProfile(
            PersonalDataFormUi(
                displayName = "Bruno Ribeiro",
                email = "bruno@example.com",
                phoneNumber = "913 005 855",
                marketingOptIn = false,
            ),
        )
        authRepository.authenticate(uid = "uid-2")
        runCurrent()

        val error = assertIs<PersonalDataSaveUiState.Error>(viewModel.saveState.value)
        assertEquals("A sessão mudou antes de guardarmos os dados. Atualize e tente novamente.", error.message)
        assertEquals(false, error.retryable)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun saveProfilePublishesUpdatedProfileAndSuccessState() = runTest {
        val repository = FakePersonalDataRepository(
            profileResult = UserProfileResult.Success(personalDataProfile()),
            mutationResult = UserProfileMutationResult.Success(
                personalDataProfile(displayName = "Bruno Atualizado", phoneNumber = "913 005 855"),
            ),
        )
        val viewModel = PersonalDataViewModel(
            authRepository = FakePersonalDataAuthRepository(authenticated = true),
            profileRepository = repository,
        )

        viewModel.saveProfile(
            PersonalDataFormUi(
                displayName = "Bruno Atualizado",
                email = "bruno@example.com",
                phoneNumber = "913 005 855",
                marketingOptIn = false,
                appointmentReminderOptIn = true,
            ),
        )
        runCurrent()

        val loaded = assertIs<PersonalDataUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Bruno Atualizado", loaded.form.displayName)
        assertIs<PersonalDataSaveUiState.Saved>(viewModel.saveState.value)
        assertEquals(1, repository.updateCalls)
        assertEquals(true, repository.lastRequest?.appointmentReminderOptIn)
    }

    @Test
    fun loadProfileMapsBackendErrorsAsRetryable() = runTest {
        val viewModel = PersonalDataViewModel(
            authRepository = FakePersonalDataAuthRepository(authenticated = true),
            profileRepository = FakePersonalDataRepository(
                profileResult = UserProfileResult.Failure(
                    UserProfileError.Unavailable("Perfil indisponível."),
                ),
            ),
        )

        viewModel.loadProfile()
        runCurrent()

        val error = assertIs<PersonalDataUiState.Error>(viewModel.uiState.value)
        assertEquals("Perfil indisponível.", error.message)
        assertEquals(true, error.retryable)
    }
}

private class QueuedPersonalDataRepository : UserProfileRepository {
    val firstLoad = CompletableDeferred<UserProfileResult>()
    val secondLoad = CompletableDeferred<UserProfileResult>()
    var loadCalls: Int = 0
        private set

    override suspend fun getMyProfile(): UserProfileResult {
        loadCalls += 1
        return when (loadCalls) {
            1 -> firstLoad.await()
            2 -> secondLoad.await()
            else -> error("Unexpected profile load")
        }
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        error("Not used")
    }
}

private class FakePersonalDataRepository(
    private val profileResult: UserProfileResult,
    private val mutationResult: UserProfileMutationResult = UserProfileMutationResult.Success(personalDataProfile()),
) : UserProfileRepository {
    var loadCalls: Int = 0
        private set
    var updateCalls: Int = 0
        private set
    var lastRequest: UserProfileSaveRequest? = null
        private set

    override suspend fun getMyProfile(): UserProfileResult {
        loadCalls += 1
        return profileResult
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        updateCalls += 1
        lastRequest = request
        return mutationResult
    }
}

private class FakePersonalDataAuthRepository(
    authenticated: Boolean,
    initialState: AuthSessionState? = null,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        initialState ?: if (authenticated) authenticatedSessionState() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    fun authenticate(uid: String = "uid-1") {
        mutableSessionState.value = authenticatedSessionState(uid = uid)
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        error("Not used")
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        error("Not used")
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        error("Not used")
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private fun authenticatedSessionState(uid: String = "uid-1"): AuthSessionState.Authenticated {
    return AuthSessionState.Authenticated(
        AuthSession(
            user = AuthUser(
                uid = uid,
                email = "$uid@example.com",
                displayName = "Bruno",
                phoneNumber = "913005855",
            ),
            idToken = "id-token-$uid",
            refreshToken = "refresh-token-$uid",
            expiresInSeconds = 3600,
        ),
    )
}

private fun personalDataProfile(
    uid: String = "uid-1",
    email: String = "bruno@example.com",
    displayName: String = "Bruno",
    phoneNumber: String = "913005855",
    marketingOptIn: Boolean = false,
    appointmentReminderOptIn: Boolean = false,
): UserProfile = UserProfile(
    uid = uid,
    email = email,
    displayName = displayName,
    phoneNumber = phoneNumber,
    marketingOptIn = marketingOptIn,
    appointmentReminderOptIn = appointmentReminderOptIn,
)
