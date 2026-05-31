package com.sudsmobile.feature.auth

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformGoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    GoogleAuthButton(
        enabled = enabled,
        onClick = {
            onError("Google Sign-In para iOS ainda não está configurado.")
        },
    )
}
