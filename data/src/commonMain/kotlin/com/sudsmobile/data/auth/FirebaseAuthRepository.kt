package com.sudsmobile.data.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FirebaseAuthRepository(
    private val api: AuthApi,
    private val sessionStore: AuthSessionStore = NoopAuthSessionStore,
    private val nowEpochSeconds: () -> Long = ::currentEpochSeconds,
    restoreOnStart: Boolean = true,
    private val repositoryScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AuthRepository {
    private val _sessionState = MutableStateFlow<AuthSessionState>(
        if (restoreOnStart) AuthSessionState.Restoring else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()
    private val refreshMutex = Mutex()

    init {
        if (restoreOnStart) {
            repositoryScope.launch {
                restorePersistedSession()
            }
        }
    }

    override suspend fun currentSession(): AuthSession? {
        return refreshMutex.withLock {
            val session = currentAuthenticatedSession() ?: return@withLock null
            if (!session.needsRefresh(nowEpochSeconds())) return@withLock session

            refreshSession(session)
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        val validationError = validateCredentials(email, password)
        if (validationError != null) return AuthResult.Failure(validationError)

        val result = api.signIn(email = email.trim().lowercase(), password = password)
        return result.applyAuthenticatedSession()
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        val validationError = validateRegistration(displayName, email, phoneNumber, password)
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
        return finalResult.applyAuthenticatedSession()
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        if (!email.trim().isValidEmail()) {
            return AuthActionResult.Failure(AuthError.Validation("Indique um email válido."))
        }

        return api.sendPasswordReset(email.trim().lowercase())
    }

    override fun signOut() {
        _sessionState.value = AuthSessionState.Unauthenticated
        clearStoredSession()
    }

    private suspend fun restorePersistedSession() {
        val persistedSession = readStoredSession()
        if (persistedSession == null) {
            completeRestore(AuthSessionState.Unauthenticated)
            return
        }

        refreshSession(persistedSession, onlyIfRestoring = true)
    }

    private fun completeRestore(state: AuthSessionState) {
        _sessionState.compareAndSet(AuthSessionState.Restoring, state)
    }

    private suspend fun refreshSession(
        session: AuthSession,
        onlyIfRestoring: Boolean = false,
    ): AuthSession? {
        if (onlyIfRestoring && _sessionState.value != AuthSessionState.Restoring) {
            return currentAuthenticatedSession()
        }

        if (session.refreshToken.isBlank()) {
            if (clearSessionState(session, onlyIfRestoring)) {
                clearStoredSession()
            }
            return null
        }

        return when (val result = api.refreshSession(session.refreshToken)) {
            is AuthResult.Success -> {
                val refreshedSession = session.mergeRefresh(result.session, nowEpochSeconds())
                if (authenticateRefreshedSession(session, refreshedSession, onlyIfRestoring)) {
                    writeStoredSession(refreshedSession)
                    refreshedSession
                } else if (onlyIfRestoring) {
                    currentAuthenticatedSession()
                } else {
                    null
                }
            }
            is AuthResult.Failure -> {
                if (onlyIfRestoring && _sessionState.value != AuthSessionState.Restoring) {
                    return currentAuthenticatedSession()
                }
                when (result.error) {
                    is AuthError.InvalidCredentials,
                    is AuthError.Permission,
                    is AuthError.Validation -> {
                        if (clearSessionState(session, onlyIfRestoring)) {
                            clearStoredSession()
                        }
                        null
                    }
                    else -> {
                        if (onlyIfRestoring) {
                            completeRestore(AuthSessionState.RestoreFailed(result.error))
                            currentAuthenticatedSession()
                        } else if (currentAuthenticatedSession() == session) {
                            session
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    private fun AuthResult.applyAuthenticatedSession(): AuthResult {
        return when (this) {
            is AuthResult.Failure -> this
            is AuthResult.Success -> {
                val issuedSession = session.copy(issuedAtEpochSeconds = nowEpochSeconds())
                _sessionState.value = AuthSessionState.Authenticated(issuedSession)
                writeStoredSession(issuedSession)
                copy(session = issuedSession)
            }
        }
    }

    private fun currentAuthenticatedSession(): AuthSession? {
        return (_sessionState.value as? AuthSessionState.Authenticated)?.session
    }

    private fun authenticateRefreshedSession(
        staleSession: AuthSession,
        refreshedSession: AuthSession,
        onlyIfRestoring: Boolean,
    ): Boolean {
        val refreshedState = AuthSessionState.Authenticated(refreshedSession)
        return if (onlyIfRestoring) {
            _sessionState.compareAndSet(AuthSessionState.Restoring, refreshedState)
        } else {
            _sessionState.compareAndSet(
                AuthSessionState.Authenticated(staleSession),
                refreshedState,
            )
        }
    }

    private fun clearSessionState(
        session: AuthSession,
        onlyIfRestoring: Boolean,
    ): Boolean {
        return if (onlyIfRestoring) {
            _sessionState.compareAndSet(
                AuthSessionState.Restoring,
                AuthSessionState.Unauthenticated,
            )
        } else {
            _sessionState.compareAndSet(
                AuthSessionState.Authenticated(session),
                AuthSessionState.Unauthenticated,
            )
        }
    }

    private fun readStoredSession(): AuthSession? = runCatching {
        sessionStore.readSession()
    }.getOrNull()

    private fun writeStoredSession(session: AuthSession) {
        runCatching { sessionStore.writeSession(session) }
    }

    private fun clearStoredSession() {
        runCatching { sessionStore.clearSession() }
    }

    private fun validateRegistration(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthError.Validation? {
        val phone = phoneNumber.trim()
        return when {
            displayName.isBlank() -> AuthError.Validation("Indique o nome para criar a conta.")
            phone.length < 6 -> AuthError.Validation("Indique um telemóvel válido.")
            phone.length > MaxPhoneLength || !phone.all { it.isDigit() || it in phoneSeparators } ->
                AuthError.Validation("Indique um telemóvel válido.")
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

private fun AuthSession.needsRefresh(nowEpochSeconds: Long): Boolean {
    val expiresAt = issuedAtEpochSeconds + expiresInSeconds
    return issuedAtEpochSeconds <= 0L ||
        expiresInSeconds <= 0L ||
        nowEpochSeconds >= expiresAt - RefreshSkewSeconds
}

private fun AuthSession.mergeRefresh(
    refreshedSession: AuthSession,
    issuedAtEpochSeconds: Long,
): AuthSession = copy(
    user = user.copy(
        uid = refreshedSession.user.uid.ifBlank { user.uid },
    ),
    idToken = refreshedSession.idToken,
    refreshToken = refreshedSession.refreshToken.ifBlank { refreshToken },
    expiresInSeconds = refreshedSession.expiresInSeconds,
    issuedAtEpochSeconds = issuedAtEpochSeconds,
)

private const val RefreshSkewSeconds = 300L

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

private const val MaxPhoneLength = 32
private val phoneSeparators = setOf('+', '-', '(', ')', '.', ' ')
