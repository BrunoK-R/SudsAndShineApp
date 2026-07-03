package com.sudsmobile.navigation

import com.sudsmobile.data.notification.NotificationDeviceRegistrar
import com.sudsmobile.data.notification.NotificationDevicePermissionStatus
import com.sudsmobile.data.notification.NotificationDeviceRegistrationRequestResult
import com.sudsmobile.data.notification.NotificationRepository
import com.sudsmobile.data.notification.NotificationTokenRegistrationResult

internal enum class NotificationAutoRegistrationResult {
    Registered,
    AlreadyRegistered,
    PermissionRequired,
    Unsupported,
    RequestFailed,
    BackendFailed,
}

internal suspend fun registerNotificationDeviceIfAllowed(
    userUid: String,
    notificationDeviceRegistrar: NotificationDeviceRegistrar,
    notificationRepository: NotificationRepository,
): NotificationAutoRegistrationResult {
    val uid = userUid.trim()
    if (uid.isBlank()) return NotificationAutoRegistrationResult.RequestFailed

    val state = notificationDeviceRegistrar.currentState(uid)
    if (state.registeredTokenId != null) {
        return NotificationAutoRegistrationResult.AlreadyRegistered
    }
    if (!state.canRegister) {
        return when (state.permissionStatus) {
            NotificationDevicePermissionStatus.RequiresPermission ->
                NotificationAutoRegistrationResult.PermissionRequired
            NotificationDevicePermissionStatus.Unsupported ->
                NotificationAutoRegistrationResult.Unsupported
            else -> NotificationAutoRegistrationResult.RequestFailed
        }
    }

    return when (val request = notificationDeviceRegistrar.buildRegistrationRequest(uid)) {
        is NotificationDeviceRegistrationRequestResult.Success -> {
            when (val result = notificationRepository.registerNotificationToken(request.request)) {
                is NotificationTokenRegistrationResult.Success -> {
                    notificationDeviceRegistrar.markRegistered(uid, result.tokenId)
                    NotificationAutoRegistrationResult.Registered
                }
                is NotificationTokenRegistrationResult.Failure -> NotificationAutoRegistrationResult.BackendFailed
            }
        }
        is NotificationDeviceRegistrationRequestResult.PermissionRequired ->
            NotificationAutoRegistrationResult.PermissionRequired
        is NotificationDeviceRegistrationRequestResult.Unsupported ->
            NotificationAutoRegistrationResult.Unsupported
        is NotificationDeviceRegistrationRequestResult.Failure ->
            NotificationAutoRegistrationResult.RequestFailed
    }
}
