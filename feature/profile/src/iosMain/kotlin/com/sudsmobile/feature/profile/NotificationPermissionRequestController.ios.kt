package com.sudsmobile.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberNotificationPermissionRequestController(
    onPermissionResult: (Boolean) -> Unit,
): NotificationPermissionRequestController {
    return remember(onPermissionResult) {
        NotificationPermissionRequestController(
            shouldRequestPostNotifications = false,
            requestPostNotifications = { onPermissionResult(false) },
        )
    }
}
