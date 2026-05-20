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
class ProfileHistoryViewModelTest {
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
    fun loadHistoryRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = repository,
            authRepository = FakeProfileHistoryAuthRepository(authenticated = false),
        )

        viewModel.loadHistory()
        runCurrent()

        assertIs<ProfileHistoryUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadHistoryBuildsCompletedSummaryFromUserReservations() = runTest {
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "completed-1",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                                vehicleLabel = "BMW 320d",
                            ),
                            historyReservation(
                                id = "completed-2",
                                slotStartIso = "2026-05-12T11:30:00.000Z",
                                upcoming = false,
                                priceCents = 2500,
                            ),
                            historyReservation(
                                id = "upcoming-1",
                                slotStartIso = "2026-05-22T09:00:00.000Z",
                                upcoming = true,
                                priceCents = 3200,
                            ),
                            historyReservation(
                                id = "cancelled-1",
                                slotStartIso = "2026-05-08T09:00:00.000Z",
                                upcoming = false,
                                status = "cancelled",
                                priceCents = 3200,
                            ),
                        ),
                    ),
                ),
            ),
            authRepository = FakeProfileHistoryAuthRepository(authenticated = true),
        )

        viewModel.loadHistory()
        runCurrent()

        val loaded = assertIs<ProfileHistoryUiState.Loaded>(viewModel.uiState.value)
        assertEquals("2", loaded.summary.washCount)
        assertEquals("57,00€", loaded.summary.totalSpent)
        assertEquals(listOf("completed-1", "completed-2"), loaded.items.map { it.id })
        assertEquals("18 de maio, 2026", loaded.items.first().date)
        assertEquals("BMW 320d", loaded.items.first().vehicle)
    }

    @Test
    fun loadHistoryMapsBackendErrorAsRetryable() = runTest {
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Failure(
                    BookingHistoryError.Unavailable("Serviço indisponível."),
                ),
            ),
            authRepository = FakeProfileHistoryAuthRepository(authenticated = true),
        )

        viewModel.loadHistory()
        runCurrent()

        val error = assertIs<ProfileHistoryUiState.Error>(viewModel.uiState.value)
        assertEquals("Serviço indisponível.", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun refreshForSessionReloadsAfterSignInAndClearsAfterSignOut() = runTest {
        val authRepository = FakeProfileHistoryAuthRepository(authenticated = false)
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(
                BookingHistory(
                    reservations = listOf(
                        historyReservation(
                            id = "completed-1",
                            slotStartIso = "2026-05-18T10:00:00.000Z",
                            upcoming = false,
                            priceCents = 3200,
                        ),
                    ),
                ),
            ),
        )
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileHistoryUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileHistoryUiState.Loaded>(viewModel.uiState.value)
        assertEquals(1, repository.historyCalls)

        authRepository.signOut()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileHistoryUiState.Unauthenticated>(viewModel.uiState.value)
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

private class FakeProfileHistoryAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) authenticatedSession() else AuthSessionState.Unauthenticated,
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

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
    upcoming: Boolean,
    status: String = if (upcoming) "pending" else "completed",
    priceCents: Int?,
    vehicleLabel: String? = null,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = slotStartIso,
    status = status,
    vehicleType = "suv",
    vehicleLabel = vehicleLabel,
    priceCents = priceCents,
    upcoming = upcoming,
)
