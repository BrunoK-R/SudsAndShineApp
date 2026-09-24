package com.sudsmobile.feature.profile

internal actual fun requestAppleAccountDeletionAuthorization(
    onAuthorizationCode: (String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
): Boolean = IosAppleAccountDeletionBridge.authorize(
    onAuthorizationCode = onAuthorizationCode,
    onError = onError,
    onCancelled = onCancelled,
)

object IosAppleAccountDeletionBridge {
    private var authorizationHandler: (((String) -> Unit, (String) -> Unit, () -> Unit) -> Unit)? = null

    fun configure(
        authorizationHandler: (((String) -> Unit, (String) -> Unit, () -> Unit) -> Unit)?,
    ) {
        this.authorizationHandler = authorizationHandler
    }

    fun authorize(
        onAuthorizationCode: (String) -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit,
    ): Boolean {
        val handler = authorizationHandler ?: return false
        handler(onAuthorizationCode, onError, onCancelled)
        return true
    }
}
