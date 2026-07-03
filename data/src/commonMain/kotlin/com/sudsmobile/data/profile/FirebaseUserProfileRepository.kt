package com.sudsmobile.data.profile

import com.sudsmobile.data.auth.AuthRepository

class FirebaseUserProfileRepository(
    private val api: ProfileFunctionsApi,
    private val authRepository: AuthRepository,
    private val profileChangeNotifier: MutableUserProfileChangeNotifier = MutableUserProfileChangeNotifier(),
) : UserProfileRepository {
    override suspend fun getMyProfile(): UserProfileResult {
        val idToken = currentIdTokenOrNull()
            ?: return UserProfileResult.Failure(unauthenticatedError())

        return api.getMyProfile(idToken)
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        val idToken = currentIdTokenOrNull()
            ?: return UserProfileMutationResult.Failure(unauthenticatedError())

        val validationError = validate(request)
        if (validationError != null) {
            return UserProfileMutationResult.Failure(validationError)
        }

        return api.updateMyProfile(request.normalized(), idToken)
            .also { result ->
                if (result is UserProfileMutationResult.Success) {
                    profileChangeNotifier.notifyProfileChanged()
                }
            }
    }

    private suspend fun currentIdTokenOrNull(): String? = authRepository.currentSession()?.idToken

    private fun validate(request: UserProfileSaveRequest): UserProfileError.Validation? {
        val phone = request.phoneNumber.trim()
        return when {
            request.displayName.isBlank() ->
                UserProfileError.Validation("Indique o nome para guardar o perfil.")
            request.displayName.trim().length > MaxDisplayNameLength ->
                UserProfileError.Validation("O nome é demasiado longo.")
            phone.length < 6 ->
                UserProfileError.Validation("Indique um telemóvel válido.")
            phone.length > MaxPhoneLength || !phone.all { it.isDigit() || it in phoneSeparators } ->
                UserProfileError.Validation("Indique um telemóvel válido.")
            request.photoUrl.isNotBlank() && !request.photoUrl.isValidProfilePhotoUrl() ->
                UserProfileError.Validation("Indique uma URL de fotografia válida.")
            else -> null
        }
    }

    private fun UserProfileSaveRequest.normalized(): UserProfileSaveRequest = copy(
        displayName = displayName.trim(),
        phoneNumber = phoneNumber.trim(),
        photoUrl = photoUrl.trim(),
    )
}

private const val MaxDisplayNameLength = 100
private const val MaxPhoneLength = 32
private const val MaxProfilePhotoUrlLength = 2048
private val phoneSeparators = setOf('+', '-', '(', ')', '.', ' ')

private fun String.isValidProfilePhotoUrl(): Boolean {
    val value = trim()
    if (value.length !in 1..MaxProfilePhotoUrlLength) return false
    if (value.any { it.isWhitespace() || it.isISOControl() }) return false
    return value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("http://", ignoreCase = true)
}

private fun unauthenticatedError(): UserProfileError.Unauthenticated {
    return UserProfileError.Unauthenticated("Inicie sessão para gerir os seus dados.")
}
