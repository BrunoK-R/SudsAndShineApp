package com.sudsmobile.feature.auth

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val AppleSignInButtonHeight = 52.dp

internal fun signInButtonLabelStyle(): TextStyle {
    // Match the system label rendered by the native 52-point Apple button.
    val fontSize = if (isAppleSignInAvailable()) 20f else 14f
    return TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.2f).sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    )
}
