package com.sudsmobile.data.notification

import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class KtorNotificationFunctionsApiTest {
    @Test
    fun mapsNotificationPreferencesResponse() = runTest {
        var requestedPath: String? = null
        val api = KtorNotificationFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "preferences": {
                      "bookingStatusEnabled": true,
                      "appointmentReminderEnabled": true,
                      "loyaltyEnabled": false,
                      "adminPendingAlertEnabled": false,
                      "marketingEnabled": false
                    }
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.getMyNotificationPreferences("id-token-1")

        val success = assertIs<NotificationPreferencesResult.Success>(result)
        assertEquals("/test-project/europe-west1/getMyNotificationPreferences", requestedPath)
        assertEquals(true, success.preferences.bookingStatusEnabled)
        assertEquals(true, success.preferences.appointmentReminderEnabled)
        assertEquals(false, success.preferences.loyaltyEnabled)
        assertEquals(false, success.preferences.adminPendingAlertEnabled)
        assertEquals(false, success.preferences.marketingEnabled)
    }

    @Test
    fun postsPreferenceUpdateWithAuthorization() = runTest {
        var authorizationHeader: String? = null
        val api = KtorNotificationFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "preferences": {
                      "bookingStatusEnabled": true,
                      "appointmentReminderEnabled": false,
                      "loyaltyEnabled": true,
                      "adminPendingAlertEnabled": false,
                      "marketingEnabled": true
                    }
                  }
                }
                """.trimIndent(),
            ) { request ->
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.updateMyNotificationPreferences(
            NotificationPreferencesUpdateRequest(
                bookingStatusEnabled = true,
                appointmentReminderEnabled = false,
                loyaltyEnabled = true,
                adminPendingAlertEnabled = false,
                marketingEnabled = true,
            ),
            idToken = "id-token-1",
        )

        assertIs<NotificationPreferencesMutationResult.Success>(result)
        assertEquals("Bearer id-token-1", authorizationHeader)
    }

    @Test
    fun mapsNotificationTokenRegistrationResponse() = runTest {
        var requestedPath: String? = null
        var requestBody: String? = null
        val api = KtorNotificationFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "token": {
                      "tokenId": "token-id-1",
                      "platform": "android",
                      "enabled": true
                    }
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
                requestBody = request.bodyText()
            },
            config = testConfig(),
        )

        val result = api.registerNotificationToken(
            NotificationTokenRegistrationRequest(
                platform = NotificationTokenPlatform.Android,
                fid = "test-firebase-installation-id-1234567890",
            ),
            idToken = "id-token-1",
        )

        val success = assertIs<NotificationTokenRegistrationResult.Success>(result)
        assertEquals("/test-project/europe-west1/registerNotificationToken", requestedPath)
        assertEquals(true, requestBody?.contains("\"fid\":\"test-firebase-installation-id-1234567890\""))
        assertEquals(false, requestBody?.contains("\"token\":"))
        assertEquals("token-id-1", success.tokenId)
        assertEquals(NotificationTokenPlatform.Android, success.platform)
        assertEquals(true, success.enabled)
    }

    @Test
    fun mapsNotificationTokenDeleteResponse() = runTest {
        var requestedPath: String? = null
        val api = KtorNotificationFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "ok": true,
                    "tokenId": "current-test-device",
                    "status": "revoked"
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.deleteNotificationToken(
            NotificationTokenDeleteRequest(tokenId = "current-test-device"),
            idToken = "id-token-1",
        )

        val success = assertIs<NotificationTokenDeleteResult.Success>(result)
        assertEquals("/test-project/europe-west1/deleteNotificationToken", requestedPath)
        assertEquals("current-test-device", success.tokenId)
        assertEquals("revoked", success.status)
    }

    @Test
    fun mapsCallableValidationError() = runTest {
        val api = KtorNotificationFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "INVALID_ARGUMENT",
                    "message": "notification token is invalid"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.registerNotificationToken(
            NotificationTokenRegistrationRequest(
                token = "test-token-for-current-device-1234567890",
                platform = NotificationTokenPlatform.Android,
            ),
            idToken = "id-token-1",
        )

        val failure = assertIs<NotificationTokenRegistrationResult.Failure>(result)
        assertIs<NotificationError.Validation>(failure.error)
        assertEquals("notification token is invalid", failure.error.message)
    }
}

private fun mockClient(
    responseJson: String,
    onRequest: (HttpRequestData) -> Unit = {},
): HttpClient {
    val engine = MockEngine {
        onRequest(it)
        respond(
            content = responseJson,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
    return HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }
    }
}

private fun HttpRequestData.bodyText(): String = when (val content = body) {
    is TextContent -> content.text
    is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
    else -> error("Unsupported request body: ${content::class.simpleName}")
}

private fun testConfig(): FirebaseFunctionsConfig = FirebaseFunctionsConfig(
    projectId = "test-project",
    region = "europe-west1",
    useEmulator = true,
    emulatorHost = "127.0.0.1",
)
