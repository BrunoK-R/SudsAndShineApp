package com.sudsmobile.data.notification

data class NotificationPreferences(
    val bookingStatusEnabled: Boolean,
    val appointmentReminderEnabled: Boolean,
    val loyaltyEnabled: Boolean,
    val adminPendingAlertEnabled: Boolean,
    val marketingEnabled: Boolean,
)

data class NotificationPreferencesUpdateRequest(
    val bookingStatusEnabled: Boolean,
    val appointmentReminderEnabled: Boolean,
    val loyaltyEnabled: Boolean,
    val adminPendingAlertEnabled: Boolean,
    val marketingEnabled: Boolean,
)

data class NotificationTokenRegistrationRequest(
    val token: String,
    val platform: NotificationTokenPlatform,
    val tokenId: String = "",
    val deviceLabel: String = "",
    val appVersion: String = "",
)

data class NotificationTokenDeleteRequest(
    val tokenId: String,
)

enum class NotificationTokenPlatform(val wireName: String) {
    Android("android"),
    Ios("ios"),
    Web("web"),
}

sealed interface NotificationPreferencesResult {
    data class Success(val preferences: NotificationPreferences) : NotificationPreferencesResult
    data class Failure(val error: NotificationError) : NotificationPreferencesResult
}

sealed interface NotificationPreferencesMutationResult {
    data class Success(val preferences: NotificationPreferences) : NotificationPreferencesMutationResult
    data class Failure(val error: NotificationError) : NotificationPreferencesMutationResult
}

sealed interface NotificationTokenRegistrationResult {
    data class Success(
        val tokenId: String,
        val platform: NotificationTokenPlatform,
        val enabled: Boolean,
    ) : NotificationTokenRegistrationResult

    data class Failure(val error: NotificationError) : NotificationTokenRegistrationResult
}

sealed interface NotificationTokenDeleteResult {
    data class Success(
        val tokenId: String,
        val status: String,
    ) : NotificationTokenDeleteResult

    data class Failure(val error: NotificationError) : NotificationTokenDeleteResult
}

sealed interface NotificationError {
    val message: String

    data class Validation(override val message: String) : NotificationError
    data class Permission(override val message: String) : NotificationError
    data class Unauthenticated(override val message: String) : NotificationError
    data class Unavailable(override val message: String) : NotificationError
    data class Backend(override val message: String) : NotificationError
}

interface NotificationRepository {
    suspend fun getMyNotificationPreferences(): NotificationPreferencesResult
    suspend fun updateMyNotificationPreferences(
        request: NotificationPreferencesUpdateRequest,
    ): NotificationPreferencesMutationResult

    suspend fun registerNotificationToken(
        request: NotificationTokenRegistrationRequest,
    ): NotificationTokenRegistrationResult

    suspend fun deleteNotificationToken(request: NotificationTokenDeleteRequest): NotificationTokenDeleteResult
}
