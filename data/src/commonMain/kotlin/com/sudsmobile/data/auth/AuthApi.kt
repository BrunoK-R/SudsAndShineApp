package com.sudsmobile.data.auth

interface AuthApi {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult = AuthResult.Failure(
        AuthError.Permission("Este método de autenticação não está ativo neste dispositivo."),
    )
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun updateProfile(session: AuthSession, displayName: String): AuthResult
    suspend fun refreshSession(refreshToken: String): AuthResult
    suspend fun sendPasswordReset(email: String): AuthActionResult
}
