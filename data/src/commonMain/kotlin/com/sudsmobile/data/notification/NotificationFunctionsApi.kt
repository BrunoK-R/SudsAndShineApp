package com.sudsmobile.data.notification

interface NotificationFunctionsApi {
    suspend fun getMyNotificationPreferences(idToken: String): NotificationPreferencesResult
    suspend fun updateMyNotificationPreferences(
        request: NotificationPreferencesUpdateRequest,
        idToken: String,
    ): NotificationPreferencesMutationResult

    suspend fun registerNotificationToken(
        request: NotificationTokenRegistrationRequest,
        idToken: String,
    ): NotificationTokenRegistrationResult

    suspend fun deleteNotificationToken(
        request: NotificationTokenDeleteRequest,
        idToken: String,
    ): NotificationTokenDeleteResult
}
