package com.sudsmobile.feature.profile

internal expect fun requestAppleAccountDeletionAuthorization(
    onAuthorizationCode: (String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
): Boolean
