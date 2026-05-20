package com.sudsmobile.feature.blog

import com.sudsmobile.data.auth.AuthActionResult
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
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
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
}

private fun loyaltyViewModel(
    authRepository: AuthRepository = FakeLoyaltyAuthRepository(authenticated = true),
    bookingRepository: FakeLoyaltyBookingRepository = FakeLoyaltyBookingRepository(
        BookingHistoryResult.Success(BookingHistory(emptyList())),
    ),
    bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
): LoyaltyViewModel = LoyaltyViewModel(
    authRepository = authRepository,
    bookingRepository = bookingRepository,
    bookingChangeNotifier = bookingChangeNotifier,
)

private class FakeLoyaltyBookingRepository(
    private val historyResult: BookingHistoryResult,
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
        historyCalls += 1
        return historyResult
    }
}

private class FakeLoyaltyAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) authenticatedSession() else AuthSessionState.Unauthenticated,
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

private fun Int.twoDigits(): String = toString().padStart(2, '0')
