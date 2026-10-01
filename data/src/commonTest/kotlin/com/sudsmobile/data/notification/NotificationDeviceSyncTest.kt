package com.sudsmobile.data.notification

import com.sudsmobile.data.auth.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationDeviceSyncTest {
    @Test
    fun disabledNativeNotificationsRemainVisibleBeforeLogin() = runTest {
        val auth = SyncAuth()
        auth.signOut()
        val registrar = SyncRegistrar(allowed = false)
        val repository = SyncRepository()
        val sync = NotificationDeviceSync(auth, repository, registrar)
        sync.refresh()
        assertEquals(false, sync.notificationsEnabled.value)
        assertEquals(0, repository.registrations)
        assertEquals(0, repository.deletions)
    }

    @Test
    fun disablingThenEnablingNativeNotificationsRevokesAndRegistersDevice() = runTest {
        val registrar = SyncRegistrar(allowed = false, tokenId = "registered-device")
        val repository = SyncRepository()
        val sync = NotificationDeviceSync(SyncAuth(), repository, registrar)
        sync.refresh()
        assertEquals(false, sync.notificationsEnabled.value)
        assertEquals(1, repository.deletions)
        assertEquals(null, registrar.tokenId)
        registrar.allowed = true
        sync.refresh()
        assertEquals(true, sync.notificationsEnabled.value)
        assertEquals(1, repository.registrations)
        assertEquals("registered-device", registrar.tokenId)
    }

    @Test
    fun accountChangeDuringRegistrationDoesNotSendForTheNewAccount() = runTest {
        val auth = SyncAuth()
        val registrar = SyncRegistrar(beforeRequest = { auth.signOut() })
        val repository = SyncRepository()
        NotificationDeviceSync(auth, repository, registrar).refresh()
        assertEquals(0, repository.registrations)
        assertEquals(null, registrar.tokenId)
    }

    @Test
    fun backendFailurePreservesNativeStateAndRetriesOnResume() = runTest {
        val registrar = SyncRegistrar()
        val repository = SyncRepository().apply { fail = true }
        val sync = NotificationDeviceSync(SyncAuth(), repository, registrar)
        sync.refresh()
        assertEquals(true, sync.notificationsEnabled.value)
        assertEquals(null, registrar.tokenId)
        repository.fail = false
        sync.refresh()
        assertEquals(2, repository.registrations)
        assertEquals("registered-device", registrar.tokenId)
    }
}

private class SyncRegistrar(
    var allowed: Boolean = true,
    var tokenId: String? = null,
    private val beforeRequest: () -> Unit = {},
) : NotificationDeviceRegistrar {
    override suspend fun currentState(userUid: String) = NotificationDeviceRegistrationState(
        if (allowed) NotificationDevicePermissionStatus.Granted else NotificationDevicePermissionStatus.RequiresPermission,
        tokenId,
        NotificationTokenPlatform.Android,
    )
    override suspend fun buildRegistrationRequest(userUid: String): NotificationDeviceRegistrationRequestResult {
        beforeRequest()
        return NotificationDeviceRegistrationRequestResult.Success(
            NotificationTokenRegistrationRequest(platform = NotificationTokenPlatform.Android, fid = "installation-id-for-test", tokenId = "registered-device"),
        )
    }
    override suspend fun markRegistered(userUid: String, tokenId: String) { this.tokenId = tokenId }
    override suspend fun markDeleted(userUid: String, tokenId: String) { this.tokenId = null }
}

private class SyncRepository : NotificationRepository {
    var registrations = 0
    var deletions = 0
    var fail = false
    override suspend fun registerNotificationToken(request: NotificationTokenRegistrationRequest): NotificationTokenRegistrationResult {
        registrations++
        return if (fail) NotificationTokenRegistrationResult.Failure(NotificationError.Unavailable("Offline"))
        else NotificationTokenRegistrationResult.Success(request.tokenId, request.platform, true)
    }
    override suspend fun deleteNotificationToken(request: NotificationTokenDeleteRequest): NotificationTokenDeleteResult {
        deletions++
        return NotificationTokenDeleteResult.Success(request.tokenId, "deleted")
    }
    override suspend fun getMyNotificationPreferences(): NotificationPreferencesResult = error("Unused")
    override suspend fun updateMyNotificationPreferences(request: NotificationPreferencesUpdateRequest): NotificationPreferencesMutationResult = error("Unused")
}

private class SyncAuth : AuthRepository {
    override val sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Authenticated(
        AuthSession(AuthUser("test-user", "user@example.test", "User", ""), "id-token", "refresh-token", 3600),
    ))
    override suspend fun currentSession() = (sessionState.value as? AuthSessionState.Authenticated)?.session
    override suspend fun signIn(email: String, password: String): AuthResult = error("Unused")
    override suspend fun register(displayName: String, email: String, phoneNumber: String, password: String): AuthResult = error("Unused")
    override suspend fun sendPasswordReset(email: String): AuthActionResult = error("Unused")
    override fun signOut() { sessionState.value = AuthSessionState.Unauthenticated }
}
