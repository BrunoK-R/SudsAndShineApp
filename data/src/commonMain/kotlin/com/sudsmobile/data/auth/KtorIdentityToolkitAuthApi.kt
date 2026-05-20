package com.sudsmobile.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

class KtorIdentityToolkitAuthApi(
    private val httpClient: HttpClient,
    private val config: FirebaseAuthConfig,
) : AuthApi {
    override suspend fun signIn(email: String, password: String): AuthResult {
        return authRequest("accounts:signInWithPassword") {
            setBody(
                PasswordAuthPayload(
                    email = email,
                    password = password,
                    returnSecureToken = true,
                ),
            )
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return authRequest("accounts:signUp") {
            setBody(
                PasswordAuthPayload(
                    email = email,
                    password = password,
                    returnSecureToken = true,
                ),
            )
        }
    }

    override suspend fun updateProfile(session: AuthSession, displayName: String): AuthResult {
        return authRequest("accounts:update") {
            setBody(
                UpdateProfilePayload(
                    idToken = session.idToken,
                    displayName = displayName,
                    returnSecureToken = true,
                ),
            )
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return try {
            val response = httpClient.post("${config.identityToolkitBaseUrl}/accounts:sendOobCode") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                parameter("key", config.apiKey)
                setBody(
                    PasswordResetPayload(
                        requestType = "PASSWORD_RESET",
                        email = email,
                    ),
                )
            }
            val body = response.body<IdentityToolkitResponse>()
            val error = body.error
            if (error != null) {
                AuthActionResult.Failure(error.toAuthError())
            } else {
                AuthActionResult.Success
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AuthActionResult.Failure(
                AuthError.Unavailable("Não foi possível contactar o Firebase Auth. Tente novamente."),
            )
        }
    }

    private suspend fun authRequest(
        endpoint: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): AuthResult {
        return try {
            val response = httpClient.post("${config.identityToolkitBaseUrl}/$endpoint") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                parameter("key", config.apiKey)
                block()
            }
            val body = response.body<IdentityToolkitResponse>()
            val error = body.error
            when {
                error != null -> AuthResult.Failure(error.toAuthError())
                body.localId != null && body.idToken != null && body.refreshToken != null -> {
                    AuthResult.Success(body.toSession())
                }
                else -> AuthResult.Failure(
                    AuthError.Backend("A resposta do Firebase Auth veio sem sessão."),
                )
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Throwable) {
            AuthResult.Failure(
                AuthError.Unavailable("Não foi possível contactar o Firebase Auth. Tente novamente."),
            )
        }
    }
}

@Serializable
private data class PasswordAuthPayload(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean,
)

@Serializable
private data class UpdateProfilePayload(
    val idToken: String,
    val displayName: String,
    val returnSecureToken: Boolean,
)

@Serializable
private data class PasswordResetPayload(
    val requestType: String,
    val email: String,
)

@Serializable
private data class IdentityToolkitResponse(
    val localId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val idToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: String? = null,
    val error: IdentityToolkitError? = null,
) {
    fun toSession(): AuthSession = AuthSession(
        user = AuthUser(
            uid = localId.orEmpty(),
            email = email.orEmpty(),
            displayName = displayName.orEmpty(),
            phoneNumber = "",
        ),
        idToken = idToken.orEmpty(),
        refreshToken = refreshToken.orEmpty(),
        expiresInSeconds = expiresIn?.toLongOrNull() ?: 0L,
    )
}

@Serializable
private data class IdentityToolkitError(
    val code: Int? = null,
    val message: String? = null,
) {
    fun toAuthError(): AuthError {
        val rawMessage = message.orEmpty()
        val code = rawMessage.substringBefore(" :").ifBlank { rawMessage }
        return when (code) {
            "EMAIL_NOT_FOUND",
            "INVALID_LOGIN_CREDENTIALS",
            "INVALID_PASSWORD" -> AuthError.InvalidCredentials("Email ou palavra-passe inválidos.")

            "USER_DISABLED" -> AuthError.Permission("Esta conta está desativada.")
            "EMAIL_EXISTS" -> AuthError.EmailInUse("Já existe uma conta com este email.")
            "INVALID_EMAIL" -> AuthError.Validation("Indique um email válido.")
            "WEAK_PASSWORD" -> AuthError.Validation("A palavra-passe deve ter pelo menos 6 caracteres.")
            "MISSING_PASSWORD" -> AuthError.Validation("Indique a palavra-passe.")
            "OPERATION_NOT_ALLOWED" -> AuthError.Permission("Este método de autenticação não está ativo.")
            "TOO_MANY_ATTEMPTS_TRY_LATER" -> AuthError.Unavailable(
                "Demasiadas tentativas. Aguarde e tente novamente.",
            )

            else -> AuthError.Backend(rawMessage.ifBlank { "Não foi possível autenticar a conta." })
        }
    }
}
