package com.sudsmobile.feature.auth

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosAppleSignInBridgeTest {
    @AfterTest
    fun reset() = IosAppleSignInBridge.configure(null)

    @Test
    fun disabledBridgeReportsUnavailableWithoutCredentials() {
        IosAppleSignInBridge.configure(null)
        assertFalse(IosAppleSignInBridge.isAvailable())
        var error: String? = null
        IosAppleSignInBridge.signIn(
            onCredential = { _, _, _ -> error("Unexpected credential") },
            onError = { error = it },
            onCancelled = { error("Unexpected cancellation") },
        )
        assertNotNull(error)
    }

    @Test
    fun forwardsCredentialAndNullableNameWithoutChangingNonce() {
        IosAppleSignInBridge.configure { onCredential, _, _ ->
            onCredential("apple-token", " raw nonce ", null)
        }
        assertTrue(IosAppleSignInBridge.isAvailable())
        var credential: Triple<String, String, String?>? = null
        IosAppleSignInBridge.signIn(
            onCredential = { token, nonce, name -> credential = Triple(token, nonce, name) },
            onError = { error(it) },
            onCancelled = { error("Unexpected cancellation") },
        )
        assertEquals(Triple("apple-token", " raw nonce ", null), credential)
    }

    @Test
    fun cancellationDoesNotBecomeAnErrorOrCredential() {
        IosAppleSignInBridge.configure { _, _, onCancelled -> onCancelled() }
        var cancelled = false
        IosAppleSignInBridge.signIn(
            onCredential = { _, _, _ -> error("Unexpected credential") },
            onError = { error(it) },
            onCancelled = { cancelled = true },
        )
        assertTrue(cancelled)
    }
}
