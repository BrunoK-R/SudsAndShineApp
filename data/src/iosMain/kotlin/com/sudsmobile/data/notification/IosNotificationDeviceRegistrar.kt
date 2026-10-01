package com.sudsmobile.data.notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import platform.UIKit.UIDevice
import platform.UserNotifications.*

object IosNotificationRegistrationBridge {
    private var handler: (((String) -> Unit, (String) -> Unit) -> Unit)? = null

    fun configure(handler: (((String) -> Unit, (String) -> Unit) -> Unit)?) { this.handler = handler }

    suspend fun installationId(): String = withTimeoutOrNull(30_000) {
        suspendCancellableCoroutine { continuation ->
        val current = handler
        if (current == null) {
            continuation.resume("")
        } else current(
            { if (continuation.isActive) continuation.resume(it) },
            { if (continuation.isActive) continuation.resume("") },
        )
        }
    }.orEmpty()
}

@OptIn(ExperimentalForeignApi::class)
class IosNotificationDeviceRegistrar : NotificationDeviceRegistrar {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun currentState(userUid: String): NotificationDeviceRegistrationState {
        val permission = suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
                val allowed = settings?.authorizationStatus in listOf(
                    UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral,
                )
                if (continuation.isActive) continuation.resume(
                    if (allowed) NotificationDevicePermissionStatus.Granted else NotificationDevicePermissionStatus.RequiresPermission,
                )
            }
        }
        return NotificationDeviceRegistrationState(permission, defaults.stringForKey("suds_registered_push:$userUid"), NotificationTokenPlatform.Ios)
    }

    override suspend fun buildRegistrationRequest(userUid: String): NotificationDeviceRegistrationRequestResult {
        if (!currentState(userUid).canRegister) return NotificationDeviceRegistrationRequestResult.PermissionRequired(
            "Autorize notificações nas definições do iPhone para ativar os avisos.",
        )
        val fid = IosNotificationRegistrationBridge.installationId().trim()
        if (fid.isBlank()) return NotificationDeviceRegistrationRequestResult.Failure(
            "Não foi possível registar notificações neste iPhone. Tente novamente.",
        )
        var tokenId = defaults.stringForKey("suds_push_device_id")
        if (tokenId.isNullOrBlank()) {
            tokenId = "ios-${NSUUID().UUIDString}"
            defaults.setObject(tokenId, "suds_push_device_id")
        }
        return NotificationDeviceRegistrationRequestResult.Success(NotificationTokenRegistrationRequest(
            platform = NotificationTokenPlatform.Ios, tokenId = tokenId,
            fid = fid, deviceLabel = UIDevice.currentDevice.model.take(120),
            appVersion = (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String).orEmpty(),
        ))
    }

    override suspend fun markRegistered(userUid: String, tokenId: String) {
        defaults.setObject(tokenId, "suds_registered_push:$userUid")
    }

    override suspend fun markDeleted(userUid: String, tokenId: String) {
        if (defaults.stringForKey("suds_registered_push:$userUid") == tokenId) defaults.removeObjectForKey("suds_registered_push:$userUid")
    }
}
