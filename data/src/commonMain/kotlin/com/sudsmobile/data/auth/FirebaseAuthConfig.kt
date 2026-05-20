package com.sudsmobile.data.auth

data class FirebaseAuthConfig(
    val apiKey: String,
    val useEmulator: Boolean,
    val emulatorHost: String,
) {
    val identityToolkitBaseUrl: String
        get() = if (useEmulator) {
            "http://$emulatorHost:9099/identitytoolkit.googleapis.com/v1"
        } else {
            "https://identitytoolkit.googleapis.com/v1"
        }

    val secureTokenBaseUrl: String
        get() = if (useEmulator) {
            "http://$emulatorHost:9099/securetoken.googleapis.com/v1"
        } else {
            "https://securetoken.googleapis.com/v1"
        }

    companion object {
        private const val PublicWebApiKey = "AIzaSyDULIWnAxdusCJn_NYyfmfqooelIrC4B6I"

        fun default(isDebugBuild: Boolean, platformName: String): FirebaseAuthConfig {
            val normalizedPlatform = platformName.lowercase()
            val emulatorHost = when {
                normalizedPlatform.contains("android") -> "10.0.2.2"
                else -> "127.0.0.1"
            }

            return FirebaseAuthConfig(
                apiKey = PublicWebApiKey,
                useEmulator = isDebugBuild,
                emulatorHost = emulatorHost,
            )
        }
    }
}
