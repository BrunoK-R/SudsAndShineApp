package com.sudsmobile.feature.blog

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.referral.ReferralAttribution
import com.sudsmobile.data.referral.ReferralError
import com.sudsmobile.data.referral.ReferralProgram
import com.sudsmobile.data.referral.ReferralProgramResult
import com.sudsmobile.data.referral.ReferralRepository
import com.sudsmobile.data.referral.ReferralStats
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ReferralViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadRequiresAuthenticationBeforeRepositoryCall() = runTest {
        val repository = FakeReferralRepository()
        val viewModel = ReferralViewModel(ReferralViewModelAuthRepository(authenticated = false), repository)

        viewModel.loadReferral()
        runCurrent()

        assertIs<ReferralUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun loadMapsProgramAndPrivacySafeStats() = runTest {
        val repository = FakeReferralRepository()
        val viewModel = ReferralViewModel(ReferralViewModelAuthRepository(), repository)

        viewModel.loadReferral()
        runCurrent()

        val loaded = assertIs<ReferralUiState.Loaded>(viewModel.uiState.value)
        assertEquals("SUDS-AABBCCDDEE", loaded.program.code)
        assertEquals(2, loaded.program.claimedCount)
        assertEquals(1, loaded.program.qualifiedCount)
        assertEquals(1, repository.loadCalls)
    }

    @Test
    fun loadExplainsWhenTheAccountAgeMakesClaimingUnavailable() = runTest {
        val repository = FakeReferralRepository(
            loadResult = ReferralProgramResult.Success(
                referralProgram(canClaimCode = false, claimIneligibleReason = "account_too_old"),
            ),
        )
        val viewModel = ReferralViewModel(ReferralViewModelAuthRepository(), repository)

        viewModel.loadReferral()
        runCurrent()

        val loaded = assertIs<ReferralUiState.Loaded>(viewModel.uiState.value)
        assertEquals(false, loaded.program.canClaimCode)
        assertEquals(true, loaded.program.claimIneligibleMessage?.contains("30 dias") == true)
    }

    @Test
    fun claimUpdatesInputAndShowsQualificationExpectation() = runTest {
        val repository = FakeReferralRepository(
            claimResult = ReferralProgramResult.Success(
                referralProgram(
                    referredBy = ReferralAttribution(
                        code = "SUDS-ABCD123456",
                        status = "claimed",
                        claimedAtIso = "2026-07-22T09:00:00.000Z",
                        qualifiedAtIso = null,
                    ),
                ),
            ),
        )
        val viewModel = ReferralViewModel(ReferralViewModelAuthRepository(), repository)
        viewModel.loadReferral()
        runCurrent()

        viewModel.updateClaimCode("suds-abcd123456")
        viewModel.claimReferralCode()
        runCurrent()

        val loaded = assertIs<ReferralUiState.Loaded>(viewModel.uiState.value)
        assertEquals("SUDS-ABCD123456", repository.lastClaimCode)
        assertEquals("À espera da primeira lavagem paga", loaded.program.referredByStatus)
        assertIs<ReferralActionUiState.Success>(loaded.actionState)
    }

    @Test
    fun claimFailureKeepsLoadedProgramAndExplainsError() = runTest {
        val repository = FakeReferralRepository(
            claimResult = ReferralProgramResult.Failure(
                ReferralError.NotEligible("O código deve ser associado antes da primeira lavagem paga."),
            ),
        )
        val viewModel = ReferralViewModel(ReferralViewModelAuthRepository(), repository)
        viewModel.loadReferral()
        runCurrent()

        viewModel.updateClaimCode("SUDS-ABCD123456")
        viewModel.claimReferralCode()
        runCurrent()

        val loaded = assertIs<ReferralUiState.Loaded>(viewModel.uiState.value)
        val error = assertIs<ReferralActionUiState.Error>(loaded.actionState)
        assertEquals(false, error.retryable)
        assertEquals("SUDS-AABBCCDDEE", loaded.program.code)
    }
}

private class FakeReferralRepository(
    private val loadResult: ReferralProgramResult = ReferralProgramResult.Success(referralProgram()),
    private val claimResult: ReferralProgramResult = ReferralProgramResult.Success(referralProgram()),
) : ReferralRepository {
    var loadCalls = 0
    var lastClaimCode: String? = null

    override suspend fun getMyReferral(): ReferralProgramResult {
        loadCalls += 1
        return loadResult
    }

    override suspend fun claimReferralCode(code: String): ReferralProgramResult {
        lastClaimCode = code
        return claimResult
    }
}

private class ReferralViewModelAuthRepository(authenticated: Boolean = true) : AuthRepository {
    private val mutableState = MutableStateFlow(
        if (authenticated) referralAuthenticatedSession() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableState
    override suspend fun currentSession(): AuthSession? =
        (mutableState.value as? AuthSessionState.Authenticated)?.session
    override suspend fun signIn(email: String, password: String): AuthResult = error("Not used")
    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult = error("Not used")
    override suspend fun sendPasswordReset(email: String): AuthActionResult = error("Not used")
    override fun signOut() {
        mutableState.value = AuthSessionState.Unauthenticated
    }
}

private fun referralAuthenticatedSession(): AuthSessionState.Authenticated = AuthSessionState.Authenticated(
    AuthSession(
        user = AuthUser("uid-1", "bruno@example.com", "Bruno", ""),
        idToken = "id-token-1",
        refreshToken = "refresh-token-1",
        expiresInSeconds = 3600,
    ),
)

private fun referralProgram(
    referredBy: ReferralAttribution? = null,
    canClaimCode: Boolean = true,
    claimIneligibleReason: String? = null,
): ReferralProgram = ReferralProgram(
    code = "SUDS-AABBCCDDEE",
    shareMessage = "Convite",
    rewardPoints = 1,
    attributionDays = 30,
    referredBy = referredBy,
    stats = ReferralStats(
        claimedCount = 2,
        qualifiedCount = 1,
        pendingCount = 1,
        bonusPointsEarned = 1,
    ),
    invitations = emptyList(),
    canClaimCode = canClaimCode,
    claimIneligibleReason = claimIneligibleReason,
)
