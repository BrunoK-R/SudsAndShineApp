package com.sudsmobile.feature.profile

import androidx.compose.runtime.Composable

internal class NotificationPermissionRequestController(
    val shouldRequestPostNotifications: Boolean,
    val requestPostNotifications: () -> Unit,
)

@Composable
internal expect fun rememberNotificationPermissionRequestController(
    onPermissionResult: (Boolean) -> Unit,
): NotificationPermissionRequestController
