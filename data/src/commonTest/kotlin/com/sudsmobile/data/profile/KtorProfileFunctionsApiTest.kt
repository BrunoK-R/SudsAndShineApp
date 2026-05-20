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
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

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
                      "marketingOptIn": true
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
                      "marketingOptIn": false
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
)
