package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ProfileStatsUi(
    val washCount: String,
    val loyaltyRemaining: String,
    val vehicleCount: String,
)

internal sealed interface ProfileStatsUiState {
    data object Idle : ProfileStatsUiState
    data object Loading : ProfileStatsUiState
    data object Unauthenticated : ProfileStatsUiState
    data class Loaded(val stats: ProfileStatsUi) : ProfileStatsUiState
    data class Error(val message: String, val retryable: Boolean) : ProfileStatsUiState
}

internal class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    private val _statsState = MutableStateFlow<ProfileStatsUiState>(ProfileStatsUiState.Idle)
    val statsState: StateFlow<ProfileStatsUiState> = _statsState.asStateFlow()

    fun loadStats() {
        if (_statsState.value is ProfileStatsUiState.Loading) return

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            _statsState.value = ProfileStatsUiState.Unauthenticated
            return
        }
        val requestedUid = session.session.user.uid

        viewModelScope.launch {
            _statsState.value = ProfileStatsUiState.Loading
            val nextState = when (val result = bookingRepository.getMyBookings()) {
                is BookingHistoryResult.Success -> ProfileStatsUiState.Loaded(result.history.toProfileStats())
                is BookingHistoryResult.Failure -> result.error.toProfileStatsState()
            }
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                _statsState.value = nextState
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _statsState.value = ProfileStatsUiState.Unauthenticated
    }

    private fun BookingHistoryError.toProfileStatsState(): ProfileStatsUiState {
        return when (this) {
            is BookingHistoryError.Unauthenticated -> ProfileStatsUiState.Unauthenticated
            is BookingHistoryError.Permission -> ProfileStatsUiState.Error(message = message, retryable = false)
            is BookingHistoryError.Unavailable,
            is BookingHistoryError.Backend -> ProfileStatsUiState.Error(message = message, retryable = true)
        }
    }
}

private const val LoyaltyRewardInterval = 5

private fun BookingHistory.toProfileStats(): ProfileStatsUi {
    val completedWashCount = reservations.count { !it.upcoming && !it.isCancelled() }
    val remainingToReward = LoyaltyRewardInterval - (completedWashCount % LoyaltyRewardInterval)

    return ProfileStatsUi(
        washCount = completedWashCount.toString(),
        loyaltyRemaining = remainingToReward.toString(),
        vehicleCount = "0",
    )
}

private fun BookingHistoryReservation.isCancelled(): Boolean {
    val normalized = status.lowercase()
    return normalized in setOf("cancelled", "canceled", "cancelado")
}
