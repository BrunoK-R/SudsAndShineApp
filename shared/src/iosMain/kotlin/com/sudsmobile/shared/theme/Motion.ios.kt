package com.sudsmobile.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
internal actual fun rememberPlatformReduceMotionEnabled(): Boolean = remember {
    UIAccessibilityIsReduceMotionEnabled()
}
