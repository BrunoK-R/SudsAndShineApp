package com.sudsmobile.data.auth

import platform.Foundation.NSUserDefaults

class UserDefaultsAuthSessionStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : AuthSessionStore {
    override fun readSession(): AuthSession? {
        val uid = defaults.stringForKey(KeyUid).orEmpty()
        val email = defaults.stringForKey(KeyEmail).orEmpty()
        val idToken = defaults.stringForKey(KeyIdToken).orEmpty()
        val refreshToken = defaults.stringForKey(KeyRefreshToken).orEmpty()
        if (uid.isBlank() || email.isBlank() || idToken.isBlank() || refreshToken.isBlank()) {
            return null
        }

        return AuthSession(
            user = AuthUser(
                uid = uid,
                email = email,
                displayName = defaults.stringForKey(KeyDisplayName).orEmpty(),
                phoneNumber = defaults.stringForKey(KeyPhoneNumber).orEmpty(),
            ),
            idToken = idToken,
            refreshToken = refreshToken,
            expiresInSeconds = defaults.integerForKey(KeyExpiresInSeconds),
            issuedAtEpochSeconds = defaults.integerForKey(KeyIssuedAtEpochSeconds),
        )
    }

    override fun writeSession(session: AuthSession) {
        defaults.setObject(session.user.uid, KeyUid)
        defaults.setObject(session.user.email, KeyEmail)
        defaults.setObject(session.user.displayName, KeyDisplayName)
        defaults.setObject(session.user.phoneNumber, KeyPhoneNumber)
        defaults.setObject(session.idToken, KeyIdToken)
        defaults.setObject(session.refreshToken, KeyRefreshToken)
        defaults.setInteger(session.expiresInSeconds, KeyExpiresInSeconds)
        defaults.setInteger(session.issuedAtEpochSeconds, KeyIssuedAtEpochSeconds)
    }

    override fun clearSession() {
        listOf(
            KeyUid,
            KeyEmail,
            KeyDisplayName,
            KeyPhoneNumber,
            KeyIdToken,
            KeyRefreshToken,
            KeyExpiresInSeconds,
            KeyIssuedAtEpochSeconds,
        ).forEach { key -> defaults.removeObjectForKey(key) }
    }

    private companion object {
        const val KeyUid = "suds_auth_uid"
        const val KeyEmail = "suds_auth_email"
        const val KeyDisplayName = "suds_auth_display_name"
        const val KeyPhoneNumber = "suds_auth_phone_number"
        const val KeyIdToken = "suds_auth_id_token"
        const val KeyRefreshToken = "suds_auth_refresh_token"
        const val KeyExpiresInSeconds = "suds_auth_expires_in_seconds"
        const val KeyIssuedAtEpochSeconds = "suds_auth_issued_at_epoch_seconds"
    }
}
