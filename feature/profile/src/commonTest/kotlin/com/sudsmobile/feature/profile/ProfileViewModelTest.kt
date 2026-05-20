package com.sudsmobile.feature.profile

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
class ProfileViewModelTest {
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
    fun loadStatsRequiresAuthenticatedSession() = runTest {
        val bookingRepository = ProfileStatsFakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = false),
            bookingRepository = bookingRepository,
        )

        viewModel.loadStats()
        runCurrent()

        assertIs<ProfileStatsUiState.Unauthenticated>(viewModel.statsState.value)
        assertEquals(0, bookingRepository.historyCalls)
    }

    @Test
    fun loadStatsBuildsWashAndLoyaltyCountsFromCompletedReservations() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            profileStatsHistoryReservation("completed-1", upcoming = false),
                            profileStatsHistoryReservation("completed-2", upcoming = false),
                            profileStatsHistoryReservation("completed-3", upcoming = false),
                            profileStatsHistoryReservation("completed-4", upcoming = false),
                            profileStatsHistoryReservation("completed-5", upcoming = false),
                            profileStatsHistoryReservation("completed-6", upcoming = false),
                            profileStatsHistoryReservation("completed-7", upcoming = false),
                            profileStatsHistoryReservation("upcoming-1", upcoming = true),
                            profileStatsHistoryReservation("cancelled-1", upcoming = false, status = "cancelled"),
                        ),
                    ),
                ),
            ),
        )

        viewModel.loadStats()
        runCurrent()

        val loaded = assertIs<ProfileStatsUiState.Loaded>(viewModel.statsState.value)
        assertEquals("7", loaded.stats.washCount)
        assertEquals("3", loaded.stats.loyaltyRemaining)
        assertEquals("0", loaded.stats.vehicleCount)
    }

    @Test
    fun loadStatsMapsBackendErrorAsRetryable() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Failure(
                    BookingHistoryError.Unavailable("Serviço indisponível."),
                ),
            ),
        )

        viewModel.loadStats()
        runCurrent()

        val error = assertIs<ProfileStatsUiState.Error>(viewModel.statsState.value)
        assertEquals("Serviço indisponível.", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun signOutWhileStatsLoadIsInFlightDoesNotPublishOldStats() = runTest {
        val authRepository = ProfileStatsFakeAuthRepository(authenticated = true)
        val bookingRepository = DeferredProfileStatsBookingRepository()
        val viewModel = ProfileViewModel(
            authRepository = authRepository,
            bookingRepository = bookingRepository,
        )

        viewModel.loadStats()
        runCurrent()

        assertIs<ProfileStatsUiState.Loading>(viewModel.statsState.value)

        viewModel.signOut()
        bookingRepository.result.complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    listOf(profileStatsHistoryReservation("completed-1", upcoming = false)),
                ),
            ),
        )
        runCurrent()

        assertIs<ProfileStatsUiState.Unauthenticated>(viewModel.statsState.value)
    }
}

private class ProfileStatsFakeBookingRepository(
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

private class DeferredProfileStatsBookingRepository : BookingRepository {
    val result = CompletableDeferred<BookingHistoryResult>()

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        error("Not used")
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        error("Not used")
    }

    override suspend fun getMyBookings(): BookingHistoryResult = result.await()
}

private class ProfileStatsFakeAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) {
            AuthSessionState.Authenticated(
                AuthSession(
                    user = AuthUser(
                        uid = "uid-1",
                        email = "bruno@example.com",
                        displayName = "Bruno Ribeiro",
                        phoneNumber = "",
                    ),
                    idToken = "id-token-1",
                    refreshToken = "refresh-token-1",
                    expiresInSeconds = 3600,
                ),
            )
        } else {
            AuthSessionState.Unauthenticated
        },
    )

    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun signIn(email: String, password: String): AuthResult {
        error("Not used")
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        error("Not used")
    }

    override suspend fun sendPasswordReset(email: String): AuthActionResult {
        error("Not used")
    }

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private fun profileStatsHistoryReservation(
    id: String,
    upcoming: Boolean,
    status: String = if (upcoming) "pending" else "completed",
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = "2026-05-20T09:30:00.000Z",
    slotEndIso = "2026-05-20T10:15:00.000Z",
    status = status,
    vehicleType = "passageiros",
    priceCents = 3200,
    upcoming = upcoming,
)
