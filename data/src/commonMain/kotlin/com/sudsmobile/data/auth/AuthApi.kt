package com.sudsmobile.data.auth

interface AuthApi {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun updateProfile(session: AuthSession, displayName: String): AuthResult
    suspend fun sendPasswordReset(email: String): AuthActionResult
}
