package com.sudsmobile.data.notification

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

class FirebaseNotificationRepositoryTest {
    @Test
    fun rejectsPreferenceLoadWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingNotificationFunctionsApi()
        val repository = FirebaseNotificationRepository(api, FakeNotificationAuthRepository(authenticated = false))

        val result = repository.getMyNotificationPreferences()

        assertIs<NotificationPreferencesResult.Failure>(result)
        assertIs<NotificationError.Unauthenticated>(result.error)
        assertEquals(0, api.preferenceLoadCalls)
    }

    @Test
    fun updatesPreferencesWithCurrentToken() = runTest {
        val api = RecordingNotificationFunctionsApi()
        val repository = FirebaseNotificationRepository(api, FakeNotificationAuthRepository(authenticated = true))

        val result = repository.updateMyNotificationPreferences(notificationPreferencesRequest())

        assertIs<NotificationPreferencesMutationResult.Success>(result)
        assertEquals("id-token-1", api.lastPreferenceIdToken)
        assertEquals(notificationPreferencesRequest(), api.lastPreferenceRequest)
    }

    @Test
    fun normalizesInstallationRegistrationWithCurrentToken() = runTest {
        val api = RecordingNotificationFunctionsApi()
        val repository = FirebaseNotificationRepository(api, FakeNotificationAuthRepository(authenticated = true))

        val result = repository.registerNotificationToken(
            NotificationTokenRegistrationRequest(
                platform = NotificationTokenPlatform.Android,
                tokenId = "current-test-device",
                deviceLabel = " Pixel   8 ",
                appVersion = " debug-1 ",
                fid = " test-firebase-installation-id-1234567890 ",
            ),
        )

        assertIs<NotificationTokenRegistrationResult.Success>(result)
        assertEquals("id-token-1", api.lastTokenIdToken)
        assertEquals("", api.lastTokenRequest?.token)
        assertEquals("test-firebase-installation-id-1234567890", api.lastTokenRequest?.fid)
        assertEquals("current-test-device", api.lastTokenRequest?.tokenId)
        assertEquals("Pixel 8", api.lastTokenRequest?.deviceLabel)
        assertEquals("debug-1", api.lastTokenRequest?.appVersion)
    }

    @Test
    fun rejectsUnsafeTokenBeforeCallingApi() = runTest {
        val api = RecordingNotificationFunctionsApi()
        val repository = FirebaseNotificationRepository(api, FakeNotificationAuthRepository(authenticated = true))

        val result = repository.registerNotificationToken(
            NotificationTokenRegistrationRequest(
                token = "test-token-for-current-device-1234567890",
                platform = NotificationTokenPlatform.Ios,
                tokenId = "users/uid-2/token",
            ),
        )

        assertIs<NotificationTokenRegistrationResult.Failure>(result)
        assertIs<NotificationError.Validation>(result.error)
        assertEquals(0, api.registerTokenCalls)
    }

    @Test
    fun rejectsRegistrationContainingBothInstallationIdAndLegacyToken() = runTest {
        val api = RecordingNotificationFunctionsApi()
        val repository = FirebaseNotificationRepository(api, FakeNotificationAuthRepository(authenticated = true))

        val result = repository.registerNotificationToken(
            NotificationTokenRegistrationRequest(
                token = "test-token-for-current-device-1234567890",
                platform = NotificationTokenPlatform.Android,
                fid = "test-firebase-installation-id-1234567890",
            ),
        )

        assertIs<NotificationTokenRegistrationResult.Failure>(result)
        assertIs<NotificationError.Validation>(result.error)
        assertEquals(0, api.registerTokenCalls)
    }

    @Test
    fun deletesTokenWithCurrentToken() = runTest {
        val api = RecordingNotificationFunctionsApi()
        val repository = FirebaseNotificationRepository(api, FakeNotificationAuthRepository(authenticated = true))

        val result = repository.deleteNotificationToken(NotificationTokenDeleteRequest(tokenId = " current-test-device "))

        val success = assertIs<NotificationTokenDeleteResult.Success>(result)
        assertEquals("current-test-device", success.tokenId)
        assertEquals("id-token-1", api.lastDeleteIdToken)
        assertEquals("current-test-device", api.lastDeleteRequest?.tokenId)
    }
}

private class RecordingNotificationFunctionsApi : NotificationFunctionsApi {
    var preferenceLoadCalls = 0
    var registerTokenCalls = 0
    var lastPreferenceRequest: NotificationPreferencesUpdateRequest? = null
    var lastPreferenceIdToken: String? = null
    var lastTokenRequest: NotificationTokenRegistrationRequest? = null
    var lastTokenIdToken: String? = null
    var lastDeleteRequest: NotificationTokenDeleteRequest? = null
    var lastDeleteIdToken: String? = null

    override suspend fun getMyNotificationPreferences(idToken: String): NotificationPreferencesResult {
        preferenceLoadCalls += 1
        lastPreferenceIdToken = idToken
        return NotificationPreferencesResult.Success(notificationPreferences())
    }

    override suspend fun updateMyNotificationPreferences(
        request: NotificationPreferencesUpdateRequest,
        idToken: String,
    ): NotificationPreferencesMutationResult {
        lastPreferenceRequest = request
        lastPreferenceIdToken = idToken
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
        idToken: String,
    ): NotificationTokenRegistrationResult {
        registerTokenCalls += 1
        lastTokenRequest = request
        lastTokenIdToken = idToken
        return NotificationTokenRegistrationResult.Success("token-id-1", request.platform, enabled = true)
    }

    override suspend fun deleteNotificationToken(
        request: NotificationTokenDeleteRequest,
        idToken: String,
    ): NotificationTokenDeleteResult {
        lastDeleteRequest = request
        lastDeleteIdToken = idToken
        return NotificationTokenDeleteResult.Success(request.tokenId, "revoked")
    }
}

private class FakeNotificationAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    override val sessionState: StateFlow<AuthSessionState> = MutableStateFlow(
        if (authenticated) {
            AuthSessionState.Authenticated(
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
        } else {
            AuthSessionState.Unauthenticated
        },
    )

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

    override fun signOut() = Unit
}

private fun notificationPreferences(): NotificationPreferences = NotificationPreferences(
    bookingStatusEnabled = true,
    appointmentReminderEnabled = false,
    loyaltyEnabled = true,
    adminPendingAlertEnabled = true,
    marketingEnabled = false,
)

private fun notificationPreferencesRequest(): NotificationPreferencesUpdateRequest {
    return NotificationPreferencesUpdateRequest(
        bookingStatusEnabled = true,
        appointmentReminderEnabled = true,
        loyaltyEnabled = false,
        adminPendingAlertEnabled = false,
        marketingEnabled = false,
    )
}
