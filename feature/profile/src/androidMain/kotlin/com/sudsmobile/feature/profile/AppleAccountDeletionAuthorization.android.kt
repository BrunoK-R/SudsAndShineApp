package com.sudsmobile.feature.profile

internal actual fun requestAppleAccountDeletionAuthorization(
    onAuthorizationCode: (String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
): Boolean = false
