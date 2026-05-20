package com.sudsmobile.data.booking

data class FirebaseFunctionsConfig(
    val projectId: String,
    val region: String,
    val useEmulator: Boolean,
    val emulatorHost: String,
) {
    val createReservationUrl: String
        get() = if (useEmulator) {
            "http://$emulatorHost:5001/$projectId/$region/createReservation"
        } else {
            "https://$region-$projectId.cloudfunctions.net/createReservation"
        }

    companion object {
        fun default(isDebugBuild: Boolean, platformName: String): FirebaseFunctionsConfig {
            val normalizedPlatform = platformName.lowercase()
            val emulatorHost = when {
                normalizedPlatform.contains("android") -> "10.0.2.2"
                else -> "127.0.0.1"
            }

            return FirebaseFunctionsConfig(
                projectId = "sudsandshine-bd3e2",
                region = "europe-west1",
                useEmulator = isDebugBuild,
                emulatorHost = emulatorHost,
            )
        }
    }
}
