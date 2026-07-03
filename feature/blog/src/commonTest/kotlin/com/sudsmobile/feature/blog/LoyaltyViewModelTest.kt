package com.sudsmobile.feature.blog

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.booking.BookingAvailabilityRequest
import com.sudsmobile.data.booking.BookingAvailabilityResult
import com.sudsmobile.data.booking.BookingCreateRequest
import com.sudsmobile.data.booking.BookingCreateResult
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingLoyalty
import com.sudsmobile.data.booking.BookingLoyaltyError
import com.sudsmobile.data.booking.BookingLoyaltyRedemption
import com.sudsmobile.data.booking.BookingLoyaltyResult
import com.sudsmobile.data.booking.BookingLoyaltyStamp
import com.sudsmobile.data.booking.BookingLoyaltySummary
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.BookingRewardRedemptionError
import com.sudsmobile.data.booking.BookingRewardRedemptionReceipt
import com.sudsmobile.data.booking.BookingRewardRedemptionResult
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.shared.loyalty.toLoyaltyProgress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
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
class LoyaltyViewModelTest {
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
    fun loadRewardsRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeLoyaltyBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = loyaltyViewModel(
            authRepository = FakeLoyaltyAuthRepository(authenticated = false),
            bookingRepository = repository,
        )

        viewModel.loadRewards()
        runCurrent()

        assertIs<LoyaltyUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadRewardsShowsLoadingWhileSessionIsRestoringWithoutRepositoryCall() = runTest {
        val repository = FakeLoyaltyBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = loyaltyViewModel(
            authRepository = FakeLoyaltyAuthRepository(initialState = AuthSessionState.Restoring),
            bookingRepository = repository,
        )

        viewModel.loadRewards()
        runCurrent()

        assertIs<LoyaltyUiState.Loading>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadRewardsMapsRestoreFailureWithoutRepositoryCall() = runTest {
        val repository = FakeLoyaltyBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = loyaltyViewModel(
            authRepository = FakeLoyaltyAuthRepository(
                initialState = AuthSessionState.RestoreFailed(
                    AuthError.Unavailable("Sessão indisponível."),
                ),
            ),
            bookingRepository = repository,
        )

        viewModel.loadRewards()
        runCurrent()

        val error = assertIs<LoyaltyUiState.Error>(viewModel.uiState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadRewardsKeepsRestoringStateWhenSessionChangesDuringLoad() = runTest {
        val authRepository = FakeLoyaltyAuthRepository()
        val repository = FakeLoyaltyBookingRepository(
            BookingHistoryResult.Success(
                BookingHistory(
                    reservations = listOf(
                        loyaltyReservation(
                            id = "stamp-1",
                            slotStartIso = "2026-05-18T10:00:00.000Z",
                        ),
                    ),
                ),
            ),
        )
        val viewModel = loyaltyViewModel(
            authRepository = authRepository,
            bookingRepository = repository,
        )

        viewModel.loadRewards()
        authRepository.setSessionState(AuthSessionState.Restoring)
        runCurrent()

        assertIs<LoyaltyUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, repository.historyCalls)
    }

    @Test
    fun loadRewardsMapsRestoreFailureWhenSessionChangesDuringLoad() = runTest {
        val authRepository = FakeLoyaltyAuthRepository()
        val repository = FakeLoyaltyBookingRepository(
            BookingHistoryResult.Success(
                BookingHistory(
                    reservations = listOf(
                        loyaltyReservation(
                            id = "stamp-1",
                            slotStartIso = "2026-05-18T10:00:00.000Z",
                        ),
                    ),
                ),
            ),
        )
        val viewModel = loyaltyViewModel(
            authRepository = authRepository,
            bookingRepository = repository,
        )

        viewModel.loadRewards()
        authRepository.setSessionState(
            AuthSessionState.RestoreFailed(AuthError.Backend("Sessão expirada.")),
        )
        runCurrent()

        val error = assertIs<LoyaltyUiState.Error>(viewModel.uiState.value)
        assertEquals("Sessão expirada.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(1, repository.historyCalls)
    }

    @Test
    fun loadRewardsMapsCompletedReservationsToProgressAndHistory() = runTest {
        val viewModel = loyaltyViewModel(
            bookingRepository = FakeLoyaltyBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            loyaltyReservation(
                                id = "stamp-7",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                serviceName = "Lavagem Premium",
                            ),
                            loyaltyReservation(
                                id = "stamp-6",
                                slotStartIso = "2026-05-12T11:30:00.000Z",
                                serviceName = "Lavagem Standard",
                            ),
                            loyaltyReservation(
                                id = "upcoming",
                                slotStartIso = "2026-05-22T09:00:00.000Z",
                                upcoming = true,
                            ),
                            loyaltyReservation(
                                id = "cancelled",
                                slotStartIso = "2026-05-08T09:00:00.000Z",
                                status = "cancelled",
                            ),
                        ) + (1..5).map {
                            loyaltyReservation(
                                id = "stamp-$it",
                                slotStartIso = "2026-04-${(10 + it).twoDigits()}T09:00:00.000Z",
                            )
                        },
                    ),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals(7, loaded.progress.totalWashes)
        assertEquals(7, loaded.progress.currentWashes)
        assertEquals(10, loaded.progress.targetWashes)
        assertEquals(3, loaded.progress.remainingWashes)
        assertEquals(0.7f, loaded.progress.progress)
        assertEquals(
            listOf("stamp-7", "stamp-6", "stamp-1", "stamp-2", "stamp-3", "stamp-4", "stamp-5"),
            loaded.history.map { it.id },
        )
        assertEquals("18 de maio, 2026", loaded.history.first().date)
        assertEquals("Lavagem Premium", loaded.history.first().service)
    }

    @Test
    fun loadRewardsShowsCompleteCycleWhenRewardIsReady() = runTest {
        val viewModel = loyaltyViewModel(
            bookingRepository = FakeLoyaltyBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = (1..10).map {
                            loyaltyReservation(
                                id = "stamp-$it",
                                slotStartIso = "2026-05-${it.twoDigits()}T09:00:00.000Z",
                            )
                        },
                    ),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals(10, loaded.progress.totalWashes)
        assertEquals(10, loaded.progress.currentWashes)
        assertEquals(0, loaded.progress.remainingWashes)
        assertEquals(1.0f, loaded.progress.progress)
        assertEquals(true, loaded.progress.rewardReady)
        assertEquals(1, loaded.progress.completedRewards)
    }

    @Test
    fun loadRewardsUsesBackendClaimedRewardSummaryWhenPresent() = runTest {
        val viewModel = loyaltyViewModel(
            bookingRepository = FakeLoyaltyBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = (1..10).map {
                            loyaltyReservation(
                                id = "stamp-$it",
                                slotStartIso = "2026-05-${it.twoDigits()}T09:00:00.000Z",
                            )
                        },
                        loyalty = loyaltySummary(
                            totalWashes = 10,
                            currentWashes = 0,
                            remainingWashes = 10,
                            progress = 0f,
                            rewardReady = false,
                            completedRewards = 1,
                            claimedRewards = 1,
                            availableRewards = 0,
                        ),
                    ),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals(10, loaded.progress.totalWashes)
        assertEquals(0, loaded.progress.currentWashes)
        assertEquals(10, loaded.progress.remainingWashes)
        assertEquals(false, loaded.progress.rewardReady)
        assertEquals(1, loaded.claimedRewards)
        assertEquals(0, loaded.availableRewards)
    }

    @Test
    fun loadRewardsExposesIssuedRewardCodesFromBackendLoyalty() = runTest {
        val viewModel = loyaltyViewModel(
            bookingRepository = FakeLoyaltyBookingRepository(
                historyResult = BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = (1..10).map {
                            loyaltyReservation(
                                id = "stamp-$it",
                                slotStartIso = "2026-05-${it.twoDigits()}T09:00:00.000Z",
                            )
                        },
                        loyalty = loyaltySummary(
                            totalWashes = 10,
                            currentWashes = 0,
                            remainingWashes = 10,
                            progress = 0f,
                            rewardReady = false,
                            completedRewards = 1,
                            claimedRewards = 1,
                            availableRewards = 0,
                        ),
                    ),
                ),
                redemptions = listOf(
                    BookingLoyaltyRedemption(
                        id = "reward-0001",
                        rewardCode = "SS-FREE-UID1-0001",
                        rewardNumber = 1,
                        status = "issued",
                        createdAtIso = "2026-05-20T12:00:00.000Z",
                    ),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals("SS-FREE-UID1-0001", loaded.rewardCodes.single().code)
        assertEquals("Disponível", loaded.rewardCodes.single().statusLabel)
        assertEquals("20 de maio, 2026", loaded.rewardCodes.single().issuedAt)
        assertEquals(true, loaded.rewardCodes.single().active)
    }

    @Test
    fun loadRewardsSubtractsActiveRewardCodesFromRedeemableRewards() = runTest {
        val repository = FakeLoyaltyBookingRepository(
            historyResult = rewardReadyHistoryResult(),
            redemptions = listOf(
                BookingLoyaltyRedemption(
                    id = "reward-0001",
                    rewardCode = "SS-FREE-UID1-0001",
                    rewardNumber = 1,
                    status = "issued",
                    createdAtIso = "2026-05-20T12:00:00.000Z",
                ),
            ),
        )
        val viewModel = loyaltyViewModel(bookingRepository = repository)

        viewModel.loadRewards()
        runCurrent()
        viewModel.redeemReward()
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals(0, loaded.availableRewards)
        assertEquals(1, loaded.rewardCodes.count { it.active })
        assertEquals(0, repository.redemptionCalls)
    }

    @Test
    fun loadRewardsMapsNoCompletedReservationsToEmptyProgress() = runTest {
        val viewModel = loyaltyViewModel(
            bookingRepository = FakeLoyaltyBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            loyaltyReservation(
                                id = "upcoming",
                                slotStartIso = "2026-05-22T09:00:00.000Z",
                                upcoming = true,
                            ),
                        ),
                    ),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val empty = assertIs<LoyaltyUiState.Empty>(viewModel.uiState.value)
        assertEquals(0, empty.progress.totalWashes)
        assertEquals(0, empty.progress.currentWashes)
        assertEquals(10, empty.progress.remainingWashes)
    }

    @Test
    fun loadRewardsMapsBackendErrorAsRetryable() = runTest {
        val viewModel = loyaltyViewModel(
            bookingRepository = FakeLoyaltyBookingRepository(
                BookingHistoryResult.Failure(
                    BookingHistoryError.Unavailable("Recompensas indisponíveis."),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val error = assertIs<LoyaltyUiState.Error>(viewModel.uiState.value)
        assertEquals("Recompensas indisponíveis.", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun refreshForSessionReloadsWhenBookingRevisionChanges() = runTest {
        val notifier = MutableBookingChangeNotifier()
        val repository = FakeLoyaltyBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = loyaltyViewModel(
            bookingRepository = repository,
            bookingChangeNotifier = notifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<LoyaltyUiState.Empty>(viewModel.uiState.value)
        assertEquals(1, repository.historyCalls)

        viewModel.refreshForSession()
        runCurrent()

        assertEquals(1, repository.historyCalls)

        notifier.notifyBookingsChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<LoyaltyUiState.Empty>(viewModel.uiState.value)
        assertEquals(2, repository.historyCalls)
    }

    @Test
    fun refreshForSessionKeepsLatestBookingRevisionWhenOlderLoyaltyLoadCompletesLast() = runTest {
        val notifier = MutableBookingChangeNotifier()
        val olderResult = CompletableDeferred<BookingLoyaltyResult>()
        val newerResult = CompletableDeferred<BookingLoyaltyResult>()
        val repository = DeferredLoyaltyBookingRepository(
            results = listOf(olderResult, newerResult),
        )
        val viewModel = loyaltyViewModel(
            bookingRepository = repository,
            bookingChangeNotifier = notifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<LoyaltyUiState.Loading>(viewModel.uiState.value)

        notifier.notifyBookingsChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertEquals(2, repository.historyCalls)

        newerResult.complete(loyaltyResultForStamp("newer-stamp", "2026-05-21T09:00:00.000Z"))
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals(listOf("newer-stamp"), loaded.history.map { it.id })

        olderResult.complete(loyaltyResultForStamp("older-stamp", "2026-05-18T09:00:00.000Z"))
        runCurrent()

        val stillLoaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals(listOf("newer-stamp"), stillLoaded.history.map { it.id })
    }

    @Test
    fun redeemRewardUpdatesLoadedStateWithBackendReceipt() = runTest {
        val repository = FakeLoyaltyBookingRepository(
            historyResult = BookingHistoryResult.Success(
                BookingHistory(
                    reservations = (1..10).map {
                        loyaltyReservation(
                            id = "stamp-$it",
                            slotStartIso = "2026-05-${it.twoDigits()}T09:00:00.000Z",
                        )
                    },
                    loyalty = loyaltySummary(
                        totalWashes = 10,
                        currentWashes = 10,
                        remainingWashes = 0,
                        progress = 1f,
                        rewardReady = true,
                        completedRewards = 1,
                        claimedRewards = 0,
                        availableRewards = 1,
                    ),
                ),
            ),
            redemptionResult = BookingRewardRedemptionResult.Success(
                BookingRewardRedemptionReceipt(
                    redemptionId = "reward-0001",
                    rewardCode = "SS-FREE-UID1-0001",
                    rewardNumber = 1,
                    status = "issued",
                    loyalty = loyaltySummary(
                        totalWashes = 10,
                        currentWashes = 0,
                        remainingWashes = 10,
                        progress = 0f,
                        rewardReady = false,
                        completedRewards = 1,
                        claimedRewards = 1,
                        availableRewards = 0,
                    ),
                ),
            ),
        )
        val viewModel = loyaltyViewModel(bookingRepository = repository)

        viewModel.loadRewards()
        runCurrent()
        viewModel.redeemReward()
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals(1, repository.redemptionCalls)
        assertEquals(0, loaded.progress.currentWashes)
        assertEquals(0, loaded.availableRewards)
        assertEquals(1, loaded.claimedRewards)
        assertEquals("SS-FREE-UID1-0001", loaded.rewardCodes.single().code)
        assertEquals("Emitida agora", loaded.rewardCodes.single().issuedAt)
        val success = assertIs<LoyaltyRedemptionUiState.Success>(loaded.redemptionState)
        assertEquals(true, success.message.contains("SS-FREE-UID1-0001"))
    }

    @Test
    fun redeemRewardSubtractsIssuedCodeWhenBackendReturnsRawAvailableReward() = runTest {
        val repository = FakeLoyaltyBookingRepository(
            historyResult = rewardReadyHistoryResult(),
            redemptionResult = BookingRewardRedemptionResult.Success(
                rewardReceipt().copy(
                    loyalty = loyaltySummary(
                        totalWashes = 10,
                        currentWashes = 10,
                        remainingWashes = 0,
                        progress = 1f,
                        rewardReady = true,
                        completedRewards = 1,
                        claimedRewards = 0,
                        availableRewards = 1,
                    ),
                ),
            ),
        )
        val viewModel = loyaltyViewModel(bookingRepository = repository)

        viewModel.loadRewards()
        runCurrent()
        viewModel.redeemReward()
        runCurrent()
        viewModel.redeemReward()
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals(1, repository.redemptionCalls)
        assertEquals(0, loaded.availableRewards)
        assertEquals("SS-FREE-UID1-0001", loaded.rewardCodes.single().code)
        assertEquals(true, loaded.rewardCodes.single().active)
    }

    @Test
    fun redeemRewardIgnoresDuplicateTapBeforeCoroutineRuns() = runTest {
        val repository = FakeLoyaltyBookingRepository(
            historyResult = rewardReadyHistoryResult(),
            redemptionResult = BookingRewardRedemptionResult.Success(rewardReceipt()),
        )
        val viewModel = loyaltyViewModel(bookingRepository = repository)

        viewModel.loadRewards()
        runCurrent()

        viewModel.redeemReward()
        viewModel.redeemReward()
        runCurrent()

        assertEquals(1, repository.redemptionCalls)
        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        assertEquals("SS-FREE-UID1-0001", loaded.rewardCodes.single().code)
    }

    @Test
    fun redeemRewardMapsBackendFailureToInlineError() = runTest {
        val repository = FakeLoyaltyBookingRepository(
            historyResult = BookingHistoryResult.Success(
                BookingHistory(
                    reservations = (1..10).map {
                        loyaltyReservation(
                            id = "stamp-$it",
                            slotStartIso = "2026-05-${it.twoDigits()}T09:00:00.000Z",
                        )
                    },
                    loyalty = loyaltySummary(
                        totalWashes = 10,
                        currentWashes = 10,
                        remainingWashes = 0,
                        progress = 1f,
                        rewardReady = true,
                        completedRewards = 1,
                        claimedRewards = 0,
                        availableRewards = 1,
                    ),
                ),
            ),
            redemptionResult = BookingRewardRedemptionResult.Failure(
                BookingRewardRedemptionError.Unavailable("Recompensas indisponíveis."),
            ),
        )
        val viewModel = loyaltyViewModel(bookingRepository = repository)

        viewModel.loadRewards()
        runCurrent()
        viewModel.redeemReward()
        runCurrent()

        val loaded = assertIs<LoyaltyUiState.Loaded>(viewModel.uiState.value)
        val error = assertIs<LoyaltyRedemptionUiState.Error>(loaded.redemptionState)
        assertEquals("Recompensas indisponíveis.", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun redeemRewardMapsRestoreFailureWithoutRepositoryCall() = runTest {
        val authRepository = FakeLoyaltyAuthRepository()
        val repository = FakeLoyaltyBookingRepository(
            historyResult = BookingHistoryResult.Success(
                BookingHistory(
                    reservations = (1..10).map {
                        loyaltyReservation(
                            id = "stamp-$it",
                            slotStartIso = "2026-05-${it.twoDigits()}T09:00:00.000Z",
                        )
                    },
                    loyalty = loyaltySummary(
                        totalWashes = 10,
                        currentWashes = 10,
                        remainingWashes = 0,
                        progress = 1f,
                        rewardReady = true,
                        completedRewards = 1,
                        claimedRewards = 0,
                        availableRewards = 1,
                    ),
                ),
            ),
        )
        val viewModel = loyaltyViewModel(
            authRepository = authRepository,
            bookingRepository = repository,
        )

        viewModel.loadRewards()
        runCurrent()
        authRepository.setSessionState(
            AuthSessionState.RestoreFailed(AuthError.Permission("Inicie sessão novamente.")),
        )
        viewModel.redeemReward()
        runCurrent()

        val error = assertIs<LoyaltyUiState.Error>(viewModel.uiState.value)
        assertEquals("Inicie sessão novamente.", error.message)
        assertEquals(false, error.retryable)
        assertEquals(0, repository.redemptionCalls)
    }

    @Test
    fun redeemRewardDoesNotCallRepositoryWhenSessionChangesBeforeCoroutineRuns() = runTest {
        val authRepository = FakeLoyaltyAuthRepository()
        val repository = FakeLoyaltyBookingRepository(
            historyResult = rewardReadyHistoryResult(),
            redemptionResult = BookingRewardRedemptionResult.Success(rewardReceipt()),
        )
        val viewModel = loyaltyViewModel(
            authRepository = authRepository,
            bookingRepository = repository,
        )

        viewModel.loadRewards()
        runCurrent()

        viewModel.redeemReward()
        authRepository.signOut()
        runCurrent()

        assertIs<LoyaltyUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.redemptionCalls)
    }

    @Test
    fun redeemRewardMapsRestoreFailureWhenSessionChangesDuringRedemption() = runTest {
        val authRepository = FakeLoyaltyAuthRepository()
        val redemptionResult = CompletableDeferred<BookingRewardRedemptionResult>()
        val repository = FakeLoyaltyBookingRepository(
            historyResult = rewardReadyHistoryResult(),
            deferredRedemptionResult = redemptionResult,
        )
        val viewModel = loyaltyViewModel(
            authRepository = authRepository,
            bookingRepository = repository,
        )

        viewModel.loadRewards()
        runCurrent()
        viewModel.redeemReward()
        runCurrent()
        assertEquals(1, repository.redemptionCalls)

        authRepository.setSessionState(
            AuthSessionState.RestoreFailed(AuthError.Unavailable("Sessão indisponível.")),
        )
        redemptionResult.complete(BookingRewardRedemptionResult.Success(rewardReceipt()))
        runCurrent()

        val error = assertIs<LoyaltyUiState.Error>(viewModel.uiState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(1, repository.redemptionCalls)
    }
}

private fun loyaltyViewModel(
    authRepository: AuthRepository = FakeLoyaltyAuthRepository(authenticated = true),
    bookingRepository: BookingRepository = FakeLoyaltyBookingRepository(
        BookingHistoryResult.Success(BookingHistory(emptyList())),
    ),
    bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
): LoyaltyViewModel = LoyaltyViewModel(
    authRepository = authRepository,
    bookingRepository = bookingRepository,
    bookingChangeNotifier = bookingChangeNotifier,
)

private class DeferredLoyaltyBookingRepository(
    private val results: List<CompletableDeferred<BookingLoyaltyResult>>,
) : BookingRepository {
    var historyCalls: Int = 0
        private set

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        error("Not used")
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        error("Not used")
    }

    override suspend fun getMyBookings(): BookingHistoryResult {
        error("Not used")
    }

    override suspend fun getMyLoyalty(): BookingLoyaltyResult {
        val result = results.getOrNull(historyCalls) ?: error("No loyalty result for call $historyCalls")
        historyCalls += 1
        return result.await()
    }

    override suspend fun redeemLoyaltyReward(): BookingRewardRedemptionResult {
        error("Not used")
    }
}

private class FakeLoyaltyBookingRepository(
    private val historyResult: BookingHistoryResult,
    private val redemptions: List<BookingLoyaltyRedemption> = emptyList(),
    private val redemptionResult: BookingRewardRedemptionResult = BookingRewardRedemptionResult.Failure(
        BookingRewardRedemptionError.NotAvailable("Ainda não tem uma recompensa disponível."),
    ),
    private val deferredRedemptionResult: CompletableDeferred<BookingRewardRedemptionResult>? = null,
) : BookingRepository {
    var historyCalls: Int = 0
        private set
    var redemptionCalls: Int = 0
        private set

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        error("Not used")
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        error("Not used")
    }

    override suspend fun getMyBookings(): BookingHistoryResult {
        error("Not used")
    }

    override suspend fun getMyLoyalty(): BookingLoyaltyResult {
        historyCalls += 1
        return historyResult.toLoyaltyResult(redemptions)
    }

    override suspend fun redeemLoyaltyReward(): BookingRewardRedemptionResult {
        redemptionCalls += 1
        return deferredRedemptionResult?.await() ?: redemptionResult
    }
}

private class FakeLoyaltyAuthRepository(
    authenticated: Boolean = true,
    initialState: AuthSessionState? = null,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        initialState ?: if (authenticated) authenticatedSession() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        mutableSessionState.value = authenticatedSession()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        mutableSessionState.value = authenticatedSession()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        return AuthActionResult.Success
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }

    fun setSessionState(state: AuthSessionState) {
        mutableSessionState.value = state
    }
}

private fun authenticatedSession(uid: String = "uid-1"): AuthSessionState.Authenticated {
    return AuthSessionState.Authenticated(
        AuthSession(
            user = AuthUser(
                uid = uid,
                email = "bruno@example.com",
                displayName = "Bruno",
                phoneNumber = "",
            ),
            idToken = "id-token-$uid",
            refreshToken = "refresh-token-$uid",
            expiresInSeconds = 3600,
        ),
    )
}

private fun BookingHistoryResult.toLoyaltyResult(
    redemptions: List<BookingLoyaltyRedemption>,
): BookingLoyaltyResult {
    return when (this) {
        is BookingHistoryResult.Success -> BookingLoyaltyResult.Success(history.toLoyalty(redemptions))
        is BookingHistoryResult.Failure -> BookingLoyaltyResult.Failure(error.toLoyaltyError())
    }
}

private fun BookingHistory.toLoyalty(redemptions: List<BookingLoyaltyRedemption>): BookingLoyalty {
    val stamps = reservations
        .filter { !it.upcoming && !it.isCancelled() }
        .map {
            BookingLoyaltyStamp(
                id = it.id,
                serviceId = it.serviceId,
                serviceName = it.serviceName,
                slotStartIso = it.slotStartIso,
                slotEndIso = it.slotEndIso,
                points = 1,
            )
        }
    val summary = loyalty ?: stamps.size.toTestLoyaltySummary()
    return BookingLoyalty(
        summary = summary,
        stampHistory = stamps,
        redemptions = redemptions,
    )
}

private fun BookingHistoryReservation.isCancelled(): Boolean {
    val normalized = status.lowercase()
    return normalized in setOf("cancelled", "canceled", "cancelado")
}

private fun rewardReadyHistoryResult(): BookingHistoryResult = BookingHistoryResult.Success(
    BookingHistory(
        reservations = (1..10).map {
            loyaltyReservation(
                id = "stamp-$it",
                slotStartIso = "2026-05-${it.twoDigits()}T09:00:00.000Z",
            )
        },
        loyalty = loyaltySummary(
            totalWashes = 10,
            currentWashes = 10,
            remainingWashes = 0,
            progress = 1f,
            rewardReady = true,
            completedRewards = 1,
            claimedRewards = 0,
            availableRewards = 1,
        ),
    ),
)

private fun rewardReceipt(): BookingRewardRedemptionReceipt = BookingRewardRedemptionReceipt(
    redemptionId = "reward-0001",
    rewardCode = "SS-FREE-UID1-0001",
    rewardNumber = 1,
    status = "issued",
    loyalty = loyaltySummary(
        totalWashes = 10,
        currentWashes = 0,
        remainingWashes = 10,
        progress = 0f,
        rewardReady = false,
        completedRewards = 1,
        claimedRewards = 1,
        availableRewards = 0,
    ),
)

private fun loyaltyResultForStamp(id: String, slotStartIso: String): BookingLoyaltyResult {
    return BookingLoyaltyResult.Success(
        BookingHistory(
            reservations = listOf(
                loyaltyReservation(
                    id = id,
                    slotStartIso = slotStartIso,
                ),
            ),
        ).toLoyalty(emptyList()),
    )
}

private fun BookingHistoryError.toLoyaltyError(): BookingLoyaltyError {
    return when (this) {
        is BookingHistoryError.Unauthenticated -> BookingLoyaltyError.Unauthenticated(message)
        is BookingHistoryError.Permission -> BookingLoyaltyError.Permission(message)
        is BookingHistoryError.Unavailable -> BookingLoyaltyError.Unavailable(message)
        is BookingHistoryError.Backend -> BookingLoyaltyError.Backend(message)
    }
}

private fun Int.toTestLoyaltySummary(): BookingLoyaltySummary {
    val progress = toLoyaltyProgress()
    return BookingLoyaltySummary(
        totalWashes = progress.totalWashes,
        currentWashes = progress.currentWashes,
        targetWashes = progress.targetWashes,
        remainingWashes = progress.remainingWashes,
        progress = progress.progress,
        rewardReady = progress.rewardReady,
        completedRewards = progress.completedRewards,
        claimedRewards = progress.claimedRewards,
        availableRewards = progress.availableRewards,
    )
}

private fun loyaltyReservation(
    id: String,
    slotStartIso: String,
    serviceName: String = "Lavagem Standard",
    status: String = "completed",
    upcoming: Boolean = false,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "standard",
    serviceName = serviceName,
    slotStartIso = slotStartIso,
    slotEndIso = slotStartIso.replace(":00.000Z", ":30.000Z"),
    status = status,
    vehicleType = "passageiros",
    vehicleLabel = "BMW 320d",
    priceCents = 2500,
    upcoming = upcoming,
)

private fun loyaltySummary(
    totalWashes: Int,
    currentWashes: Int,
    remainingWashes: Int,
    progress: Float,
    rewardReady: Boolean,
    completedRewards: Int,
    claimedRewards: Int,
    availableRewards: Int,
): BookingLoyaltySummary = BookingLoyaltySummary(
    totalWashes = totalWashes,
    currentWashes = currentWashes,
    targetWashes = 10,
    remainingWashes = remainingWashes,
    progress = progress,
    rewardReady = rewardReady,
    completedRewards = completedRewards,
    claimedRewards = claimedRewards,
    availableRewards = availableRewards,
)

private fun Int.twoDigits(): String = toString().padStart(2, '0')
