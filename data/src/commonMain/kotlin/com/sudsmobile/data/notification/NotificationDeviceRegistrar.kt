package com.sudsmobile.data.notification

data class NotificationDeviceRegistrationState(
    val permissionStatus: NotificationDevicePermissionStatus,
    val registeredTokenId: String? = null,
    val platform: NotificationTokenPlatform? = null,
) {
    val canRegister: Boolean
        get() = permissionStatus == NotificationDevicePermissionStatus.Granted ||
            permissionStatus == NotificationDevicePermissionStatus.NotRequired
}

enum class NotificationDevicePermissionStatus {
    Granted,
    NotRequired,
    RequiresPermission,
    Unsupported,
}

sealed interface NotificationDeviceRegistrationRequestResult {
    data class Success(val request: NotificationTokenRegistrationRequest) : NotificationDeviceRegistrationRequestResult
    data class PermissionRequired(val message: String) : NotificationDeviceRegistrationRequestResult
    data class Unsupported(val message: String) : NotificationDeviceRegistrationRequestResult
    data class Failure(val message: String) : NotificationDeviceRegistrationRequestResult
}

interface NotificationDeviceRegistrar {
    suspend fun currentState(userUid: String): NotificationDeviceRegistrationState
    suspend fun buildRegistrationRequest(userUid: String): NotificationDeviceRegistrationRequestResult
    suspend fun markRegistered(userUid: String, tokenId: String)
    suspend fun markDeleted(userUid: String, tokenId: String)
}

class UnsupportedNotificationDeviceRegistrar : NotificationDeviceRegistrar {
    override suspend fun currentState(userUid: String): NotificationDeviceRegistrationState {
        return NotificationDeviceRegistrationState(
            permissionStatus = NotificationDevicePermissionStatus.Unsupported,
        )
    }

    override suspend fun buildRegistrationRequest(userUid: String): NotificationDeviceRegistrationRequestResult {
        return NotificationDeviceRegistrationRequestResult.Unsupported(
            "As notificações push ainda não estão disponíveis nesta plataforma.",
        )
    }

    override suspend fun markRegistered(userUid: String, tokenId: String) = Unit

    override suspend fun markDeleted(userUid: String, tokenId: String) = Unit
}
