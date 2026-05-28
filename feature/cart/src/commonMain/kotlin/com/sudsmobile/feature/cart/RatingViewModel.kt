package com.sudsmobile.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.BookingReviewError
import com.sudsmobile.data.booking.BookingReviewRequest
import com.sudsmobile.data.booking.BookingReviewResult
import com.sudsmobile.data.booking.isReviewableReservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class RatingTargetUi(
    val reservationId: String,
    val service: String,
    val date: String,
    val vehicle: String,
)

internal sealed interface RatingTargetUiState {
    data object Idle : RatingTargetUiState
    data object Loading : RatingTargetUiState
    data object Unauthenticated : RatingTargetUiState
    data object NotFound : RatingTargetUiState
    data class Loaded(val target: RatingTargetUi) : RatingTargetUiState
    data class Error(val message: String, val retryable: Boolean) : RatingTargetUiState
}

internal sealed interface RatingSubmitUiState {
    data object Idle : RatingSubmitUiState
    data object Loading : RatingSubmitUiState
    data object Success : RatingSubmitUiState
    data class ValidationError(val message: String) : RatingSubmitUiState
    data class Error(
        val message: String,
        val retryable: Boolean,
        val requiresSignIn: Boolean = false,
    ) : RatingSubmitUiState
}

internal class RatingViewModel(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _targetState = MutableStateFlow<RatingTargetUiState>(RatingTargetUiState.Idle)
    val targetState: StateFlow<RatingTargetUiState> = _targetState.asStateFlow()
    private val _submitState = MutableStateFlow<RatingSubmitUiState>(RatingSubmitUiState.Idle)
    val submitState: StateFlow<RatingSubmitUiState> = _submitState.asStateFlow()
    private var loadedUid: String? = null
    private var loadedReservationId: String? = null

    fun refreshTarget(reservationId: String) {
        val cleanReservationId = reservationId.trim()
        if (cleanReservationId.isBlank()) {
            _targetState.value = RatingTargetUiState.NotFound
            return
        }

        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedTarget()
                _targetState.value = RatingTargetUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedTarget()
                _targetState.value = currentSessionState.error.toTargetState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedTarget()
                _targetState.value = RatingTargetUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val hasReusableState = loadedUid == session.session.user.uid &&
            loadedReservationId == cleanReservationId &&
            _targetState.value is RatingTargetUiState.Loaded
        if (hasReusableState) return
        loadTarget(cleanReservationId)
    }

    fun loadTarget(reservationId: String) {
        if (_targetState.value is RatingTargetUiState.Loading) return

        val cleanReservationId = reservationId.trim()
        if (cleanReservationId.isBlank()) {
            _targetState.value = RatingTargetUiState.NotFound
            return
        }

        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedTarget()
                _targetState.value = RatingTargetUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedTarget()
                _targetState.value = currentSessionState.error.toTargetState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedTarget()
                _targetState.value = RatingTargetUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }
        val requestedUid = session.session.user.uid

        viewModelScope.launch {
            _targetState.value = RatingTargetUiState.Loading
            val nextState = when (val result = bookingRepository.getMyBookings()) {
                is BookingHistoryResult.Success -> {
                    result.history.reservations
                        .firstOrNull { it.id == cleanReservationId }
                        ?.toRatingTargetState()
                        ?: RatingTargetUiState.NotFound
                }
                is BookingHistoryResult.Failure -> result.error.toTargetState()
            }

            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    clearLoadedTarget()
                    _targetState.value = RatingTargetUiState.Loading
                }
                is AuthSessionState.RestoreFailed -> {
                    clearLoadedTarget()
                    _targetState.value = currentSessionState.error.toTargetState()
                }
                AuthSessionState.Unauthenticated -> {
                    clearLoadedTarget()
                    _targetState.value = RatingTargetUiState.Unauthenticated
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid == requestedUid) {
                        loadedUid = requestedUid
                        loadedReservationId = cleanReservationId
                        _targetState.value = nextState
                    } else {
                        clearLoadedTarget()
                        _targetState.value = RatingTargetUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun submitReview(reservationId: String, rating: Int, tags: List<String>, comment: String) {
        if (_submitState.value is RatingSubmitUiState.Loading) return

        val cleanReservationId = reservationId.trim()
        if (cleanReservationId.isBlank()) {
            _submitState.value = RatingSubmitUiState.ValidationError("Selecione uma marcação válida para avaliar.")
            return
        }
        if (rating !in 1..5) {
            _submitState.value = RatingSubmitUiState.ValidationError("Escolha uma avaliação entre 1 e 5 estrelas.")
            return
        }

        val expectedUid = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                _submitState.value = restoringSubmitState()
                return
            }
            is AuthSessionState.RestoreFailed -> {
                _submitState.value = currentSessionState.error.toSubmitState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                _submitState.value = unauthenticatedSubmitState()
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState.session.user.uid
        }

        viewModelScope.launch {
            _submitState.value = RatingSubmitUiState.Loading
            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    _submitState.value = restoringSubmitState()
                    return@launch
                }
                is AuthSessionState.RestoreFailed -> {
                    _submitState.value = currentSessionState.error.toSubmitState()
                    return@launch
                }
                AuthSessionState.Unauthenticated -> {
                    _submitState.value = unauthenticatedSubmitState()
                    return@launch
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid != expectedUid) {
                        _submitState.value = changedSessionSubmitState()
                        return@launch
                    }
                }
            }

            _submitState.value = when (
                val result = bookingRepository.submitReview(
                    BookingReviewRequest(
                        reservationId = cleanReservationId,
                        rating = rating,
                        tags = tags,
                        comment = comment,
                    ),
                )
            ) {
                is BookingReviewResult.Success -> RatingSubmitUiState.Success
                is BookingReviewResult.Failure -> result.error.toSubmitState()
            }
        }
    }

    fun clearSubmitError() {
        if (_submitState.value is RatingSubmitUiState.Error ||
            _submitState.value is RatingSubmitUiState.ValidationError
        ) {
            _submitState.value = RatingSubmitUiState.Idle
        }
    }

    private fun BookingHistoryError.toTargetState(): RatingTargetUiState {
        return when (this) {
            is BookingHistoryError.Unauthenticated -> RatingTargetUiState.Unauthenticated
            is BookingHistoryError.Permission -> RatingTargetUiState.Error(message = message, retryable = false)
            is BookingHistoryError.Unavailable,
            is BookingHistoryError.Backend -> RatingTargetUiState.Error(message = message, retryable = true)
        }
    }

    private fun BookingReviewError.toSubmitState(): RatingSubmitUiState {
        return when (this) {
            is BookingReviewError.Validation -> RatingSubmitUiState.ValidationError(message)
            is BookingReviewError.Unauthenticated -> RatingSubmitUiState.Error(
                message = message,
                retryable = false,
                requiresSignIn = true,
            )
            is BookingReviewError.Permission,
            is BookingReviewError.NotFound,
            is BookingReviewError.NotReviewable -> RatingSubmitUiState.Error(message = message, retryable = false)
            is BookingReviewError.Unavailable,
            is BookingReviewError.Backend -> RatingSubmitUiState.Error(message = message, retryable = true)
        }
    }

    private fun clearLoadedTarget() {
        loadedUid = null
        loadedReservationId = null
    }
}

private fun AuthError.toTargetState(): RatingTargetUiState.Error {
    return RatingTargetUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.toSubmitState(): RatingSubmitUiState.Error {
    val retryable = isRetryableSessionError()
    return RatingSubmitUiState.Error(
        message = message,
        retryable = retryable,
        requiresSignIn = !retryable,
    )
}

private fun restoringSubmitState(): RatingSubmitUiState.Error {
    return RatingSubmitUiState.Error(
        message = "A sessão ainda está a ser validada. Tente novamente dentro de momentos.",
        retryable = true,
    )
}

private fun unauthenticatedSubmitState(): RatingSubmitUiState.Error {
    return RatingSubmitUiState.Error(
        message = "Inicie sessão para enviar esta avaliação.",
        retryable = false,
        requiresSignIn = true,
    )
}

private fun changedSessionSubmitState(): RatingSubmitUiState.Error {
    return RatingSubmitUiState.Error(
        message = "A sessão mudou antes de enviarmos a avaliação. Atualize e tente novamente.",
        retryable = false,
    )
}

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private fun BookingHistoryReservation.toRatingTargetState(): RatingTargetUiState {
    if (!isReviewableReservation() || id.isBlank() || slotStartIso.isBlank()) {
        return RatingTargetUiState.NotFound
    }

    return RatingTargetUiState.Loaded(
        RatingTargetUi(
            reservationId = id,
            service = serviceName.ifBlank { "Serviço" },
            date = slotStartIso.toDateLabel(),
            vehicle = vehicleLabel?.takeIf { it.isNotBlank() } ?: vehicleType.toVehicleLabel(),
        ),
    )
}

private fun String.toVehicleLabel(): String = when (lowercase()) {
    "suv" -> "SUV"
    "passageiros", "passenger" -> "Passageiros"
    else -> replaceFirstChar { it.titlecase() }
}

private fun String.toDateLabel(): String {
    val date = substringBefore("T")
    val parts = date.split("-")
    if (parts.size != 3) return date.ifBlank { "Data a confirmar" }
    val year = parts[0]
    val month = parts[1].toIntOrNull()?.let { monthNames.getOrNull(it - 1) } ?: return date
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "$day de $month, $year"
}

private val monthNames = listOf(
    "janeiro",
    "fevereiro",
    "março",
    "abril",
    "maio",
    "junho",
    "julho",
    "agosto",
    "setembro",
    "outubro",
    "novembro",
    "dezembro",
)
