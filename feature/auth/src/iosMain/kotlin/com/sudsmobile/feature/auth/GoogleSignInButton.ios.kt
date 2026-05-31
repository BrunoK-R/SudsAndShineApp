package com.sudsmobile.feature.auth

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformGoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    // Hidden until the iOS Firebase app, URL scheme, and native Google Sign-In flow are configured.
}

internal actual fun isGoogleSignInAvailable(): Boolean = false
