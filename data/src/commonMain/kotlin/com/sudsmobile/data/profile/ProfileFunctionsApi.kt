package com.sudsmobile.data.profile

interface ProfileFunctionsApi {
    suspend fun getMyProfile(idToken: String): UserProfileResult
    suspend fun updateMyProfile(
        request: UserProfileSaveRequest,
        idToken: String,
    ): UserProfileMutationResult
}

interface AccountDeletionFunctionsApi {
    suspend fun deleteMyAccount(
        request: AccountDeletionRequest,
        idToken: String,
    ): AccountDeletionResult
}

interface ProfilePhotoFunctionsApi {
    suspend fun updateMyProfilePhoto(
        request: UserProfilePhotoSaveRequest,
        idToken: String,
    ): UserProfileMutationResult

    suspend fun removeMyProfilePhoto(idToken: String): UserProfileMutationResult
}
