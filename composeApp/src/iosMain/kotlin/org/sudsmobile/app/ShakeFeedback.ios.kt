package org.sudsmobile.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sudsmobile.data.feedback.FeedbackScreenshot

internal actual fun platformName() = "ios"

@Composable
internal actual fun PlatformShakeEffect(enabled: Boolean, onShake: () -> Unit) = Unit

@Composable
internal actual fun rememberFeedbackScreenshotCapture(): suspend () -> FeedbackScreenshot? =
    remember { { null } }
