package com.sudsmobile.feature.auth

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformGoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    GoogleAuthButton(
        enabled = enabled && IosGoogleSignInBridge.isAvailable(),
        onClick = {
            IosGoogleSignInBridge.signIn(
                onIdToken = onIdToken,
                onError = onError,
            )
        },
    )
}

internal actual fun isGoogleSignInAvailable(): Boolean = IosGoogleSignInBridge.isAvailable()

object IosGoogleSignInBridge {
    private var signInHandler: (((String) -> Unit, (String) -> Unit) -> Unit)? = null

    fun configure(
        signInHandler: (((String) -> Unit, (String) -> Unit) -> Unit)?,
    ) {
        this.signInHandler = signInHandler
    }

    fun isAvailable(): Boolean = signInHandler != null

    fun signIn(
        onIdToken: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val handler = signInHandler
        if (handler == null) {
            onError("O início de sessão com Google ainda não está configurado neste dispositivo.")
            return
        }

        handler(onIdToken, onError)
    }
}
