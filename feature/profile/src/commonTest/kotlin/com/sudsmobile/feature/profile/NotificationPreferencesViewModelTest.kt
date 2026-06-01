package com.sudsmobile.feature.profile

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.notification.NotificationError
import com.sudsmobile.data.notification.NotificationDevicePermissionStatus
import com.sudsmobile.data.notification.NotificationDeviceRegistrar
import com.sudsmobile.data.notification.NotificationDeviceRegistrationRequestResult
import com.sudsmobile.data.notification.NotificationDeviceRegistrationState
import com.sudsmobile.data.notification.NotificationPreferences
import com.sudsmobile.data.notification.NotificationPreferencesMutationResult
import com.sudsmobile.data.notification.NotificationPreferencesResult
import com.sudsmobile.data.notification.NotificationPreferencesUpdateRequest
import com.sudsmobile.data.notification.NotificationRepository
import com.sudsmobile.data.notification.NotificationTokenDeleteRequest
import com.sudsmobile.data.notification.NotificationTokenDeleteResult
import com.sudsmobile.data.notification.NotificationTokenPlatform
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
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(),
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
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(),
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
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(),
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
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(),
        )

        viewModel.loadPreferences()
        runCurrent()
        viewModel.updateForm(
            NotificationPreferencesForm(
                bookingStatusEnabled = false,
                appointmentReminderEnabled = true,
                loyaltyEnabled = false,
                adminPendingAlertEnabled = false,
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
                adminPendingAlertEnabled = false,
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
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(),
        )

        viewModel.loadPreferences()
        runCurrent()
        viewModel.save()
        authRepository.signOut()
        runCurrent()

        assertEquals(0, repository.updateRequests.size)
        assertIs<NotificationPreferencesUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun registerCurrentDeviceRequiresPermissionBeforeRepositoryCall() = runTest {
        val repository = FakeNotificationPreferencesRepository()
        val viewModel = NotificationPreferencesViewModel(
            authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true),
            notificationRepository = repository,
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(
                requestResult = NotificationDeviceRegistrationRequestResult.PermissionRequired("permission needed"),
            ),
        )

        viewModel.registerCurrentDevice()
        runCurrent()

        val state = assertIs<NotificationDeviceUiState.PermissionRequired>(viewModel.deviceState.value)
        assertEquals("permission needed", state.message)
        assertEquals(0, repository.registerRequests.size)
    }

    @Test
    fun registerCurrentDeviceStoresBackendTokenId() = runTest {
        val repository = FakeNotificationPreferencesRepository()
        val registrar = FakeNotificationDeviceRegistrar()
        val viewModel = NotificationPreferencesViewModel(
            authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true),
            notificationRepository = repository,
            notificationDeviceRegistrar = registrar,
        )

        viewModel.registerCurrentDevice()
        runCurrent()

        val state = assertIs<NotificationDeviceUiState.Success>(viewModel.deviceState.value)
        assertEquals("token-id-1", state.registeredTokenId)
        assertEquals(notificationTokenRequest(), repository.registerRequests.single())
        assertEquals(listOf("token-id-1"), registrar.markedRegisteredTokenIds)
    }

    @Test
    fun registerCurrentDeviceStopsWhenSessionChangesBeforeRepositoryCall() = runTest {
        val requestDeferred = CompletableDeferred<NotificationDeviceRegistrationRequestResult>()
        val authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true)
        val repository = FakeNotificationPreferencesRepository()
        val viewModel = NotificationPreferencesViewModel(
            authRepository = authRepository,
            notificationRepository = repository,
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(requestDeferred = requestDeferred),
        )

        viewModel.registerCurrentDevice()
        runCurrent()
        authRepository.signOut()
        requestDeferred.complete(NotificationDeviceRegistrationRequestResult.Success(notificationTokenRequest()))
        runCurrent()

        assertIs<NotificationDeviceUiState.Unauthenticated>(viewModel.deviceState.value)
        assertEquals(0, repository.registerRequests.size)
    }

    @Test
    fun registerCurrentDeviceIgnoresPermissionResultAfterSignOut() = runTest {
        val requestDeferred = CompletableDeferred<NotificationDeviceRegistrationRequestResult>()
        val authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true)
        val repository = FakeNotificationPreferencesRepository()
        val viewModel = NotificationPreferencesViewModel(
            authRepository = authRepository,
            notificationRepository = repository,
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(requestDeferred = requestDeferred),
        )

        viewModel.registerCurrentDevice()
        runCurrent()
        authRepository.signOut()
        requestDeferred.complete(
            NotificationDeviceRegistrationRequestResult.PermissionRequired("permission needed"),
        )
        runCurrent()

        assertIs<NotificationDeviceUiState.Unauthenticated>(viewModel.deviceState.value)
        assertEquals(0, repository.registerRequests.size)
    }

    @Test
    fun permissionDenialAfterSignOutShowsUnauthenticatedDeviceState() = runTest {
        val authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true)
        val viewModel = NotificationPreferencesViewModel(
            authRepository = authRepository,
            notificationRepository = FakeNotificationPreferencesRepository(),
            notificationDeviceRegistrar = FakeNotificationDeviceRegistrar(),
        )

        authRepository.signOut()
        viewModel.handlePermissionResult(granted = false)

        assertIs<NotificationDeviceUiState.Unauthenticated>(viewModel.deviceState.value)
    }

    @Test
    fun removeCurrentDeviceDeletesRegisteredToken() = runTest {
        val repository = FakeNotificationPreferencesRepository()
        val registrar = FakeNotificationDeviceRegistrar(
            currentState = NotificationDeviceRegistrationState(
                permissionStatus = NotificationDevicePermissionStatus.Granted,
                registeredTokenId = "token-id-1",
                platform = NotificationTokenPlatform.Android,
            ),
        )
        val viewModel = NotificationPreferencesViewModel(
            authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true),
            notificationRepository = repository,
            notificationDeviceRegistrar = registrar,
        )

        viewModel.refreshForSession()
        runCurrent()
        viewModel.removeCurrentDevice()
        runCurrent()

        val state = assertIs<NotificationDeviceUiState.Success>(viewModel.deviceState.value)
        assertEquals(null, state.registeredTokenId)
        assertEquals(NotificationTokenDeleteRequest("token-id-1"), repository.deleteRequests.single())
        assertEquals(listOf("token-id-1"), registrar.markedDeletedTokenIds)
    }

    @Test
    fun removeCurrentDeviceIgnoresDuplicateTapWhileRemoving() = runTest {
        val deleteDeferred = CompletableDeferred<NotificationTokenDeleteResult>()
        val repository = FakeNotificationPreferencesRepository(deleteDeferred = deleteDeferred)
        val registrar = FakeNotificationDeviceRegistrar(
            currentState = NotificationDeviceRegistrationState(
                permissionStatus = NotificationDevicePermissionStatus.Granted,
                registeredTokenId = "token-id-1",
                platform = NotificationTokenPlatform.Android,
            ),
        )
        val viewModel = NotificationPreferencesViewModel(
            authRepository = FakeNotificationPreferencesAuthRepository(authenticated = true),
            notificationRepository = repository,
            notificationDeviceRegistrar = registrar,
        )

        viewModel.refreshForSession()
        runCurrent()
        viewModel.removeCurrentDevice()
        runCurrent()
        viewModel.removeCurrentDevice()
        runCurrent()
        deleteDeferred.complete(NotificationTokenDeleteResult.Success("token-id-1", "revoked"))
        runCurrent()

        assertEquals(listOf(NotificationTokenDeleteRequest("token-id-1")), repository.deleteRequests)
        assertEquals(listOf("token-id-1"), registrar.markedDeletedTokenIds)
    }
}

private class FakeNotificationPreferencesRepository(
    private val loadResult: NotificationPreferencesResult =
        NotificationPreferencesResult.Success(notificationPreferences()),
    private val loadDeferred: CompletableDeferred<NotificationPreferencesResult>? = null,
    private val deleteDeferred: CompletableDeferred<NotificationTokenDeleteResult>? = null,
) : NotificationRepository {
    var loadCalls = 0
        private set
    val updateRequests = mutableListOf<NotificationPreferencesUpdateRequest>()
    val registerRequests = mutableListOf<NotificationTokenRegistrationRequest>()
    val deleteRequests = mutableListOf<NotificationTokenDeleteRequest>()

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
                adminPendingAlertEnabled = request.adminPendingAlertEnabled,
                marketingEnabled = request.marketingEnabled,
            ),
        )
    }

    override suspend fun registerNotificationToken(
        request: NotificationTokenRegistrationRequest,
    ): NotificationTokenRegistrationResult {
        registerRequests += request
        return NotificationTokenRegistrationResult.Success("token-id-1", request.platform, enabled = true)
    }

    override suspend fun deleteNotificationToken(
        request: NotificationTokenDeleteRequest,
    ): NotificationTokenDeleteResult {
        deleteRequests += request
        return deleteDeferred?.await() ?: NotificationTokenDeleteResult.Success(request.tokenId, "revoked")
    }
}

private class FakeNotificationDeviceRegistrar(
    private val currentState: NotificationDeviceRegistrationState = NotificationDeviceRegistrationState(
        permissionStatus = NotificationDevicePermissionStatus.Granted,
        registeredTokenId = null,
        platform = NotificationTokenPlatform.Android,
    ),
    private val requestResult: NotificationDeviceRegistrationRequestResult =
        NotificationDeviceRegistrationRequestResult.Success(notificationTokenRequest()),
    private val requestDeferred: CompletableDeferred<NotificationDeviceRegistrationRequestResult>? = null,
) : NotificationDeviceRegistrar {
    val markedRegisteredTokenIds = mutableListOf<String>()
    val markedDeletedTokenIds = mutableListOf<String>()

    override suspend fun currentState(): NotificationDeviceRegistrationState {
        return currentState
    }

    override suspend fun buildRegistrationRequest(): NotificationDeviceRegistrationRequestResult {
        return requestDeferred?.await() ?: requestResult
    }

    override suspend fun markRegistered(tokenId: String) {
        markedRegisteredTokenIds += tokenId
    }

    override suspend fun markDeleted(tokenId: String) {
        markedDeletedTokenIds += tokenId
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
    adminPendingAlertEnabled = true,
    marketingEnabled = false,
)

private fun notificationTokenRequest(): NotificationTokenRegistrationRequest {
    return NotificationTokenRegistrationRequest(
        token = "test-token-for-current-device-1234567890",
        platform = NotificationTokenPlatform.Android,
        tokenId = "current-test-device",
        deviceLabel = "Pixel Test",
        appVersion = "debug-1",
    )
}
