package com.sudsmobile.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString

@Composable
internal actual fun rememberOpenNotificationSettings(): () -> Unit = remember {
    {
        NSURL.URLWithString(UIApplicationOpenNotificationSettingsURLString)?.let {
            UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any?>(), null)
        }
        Unit
    }
}
