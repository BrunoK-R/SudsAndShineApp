package com.sudsmobile.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class KtorIdentityToolkitAuthApiTest {
    @Test
    fun mapsSignInResponseToAuthSession() = runTest {
        val api = KtorIdentityToolkitAuthApi(
            httpClient = mockClient(
                """
                {
                  "localId": "user-1",
                  "email": "bruno@example.com",
                  "displayName": "Bruno Ribeiro",
                  "idToken": "id-token",
                  "refreshToken": "refresh-token",
                  "expiresIn": "3600"
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.signIn(email = "bruno@example.com", password = "password123")

        val success = assertIs<AuthResult.Success>(result)
        assertEquals("user-1", success.session.user.uid)
        assertEquals("bruno@example.com", success.session.user.email)
        assertEquals("Bruno Ribeiro", success.session.user.displayName)
        assertEquals("id-token", success.session.idToken)
        assertEquals(3600L, success.session.expiresInSeconds)
    }

    @Test
    fun mapsGoogleSignInResponseToAuthSession() = runTest {
        var requestUrl = ""
        val api = KtorIdentityToolkitAuthApi(
            httpClient = mockClient(
                """
                {
                  "localId": "google-user-1",
                  "email": "bruno@gmail.com",
                  "displayName": "Bruno Ribeiro",
                  "idToken": "firebase-id-token",
                  "refreshToken": "firebase-refresh-token",
                  "expiresIn": "3600"
                }
                """.trimIndent(),
                onRequest = { requestUrl = it.url.toString() },
            ),
            config = testConfig(),
        )

        val result = api.signInWithGoogleIdToken("google-id-token")

        val success = assertIs<AuthResult.Success>(result)
        assertEquals(
            "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=test-api-key",
            requestUrl,
        )
        assertEquals("google-user-1", success.session.user.uid)
        assertEquals("bruno@gmail.com", success.session.user.email)
        assertEquals("Bruno Ribeiro", success.session.user.displayName)
        assertEquals("firebase-id-token", success.session.idToken)
        assertEquals("firebase-refresh-token", success.session.refreshToken)
    }

    @Test
    fun mapsInvalidCredentialsError() = runTest {
        val api = KtorIdentityToolkitAuthApi(
            httpClient = mockClient(
                """
                {
                  "error": {
                    "code": 400,
                    "message": "INVALID_PASSWORD"
                  }
                }
                """.trimIndent(),
                status = HttpStatusCode.BadRequest,
            ),
            config = testConfig(),
        )

        val result = api.signIn(email = "bruno@example.com", password = "wrong-password")

        val failure = assertIs<AuthResult.Failure>(result)
        assertIs<AuthError.InvalidCredentials>(failure.error)
        assertEquals("Email ou palavra-passe inválidos.", failure.error.message)
    }

    @Test
    fun mapsPasswordResetSuccess() = runTest {
        val api = KtorIdentityToolkitAuthApi(
            httpClient = mockClient(
                """
                {
                  "email": "bruno@example.com"
                }
                """.trimIndent(),
            ),
            config = testConfig(),
        )

        val result = api.sendPasswordReset(email = "bruno@example.com")

        assertEquals(AuthActionResult.Success, result)
    }

    @Test
    fun refreshesSessionWithSecureTokenEndpoint() = runTest {
        var requestUrl = ""
        var requestMethod: HttpMethod? = null
        val api = KtorIdentityToolkitAuthApi(
            httpClient = mockClient(
                responseJson = """
                {
                  "user_id": "user-1",
                  "id_token": "new-id-token",
                  "refresh_token": "new-refresh-token",
                  "expires_in": "3600"
                }
                """.trimIndent(),
                onRequest = {
                    requestUrl = it.url.toString()
                    requestMethod = it.method
                },
            ),
            config = testConfig(),
        )

        val result = api.refreshSession("old-refresh-token")

        val success = assertIs<AuthResult.Success>(result)
        assertEquals(HttpMethod.Post, requestMethod)
        assertEquals(
            "http://127.0.0.1:9099/securetoken.googleapis.com/v1/token?key=test-api-key",
            requestUrl,
        )
        assertEquals("user-1", success.session.user.uid)
        assertEquals("new-id-token", success.session.idToken)
        assertEquals("new-refresh-token", success.session.refreshToken)
        assertEquals(3600L, success.session.expiresInSeconds)
    }
}

private fun mockClient(
    responseJson: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {},
): HttpClient {
    val engine = MockEngine {
        onRequest(it)
        respond(
            content = responseJson,
            status = status,
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

private fun testConfig(): FirebaseAuthConfig = FirebaseAuthConfig(
    apiKey = "test-api-key",
    useEmulator = true,
    emulatorHost = "127.0.0.1",
)
