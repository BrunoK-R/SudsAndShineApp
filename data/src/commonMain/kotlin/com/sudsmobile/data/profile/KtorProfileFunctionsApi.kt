package com.sudsmobile.data.profile

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

class KtorProfileFunctionsApi(
    private val httpClient: HttpClient,
    private val config: FirebaseFunctionsConfig,
) : ProfileFunctionsApi {
    override suspend fun getMyProfile(idToken: String): UserProfileResult {
        return try {
            val response = httpClient.post(config.getMyProfileUrl) {
                callableHeaders(idToken)
                setBody(CallableProfileRequest(data = emptyMap()))
            }
            val body = response.body<CallableProfileResponse>()
            val error = body.error
            when {
                error != null -> UserProfileResult.Failure(error.toProfileError())
                body.result?.profile != null -> UserProfileResult.Success(body.result.profile.toUserProfile())
                else -> UserProfileResult.Failure(
                    UserProfileError.Backend("A resposta do perfil veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            UserProfileResult.Failure(
                UserProfileError.Unavailable("Não foi possível carregar os dados pessoais. Tente novamente."),
            )
        }
    }

    override suspend fun updateMyProfile(
        request: UserProfileSaveRequest,
        idToken: String,
    ): UserProfileMutationResult {
        return try {
            val response = httpClient.post(config.updateMyProfileUrl) {
                callableHeaders(idToken)
                setBody(CallableProfileSaveRequest(ProfileSavePayload.from(request)))
            }
            val body = response.body<CallableProfileResponse>()
            val error = body.error
            when {
                error != null -> UserProfileMutationResult.Failure(error.toProfileError())
                body.result?.profile != null -> UserProfileMutationResult.Success(body.result.profile.toUserProfile())
                else -> UserProfileMutationResult.Failure(
                    UserProfileError.Backend("A resposta do perfil veio sem dados."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            UserProfileMutationResult.Failure(
                UserProfileError.Unavailable("Não foi possível guardar os dados pessoais. Tente novamente."),
            )
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.callableHeaders(idToken: String) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        header(HttpHeaders.Authorization, "Bearer $idToken")
    }
}

@Serializable
private data class CallableProfileRequest(
    val data: Map<String, String>,
)

@Serializable
private data class CallableProfileSaveRequest(
    val data: ProfileSavePayload,
)

@Serializable
private data class ProfileSavePayload(
    val displayName: String,
    val phoneNumber: String,
    val marketingOptIn: Boolean,
    val appointmentReminderOptIn: Boolean,
) {
    companion object {
        fun from(request: UserProfileSaveRequest): ProfileSavePayload = ProfileSavePayload(
            displayName = request.displayName,
            phoneNumber = request.phoneNumber,
            marketingOptIn = request.marketingOptIn,
            appointmentReminderOptIn = request.appointmentReminderOptIn,
        )
    }
}

@Serializable
private data class CallableProfileResponse(
    val result: ProfileResult? = null,
    val error: CallableError? = null,
)

@Serializable
private data class ProfileResult(
    val profile: ProfileItem,
)

@Serializable
private data class ProfileItem(
    val uid: String,
    val email: String = "",
    val displayName: String = "",
    val phoneNumber: String = "",
    val marketingOptIn: Boolean = false,
    val appointmentReminderOptIn: Boolean = false,
) {
    fun toUserProfile(): UserProfile = UserProfile(
        uid = uid,
        email = email,
        displayName = displayName,
        phoneNumber = phoneNumber,
        marketingOptIn = marketingOptIn,
        appointmentReminderOptIn = appointmentReminderOptIn,
    )
}

@Serializable
private data class CallableError(
    val status: String? = null,
    val code: String? = null,
    val message: String? = null,
) {
    fun toProfileError(): UserProfileError {
        val normalizedCode = (status ?: code).orEmpty().lowercase()
        val fallbackMessage = message ?: "Não foi possível gerir os dados pessoais."
        return when (normalizedCode) {
            "invalid_argument", "invalid-argument" -> UserProfileError.Validation(fallbackMessage)
            "permission_denied", "permission-denied" ->
                UserProfileError.Permission("Não tem permissões para gerir estes dados.")
            "unauthenticated" -> UserProfileError.Unauthenticated("Inicie sessão para gerir os seus dados.")
            "unavailable" -> UserProfileError.Unavailable("O serviço de perfil está indisponível.")
            else -> UserProfileError.Backend(fallbackMessage)
        }
    }
}
