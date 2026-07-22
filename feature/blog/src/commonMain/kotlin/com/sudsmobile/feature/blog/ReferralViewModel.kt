package com.sudsmobile.feature.blog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.referral.ReferralAttribution
import com.sudsmobile.data.referral.ReferralError
import com.sudsmobile.data.referral.ReferralProgram
import com.sudsmobile.data.referral.ReferralProgramResult
import com.sudsmobile.data.referral.ReferralRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ReferralProgramUi(
    val code: String,
    val shareMessage: String,
    val rewardPoints: Int,
    val attributionDays: Int,
    val canClaimCode: Boolean,
    val claimIneligibleMessage: String?,
    val referredByStatus: String?,
    val referredByCode: String?,
    val claimedCount: Int,
    val qualifiedCount: Int,
    val pendingCount: Int,
    val bonusPointsEarned: Int,
)

internal sealed interface ReferralActionUiState {
    data object Idle : ReferralActionUiState
    data object Submitting : ReferralActionUiState
    data class Success(val message: String) : ReferralActionUiState
    data class Error(val message: String, val retryable: Boolean) : ReferralActionUiState
    data object Copied : ReferralActionUiState
}

internal sealed interface ReferralUiState {
    data object Idle : ReferralUiState
    data object Loading : ReferralUiState
    data object Unauthenticated : ReferralUiState
    data class Loaded(
        val program: ReferralProgramUi,
        val claimCode: String = "",
        val actionState: ReferralActionUiState = ReferralActionUiState.Idle,
    ) : ReferralUiState
    data class Error(val message: String, val retryable: Boolean) : ReferralUiState
}

internal class ReferralViewModel(
    private val authRepository: AuthRepository,
    private val referralRepository: ReferralRepository,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState

    private val _uiState = MutableStateFlow<ReferralUiState>(ReferralUiState.Idle)
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    private var loadedUid: String? = null
    private var loadingUid: String? = null
    private var requestSequence: Long = 0

    fun refreshForSession(force: Boolean = false) {
        when (val session = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearSession()
                _uiState.value = ReferralUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                clearSession()
                _uiState.value = session.error.toReferralUiState()
            }
            AuthSessionState.Unauthenticated -> {
                clearSession()
                _uiState.value = ReferralUiState.Unauthenticated
            }
            is AuthSessionState.Authenticated -> {
                val uid = session.session.user.uid
                if (!force && loadedUid == uid && _uiState.value is ReferralUiState.Loaded) return
                loadReferral()
            }
        }
    }

    fun loadReferral() {
        val session = authenticatedSessionOrUpdateState() ?: return
        val uid = session.session.user.uid
        if (loadingUid == uid) return
        val sequence = ++requestSequence
        loadingUid = uid
        viewModelScope.launch {
            try {
                _uiState.value = ReferralUiState.Loading
                val nextState = when (val result = referralRepository.getMyReferral()) {
                    is ReferralProgramResult.Success -> ReferralUiState.Loaded(result.program.toUi())
                    is ReferralProgramResult.Failure -> result.error.toReferralUiState()
                }
                if (sequence != requestSequence || !sessionStillMatches(uid)) return@launch
                loadedUid = uid
                _uiState.value = nextState
            } finally {
                if (sequence == requestSequence) loadingUid = null
            }
        }
    }

    fun updateClaimCode(value: String) {
        val loaded = _uiState.value as? ReferralUiState.Loaded ?: return
        if (loaded.actionState is ReferralActionUiState.Submitting) return
        _uiState.value = loaded.copy(
            claimCode = value.take(20).uppercase(),
            actionState = ReferralActionUiState.Idle,
        )
    }

    fun claimReferralCode() {
        val loaded = _uiState.value as? ReferralUiState.Loaded ?: return
        if (
            loaded.program.referredByStatus != null ||
            !loaded.program.canClaimCode ||
            loaded.actionState is ReferralActionUiState.Submitting
        ) return
        val uid = authenticatedSessionOrUpdateState()?.session?.user?.uid ?: return
        _uiState.value = loaded.copy(actionState = ReferralActionUiState.Submitting)
        val sequence = ++requestSequence
        viewModelScope.launch {
            val nextState = when (val result = referralRepository.claimReferralCode(loaded.claimCode)) {
                is ReferralProgramResult.Success -> ReferralUiState.Loaded(
                    program = result.program.toUi(),
                    actionState = ReferralActionUiState.Success(
                        "Código associado. O selo é atribuído após a primeira lavagem paga concluída.",
                    ),
                )
                is ReferralProgramResult.Failure -> loaded.copy(
                    actionState = ReferralActionUiState.Error(
                        message = result.error.message,
                        retryable = result.error.isRetryable(),
                    ),
                )
            }
            if (sequence != requestSequence || !sessionStillMatches(uid)) return@launch
            loadedUid = uid
            _uiState.value = nextState
        }
    }

    fun markShareCopied() {
        val loaded = _uiState.value as? ReferralUiState.Loaded ?: return
        if (loaded.actionState is ReferralActionUiState.Submitting) return
        _uiState.value = loaded.copy(actionState = ReferralActionUiState.Copied)
    }

    private fun authenticatedSessionOrUpdateState(): AuthSessionState.Authenticated? {
        return when (val session = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearSession()
                _uiState.value = ReferralUiState.Loading
                null
            }
            is AuthSessionState.RestoreFailed -> {
                clearSession()
                _uiState.value = session.error.toReferralUiState()
                null
            }
            AuthSessionState.Unauthenticated -> {
                clearSession()
                _uiState.value = ReferralUiState.Unauthenticated
                null
            }
            is AuthSessionState.Authenticated -> session
        }
    }

    private fun sessionStillMatches(uid: String): Boolean {
        val current = sessionState.value
        if (current is AuthSessionState.Authenticated && current.session.user.uid == uid) return true
        refreshForSession(force = true)
        return false
    }

    private fun clearSession() {
        loadedUid = null
        loadingUid = null
        requestSequence += 1
    }
}

private fun ReferralProgram.toUi(): ReferralProgramUi = ReferralProgramUi(
    code = code,
    shareMessage = shareMessage,
    rewardPoints = rewardPoints,
    attributionDays = attributionDays,
    canClaimCode = canClaimCode,
    claimIneligibleMessage = claimIneligibleReason.toClaimIneligibleMessage(),
    referredByStatus = referredBy?.toStatusLabel(),
    referredByCode = referredBy?.code,
    claimedCount = stats.claimedCount,
    qualifiedCount = stats.qualifiedCount,
    pendingCount = stats.pendingCount,
    bonusPointsEarned = stats.bonusPointsEarned,
)

private fun String?.toClaimIneligibleMessage(): String? = when (this) {
    "account_too_old" -> "O prazo para associar um código terminou. Os códigos só podem ser usados nos primeiros 30 dias da conta."
    "first_paid_wash_completed" -> "O código tem de ser associado antes da primeira lavagem paga."
    else -> null
}

private fun ReferralAttribution.toStatusLabel(): String = when (status.trim().lowercase()) {
    "qualified" -> "Bónus atribuído"
    else -> "À espera da primeira lavagem paga"
}

private fun ReferralError.toReferralUiState(): ReferralUiState.Error = ReferralUiState.Error(
    message = message,
    retryable = isRetryable(),
)

private fun ReferralError.isRetryable(): Boolean = this is ReferralError.Unavailable || this is ReferralError.Backend

private fun AuthError.toReferralUiState(): ReferralUiState.Error = ReferralUiState.Error(
    message = message,
    retryable = this is AuthError.Unavailable || this is AuthError.Backend,
)
