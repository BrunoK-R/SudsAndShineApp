package com.sudsmobile.data.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class NotificationDeviceRegistrarTest {
    @Test
    fun unsupportedRegistrarReportsPlatformUnavailable() = runTest {
        val registrar = UnsupportedNotificationDeviceRegistrar()

        val state = registrar.currentState("uid-1")
        val request = registrar.buildRegistrationRequest("uid-1")

        assertEquals(NotificationDevicePermissionStatus.Unsupported, state.permissionStatus)
        assertEquals(null, state.registeredTokenId)
        assertIs<NotificationDeviceRegistrationRequestResult.Unsupported>(request)
    }
}
