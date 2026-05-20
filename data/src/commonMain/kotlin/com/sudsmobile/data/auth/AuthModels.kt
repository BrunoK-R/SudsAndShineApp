package com.sudsmobile.data.auth

import kotlinx.coroutines.flow.StateFlow

data class AuthSession(
    val user: AuthUser,
    val idToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val phoneNumber: String,
) {
    val resolvedDisplayName: String
        get() = displayName.ifBlank { email.substringBefore("@").replaceFirstChar { it.titlecase() } }
}

sealed interface AuthSessionState {
    data object Unauthenticated : AuthSessionState
    data class Authenticated(val session: AuthSession) : AuthSessionState
}

sealed interface AuthResult {
    data class Success(val session: AuthSession) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

sealed interface AuthActionResult {
    data object Success : AuthActionResult
    data class Failure(val error: AuthError) : AuthActionResult
}

sealed interface AuthError {
    val message: String

    data class Validation(override val message: String) : AuthError
    data class InvalidCredentials(override val message: String) : AuthError
    data class EmailInUse(override val message: String) : AuthError
    data class Permission(override val message: String) : AuthError
    data class Unavailable(override val message: String) : AuthError
    data class Backend(override val message: String) : AuthError
}

interface AuthRepository {
    val sessionState: StateFlow<AuthSessionState>

    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult

    suspend fun sendPasswordReset(email: String): AuthActionResult
    fun signOut()
}
