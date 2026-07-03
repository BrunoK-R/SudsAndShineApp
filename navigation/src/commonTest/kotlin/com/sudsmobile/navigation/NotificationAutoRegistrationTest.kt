package com.sudsmobile.navigation

import com.sudsmobile.data.notification.NotificationDevicePermissionStatus
import com.sudsmobile.data.notification.NotificationDeviceRegistrar
import com.sudsmobile.data.notification.NotificationDeviceRegistrationRequestResult
import com.sudsmobile.data.notification.NotificationDeviceRegistrationState
import com.sudsmobile.data.notification.NotificationError
import com.sudsmobile.data.notification.NotificationPreferencesMutationResult
import com.sudsmobile.data.notification.NotificationPreferencesResult
import com.sudsmobile.data.notification.NotificationPreferencesUpdateRequest
import com.sudsmobile.data.notification.NotificationRepository
import com.sudsmobile.data.notification.NotificationTokenDeleteRequest
import com.sudsmobile.data.notification.NotificationTokenDeleteResult
import com.sudsmobile.data.notification.NotificationTokenPlatform
import com.sudsmobile.data.notification.NotificationTokenRegistrationRequest
import com.sudsmobile.data.notification.NotificationTokenRegistrationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class NotificationAutoRegistrationTest {
    @Test
    fun registersDeviceWhenPermissionIsGrantedAndTokenIsMissing() = runTest {
        val registrar = FakeNotificationDeviceRegistrar()
        val repository = FakeNotificationRepository()

        val result = registerNotificationDeviceIfAllowed(
            userUid = " uid-1 ",
            notificationDeviceRegistrar = registrar,
            notificationRepository = repository,
        )

        assertEquals(NotificationAutoRegistrationResult.Registered, result)
        assertEquals(1, registrar.requestCalls)
        assertEquals(1, repository.registerCalls)
        assertEquals("backend-token-id", registrar.markedRegistered.single())
    }

    @Test
    fun skipsWhenDeviceAlreadyHasRegisteredTokenForUser() = runTest {
        val registrar = FakeNotificationDeviceRegistrar(
            state = NotificationDeviceRegistrationState(
                permissionStatus = NotificationDevicePermissionStatus.Granted,
                registeredTokenId = "existing-token-id",
                platform = NotificationTokenPlatform.Android,
            ),
        )
        val repository = FakeNotificationRepository()

        val result = registerNotificationDeviceIfAllowed(
            userUid = "uid-1",
            notificationDeviceRegistrar = registrar,
            notificationRepository = repository,
        )

        assertEquals(NotificationAutoRegistrationResult.AlreadyRegistered, result)
        assertEquals(0, registrar.requestCalls)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun skipsWithoutRequestingPermissionWhenPermissionIsRequired() = runTest {
        val registrar = FakeNotificationDeviceRegistrar(
            state = NotificationDeviceRegistrationState(
                permissionStatus = NotificationDevicePermissionStatus.RequiresPermission,
                platform = NotificationTokenPlatform.Android,
            ),
        )

        val result = registerNotificationDeviceIfAllowed(
            userUid = "uid-1",
            notificationDeviceRegistrar = registrar,
            notificationRepository = FakeNotificationRepository(),
        )

        assertEquals(NotificationAutoRegistrationResult.PermissionRequired, result)
        assertEquals(0, registrar.requestCalls)
    }

    @Test
    fun reportsBackendFailureWithoutMarkingDeviceRegistered() = runTest {
        val registrar = FakeNotificationDeviceRegistrar()
        val repository = FakeNotificationRepository(
            registerResult = NotificationTokenRegistrationResult.Failure(
                NotificationError.Unavailable("Indisponível."),
            ),
        )

        val result = registerNotificationDeviceIfAllowed(
            userUid = "uid-1",
            notificationDeviceRegistrar = registrar,
            notificationRepository = repository,
        )

        assertEquals(NotificationAutoRegistrationResult.BackendFailed, result)
        assertEquals(1, repository.registerCalls)
        assertEquals(emptyList(), registrar.markedRegistered)
    }
}

private class FakeNotificationDeviceRegistrar(
    private val state: NotificationDeviceRegistrationState = NotificationDeviceRegistrationState(
        permissionStatus = NotificationDevicePermissionStatus.Granted,
        platform = NotificationTokenPlatform.Android,
    ),
    private val requestResult: NotificationDeviceRegistrationRequestResult =
        NotificationDeviceRegistrationRequestResult.Success(
            NotificationTokenRegistrationRequest(
                token = "fcm-token",
                platform = NotificationTokenPlatform.Android,
                tokenId = "local-token-id",
            ),
        ),
) : NotificationDeviceRegistrar {
    var requestCalls = 0
        private set
    val markedRegistered = mutableListOf<String>()

    override suspend fun currentState(userUid: String): NotificationDeviceRegistrationState = state

    override suspend fun buildRegistrationRequest(userUid: String): NotificationDeviceRegistrationRequestResult {
        requestCalls += 1
        return requestResult
    }

    override suspend fun markRegistered(userUid: String, tokenId: String) {
        markedRegistered += tokenId
    }

    override suspend fun markDeleted(userUid: String, tokenId: String) = Unit
}

private class FakeNotificationRepository(
    private val registerResult: NotificationTokenRegistrationResult = NotificationTokenRegistrationResult.Success(
        tokenId = "backend-token-id",
        platform = NotificationTokenPlatform.Android,
        enabled = true,
    ),
) : NotificationRepository {
    var registerCalls = 0
        private set

    override suspend fun getMyNotificationPreferences(): NotificationPreferencesResult {
        error("Not used")
    }

    override suspend fun updateMyNotificationPreferences(
        request: NotificationPreferencesUpdateRequest,
    ): NotificationPreferencesMutationResult {
        error("Not used")
    }

    override suspend fun registerNotificationToken(
        request: NotificationTokenRegistrationRequest,
    ): NotificationTokenRegistrationResult {
        registerCalls += 1
        return registerResult
    }

    override suspend fun deleteNotificationToken(
        request: NotificationTokenDeleteRequest,
    ): NotificationTokenDeleteResult {
        error("Not used")
    }
}
