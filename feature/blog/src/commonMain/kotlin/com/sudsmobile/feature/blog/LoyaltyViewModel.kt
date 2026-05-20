package com.sudsmobile.feature.blog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class LoyaltyProgressUi(
    val totalWashes: Int,
    val currentWashes: Int,
    val targetWashes: Int,
    val remainingWashes: Int,
    val progress: Float,
)

internal data class LoyaltyHistoryItemUi(
    val id: String,
    val date: String,
    val service: String,
    val points: Int,
)

internal sealed interface LoyaltyUiState {
    data object Idle : LoyaltyUiState
    data object Loading : LoyaltyUiState
    data object Unauthenticated : LoyaltyUiState
    data class Empty(val progress: LoyaltyProgressUi) : LoyaltyUiState
    data class Loaded(
        val progress: LoyaltyProgressUi,
        val history: List<LoyaltyHistoryItemUi>,
    ) : LoyaltyUiState
    data class Error(val message: String, val retryable: Boolean) : LoyaltyUiState
}

internal class LoyaltyViewModel(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository,
    private val bookingChangeNotifier: BookingChangeNotifier,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision

    private val _uiState = MutableStateFlow<LoyaltyUiState>(LoyaltyUiState.Idle)
    val uiState: StateFlow<LoyaltyUiState> = _uiState.asStateFlow()

    private var loadedUid: String? = null
    private var loadedRevision: Long? = null

    fun refreshForSession() {
        when (val state = sessionState.value) {
            AuthSessionState.Restoring -> {
                loadedUid = null
                loadedRevision = null
                _uiState.value = LoyaltyUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                loadedUid = null
                loadedRevision = null
                _uiState.value = LoyaltyUiState.Error(
                    message = state.error.message,
                    retryable = state.error.isRetryable(),
                )
            }
            AuthSessionState.Unauthenticated -> {
                loadedUid = null
                loadedRevision = null
                _uiState.value = LoyaltyUiState.Unauthenticated
            }
            is AuthSessionState.Authenticated -> {
                val uid = state.session.user.uid
                val revision = bookingRevision.value
                val hasReusableState = _uiState.value is LoyaltyUiState.Loaded ||
                    _uiState.value is LoyaltyUiState.Empty
                if (loadedUid == uid && loadedRevision == revision && hasReusableState) return
                loadRewards()
            }
        }
    }

    fun loadRewards() {
        if (_uiState.value is LoyaltyUiState.Loading) return

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            loadedRevision = null
            _uiState.value = LoyaltyUiState.Unauthenticated
            return
        }

        val requestedUid = session.session.user.uid
        val requestedRevision = bookingRevision.value
        viewModelScope.launch {
            _uiState.value = LoyaltyUiState.Loading
            val nextState = when (val result = bookingRepository.getMyBookings()) {
                is BookingHistoryResult.Success -> result.history.toLoyaltyState()
                is BookingHistoryResult.Failure -> result.error.toLoyaltyState()
            }

            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedUid = requestedUid
                loadedRevision = requestedRevision
                _uiState.value = nextState
            } else {
                loadedUid = null
                loadedRevision = null
                _uiState.value = LoyaltyUiState.Unauthenticated
            }
        }
    }
}

private const val LoyaltyRewardInterval = 10

private fun BookingHistory.toLoyaltyState(): LoyaltyUiState {
    val earnedItems = reservations
        .filter { !it.upcoming && !it.isCancelled() }
        .mapNotNull { it.toLoyaltyHistoryItemOrNull() }

    val progress = earnedItems.size.toLoyaltyProgress()
    return if (earnedItems.isEmpty()) {
        LoyaltyUiState.Empty(progress)
    } else {
        LoyaltyUiState.Loaded(
            progress = progress,
            history = earnedItems,
        )
    }
}

private fun BookingHistoryError.toLoyaltyState(): LoyaltyUiState {
    return when (this) {
        is BookingHistoryError.Unauthenticated -> LoyaltyUiState.Unauthenticated
        is BookingHistoryError.Permission -> LoyaltyUiState.Error(message = message, retryable = false)
        is BookingHistoryError.Unavailable,
        is BookingHistoryError.Backend -> LoyaltyUiState.Error(message = message, retryable = true)
    }
}

private fun BookingHistoryReservation.toLoyaltyHistoryItemOrNull(): LoyaltyHistoryItemUi? {
    if (id.isBlank() || slotStartIso.isBlank()) return null
    return LoyaltyHistoryItemUi(
        id = id,
        date = slotStartIso.toDateLabel(),
        service = serviceName.ifBlank { "Lavagem" },
        points = 1,
    )
}

private fun BookingHistoryReservation.isCancelled(): Boolean {
    val normalized = status.lowercase()
    return normalized in setOf("cancelled", "canceled", "cancelado")
}

private fun Int.toLoyaltyProgress(): LoyaltyProgressUi {
    val currentCycle = this % LoyaltyRewardInterval
    val currentWashes = if (this > 0 && currentCycle == 0) {
        LoyaltyRewardInterval
    } else {
        currentCycle
    }
    return LoyaltyProgressUi(
        totalWashes = this,
        currentWashes = currentWashes,
        targetWashes = LoyaltyRewardInterval,
        remainingWashes = LoyaltyRewardInterval - currentWashes,
        progress = currentWashes.toFloat() / LoyaltyRewardInterval.toFloat(),
    )
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

private fun AuthError.isRetryable(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
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
