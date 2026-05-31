package com.sudsmobile.feature.profile

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.notification.NotificationError
import com.sudsmobile.data.notification.NotificationPreferences
import com.sudsmobile.data.notification.NotificationPreferencesMutationResult
import com.sudsmobile.data.notification.NotificationPreferencesResult
import com.sudsmobile.data.notification.NotificationPreferencesUpdateRequest
import com.sudsmobile.data.notification.NotificationRepository
import com.sudsmobile.data.notification.NotificationTokenDeleteRequest
import com.sudsmobile.data.notification.NotificationTokenDeleteResult
import com.sudsmobile.data.notification.NotificationTokenRegistrationResult
import com.sudsmobile.data.notification.NotificationTokenRegistrationRequest
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
class NotificationPreferencesViewModelTest {
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
    fun loadPreferencesRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeNotificationPreferencesRepository()
        val viewModel = NotificationPreferencesViewModel(
            authRepository = FakeNotificationPreferencesAuthRepository(authenticated = false),
            notificationRepository = repository,
        )

        viewModel.loadPreferences()
        runCurrent()

        assertIs<NotificationPreferencesUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadPreferencesMapsBackendError() = runTest {
        val viewModel = NotificationPreferencesViewModel(
            authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true),
            notificationRepository = FakeNotificationPreferencesRepository(
                loadResult = NotificationPreferencesResult.Failure(NotificationError.Backend("failed")),
            ),
        )

        viewModel.loadPreferences()
        runCurrent()

        val state = assertIs<NotificationPreferencesUiState.Error>(viewModel.uiState.value)
        assertEquals("failed", state.message)
        assertEquals(true, state.retryable)
    }

    @Test
    fun loadPreferencesIgnoresStaleResponseAfterSignOut() = runTest {
        val deferred = CompletableDeferred<NotificationPreferencesResult>()
        val authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true)
        val viewModel = NotificationPreferencesViewModel(
            authRepository = authRepository,
            notificationRepository = FakeNotificationPreferencesRepository(loadDeferred = deferred),
        )

        viewModel.loadPreferences()
        runCurrent()
        authRepository.signOut()
        deferred.complete(NotificationPreferencesResult.Success(notificationPreferences()))
        runCurrent()

        assertIs<NotificationPreferencesUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun saveSubmitsCurrentForm() = runTest {
        val repository = FakeNotificationPreferencesRepository()
        val viewModel = NotificationPreferencesViewModel(
            authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true),
            notificationRepository = repository,
        )

        viewModel.loadPreferences()
        runCurrent()
        viewModel.updateForm(
            NotificationPreferencesForm(
                bookingStatusEnabled = false,
                appointmentReminderEnabled = true,
                loyaltyEnabled = false,
                marketingEnabled = true,
            ),
        )
        viewModel.save()
        runCurrent()

        assertIs<NotificationPreferencesSaveState.Success>(viewModel.saveState.value)
        assertEquals(
            NotificationPreferencesUpdateRequest(
                bookingStatusEnabled = false,
                appointmentReminderEnabled = true,
                loyaltyEnabled = false,
                marketingEnabled = true,
            ),
            repository.updateRequests.single(),
        )
    }

    @Test
    fun saveStopsWhenSessionChangesBeforeRepositoryCall() = runTest {
        val authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true)
        val repository = FakeNotificationPreferencesRepository()
        val viewModel = NotificationPreferencesViewModel(
            authRepository = authRepository,
            notificationRepository = repository,
        )

        viewModel.loadPreferences()
        runCurrent()
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.updateRequests.size)
        assertIs<NotificationPreferencesUiState.Unauthenticated>(viewModel.uiState.value)
    }
}

private class FakeNotificationPreferencesRepository(
    private val loadResult: NotificationPreferencesResult =
        NotificationPreferencesResult.Success(notificationPreferences()),
    private val loadDeferred: CompletableDeferred<NotificationPreferencesResult>? = null,
) : NotificationRepository {
    var loadCalls = 0
        private set
    val updateRequests = mutableListOf<NotificationPreferencesUpdateRequest>()

    override suspend fun getMyNotificationPreferences(): NotificationPreferencesResult {
        loadCalls += 1
        return loadDeferred?.await() ?: loadResult
    }

    override suspend fun updateMyNotificationPreferences(
        request: NotificationPreferencesUpdateRequest,
    ): NotificationPreferencesMutationResult {
        updateRequests += request
        return NotificationPreferencesMutationResult.Success(
            NotificationPreferences(
                bookingStatusEnabled = request.bookingStatusEnabled,
                appointmentReminderEnabled = request.appointmentReminderEnabled,
                loyaltyEnabled = request.loyaltyEnabled,
                marketingEnabled = request.marketingEnabled,
            ),
        )
    }

    override suspend fun registerNotificationToken(
        request: NotificationTokenRegistrationRequest,
    ): NotificationTokenRegistrationResult {
        error("Not used")
    }

    override suspend fun deleteNotificationToken(
        request: NotificationTokenDeleteRequest,
    ): NotificationTokenDeleteResult {
        error("Not used")
    }
}

private class FakeNotificationPreferencesAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) {
            AuthSessionState.Authenticated(authSession())
        } else {
            AuthSessionState.Unauthenticated
        },
    )

    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session
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

private fun authSession(uid: String = "uid-1"): AuthSession {
    return AuthSession(
        user = AuthUser(
            uid = uid,
            email = "$uid@example.com",
            displayName = "Bruno",
            phoneNumber = "913005855",
        ),
        idToken = "id-token-$uid",
        refreshToken = "refresh-token-$uid",
        expiresInSeconds = 3600,
    )
}

private fun notificationPreferences(): NotificationPreferences = NotificationPreferences(
    bookingStatusEnabled = true,
    appointmentReminderEnabled = true,
    loyaltyEnabled = true,
    marketingEnabled = false,
)
