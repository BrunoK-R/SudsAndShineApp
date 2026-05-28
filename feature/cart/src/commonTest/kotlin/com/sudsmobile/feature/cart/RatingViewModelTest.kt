package com.sudsmobile.feature.cart

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
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.BookingReviewError
import com.sudsmobile.data.booking.BookingReviewReceipt
import com.sudsmobile.data.booking.BookingReviewRequest
import com.sudsmobile.data.booking.BookingReviewResult
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
class RatingViewModelTest {
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
    fun refreshTargetRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeRatingBookingRepository(historyResult = completedHistory())
        val viewModel = RatingViewModel(
            bookingRepository = repository,
            authRepository = FakeRatingAuthRepository(authenticated = false),
        )

        viewModel.refreshTarget("reservation-1")
        runCurrent()

        assertIs<RatingTargetUiState.Unauthenticated>(viewModel.targetState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun refreshTargetWaitsWhileSessionIsRestoring() = runTest {
        val repository = FakeRatingBookingRepository(historyResult = completedHistory())
        val viewModel = RatingViewModel(
            bookingRepository = repository,
            authRepository = FakeRatingAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
        )

        viewModel.refreshTarget("reservation-1")
        runCurrent()

        assertIs<RatingTargetUiState.Loading>(viewModel.targetState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun refreshTargetMapsRestoreFailureWithoutRepositoryCall() = runTest {
        val repository = FakeRatingBookingRepository(historyResult = completedHistory())
        val viewModel = RatingViewModel(
            bookingRepository = repository,
            authRepository = FakeRatingAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.RestoreFailed(AuthError.Unavailable("Sessão indisponível.")),
            ),
        )

        viewModel.refreshTarget("reservation-1")
        runCurrent()

        val error = assertIs<RatingTargetUiState.Error>(viewModel.targetState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadTargetMapsCompletedReservation() = runTest {
        val viewModel = RatingViewModel(
            bookingRepository = FakeRatingBookingRepository(historyResult = completedHistory()),
            authRepository = FakeRatingAuthRepository(authenticated = true),
        )

        viewModel.loadTarget("reservation-1")
        runCurrent()

        val loaded = assertIs<RatingTargetUiState.Loaded>(viewModel.targetState.value)
        assertEquals("Lavagem Premium", loaded.target.service)
        assertEquals("18 de maio, 2026", loaded.target.date)
        assertEquals("BMW 320d", loaded.target.vehicle)
    }

    @Test
    fun loadTargetRejectsUpcomingReservation() = runTest {
        val viewModel = RatingViewModel(
            bookingRepository = FakeRatingBookingRepository(
                historyResult = BookingHistoryResult.Success(
                    BookingHistory(
                        listOf(historyReservation(id = "reservation-1", upcoming = true)),
                    ),
                ),
            ),
            authRepository = FakeRatingAuthRepository(authenticated = true),
        )

        viewModel.loadTarget("reservation-1")
        runCurrent()

        assertIs<RatingTargetUiState.NotFound>(viewModel.targetState.value)
    }

    @Test
    fun submitReviewValidatesRatingBeforeRepositoryCall() = runTest {
        val repository = FakeRatingBookingRepository(historyResult = completedHistory())
        val viewModel = RatingViewModel(
            bookingRepository = repository,
            authRepository = FakeRatingAuthRepository(authenticated = true),
        )

        viewModel.submitReview("reservation-1", rating = 0, tags = emptyList(), comment = "")
        runCurrent()

        assertIs<RatingSubmitUiState.ValidationError>(viewModel.submitState.value)
        assertEquals(0, repository.reviewCalls)
    }

    @Test
    fun submitReviewSendsSelectedTagsAndComment() = runTest {
        val repository = FakeRatingBookingRepository(
            historyResult = completedHistory(),
            reviewResult = BookingReviewResult.Success(
                BookingReviewReceipt(reviewId = "review-1", reservationId = "reservation-1"),
            ),
        )
        val viewModel = RatingViewModel(
            bookingRepository = repository,
            authRepository = FakeRatingAuthRepository(authenticated = true),
        )

        viewModel.submitReview(
            reservationId = "reservation-1",
            rating = 5,
            tags = listOf("Qualidade", "Rápido"),
            comment = "Ficou impecável.",
        )
        runCurrent()

        assertIs<RatingSubmitUiState.Success>(viewModel.submitState.value)
        assertEquals(1, repository.reviewCalls)
        assertEquals(listOf("Qualidade", "Rápido"), repository.lastReviewRequest?.tags)
        assertEquals("Ficou impecável.", repository.lastReviewRequest?.comment)
    }

    @Test
    fun submitReviewMapsBackendFailureToRetryableError() = runTest {
        val viewModel = RatingViewModel(
            bookingRepository = FakeRatingBookingRepository(
                historyResult = completedHistory(),
                reviewResult = BookingReviewResult.Failure(
                    BookingReviewError.Unavailable("Não foi possível enviar a avaliação."),
                ),
            ),
            authRepository = FakeRatingAuthRepository(authenticated = true),
        )

        viewModel.submitReview("reservation-1", rating = 4, tags = emptyList(), comment = "")
        runCurrent()

        val error = assertIs<RatingSubmitUiState.Error>(viewModel.submitState.value)
        assertEquals(true, error.retryable)
        assertEquals(false, error.requiresSignIn)
    }
}

private class FakeRatingBookingRepository(
    private val historyResult: BookingHistoryResult,
    private val reviewResult: BookingReviewResult = BookingReviewResult.Success(
        BookingReviewReceipt(reviewId = "review-1", reservationId = "reservation-1"),
    ),
) : BookingRepository {
    var historyCalls: Int = 0
        private set
    var reviewCalls: Int = 0
        private set
    var lastReviewRequest: BookingReviewRequest? = null
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

    override suspend fun submitReview(request: BookingReviewRequest): BookingReviewResult {
        reviewCalls += 1
        lastReviewRequest = request
        return reviewResult
    }
}

private class FakeRatingAuthRepository(
    authenticated: Boolean,
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
        return AuthResult.Success((mutableSessionState.value as AuthSessionState.Authenticated).session)
    }

    override suspend fun register(
        displayName: String,
        email: String,
        phoneNumber: String,
        password: String,
    ): AuthResult {
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

private fun completedHistory(): BookingHistoryResult {
    return BookingHistoryResult.Success(
        BookingHistory(
            listOf(historyReservation(id = "reservation-1", upcoming = false)),
        ),
    )
}

private fun historyReservation(
    id: String,
    upcoming: Boolean,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = "2026-05-18T10:00:00.000Z",
    slotEndIso = "2026-05-18T10:45:00.000Z",
    status = if (upcoming) "pending" else "completed",
    vehicleType = "suv",
    vehicleLabel = "BMW 320d",
    priceCents = 3400,
    upcoming = upcoming,
)
