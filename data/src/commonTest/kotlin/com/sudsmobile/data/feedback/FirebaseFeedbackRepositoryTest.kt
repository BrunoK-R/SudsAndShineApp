package com.sudsmobile.data.feedback

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FirebaseFeedbackRepositoryTest {
    @Test
    fun submitsAuthenticatedFeedbackAndScreenshotToCallable() = runTest {
        var path = ""
        var authorization = ""
        var body = ""
        val repository = repository("""{"result":{"id":"feedback-1"}}""") { request ->
            path = request.url.fullPath
            authorization = request.headers[HttpHeaders.Authorization].orEmpty()
            body = when (val content = request.body) {
                is TextContent -> content.text
                is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
                else -> error("Unsupported request body")
            }
        }

        val result = repository.submit(" Título ", " Texto ", "android", FeedbackScreenshot(
            byteArrayOf(1, 2, 3), "image/jpeg", 10, 20,
        ))

        assertEquals("feedback-1", assertIs<FeedbackResult.Success<String>>(result).value)
        assertEquals("/test-project/europe-west1/submitShakeFeedback", path)
        assertEquals("Bearer test-token", authorization)
        val payload = Json.parseToJsonElement(body).jsonObject["data"]!!.jsonObject
        assertEquals("Título", payload["title"]!!.jsonPrimitive.content)
        assertEquals("AQID", payload["screenshot"]!!.jsonObject["base64"]!!.jsonPrimitive.content)
    }

    @Test
    fun parsesAdminListWithoutExposingFirestoreTimestamp() = runTest {
        val repository = repository("""
            {"result":{"items":[{"id":"feedback-1","title":"Erro","body":"Detalhe",
            "submitterEmail":"a@example.com","platform":"ios","createdAtIso":"2026-09-30T10:00:00Z",
            "screenshotStoragePath":"admin-shake-feedback/u/feedback-1/screenshot.jpg"}]}}
        """.trimIndent())

        val result = repository.listForAdmin()

        val item = assertIs<FeedbackResult.Success<List<FeedbackItem>>>(result).value.single()
        assertEquals("Erro", item.title)
        assertEquals("a@example.com", item.submitterEmail)
    }

    @Test
    fun mapsAdminInteractions() = runTest {
        val repository = repository("""
            {"result":{"interactions":{"likeCount":2,"likedByCurrentAdmin":true,
            "comments":[{"id":"comment-1","body":"Confirmado","adminEmail":"admin@example.com",
            "createdAtIso":"2026-09-30T11:00:00Z"}]}}}
        """.trimIndent())

        val result = repository.loadInteractions("feedback-1")

        val interactions = assertIs<FeedbackResult.Success<FeedbackInteractions>>(result).value
        assertEquals(2, interactions.likeCount)
        assertEquals(true, interactions.likedByCurrentAdmin)
        assertEquals("Confirmado", interactions.comments.single().body)
    }
}

private fun repository(
    response: String,
    onRequest: (io.ktor.client.request.HttpRequestData) -> Unit = {},
): FirebaseFeedbackRepository {
    val client = HttpClient(MockEngine {
        onRequest(it)
        respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; explicitNulls = false }) }
    }
    return FirebaseFeedbackRepository(
        client,
        FirebaseFunctionsConfig("test-project", "europe-west1", true, "127.0.0.1"),
        TestAuthRepository,
    )
}

private object TestAuthRepository : AuthRepository {
    private val session = AuthSession(
        AuthUser("uid-1", "a@example.com", "Admin", ""), "test-token", "refresh", 3600,
    )
    override val sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Authenticated(session))
    override suspend fun currentSession() = session
    override suspend fun signIn(email: String, password: String): AuthResult = error("unused")
    override suspend fun register(displayName: String, email: String, phoneNumber: String, password: String): AuthResult = error("unused")
    override suspend fun sendPasswordReset(email: String): AuthActionResult = error("unused")
    override fun signOut() = Unit
}
