package com.sudsmobile.data.notification

import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Refresh native settings first, then refresh this account's push registration. */
class NotificationDeviceSync(
    private val authRepository: AuthRepository,
    private val repository: NotificationRepository,
    private val registrar: NotificationDeviceRegistrar,
) {
    val sessionState = authRepository.sessionState
    private val _notificationsEnabled = MutableStateFlow<Boolean?>(null)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()
    private val mutex = Mutex()

    suspend fun refresh() = mutex.withLock {
        val uid = authenticatedUid()
        try {
            val device = registrar.currentState(uid.orEmpty())
            _notificationsEnabled.value = when (device.permissionStatus) {
                NotificationDevicePermissionStatus.Granted,
                NotificationDevicePermissionStatus.NotRequired -> true
                NotificationDevicePermissionStatus.RequiresPermission -> false
                NotificationDevicePermissionStatus.Unsupported -> null
            }
            if (uid == null || uid != authenticatedUid()) return@withLock
            if (!device.canRegister) {
                device.registeredTokenId?.let { tokenId ->
                    if (repository.deleteNotificationToken(NotificationTokenDeleteRequest(tokenId)) is NotificationTokenDeleteResult.Success &&
                        uid == authenticatedUid()) registrar.markDeleted(uid, tokenId)
                }
                return@withLock
            }
            val request = registrar.buildRegistrationRequest(uid)
            if (uid != authenticatedUid() || request !is NotificationDeviceRegistrationRequestResult.Success) return@withLock
            val result = repository.registerNotificationToken(request.request)
            if (uid == authenticatedUid() && result is NotificationTokenRegistrationResult.Success) {
                registrar.markRegistered(uid, result.tokenId)
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (_: Exception) {
            // Native permission remains visible even when registration must retry on the next resume.
        }
    }

    private fun authenticatedUid(): String? =
        (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
}
