package com.sudsmobile.feature.auth

import androidx.compose.runtime.Composable

internal actual fun isAppleSignInAvailable(): Boolean = false

@Composable
internal actual fun PlatformAppleSignInButton(
    enabled: Boolean,
    onStart: () -> Boolean,
    onCredential: (String, String, String?) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
) = Unit
