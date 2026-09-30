package com.sudsmobile.data.feedback

import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.booking.FirebaseFunctionsConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FirebaseFeedbackRepository(
    private val client: HttpClient,
    private val config: FirebaseFunctionsConfig,
    private val auth: AuthRepository,
) : FeedbackRepository {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun submit(
        title: String,
        body: String,
        platform: String,
        screenshot: FeedbackScreenshot?,
    ): FeedbackResult<String> {
        if (title.trim().isEmpty() || title.trim().length > FEEDBACK_TITLE_MAX_LENGTH ||
            body.trim().length > FEEDBACK_BODY_MAX_LENGTH ||
            (screenshot != null && (screenshot.bytes.isEmpty() || screenshot.bytes.size > FEEDBACK_SCREENSHOT_MAX_BYTES))
        ) return FeedbackResult.Failure("Verifique os campos do feedback e tente novamente.")

        val payload = buildJsonObject {
            put("title", title.trim())
            put("body", body.trim())
            put("platform", platform)
            screenshot?.let {
                put("screenshot", buildJsonObject {
                    put("base64", Base64.Default.encode(it.bytes))
                    put("mimeType", it.mimeType)
                    put("widthPx", it.widthPx)
                    put("heightPx", it.heightPx)
                })
            }
        }
        return call(config.submitShakeFeedbackUrl, payload) { response ->
            response.id?.let { FeedbackResult.Success(it) }
        }
    }

    override suspend fun listForAdmin(): FeedbackResult<List<FeedbackItem>> =
        call(config.getAdminShakeFeedbackUrl, buildJsonObject { }) { response ->
            response.items?.map { it.toFeedbackItem() }?.let { FeedbackResult.Success(it) }
        }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun loadScreenshot(feedbackId: String): FeedbackResult<ByteArray> =
        call(config.getAdminShakeFeedbackScreenshotUrl, buildJsonObject { put("feedbackId", feedbackId) }) { response ->
            response.base64?.let { FeedbackResult.Success(Base64.Default.decode(it)) }
        }

    override suspend fun loadInteractions(feedbackId: String): FeedbackResult<FeedbackInteractions> =
        call(config.getAdminShakeFeedbackInteractionsUrl, buildJsonObject { put("feedbackId", feedbackId) }) { response ->
            response.interactions?.let { FeedbackResult.Success(it.toInteractions()) }
        }

    override suspend fun setLiked(feedbackId: String, liked: Boolean): FeedbackResult<Unit> =
        call(config.setAdminShakeFeedbackLikedUrl, buildJsonObject {
            put("feedbackId", feedbackId)
            put("liked", liked)
        }) { response -> if (response.ok == true) FeedbackResult.Success(Unit) else null }

    override suspend fun addComment(feedbackId: String, body: String): FeedbackResult<Unit> {
        val normalized = body.trim()
        if (normalized.isEmpty() || normalized.length > 1_000) {
            return FeedbackResult.Failure("Escreva um comentário até 1000 caracteres.")
        }
        return call(config.addAdminShakeFeedbackCommentUrl, buildJsonObject {
            put("feedbackId", feedbackId)
            put("body", normalized)
        }) { response -> if (response.ok == true) FeedbackResult.Success(Unit) else null }
    }

    override suspend fun delete(feedbackId: String): FeedbackResult<Unit> =
        call(config.deleteAdminShakeFeedbackUrl, buildJsonObject { put("feedbackId", feedbackId) }) { response ->
            if (response.ok == true) FeedbackResult.Success(Unit) else null
        }

    private suspend fun <T> call(
        url: String,
        payload: JsonObject,
        extract: (FeedbackResponsePayload) -> FeedbackResult<T>?,
    ): FeedbackResult<T> {
        val session = auth.currentSession()
            ?: return FeedbackResult.Failure("Inicie sessão para continuar.")
        return try {
            val response = client.post(url) {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                header(HttpHeaders.Authorization, "Bearer ${session.idToken}")
                setBody(CallableRequest(payload))
            }.body<CallableResponse>()
            when {
                response.error != null -> FeedbackResult.Failure("Não foi possível concluir a operação. Tente novamente.")
                response.result != null -> extract(response.result)
                    ?: FeedbackResult.Failure("A resposta do servidor veio incompleta.")
                else -> FeedbackResult.Failure("A resposta do servidor veio incompleta.")
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            FeedbackResult.Failure("Não foi possível comunicar com o servidor. Tente novamente.")
        }
    }
}

@Serializable
private data class CallableRequest(val data: JsonObject)

@Serializable
private data class CallableResponse(
    val result: FeedbackResponsePayload? = null,
    val error: CallableError? = null,
)

@Serializable
private data class CallableError(val message: String? = null)

@Serializable
private data class FeedbackResponsePayload(
    val id: String? = null,
    val items: List<FeedbackItemPayload>? = null,
    val base64: String? = null,
    val interactions: FeedbackInteractionsPayload? = null,
    val ok: Boolean? = null,
)

@Serializable
private data class FeedbackInteractionsPayload(
    val likeCount: Int = 0,
    val likedByCurrentAdmin: Boolean = false,
    val comments: List<FeedbackCommentPayload> = emptyList(),
) {
    fun toInteractions() = FeedbackInteractions(
        likeCount = likeCount,
        likedByCurrentAdmin = likedByCurrentAdmin,
        comments = comments.map { FeedbackComment(it.id, it.body, it.adminEmail, it.createdAtIso) },
    )
}

@Serializable
private data class FeedbackCommentPayload(
    val id: String,
    val body: String,
    val adminEmail: String = "",
    val createdAtIso: String = "",
)

@Serializable
private data class FeedbackItemPayload(
    val id: String,
    val title: String,
    val body: String = "",
    val submitterEmail: String = "",
    val platform: String = "",
    val createdAtIso: String = "",
    val screenshotStoragePath: String = "",
    val likeCount: Int = 0,
) {
    fun toFeedbackItem() = FeedbackItem(id, title, body, submitterEmail, platform, createdAtIso, screenshotStoragePath, likeCount)
}
