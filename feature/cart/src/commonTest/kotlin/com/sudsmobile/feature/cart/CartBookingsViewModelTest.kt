package com.sudsmobile.feature.cart

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
class CartBookingsViewModelTest {
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
    fun loadBookingsRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = false),
        )

        viewModel.loadBookings()
        runCurrent()

        assertIs<CartBookingsUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadBookingsSplitsUpcomingAndCompletedReservations() = runTest {
        val viewModel = CartBookingsViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "upcoming-1",
                                slotStartIso = "2026-05-21T10:00:00.000Z",
                                slotEndIso = "2026-05-21T10:45:00.000Z",
                                upcoming = true,
                                priceCents = 3400,
                                vehicleLabel = "BMW 320d",
                            ),
                            historyReservation(
                                id = "completed-1",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                slotEndIso = "2026-05-18T10:45:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                            ),
                        ),
                    ),
                ),
            ),
            authRepository = FakeCartAuthRepository(authenticated = true),
        )

        viewModel.loadBookings()
        runCurrent()

        val loaded = assertIs<CartBookingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals("upcoming-1", loaded.upcoming.single().id)
        assertEquals("21 de maio, 2026", loaded.upcoming.single().date)
        assertEquals("BMW 320d", loaded.upcoming.single().vehicle)
        assertEquals("34,00€", loaded.upcoming.single().price)
        assertEquals("completed-1", loaded.completed.single().id)
    }

    @Test
    fun loadBookingsCarriesReviewedStateForCompletedReservations() = runTest {
        val viewModel = CartBookingsViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "completed-1",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                slotEndIso = "2026-05-18T10:45:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                                reviewed = true,
                                reviewRating = 5,
                            ),
                        ),
                    ),
                ),
            ),
            authRepository = FakeCartAuthRepository(authenticated = true),
        )

        viewModel.loadBookings()
        runCurrent()

        val loaded = assertIs<CartBookingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals(true, loaded.completed.single().reviewed)
        assertEquals(5, loaded.completed.single().reviewRating)
    }

    @Test
    fun refreshForSessionReloadsAfterSignInAndClearsAfterSignOut() = runTest {
        val authRepository = FakeCartAuthRepository(authenticated = false)
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(
                BookingHistory(
                    reservations = listOf(
                        historyReservation(
                            id = "upcoming-1",
                            slotStartIso = "2026-05-21T10:00:00.000Z",
                            slotEndIso = "2026-05-21T10:45:00.000Z",
                            upcoming = true,
                            priceCents = 3400,
                        ),
                    ),
                ),
            ),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<CartBookingsUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshForSession()
        runCurrent()

        assertIs<CartBookingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals(1, repository.historyCalls)

        authRepository.signOut()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<CartBookingsUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun refreshForSessionReloadsWhenBookingRevisionChanges() = runTest {
        val bookingChangeNotifier = MutableBookingChangeNotifier()
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            bookingChangeNotifier = bookingChangeNotifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<CartBookingsUiState.Empty>(viewModel.uiState.value)
        assertEquals(1, repository.historyCalls)

        viewModel.refreshForSession()
        runCurrent()

        assertEquals(1, repository.historyCalls)

        bookingChangeNotifier.notifyBookingsChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<CartBookingsUiState.Empty>(viewModel.uiState.value)
        assertEquals(2, repository.historyCalls)
    }
}

private class FakeBookingRepository(
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

private class FakeCartAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) authenticatedSession() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

    fun authenticate(uid: String = "uid-1") {
        mutableSessionState.value = authenticatedSession(uid)
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        authenticate()
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
        authenticate()
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

private fun historyReservation(
    id: String,
    slotStartIso: String,
    slotEndIso: String,
    upcoming: Boolean,
    priceCents: Int?,
    vehicleLabel: String? = null,
    reviewed: Boolean = false,
    reviewRating: Int? = null,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = slotEndIso,
    status = if (upcoming) "pending" else "completed",
    vehicleType = "suv",
    vehicleLabel = vehicleLabel,
    priceCents = priceCents,
    upcoming = upcoming,
    reviewed = reviewed,
    reviewRating = reviewRating,
)
