package com.sudsmobile.data.profile

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val phoneNumber: String,
    val marketingOptIn: Boolean,
    val appointmentReminderOptIn: Boolean = false,
    val photoUrl: String = "",
)

data class UserProfileSaveRequest(
    val displayName: String,
    val phoneNumber: String,
    val marketingOptIn: Boolean,
    val appointmentReminderOptIn: Boolean = false,
    val photoUrl: String = "",
)

sealed interface UserProfileResult {
    data class Success(val profile: UserProfile) : UserProfileResult
    data class Failure(val error: UserProfileError) : UserProfileResult
}

sealed interface UserProfileMutationResult {
    data class Success(val profile: UserProfile) : UserProfileMutationResult
    data class Failure(val error: UserProfileError) : UserProfileMutationResult
}

sealed interface UserProfileError {
    val message: String

    data class Validation(override val message: String) : UserProfileError
    data class Permission(override val message: String) : UserProfileError
    data class Unauthenticated(override val message: String) : UserProfileError
    data class Unavailable(override val message: String) : UserProfileError
    data class Backend(override val message: String) : UserProfileError
}

interface UserProfileRepository {
    suspend fun getMyProfile(): UserProfileResult
    suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult
}
