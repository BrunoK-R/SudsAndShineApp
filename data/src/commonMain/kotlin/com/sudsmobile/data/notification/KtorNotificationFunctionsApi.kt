package com.sudsmobile.data.notification

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

class KtorNotificationFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : NotificationFunctionsApi {
    override suspend fun getMyNotificationPreferences(idToken: String): NotificationPreferencesResult {
        return try {
            val response = httpClient.post(config.getMyNotificationPreferencesUrl) {
                callableHeaders(idToken)
                setBody(CallableNotificationPreferencesRequest(data = emptyMap()))
            }
            val body = response.body<CallableNotificationPreferencesResponse>()
            val error = body.error
            when {
                error != null -> NotificationPreferencesResult.Failure(error.toNotificationError())
                body.result?.preferences != null ->
                    NotificationPreferencesResult.Success(body.result.preferences.toPreferences())
                else -> NotificationPreferencesResult.Failure(
                    NotificationError.Backend("A resposta de notificações veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            NotificationPreferencesResult.Failure(
                NotificationError.Unavailable("Não foi possível carregar notificações. Tente novamente."),
            )
        }
    }

    override suspend fun updateMyNotificationPreferences(
        request: NotificationPreferencesUpdateRequest,
        idToken: String,
    ): NotificationPreferencesMutationResult {
        return try {
            val response = httpClient.post(config.updateMyNotificationPreferencesUrl) {
                callableHeaders(idToken)
                setBody(
                    CallableNotificationPreferencesUpdateRequest(
                        data = NotificationPreferencesPayload.from(request),
                    ),
                )
            }
            val body = response.body<CallableNotificationPreferencesResponse>()
            val error = body.error
            when {
                error != null -> NotificationPreferencesMutationResult.Failure(error.toNotificationError())
                body.result?.preferences != null ->
                    NotificationPreferencesMutationResult.Success(body.result.preferences.toPreferences())
                else -> NotificationPreferencesMutationResult.Failure(
                    NotificationError.Backend("A resposta de notificações veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            NotificationPreferencesMutationResult.Failure(
                NotificationError.Unavailable("Não foi possível guardar notificações. Tente novamente."),
            )
        }
    }

    override suspend fun registerNotificationToken(
        request: NotificationTokenRegistrationRequest,
        idToken: String,
    ): NotificationTokenRegistrationResult {
        return try {
            val response = httpClient.post(config.registerNotificationTokenUrl) {
                callableHeaders(idToken)
                setBody(CallableNotificationTokenRegistrationRequest(NotificationTokenPayload.from(request)))
            }
            val body = response.body<CallableNotificationTokenRegistrationResponse>()
            val error = body.error
            val token = body.result?.token
            when {
                error != null -> NotificationTokenRegistrationResult.Failure(error.toNotificationError())
                token != null -> NotificationTokenRegistrationResult.Success(
                    tokenId = token.tokenId,
                    platform = token.platform.toNotificationTokenPlatform() ?: request.platform,
                    enabled = token.enabled,
                )
                else -> NotificationTokenRegistrationResult.Failure(
                    NotificationError.Backend("A resposta do token de notificações veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            NotificationTokenRegistrationResult.Failure(
                NotificationError.Unavailable("Não foi possível registar este dispositivo para notificações."),
            )
        }
    }

    override suspend fun deleteNotificationToken(
        request: NotificationTokenDeleteRequest,
        idToken: String,
    ): NotificationTokenDeleteResult {
        return try {
            val response = httpClient.post(config.deleteNotificationTokenUrl) {
                callableHeaders(idToken)
                setBody(CallableNotificationTokenDeleteRequest(NotificationTokenDeletePayload.from(request)))
            }
            val body = response.body<CallableNotificationTokenDeleteResponse>()
            val error = body.error
            when {
                error != null -> NotificationTokenDeleteResult.Failure(error.toNotificationError())
                body.result?.tokenId != null -> NotificationTokenDeleteResult.Success(
                    tokenId = body.result.tokenId,
                    status = body.result.status,
                )
                else -> NotificationTokenDeleteResult.Failure(
                    NotificationError.Backend("A resposta do token de notificações veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            NotificationTokenDeleteResult.Failure(
                NotificationError.Unavailable("Não foi possível remover este dispositivo das notificações."),
            )
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.callableHeaders(idToken: String) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        header(HttpHeaders.Authorization, "Bearer $idToken")
    }
}

@Serializable
private data class CallableNotificationPreferencesRequest(
    val data: Map<String, String>,
)

@Serializable
private data class CallableNotificationPreferencesUpdateRequest(
    val data: NotificationPreferencesPayload,
)

@Serializable
private data class NotificationPreferencesPayload(
    val bookingStatusEnabled: Boolean,
    val appointmentReminderEnabled: Boolean,
    val loyaltyEnabled: Boolean,
    val adminPendingAlertEnabled: Boolean,
    val marketingEnabled: Boolean,
) {
    companion object {
        fun from(request: NotificationPreferencesUpdateRequest): NotificationPreferencesPayload {
            return NotificationPreferencesPayload(
                bookingStatusEnabled = request.bookingStatusEnabled,
                appointmentReminderEnabled = request.appointmentReminderEnabled,
                loyaltyEnabled = request.loyaltyEnabled,
                adminPendingAlertEnabled = request.adminPendingAlertEnabled,
                marketingEnabled = request.marketingEnabled,
            )
        }
    }
}

@Serializable
private data class CallableNotificationTokenRegistrationRequest(
    val data: NotificationTokenPayload,
)

@Serializable
private data class NotificationTokenPayload(
    val token: String = "",
    val fid: String = "",
    val platform: String,
    val tokenId: String = "",
    val deviceLabel: String = "",
    val appVersion: String = "",
) {
    companion object {
        fun from(request: NotificationTokenRegistrationRequest): NotificationTokenPayload {
            return NotificationTokenPayload(
                token = request.token,
                fid = request.fid,
                platform = request.platform.wireName,
                tokenId = request.tokenId,
                deviceLabel = request.deviceLabel,
                appVersion = request.appVersion,
            )
        }
    }
}

@Serializable
private data class CallableNotificationTokenDeleteRequest(
    val data: NotificationTokenDeletePayload,
)

@Serializable
private data class NotificationTokenDeletePayload(
    val tokenId: String,
) {
    companion object {
        fun from(request: NotificationTokenDeleteRequest): NotificationTokenDeletePayload {
            return NotificationTokenDeletePayload(tokenId = request.tokenId)
        }
    }
}

@Serializable
private data class CallableNotificationPreferencesResponse(
    val result: NotificationPreferencesResultBody? = null,
    val error: CallableNotificationError? = null,
)

@Serializable
private data class NotificationPreferencesResultBody(
    val preferences: NotificationPreferencesItem? = null,
)

@Serializable
private data class NotificationPreferencesItem(
    val bookingStatusEnabled: Boolean = true,
    val appointmentReminderEnabled: Boolean = true,
    val loyaltyEnabled: Boolean = true,
    val adminPendingAlertEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
) {
    fun toPreferences(): NotificationPreferences = NotificationPreferences(
        bookingStatusEnabled = bookingStatusEnabled,
        appointmentReminderEnabled = appointmentReminderEnabled,
        loyaltyEnabled = loyaltyEnabled,
        adminPendingAlertEnabled = adminPendingAlertEnabled,
        marketingEnabled = marketingEnabled,
    )
}

@Serializable
private data class CallableNotificationTokenRegistrationResponse(
    val result: NotificationTokenRegistrationResultBody? = null,
    val error: CallableNotificationError? = null,
)

@Serializable
private data class NotificationTokenRegistrationResultBody(
    val token: NotificationTokenItem? = null,
)

@Serializable
private data class NotificationTokenItem(
    val tokenId: String,
    val platform: String,
    val enabled: Boolean = true,
)

@Serializable
private data class CallableNotificationTokenDeleteResponse(
    val result: NotificationTokenDeleteResultBody? = null,
    val error: CallableNotificationError? = null,
)

@Serializable
private data class NotificationTokenDeleteResultBody(
    val ok: Boolean = false,
    val tokenId: String? = null,
    val status: String = "",
)

@Serializable
private data class CallableNotificationError(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
) {
    fun toNotificationError(): NotificationError {
        val normalizedCode = (status ?: code).orEmpty().lowercase()
        val fallbackMessage = message ?: "Não foi possível gerir notificações."
        return when (normalizedCode) {
            "invalid_argument", "invalid-argument" -> NotificationError.Validation(fallbackMessage)
            "permission_denied", "permission-denied" ->
                NotificationError.Permission("Não tem permissões para gerir notificações.")
            "unauthenticated" -> NotificationError.Unauthenticated("Inicie sessão para gerir notificações.")
            "unavailable" -> NotificationError.Unavailable("O serviço de notificações está indisponível.")
            else -> NotificationError.Backend(fallbackMessage)
        }
    }
}

private fun String.toNotificationTokenPlatform(): NotificationTokenPlatform? {
    return NotificationTokenPlatform.entries.firstOrNull { it.wireName == lowercase() }
}
