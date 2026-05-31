package com.sudsmobile.data.admin

import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier

class FirebaseAdminRepository(
    private val api: AdminFunctionsApi,
    private val authRepository: AuthRepository,
    private val bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
) : AdminRepository {
    override suspend fun syncMyRole(): AdminRoleResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminRoleResult.Failure(unauthenticatedError())

        return api.syncMyRole(idToken)
    }

    override suspend fun getPendingBookingRequests(): AdminBookingRequestsResult {
        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingRequestsResult.Failure(unauthenticatedError())

        return api.getPendingBookingRequests(idToken)
    }

    override suspend fun acceptBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBookingDecisionResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingDecisionResult.Failure(unauthenticatedError())

        return api.acceptBookingRequest(normalizedRequest, idToken)
            .also { result ->
                if (result is AdminBookingDecisionResult.Success) {
                    bookingChangeNotifier.notifyBookingsChanged()
                }
            }
    }

    override suspend fun rejectBookingRequest(
        request: AdminBookingDecisionRequest,
    ): AdminBookingDecisionResult {
        val normalizedRequest = request.normalized()
        val validationError = validate(normalizedRequest)
        if (validationError != null) return AdminBookingDecisionResult.Failure(validationError)

        val idToken = currentIdTokenOrNull()
            ?: return AdminBookingDecisionResult.Failure(unauthenticatedError())

        return api.rejectBookingRequest(normalizedRequest, idToken)
            .also { result ->
                if (result is AdminBookingDecisionResult.Success) {
                    bookingChangeNotifier.notifyBookingsChanged()
                }
            }
    }

    private suspend fun currentIdTokenOrNull(): String? = authRepository.currentSession()?.idToken

    private fun validate(request: AdminBookingDecisionRequest): AdminError.Validation? {
        return when {
            request.reservationId.isBlank() || request.reservationId.contains("/") ->
                AdminError.Validation("A marcação selecionada é inválida.")
            request.reservationId.length > 160 ->
                AdminError.Validation("A marcação selecionada é inválida.")
            request.rejectionReason.length > MaxRejectionReasonLength ->
                AdminError.Validation("O motivo deve ter no máximo 500 caracteres.")
            else -> null
        }
    }

    private fun AdminBookingDecisionRequest.normalized(): AdminBookingDecisionRequest = copy(
        reservationId = reservationId.trim(),
        rejectionReason = rejectionReason.trim().replace(Regex("\\s+"), " "),
    )
}

private const val MaxRejectionReasonLength = 500

private fun unauthenticatedError(): AdminError.Unauthenticated {
    return AdminError.Unauthenticated("Inicie sessão para gerir marcações.")
}
