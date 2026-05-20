package com.sudsmobile.data.profile

interface ProfileFunctionsApi {
    suspend fun getMyProfile(idToken: String): UserProfileResult
    suspend fun updateMyProfile(
        request: UserProfileSaveRequest,
        idToken: String,
    ): UserProfileMutationResult
}
