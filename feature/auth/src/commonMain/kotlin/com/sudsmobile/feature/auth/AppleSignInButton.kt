package com.sudsmobile.feature.auth

import androidx.compose.runtime.Composable

internal expect fun isAppleSignInAvailable(): Boolean

@Composable
internal expect fun PlatformAppleSignInButton(
    enabled: Boolean,
    onStart: () -> Boolean,
    onCredential: (String, String, String?) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
)
