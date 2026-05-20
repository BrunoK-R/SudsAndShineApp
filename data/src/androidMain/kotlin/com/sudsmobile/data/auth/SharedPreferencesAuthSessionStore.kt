package com.sudsmobile.data.auth

import android.content.Context

class SharedPreferencesAuthSessionStore(
    context: Context,
) : AuthSessionStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "suds_auth_session",
        Context.MODE_PRIVATE,
    )

    override fun readSession(): AuthSession? {
        val uid = preferences.getString(KeyUid, null).orEmpty()
        val email = preferences.getString(KeyEmail, null).orEmpty()
        val idToken = preferences.getString(KeyIdToken, null).orEmpty()
        val refreshToken = preferences.getString(KeyRefreshToken, null).orEmpty()
        if (uid.isBlank() || email.isBlank() || idToken.isBlank() || refreshToken.isBlank()) {
            return null
        }

        return AuthSession(
            user = AuthUser(
                uid = uid,
                email = email,
                displayName = preferences.getString(KeyDisplayName, null).orEmpty(),
                phoneNumber = preferences.getString(KeyPhoneNumber, null).orEmpty(),
            ),
            idToken = idToken,
            refreshToken = refreshToken,
            expiresInSeconds = preferences.getLong(KeyExpiresInSeconds, 0L),
            issuedAtEpochSeconds = preferences.getLong(KeyIssuedAtEpochSeconds, 0L),
        )
    }

    override fun writeSession(session: AuthSession) {
        preferences.edit()
            .putString(KeyUid, session.user.uid)
            .putString(KeyEmail, session.user.email)
            .putString(KeyDisplayName, session.user.displayName)
            .putString(KeyPhoneNumber, session.user.phoneNumber)
            .putString(KeyIdToken, session.idToken)
            .putString(KeyRefreshToken, session.refreshToken)
            .putLong(KeyExpiresInSeconds, session.expiresInSeconds)
            .putLong(KeyIssuedAtEpochSeconds, session.issuedAtEpochSeconds)
            .apply()
    }

    override fun clearSession() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KeyUid = "uid"
        const val KeyEmail = "email"
        const val KeyDisplayName = "display_name"
        const val KeyPhoneNumber = "phone_number"
        const val KeyIdToken = "id_token"
        const val KeyRefreshToken = "refresh_token"
        const val KeyExpiresInSeconds = "expires_in_seconds"
        const val KeyIssuedAtEpochSeconds = "issued_at_epoch_seconds"
    }
}
