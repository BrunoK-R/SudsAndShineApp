package com.sudsmobile.feature.profile

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosAppleAccountDeletionBridgeTest {
    @AfterTest
    fun reset() = IosAppleAccountDeletionBridge.configure(null)

    @Test
    fun disabledBridgeReportsUnavailable() {
        IosAppleAccountDeletionBridge.configure(null)

        assertFalse(
            requestAppleAccountDeletionAuthorization(
                onAuthorizationCode = { error("Unexpected authorization code") },
                onError = { error("Unexpected error") },
                onCancelled = { error("Unexpected cancellation") },
            ),
        )
    }

    @Test
    fun configuredBridgeForwardsAuthorizationCode() {
        IosAppleAccountDeletionBridge.configure { onAuthorizationCode, _, _ ->
            onAuthorizationCode("apple-code")
        }
        var authorizationCode: String? = null

        val started = requestAppleAccountDeletionAuthorization(
            onAuthorizationCode = { authorizationCode = it },
            onError = { error(it) },
            onCancelled = { error("Unexpected cancellation") },
        )

        assertTrue(started)
        assertEquals("apple-code", authorizationCode)
    }
}
