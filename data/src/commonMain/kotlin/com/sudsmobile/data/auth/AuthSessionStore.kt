package com.sudsmobile.data.auth

interface AuthSessionStore {
    fun readSession(): AuthSession?
    fun writeSession(session: AuthSession)
    fun clearSession()
}

object NoopAuthSessionStore : AuthSessionStore {
    override fun readSession(): AuthSession? = null
    override fun writeSession(session: AuthSession) = Unit
    override fun clearSession() = Unit
}
