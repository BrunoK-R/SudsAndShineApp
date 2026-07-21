package com.sudsmobile.data.profile

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
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KtorProfileFunctionsApiTest {
    @Test
    fun mapsProfileResponse() = runTest {
        var requestedPath: String? = null
        val api = KtorProfileFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "profile": {
                      "uid": "uid-1",
                      "email": "bruno@example.com",
                      "displayName": "Bruno Ribeiro",
                      "phoneNumber": "913005855",
                      "marketingOptIn": true,
                      "appointmentReminderOptIn": true
                    }
                  }
                }
                """.trimIndent(),
            ) { request ->
                requestedPath = request.url.fullPath
            },
            config = testConfig(),
        )

        val result = api.getMyProfile("id-token-1")

        val success = assertIs<UserProfileResult.Success>(result)
        assertEquals("/test-project/europe-west1/getMyProfile", requestedPath)
        assertEquals("uid-1", success.profile.uid)
        assertEquals("Bruno Ribeiro", success.profile.displayName)
        assertEquals(true, success.profile.marketingOptIn)
        assertEquals(true, success.profile.appointmentReminderOptIn)
    }

    @Test
    fun sendsAuthorizationHeaderWhenUpdatingProfile() = runTest {
        var authorizationHeader: String? = null
        val api = KtorProfileFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "result": {
                    "profile": {
                      "uid": "uid-1",
                      "email": "bruno@example.com",
                      "displayName": "Bruno Ribeiro",
                      "phoneNumber": "913005855",
                      "marketingOptIn": false,
                      "appointmentReminderOptIn": true
                    }
                  }
                }
                """.trimIndent(),
            ) { request ->
                authorizationHeader = request.headers[HttpHeaders.Authorization]
            },
            config = testConfig(),
        )

        val result = api.updateMyProfile(validProfileRequest(), idToken = "id-token-1")

        assertIs<UserProfileMutationResult.Success>(result)
        assertEquals("Bearer id-token-1", authorizationHeader)
    }

    @Test
    fun mapsCallableValidationError() = runTest {
        val api = KtorProfileFunctionsApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "status": "INVALID_ARGUMENT",
                    "message": "phoneNumber is invalid"
                  }
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.updateMyProfile(validProfileRequest(), idToken = "id-token-1")

        val failure = assertIs<UserProfileMutationResult.Failure>(result)
        assertIs<UserProfileError.Validation>(failure.error)
        assertEquals("phoneNumber is invalid", failure.error.message)
    }

    @Test
    fun uploadsProfilePhotoAsBase64ToDedicatedCallable() = runTest {
        var requestedPath: String? = null
        var authorizationHeader: String? = null
        var requestBody = ""
        val api = KtorProfileFunctionsApi(
            httpClient = mockClient(profilePhotoResponse("https://example.com/avatar.jpg")) { request ->
                requestedPath = request.url.fullPath
                authorizationHeader = request.headers[HttpHeaders.Authorization]
                requestBody = request.bodyText()
            },
            config = testConfig(),
        )

        val result = api.updateMyProfilePhoto(
            request = UserProfilePhotoSaveRequest(byteArrayOf(1, 2, 3), "image/jpeg"),
            idToken = "id-token-1",
        )

        val success = assertIs<UserProfileMutationResult.Success>(result)
        val data = Json.parseToJsonElement(requestBody).jsonObject.getValue("data").jsonObject
        assertEquals("/test-project/europe-west1/updateMyProfilePhoto", requestedPath)
        assertEquals("Bearer id-token-1", authorizationHeader)
        assertEquals("AQID", data.getValue("imageBase64").jsonPrimitive.content)
        assertEquals("image/jpeg", data.getValue("mimeType").jsonPrimitive.content)
        assertEquals("https://example.com/avatar.jpg", success.profile.photoUrl)
    }

    @Test
    fun requestsProfilePhotoRemovalWithoutImagePayload() = runTest {
        var requestBody = ""
        val api = KtorProfileFunctionsApi(
            httpClient = mockClient(profilePhotoResponse("")) { request ->
                requestBody = request.bodyText()
            },
            config = testConfig(),
        )

        val result = api.removeMyProfilePhoto(idToken = "id-token-1")

        assertIs<UserProfileMutationResult.Success>(result)
        val data = Json.parseToJsonElement(requestBody).jsonObject.getValue("data").jsonObject
        assertEquals(true, data.getValue("remove").jsonPrimitive.boolean)
        assertEquals(false, data.containsKey("imageBase64"))
    }
}

private fun HttpRequestData.bodyText(): String {
    return when (val content = body) {
        is TextContent -> content.text
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        else -> error("Unsupported request body: ${content::class}")
    }
}

private fun profilePhotoResponse(photoUrl: String): String =
    """
    {
      "result": {
        "profile": {
          "uid": "uid-1",
          "email": "bruno@example.com",
          "displayName": "Bruno Ribeiro",
          "phoneNumber": "913005855",
          "marketingOptIn": true,
          "appointmentReminderOptIn": true,
          "photoUrl": "$photoUrl"
        }
      }
    }
    """.trimIndent()

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

private fun testConfig(): FirebaseFunctionsConfig = FirebaseFunctionsConfig(
    projectId = "test-project",
    region = "europe-west1",
    useEmulator = true,
    emulatorHost = "127.0.0.1",
)

private fun validProfileRequest(): UserProfileSaveRequest = UserProfileSaveRequest(
    displayName = "Bruno Ribeiro",
    phoneNumber = "913005855",
    marketingOptIn = false,
    appointmentReminderOptIn = true,
)
