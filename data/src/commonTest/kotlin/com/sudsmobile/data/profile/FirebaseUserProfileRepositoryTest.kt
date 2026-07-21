package com.sudsmobile.data.profile

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest

class FirebaseUserProfileRepositoryTest {
    @Test
    fun rejectsProfileLoadWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingProfileFunctionsApi()
        val repository = FirebaseUserProfileRepository(api, FakeProfileAuthRepository(authenticated = false))

        val result = repository.getMyProfile()

        assertIs<UserProfileResult.Failure>(result)
        assertIs<UserProfileError.Unauthenticated>(result.error)
        assertEquals(0, api.loadCalls)
    }

    @Test
    fun normalizesUpdateRequestAndPassesIdToken() = runTest {
        val api = RecordingProfileFunctionsApi()
        val profileChangeNotifier = MutableUserProfileChangeNotifier()
        val repository = FirebaseUserProfileRepository(
            api,
            FakeProfileAuthRepository(authenticated = true),
            profileChangeNotifier,
        )

        val result = repository.updateMyProfile(
            UserProfileSaveRequest(
                displayName = "  Bruno Ribeiro  ",
                phoneNumber = " 913 005 855 ",
                marketingOptIn = true,
                appointmentReminderOptIn = true,
            ),
        )

        assertIs<UserProfileMutationResult.Success>(result)
        assertEquals("id-token-1", api.lastIdToken)
        assertEquals("Bruno Ribeiro", api.lastRequest?.displayName)
        assertEquals("913 005 855", api.lastRequest?.phoneNumber)
        assertEquals(true, api.lastRequest?.marketingOptIn)
        assertEquals(true, api.lastRequest?.appointmentReminderOptIn)
        assertEquals(1L, profileChangeNotifier.revision.value)
    }

    @Test
    fun rejectsInvalidPhoneBeforeCallingApi() = runTest {
        val api = RecordingProfileFunctionsApi()
        val repository = FirebaseUserProfileRepository(api, FakeProfileAuthRepository(authenticated = true))

        val result = repository.updateMyProfile(
            UserProfileSaveRequest(
                displayName = "Bruno Ribeiro",
                phoneNumber = "abc",
                marketingOptIn = false,
                appointmentReminderOptIn = true,
            ),
        )

        assertIs<UserProfileMutationResult.Failure>(result)
        assertIs<UserProfileError.Validation>(result.error)
        assertEquals(0, api.updateCalls)
    }

    @Test
    fun uploadsProcessedJpegAndNotifiesProfileObservers() = runTest {
        val api = RecordingProfileFunctionsApi()
        val profileChangeNotifier = MutableUserProfileChangeNotifier()
        val repository = FirebaseUserProfileRepository(
            api = api,
            authRepository = FakeProfileAuthRepository(authenticated = true),
            profileChangeNotifier = profileChangeNotifier,
        )
        val imageBytes = byteArrayOf(1, 2, 3, 4)

        val result = repository.updateMyProfilePhoto(
            UserProfilePhotoSaveRequest(imageBytes = imageBytes, mimeType = " IMAGE/JPEG "),
        )

        assertIs<UserProfileMutationResult.Success>(result)
        assertEquals("id-token-1", api.lastPhotoIdToken)
        assertEquals(imageBytes.toList(), api.lastPhotoRequest?.imageBytes?.toList())
        assertEquals("image/jpeg", api.lastPhotoRequest?.mimeType)
        assertEquals(1, api.photoUpdateCalls)
        assertEquals(1L, profileChangeNotifier.revision.value)
    }

    @Test
    fun rejectsOversizedProfilePhotoBeforeCallingApi() = runTest {
        val api = RecordingProfileFunctionsApi()
        val repository = FirebaseUserProfileRepository(
            api = api,
            authRepository = FakeProfileAuthRepository(authenticated = true),
        )

        val result = repository.updateMyProfilePhoto(
            UserProfilePhotoSaveRequest(imageBytes = ByteArray(1_000_001)),
        )

        val failure = assertIs<UserProfileMutationResult.Failure>(result)
        assertIs<UserProfileError.Validation>(failure.error)
        assertEquals(0, api.photoUpdateCalls)
    }

    @Test
    fun removesProfilePhotoAndNotifiesProfileObservers() = runTest {
        val api = RecordingProfileFunctionsApi()
        val profileChangeNotifier = MutableUserProfileChangeNotifier()
        val repository = FirebaseUserProfileRepository(
            api = api,
            authRepository = FakeProfileAuthRepository(authenticated = true),
            profileChangeNotifier = profileChangeNotifier,
        )

        val result = repository.removeMyProfilePhoto()

        assertIs<UserProfileMutationResult.Success>(result)
        assertEquals("id-token-1", api.lastPhotoIdToken)
        assertEquals(1, api.photoRemoveCalls)
        assertEquals(1L, profileChangeNotifier.revision.value)
    }
}

private class RecordingProfileFunctionsApi : ProfileFunctionsApi, ProfilePhotoFunctionsApi {
    var loadCalls: Int = 0
        private set
    var updateCalls: Int = 0
        private set
    var lastRequest: UserProfileSaveRequest? = null
        private set
    var lastIdToken: String? = null
        private set
    var photoUpdateCalls: Int = 0
        private set
    var photoRemoveCalls: Int = 0
        private set
    var lastPhotoRequest: UserProfilePhotoSaveRequest? = null
        private set
    var lastPhotoIdToken: String? = null
        private set

    override suspend fun getMyProfile(idToken: String): UserProfileResult {
        loadCalls += 1
        lastIdToken = idToken
        return UserProfileResult.Success(profile())
    }

    override suspend fun updateMyProfile(
        request: UserProfileSaveRequest,
        idToken: String,
    ): UserProfileMutationResult {
        updateCalls += 1
        lastRequest = request
        lastIdToken = idToken
        return UserProfileMutationResult.Success(
            profile(
                displayName = request.displayName,
                phoneNumber = request.phoneNumber,
                marketingOptIn = request.marketingOptIn,
                appointmentReminderOptIn = request.appointmentReminderOptIn,
            ),
        )
    }

    override suspend fun updateMyProfilePhoto(
        request: UserProfilePhotoSaveRequest,
        idToken: String,
    ): UserProfileMutationResult {
        photoUpdateCalls += 1
        lastPhotoRequest = request
        lastPhotoIdToken = idToken
        return UserProfileMutationResult.Success(profile(photoUrl = "https://example.com/avatar.jpg"))
    }

    override suspend fun removeMyProfilePhoto(idToken: String): UserProfileMutationResult {
        photoRemoveCalls += 1
        lastPhotoIdToken = idToken
        return UserProfileMutationResult.Success(profile(photoUrl = ""))
    }
}

private class FakeProfileAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    override val sessionState: StateFlow<AuthSessionState> = MutableStateFlow(
        if (authenticated) {
            AuthSessionState.Authenticated(
                AuthSession(
                    user = AuthUser(
                        uid = "uid-1",
                        email = "bruno@example.com",
                        displayName = "Bruno",
                        phoneNumber = "913005855",
                    ),
                    idToken = "id-token-1",
                    refreshToken = "refresh-token-1",
                    expiresInSeconds = 3600,
                ),
            )
        } else {
            AuthSessionState.Unauthenticated
        },
    )

    override suspend fun currentSession(): AuthSession? {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        error("Not used")
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        error("Not used")
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        error("Not used")
    }

    override fun signOut() = Unit
}

private fun profile(
    displayName: String = "Bruno Ribeiro",
    phoneNumber: String = "913005855",
    marketingOptIn: Boolean = false,
    appointmentReminderOptIn: Boolean = false,
    photoUrl: String = "",
): UserProfile = UserProfile(
    uid = "uid-1",
    email = "bruno@example.com",
    displayName = displayName,
    phoneNumber = phoneNumber,
    marketingOptIn = marketingOptIn,
    appointmentReminderOptIn = appointmentReminderOptIn,
    photoUrl = photoUrl,
)
