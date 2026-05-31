package com.sudsmobile.feature.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberNotificationPermissionRequestController(
    onPermissionResult: (Boolean) -> Unit,
): NotificationPermissionRequestController {
    val context = LocalContext.current
    val latestOnPermissionResult by rememberUpdatedState(onPermissionResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> latestOnPermissionResult(granted) },
    )
    val shouldRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    return remember(launcher, shouldRequest) {
        NotificationPermissionRequestController(
            shouldRequestPostNotifications = shouldRequest,
            requestPostNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    latestOnPermissionResult(true)
                }
            },
        )
    }
}
