package com.sudsmobile.feature.profile

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
    fun loadProfileMapsBackendProfileToForm() = runTest {
        val viewModel = PersonalDataViewModel(
            authRepository = FakePersonalDataAuthRepository(authenticated = true),
            profileRepository = FakePersonalDataRepository(
                profileResult = UserProfileResult.Success(
                    personalDataProfile(displayName = "Bruno Ribeiro", marketingOptIn = true),
                ),
            ),
        )

        viewModel.loadProfile()
        runCurrent()

        val loaded = assertIs<PersonalDataUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Bruno Ribeiro", loaded.form.displayName)
        assertEquals("bruno@example.com", loaded.form.email)
        assertEquals(true, loaded.form.marketingOptIn)
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
            ),
        )
        runCurrent()

        val loaded = assertIs<PersonalDataUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Bruno Atualizado", loaded.form.displayName)
        assertIs<PersonalDataSaveUiState.Saved>(viewModel.saveState.value)
        assertEquals(1, repository.updateCalls)
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

private class FakePersonalDataRepository(
    private val profileResult: UserProfileResult,
    private val mutationResult: UserProfileMutationResult = UserProfileMutationResult.Success(personalDataProfile()),
) : UserProfileRepository {
    var loadCalls: Int = 0
        private set
    var updateCalls: Int = 0
        private set

    override suspend fun getMyProfile(): UserProfileResult {
        loadCalls += 1
        return profileResult
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        updateCalls += 1
        return mutationResult
    }
}

private class FakePersonalDataAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) authenticatedSessionState() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
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

private fun authenticatedSessionState(): AuthSessionState.Authenticated {
    return AuthSessionState.Authenticated(
        AuthSession(
            user = AuthUser(
                uid = "uid-1",
                email = "bruno@example.com",
                displayName = "Bruno",
                phoneNumber = "913005855",
            ),
            idToken = "id-token-1",
            refreshToken = "refresh-token-1",
            expiresInSeconds = 3600,
        ),
    )
}

private fun personalDataProfile(
    displayName: String = "Bruno",
    phoneNumber: String = "913005855",
    marketingOptIn: Boolean = false,
): UserProfile = UserProfile(
    uid = "uid-1",
    email = "bruno@example.com",
    displayName = displayName,
    phoneNumber = phoneNumber,
    marketingOptIn = marketingOptIn,
)
