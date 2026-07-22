package com.sudsmobile.data.notification

import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.profile.MutableUserProfileChangeNotifier

class FirebaseNotificationRepository(
    private val api: NotificationFunctionsApi,
    private val authRepository: AuthRepository,
    private val profileChangeNotifier: MutableUserProfileChangeNotifier = MutableUserProfileChangeNotifier(),
) : NotificationRepository {
    override suspend fun getMyNotificationPreferences(): NotificationPreferencesResult {
        val idToken = currentIdTokenOrNull()
            ?: return NotificationPreferencesResult.Failure(unauthenticatedError())

        return api.getMyNotificationPreferences(idToken)
    }

    override suspend fun updateMyNotificationPreferences(
        request: NotificationPreferencesUpdateRequest,
    ): NotificationPreferencesMutationResult {
        val idToken = currentIdTokenOrNull()
            ?: return NotificationPreferencesMutationResult.Failure(unauthenticatedError())

        return api.updateMyNotificationPreferences(request, idToken)
            .also { result ->
                if (result is NotificationPreferencesMutationResult.Success) {
                    profileChangeNotifier.notifyProfileChanged()
                }
            }
    }

    override suspend fun registerNotificationToken(
        request: NotificationTokenRegistrationRequest,
    ): NotificationTokenRegistrationResult {
        val idToken = currentIdTokenOrNull()
            ?: return NotificationTokenRegistrationResult.Failure(unauthenticatedError())

        val validationError = validateTokenRequest(request)
        if (validationError != null) {
            return NotificationTokenRegistrationResult.Failure(validationError)
        }

        return api.registerNotificationToken(request.normalized(), idToken)
    }

    override suspend fun deleteNotificationToken(request: NotificationTokenDeleteRequest): NotificationTokenDeleteResult {
        val idToken = currentIdTokenOrNull()
            ?: return NotificationTokenDeleteResult.Failure(unauthenticatedError())

        val normalizedRequest = request.normalized()
        val validationError = validateDeleteRequest(normalizedRequest)
        if (validationError != null) {
            return NotificationTokenDeleteResult.Failure(validationError)
        }

        return api.deleteNotificationToken(normalizedRequest, idToken)
    }

    private suspend fun currentIdTokenOrNull(): String? = authRepository.currentSession()?.idToken

    private fun validateTokenRequest(
        request: NotificationTokenRegistrationRequest,
    ): NotificationError.Validation? {
        val token = request.token.trim()
        val fid = request.fid.trim()
        val registrationTarget = fid.ifBlank { token }
        return when {
            token.isNotBlank() == fid.isNotBlank() ->
                NotificationError.Validation("O registo de notificações é inválido.")
            registrationTarget.length !in MinRegistrationTargetLength..MaxRegistrationTargetLength ->
                NotificationError.Validation("O registo de notificações é inválido.")
            request.tokenId.isNotBlank() && !request.tokenId.isSafeTokenId() ->
                NotificationError.Validation("O identificador do dispositivo é inválido.")
            request.deviceLabel.length > MaxDeviceLabelLength ->
                NotificationError.Validation("O nome do dispositivo é demasiado longo.")
            request.appVersion.length > MaxMetadataLength ->
                NotificationError.Validation("A versão da aplicação é demasiado longa.")
            else -> null
        }
    }

    private fun NotificationTokenRegistrationRequest.normalized(): NotificationTokenRegistrationRequest = copy(
        token = token.trim(),
        fid = fid.trim(),
        tokenId = tokenId.trim(),
        deviceLabel = deviceLabel.trim().replace(Regex("\\s+"), " "),
        appVersion = appVersion.trim(),
    )

    private fun validateDeleteRequest(request: NotificationTokenDeleteRequest): NotificationError.Validation? {
        return when {
            !request.tokenId.isSafeTokenId() ->
                NotificationError.Validation("O identificador do dispositivo é inválido.")
            else -> null
        }
    }

    private fun NotificationTokenDeleteRequest.normalized(): NotificationTokenDeleteRequest = copy(
        tokenId = tokenId.trim(),
    )
}

private const val MinRegistrationTargetLength = 20
private const val MaxRegistrationTargetLength = 4096
private const val MaxDeviceLabelLength = 120
private const val MaxMetadataLength = 64
private val TokenIdRegex = Regex("^[A-Za-z0-9_-]{8,128}$")

private fun String.isSafeTokenId(): Boolean = TokenIdRegex.matches(trim())

private fun unauthenticatedError(): NotificationError.Unauthenticated {
    return NotificationError.Unauthenticated("Inicie sessão para gerir notificações.")
}
