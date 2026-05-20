package com.sudsmobile.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirebaseAuthRepository(
    private val api: AuthApi,
) : AuthRepository {
    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Unauthenticated)
    override val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    override suspend fun signIn(email: String, password: String): AuthResult {
        val validationError = validateCredentials(email, password)
        if (validationError != null) return AuthResult.Failure(validationError)

        val result = api.signIn(email = email.trim().lowercase(), password = password)
        return result.also(::updateSessionFromResult)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        val validationError = validateRegistration(displayName, email, password)
        if (validationError != null) return AuthResult.Failure(validationError)

        val sanitizedDisplayName = displayName.trim()
        val sanitizedPhoneNumber = phoneNumber.trim()
        val signUpResult = api.signUp(email = email.trim().lowercase(), password = password)
        val profileResult = when (signUpResult) {
            is AuthResult.Success -> api.updateProfile(
                session = signUpResult.session,
                displayName = sanitizedDisplayName,
            )
            is AuthResult.Failure -> signUpResult
        }

        val finalResult = profileResult.withLocalProfile(
            displayName = sanitizedDisplayName,
            phoneNumber = sanitizedPhoneNumber,
        )
        return finalResult.also(::updateSessionFromResult)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        if (!email.trim().isValidEmail()) {
            return AuthActionResult.Failure(AuthError.Validation("Indique um email válido."))
        }

        return api.sendPasswordReset(email.trim().lowercase())
    }

    override fun signOut() {
        _sessionState.value = AuthSessionState.Unauthenticated
    }

    private fun updateSessionFromResult(result: AuthResult) {
        if (result is AuthResult.Success) {
            _sessionState.value = AuthSessionState.Authenticated(result.session)
        }
    }

    private fun validateRegistration(
        displayName: String,
        email: String,
        password: String,
    ): AuthError.Validation? {
        return when {
            displayName.isBlank() -> AuthError.Validation("Indique o nome para criar a conta.")
            else -> validateCredentials(email, password)
        }
    }

    private fun validateCredentials(email: String, password: String): AuthError.Validation? {
        return when {
            !email.trim().isValidEmail() -> AuthError.Validation("Indique um email válido.")
            password.length < 6 -> AuthError.Validation("A palavra-passe deve ter pelo menos 6 caracteres.")
            else -> null
        }
    }
}

private fun AuthResult.withLocalProfile(
    displayName: String,
    phoneNumber: String,
): AuthResult {
    return when (this) {
        is AuthResult.Failure -> this
        is AuthResult.Success -> copy(
            session = session.copy(
                user = session.user.copy(
                    displayName = displayName.ifBlank { session.user.displayName },
                    phoneNumber = phoneNumber,
                ),
            ),
        )
    }
}

private fun String.isValidEmail(): Boolean {
    val trimmed = trim()
    return trimmed.contains("@") && !trimmed.startsWith("@") && !trimmed.endsWith("@")
}
