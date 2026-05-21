package com.sudsmobile.data

import com.sudsmobile.data.auth.FirebaseAuthConfig
import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirebaseConfigDefaultsTest {
    @Test
    fun debugBuildUsesLiveFirebaseByDefault() {
        val authConfig = FirebaseAuthConfig.default(
            isDebugBuild = true,
            platformName = "android",
        )
        val functionsConfig = FirebaseFunctionsConfig.default(
            isDebugBuild = true,
            platformName = "android",
        )

        assertFalse(authConfig.useEmulator)
        assertFalse(functionsConfig.useEmulator)
        assertTrue(authConfig.identityToolkitBaseUrl.startsWith("https://"))
        assertTrue(functionsConfig.getServiceCatalogUrl.startsWith("https://"))
    }

    @Test
    fun debugBuildCanOptIntoLocalFirebaseEmulators() {
        val authConfig = FirebaseAuthConfig.default(
            isDebugBuild = true,
            platformName = "android",
            useFirebaseEmulators = true,
        )
        val functionsConfig = FirebaseFunctionsConfig.default(
            isDebugBuild = true,
            platformName = "android",
            useFirebaseEmulators = true,
        )

        assertTrue(authConfig.useEmulator)
        assertTrue(functionsConfig.useEmulator)
        assertTrue(authConfig.identityToolkitBaseUrl.startsWith("http://10.0.2.2:9099"))
        assertTrue(functionsConfig.getServiceCatalogUrl.startsWith("http://10.0.2.2:5001"))
    }

    @Test
    fun releaseBuildDoesNotUseLocalFirebaseEmulators() {
        val authConfig = FirebaseAuthConfig.default(
            isDebugBuild = false,
            platformName = "android",
            useFirebaseEmulators = true,
        )
        val functionsConfig = FirebaseFunctionsConfig.default(
            isDebugBuild = false,
            platformName = "android",
            useFirebaseEmulators = true,
        )

        assertFalse(authConfig.useEmulator)
        assertFalse(functionsConfig.useEmulator)
    }
}
