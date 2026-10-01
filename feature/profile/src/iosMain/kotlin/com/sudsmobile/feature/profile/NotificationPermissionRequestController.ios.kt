package com.sudsmobile.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.*
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString

@Composable
@OptIn(ExperimentalForeignApi::class)
internal actual fun rememberNotificationPermissionRequestController(
    onPermissionResult: (Boolean) -> Unit,
): NotificationPermissionRequestController {
    val scope = rememberCoroutineScope()
    return remember(onPermissionResult, scope) {
        NotificationPermissionRequestController(
            shouldRequestPostNotifications = true,
            requestPostNotifications = {
                scope.launch {
                    val center = UNUserNotificationCenter.currentNotificationCenter()
                    val status = suspendCancellableCoroutine { continuation ->
                        center.getNotificationSettingsWithCompletionHandler {
                            if (continuation.isActive) continuation.resume(it?.authorizationStatus)
                        }
                    }
                    if (status == UNAuthorizationStatusDenied) {
                        NSURL.URLWithString(UIApplicationOpenNotificationSettingsURLString)?.let {
                            UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any?>(), null)
                        }
                        onPermissionResult(false)
                    } else {
                        val allowed = suspendCancellableCoroutine { continuation ->
                            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound) { granted, _ ->
                                if (continuation.isActive) continuation.resume(granted)
                            }
                        }
                        onPermissionResult(allowed)
                    }
                }
            },
        )
    }
}
