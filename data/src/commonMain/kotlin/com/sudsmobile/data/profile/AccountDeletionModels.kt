package com.sudsmobile.data.profile

data class AccountDeletionRequest(
    val confirmationName: String,
    val appleAuthorizationCode: String = "",
)

sealed interface AccountDeletionResult {
    data object Success : AccountDeletionResult
    data object RequiresAppleReauthentication : AccountDeletionResult
    data class Failure(val error: UserProfileError) : AccountDeletionResult
}

interface AccountDeletionRepository {
    suspend fun deleteMyAccount(request: AccountDeletionRequest): AccountDeletionResult
}
