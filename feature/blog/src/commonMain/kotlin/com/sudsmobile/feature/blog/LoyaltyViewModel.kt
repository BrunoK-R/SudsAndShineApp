package com.sudsmobile.feature.blog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingLoyalty
import com.sudsmobile.data.booking.BookingLoyaltyError
import com.sudsmobile.data.booking.BookingLoyaltyRedemption
import com.sudsmobile.data.booking.BookingLoyaltyResult
import com.sudsmobile.data.booking.BookingLoyaltyStamp
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.BookingRewardRedemptionReceipt
import com.sudsmobile.data.booking.BookingRewardRedemptionError
import com.sudsmobile.data.booking.BookingRewardRedemptionResult
import com.sudsmobile.data.booking.toLoyaltyProgress as toBackendLoyaltyProgress
import com.sudsmobile.shared.loyalty.LoyaltyProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal typealias LoyaltyProgressUi = LoyaltyProgress

internal data class LoyaltyHistoryItemUi(
    val id: String,
    val date: String,
    val service: String,
    val points: Int,
)

internal data class LoyaltyRewardCodeUi(
    val id: String,
    val code: String,
    val statusLabel: String,
    val issuedAt: String,
)

internal sealed interface LoyaltyRedemptionUiState {
    data object Idle : LoyaltyRedemptionUiState
    data object Redeeming : LoyaltyRedemptionUiState
    data class Success(val message: String) : LoyaltyRedemptionUiState
    data class Error(val message: String, val retryable: Boolean) : LoyaltyRedemptionUiState
}

internal sealed interface LoyaltyUiState {
    data object Idle : LoyaltyUiState
    data object Loading : LoyaltyUiState
    data object Unauthenticated : LoyaltyUiState
    data class Empty(
        val progress: LoyaltyProgressUi,
        val availableRewards: Int = progress.availableRewards,
        val claimedRewards: Int = progress.claimedRewards,
        val rewardCodes: List<LoyaltyRewardCodeUi> = emptyList(),
        val redemptionState: LoyaltyRedemptionUiState = LoyaltyRedemptionUiState.Idle,
    ) : LoyaltyUiState
    data class Loaded(
        val progress: LoyaltyProgressUi,
        val history: List<LoyaltyHistoryItemUi>,
        val availableRewards: Int = progress.availableRewards,
        val claimedRewards: Int = progress.claimedRewards,
        val rewardCodes: List<LoyaltyRewardCodeUi> = emptyList(),
        val redemptionState: LoyaltyRedemptionUiState = LoyaltyRedemptionUiState.Idle,
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
                clearLoadedSession()
                _uiState.value = LoyaltyUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = state.error.toLoyaltyState()
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = LoyaltyUiState.Unauthenticated
            }
            is AuthSessionState.Authenticated -> {
                val uid = state.session.user.uid
                val revision = bookingRevision.value
                if (_uiState.value.isRedeeming()) return
                val hasReusableState = _uiState.value is LoyaltyUiState.Loaded ||
                    _uiState.value is LoyaltyUiState.Empty
                if (loadedUid == uid && loadedRevision == revision && hasReusableState) return
                loadRewards()
            }
        }
    }

    fun loadRewards() {
        if (_uiState.value is LoyaltyUiState.Loading || _uiState.value.isRedeeming()) return

        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = LoyaltyUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toLoyaltyState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = LoyaltyUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val requestedUid = session.session.user.uid
        val requestedRevision = bookingRevision.value
        viewModelScope.launch {
            _uiState.value = LoyaltyUiState.Loading
            val nextState = when (val result = bookingRepository.getMyLoyalty()) {
                is BookingLoyaltyResult.Success -> result.loyalty.toLoyaltyState()
                is BookingLoyaltyResult.Failure -> result.error.toLoyaltyState()
            }

            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    clearLoadedSession()
                    _uiState.value = LoyaltyUiState.Loading
                }
                is AuthSessionState.RestoreFailed -> {
                    clearLoadedSession()
                    _uiState.value = currentSessionState.error.toLoyaltyState()
                }
                AuthSessionState.Unauthenticated -> {
                    clearLoadedSession()
                    _uiState.value = LoyaltyUiState.Unauthenticated
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid == requestedUid) {
                        loadedUid = requestedUid
                        loadedRevision = requestedRevision
                        _uiState.value = nextState
                    } else {
                        clearLoadedSession()
                        _uiState.value = LoyaltyUiState.Unauthenticated
                    }
                }
            }
        }
    }

    fun redeemReward() {
        val content = _uiState.value.contentOrNull() ?: return
        if (content.availableRewards <= 0 || content.redemptionState is LoyaltyRedemptionUiState.Redeeming) {
            return
        }

        val requestedUid = authenticatedUidOrUpdateState() ?: return
        viewModelScope.launch {
            _uiState.value = content.toUiState(LoyaltyRedemptionUiState.Redeeming)
            val nextState = when (val result = bookingRepository.redeemLoyaltyReward()) {
                is BookingRewardRedemptionResult.Success -> {
                    val progress = result.receipt.loyalty.toBackendLoyaltyProgress()
                    val rewardCodes = content.rewardCodes.withLatest(result.receipt.toRewardCodeUi())
                    content.copy(
                        progress = progress,
                        availableRewards = result.receipt.loyalty.availableRewards,
                        claimedRewards = result.receipt.loyalty.claimedRewards,
                        rewardCodes = rewardCodes,
                    ).toUiState(
                        LoyaltyRedemptionUiState.Success(
                            "Recompensa ${result.receipt.rewardCode} emitida para validar na loja.",
                        ),
                    )
                }
                is BookingRewardRedemptionResult.Failure -> content.toUiState(
                    LoyaltyRedemptionUiState.Error(
                        message = result.error.message,
                        retryable = result.error.isRetryable(),
                    ),
                )
            }

            when (val currentSessionState = sessionState.value) {
                AuthSessionState.Restoring -> {
                    clearLoadedSession()
                    _uiState.value = LoyaltyUiState.Loading
                }
                is AuthSessionState.RestoreFailed -> {
                    clearLoadedSession()
                    _uiState.value = currentSessionState.error.toLoyaltyState()
                }
                AuthSessionState.Unauthenticated -> {
                    clearLoadedSession()
                    _uiState.value = LoyaltyUiState.Unauthenticated
                }
                is AuthSessionState.Authenticated -> {
                    if (currentSessionState.session.user.uid == requestedUid) {
                        loadedUid = requestedUid
                        loadedRevision = bookingRevision.value
                        _uiState.value = nextState
                    } else {
                        clearLoadedSession()
                        _uiState.value = LoyaltyUiState.Unauthenticated
                    }
                }
            }
        }
    }

    private fun authenticatedUidOrUpdateState(): String? {
        return when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedSession()
                _uiState.value = LoyaltyUiState.Loading
                null
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedSession()
                _uiState.value = currentSessionState.error.toLoyaltyState()
                null
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedSession()
                _uiState.value = LoyaltyUiState.Unauthenticated
                null
            }
            is AuthSessionState.Authenticated -> currentSessionState.session.user.uid
        }
    }

    private fun clearLoadedSession() {
        loadedUid = null
        loadedRevision = null
    }
}

private fun BookingLoyalty.toLoyaltyState(): LoyaltyUiState {
    val earnedItems = stampHistory.mapNotNull { it.toLoyaltyHistoryItemOrNull() }
    val rewardCodes = redemptions.mapNotNull { it.toRewardCodeUiOrNull() }
    val progress = summary.toBackendLoyaltyProgress()
    return if (earnedItems.isEmpty()) {
        LoyaltyUiState.Empty(
            progress = progress,
            availableRewards = progress.availableRewards,
            claimedRewards = progress.claimedRewards,
            rewardCodes = rewardCodes,
        )
    } else {
        LoyaltyUiState.Loaded(
            progress = progress,
            history = earnedItems,
            availableRewards = progress.availableRewards,
            claimedRewards = progress.claimedRewards,
            rewardCodes = rewardCodes,
        )
    }
}

private fun BookingLoyaltyError.toLoyaltyState(): LoyaltyUiState {
    return when (this) {
        is BookingLoyaltyError.Unauthenticated -> LoyaltyUiState.Unauthenticated
        is BookingLoyaltyError.Permission -> LoyaltyUiState.Error(message = message, retryable = false)
        is BookingLoyaltyError.Unavailable,
        is BookingLoyaltyError.Backend -> LoyaltyUiState.Error(message = message, retryable = true)
    }
}

private fun BookingLoyaltyStamp.toLoyaltyHistoryItemOrNull(): LoyaltyHistoryItemUi? {
    if (id.isBlank() || slotStartIso.isBlank()) return null
    return LoyaltyHistoryItemUi(
        id = id,
        date = slotStartIso.toDateLabel(),
        service = serviceName.ifBlank { "Lavagem" },
        points = points.coerceAtLeast(1),
    )
}

private fun BookingLoyaltyRedemption.toRewardCodeUiOrNull(): LoyaltyRewardCodeUi? {
    if (id.isBlank() || rewardCode.isBlank()) return null
    return LoyaltyRewardCodeUi(
        id = id,
        code = rewardCode,
        statusLabel = status.toRewardStatusLabel(),
        issuedAt = createdAtIso.toIssuedAtLabel(),
    )
}

private fun BookingRewardRedemptionReceipt.toRewardCodeUi(): LoyaltyRewardCodeUi = LoyaltyRewardCodeUi(
    id = redemptionId,
    code = rewardCode,
    statusLabel = status.toRewardStatusLabel(),
    issuedAt = "Emitida agora",
)

private fun List<LoyaltyRewardCodeUi>.withLatest(item: LoyaltyRewardCodeUi): List<LoyaltyRewardCodeUi> {
    return listOf(item) + filterNot { it.id == item.id || it.code == item.code }
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

private fun String.toIssuedAtLabel(): String {
    return if (isBlank()) "Emitida" else toDateLabel()
}

private fun String.toRewardStatusLabel(): String {
    return when (lowercase()) {
        "issued" -> "Disponível"
        "redeemed" -> "Usada"
        "reserved" -> "Reservada"
        else -> replaceFirstChar { it.titlecase() }.ifBlank { "Disponível" }
    }
}

private fun AuthError.isRetryable(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private fun AuthError.toLoyaltyState(): LoyaltyUiState.Error {
    return LoyaltyUiState.Error(message = message, retryable = isRetryable())
}

private data class LoyaltyContentSnapshot(
    val progress: LoyaltyProgressUi,
    val availableRewards: Int,
    val claimedRewards: Int,
    val history: List<LoyaltyHistoryItemUi>,
    val rewardCodes: List<LoyaltyRewardCodeUi>,
    val redemptionState: LoyaltyRedemptionUiState,
)

private fun LoyaltyContentSnapshot.toUiState(
    redemptionState: LoyaltyRedemptionUiState = this.redemptionState,
): LoyaltyUiState {
    return if (history.isEmpty()) {
        LoyaltyUiState.Empty(
            progress = progress,
            availableRewards = availableRewards,
            claimedRewards = claimedRewards,
            rewardCodes = rewardCodes,
            redemptionState = redemptionState,
        )
    } else {
        LoyaltyUiState.Loaded(
            progress = progress,
            history = history,
            availableRewards = availableRewards,
            claimedRewards = claimedRewards,
            rewardCodes = rewardCodes,
            redemptionState = redemptionState,
        )
    }
}

private fun LoyaltyUiState.contentOrNull(): LoyaltyContentSnapshot? {
    return when (this) {
        is LoyaltyUiState.Empty -> LoyaltyContentSnapshot(
            progress = progress,
            availableRewards = availableRewards,
            claimedRewards = claimedRewards,
            history = emptyList(),
            rewardCodes = rewardCodes,
            redemptionState = redemptionState,
        )
        is LoyaltyUiState.Loaded -> LoyaltyContentSnapshot(
            progress = progress,
            availableRewards = availableRewards,
            claimedRewards = claimedRewards,
            history = history,
            rewardCodes = rewardCodes,
            redemptionState = redemptionState,
        )
        LoyaltyUiState.Idle,
        LoyaltyUiState.Loading,
        LoyaltyUiState.Unauthenticated,
        is LoyaltyUiState.Error -> null
    }
}

private fun LoyaltyUiState.isRedeeming(): Boolean {
    return when (this) {
        is LoyaltyUiState.Empty -> redemptionState is LoyaltyRedemptionUiState.Redeeming
        is LoyaltyUiState.Loaded -> redemptionState is LoyaltyRedemptionUiState.Redeeming
        LoyaltyUiState.Idle,
        LoyaltyUiState.Loading,
        LoyaltyUiState.Unauthenticated,
        is LoyaltyUiState.Error -> false
    }
}

private fun BookingRewardRedemptionError.isRetryable(): Boolean {
    return this is BookingRewardRedemptionError.Unavailable || this is BookingRewardRedemptionError.Backend
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
