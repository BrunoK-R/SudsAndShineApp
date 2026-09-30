package com.sudsmobile.data.feedback

const val FEEDBACK_TITLE_MAX_LENGTH = 120
const val FEEDBACK_BODY_MAX_LENGTH = 4_000
const val FEEDBACK_SCREENSHOT_MAX_BYTES = 5 * 1024 * 1024

data class FeedbackScreenshot(
    val bytes: ByteArray,
    val mimeType: String,
    val widthPx: Int,
    val heightPx: Int,
)

data class FeedbackItem(
    val id: String,
    val title: String,
    val body: String,
    val submitterEmail: String,
    val platform: String,
    val createdAtIso: String,
    val screenshotStoragePath: String,
    val likeCount: Int,
)

data class FeedbackComment(
    val id: String,
    val body: String,
    val adminEmail: String,
    val createdAtIso: String,
)

data class FeedbackInteractions(
    val likeCount: Int,
    val likedByCurrentAdmin: Boolean,
    val comments: List<FeedbackComment>,
)

sealed interface FeedbackResult<out T> {
    data class Success<T>(val value: T) : FeedbackResult<T>
    data class Failure(val message: String) : FeedbackResult<Nothing>
}

interface FeedbackRepository {
    suspend fun submit(title: String, body: String, platform: String, screenshot: FeedbackScreenshot?): FeedbackResult<String>
    suspend fun listForAdmin(): FeedbackResult<List<FeedbackItem>>
    suspend fun loadScreenshot(feedbackId: String): FeedbackResult<ByteArray>
    suspend fun loadInteractions(feedbackId: String): FeedbackResult<FeedbackInteractions>
    suspend fun setLiked(feedbackId: String, liked: Boolean): FeedbackResult<Unit>
    suspend fun addComment(feedbackId: String, body: String): FeedbackResult<Unit>
    suspend fun delete(feedbackId: String): FeedbackResult<Unit>
}
