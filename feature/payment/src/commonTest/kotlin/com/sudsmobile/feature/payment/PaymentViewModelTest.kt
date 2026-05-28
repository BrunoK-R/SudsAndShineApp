package com.sudsmobile.feature.payment

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
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.DefaultBusinessInfo
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
class PaymentViewModelTest {
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
    fun loadPaymentsRequiresAuthenticatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakePaymentBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = PaymentViewModel(
            bookingRepository = repository,
            authRepository = FakePaymentAuthRepository(authenticated = false),
            businessInfoRepository = FakePaymentBusinessInfoRepository(),
        )

        viewModel.loadPayments()
        runCurrent()

        assertIs<PaymentUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadPaymentsWaitsWhileSessionIsRestoring() = runTest {
        val repository = FakePaymentBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = PaymentViewModel(
            bookingRepository = repository,
            authRepository = FakePaymentAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
            businessInfoRepository = FakePaymentBusinessInfoRepository(),
        )

        viewModel.loadPayments()
        runCurrent()

        assertIs<PaymentUiState.Loading>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadPaymentsMapsRestoreFailureWithoutRepositoryCall() = runTest {
        val repository = FakePaymentBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = PaymentViewModel(
            bookingRepository = repository,
            authRepository = FakePaymentAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.RestoreFailed(AuthError.Unavailable("Sessão indisponível.")),
            ),
            businessInfoRepository = FakePaymentBusinessInfoRepository(),
        )

        viewModel.loadPayments()
        runCurrent()

        val error = assertIs<PaymentUiState.Error>(viewModel.uiState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun loadPaymentsListsOnlyUpcomingPayableReservations() = runTest {
        val viewModel = PaymentViewModel(
            bookingRepository = FakePaymentBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "payable-1",
                                reservationCode = "SS-ABCDEFGH",
                                slotStartIso = "2026-05-22T10:00:00.000Z",
                                paymentStatus = "pending",
                                priceCents = 3400,
                                upcoming = true,
                            ),
                            historyReservation(
                                id = "paid-1",
                                paymentStatus = "paid",
                                priceCents = 3200,
                                upcoming = true,
                            ),
                            historyReservation(
                                id = "past-1",
                                paymentStatus = "pending",
                                priceCents = 2500,
                                upcoming = false,
                            ),
                        ),
                    ),
                ),
            ),
            authRepository = FakePaymentAuthRepository(authenticated = true),
            businessInfoRepository = FakePaymentBusinessInfoRepository(),
        )

        viewModel.loadPayments()
        runCurrent()

        val loaded = assertIs<PaymentUiState.Loaded>(viewModel.uiState.value)
        assertEquals("34,00€", loaded.totalDue)
        assertEquals("payable-1", loaded.bookings.single().id)
        assertEquals("SS-ABCDEFGH", loaded.bookings.single().reference)
        assertEquals("22 de maio, 2026", loaded.bookings.single().date)
        assertEquals("10:00", loaded.bookings.single().time)
        assertEquals("Pendente", loaded.bookings.single().statusLabel)
    }

    @Test
    fun loadPaymentsMapsAuthenticatedEmptyState() = runTest {
        val viewModel = PaymentViewModel(
            bookingRepository = FakePaymentBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(paymentStatus = "covered_by_loyalty", priceCents = 0),
                        ),
                    ),
                ),
            ),
            authRepository = FakePaymentAuthRepository(authenticated = true),
            businessInfoRepository = FakePaymentBusinessInfoRepository(),
        )

        viewModel.loadPayments()
        runCurrent()

        assertIs<PaymentUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun loadPaymentsMapsRetryableBackendError() = runTest {
        val viewModel = PaymentViewModel(
            bookingRepository = FakePaymentBookingRepository(
                BookingHistoryResult.Failure(BookingHistoryError.Unavailable("offline")),
            ),
            authRepository = FakePaymentAuthRepository(authenticated = true),
            businessInfoRepository = FakePaymentBusinessInfoRepository(),
        )

        viewModel.loadPayments()
        runCurrent()

        val error = assertIs<PaymentUiState.Error>(viewModel.uiState.value)
        assertEquals("offline", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun loadPaymentsKeepsRestoringStateWhenSessionChangesDuringHistoryLoad() = runTest {
        val repository = DeferredPaymentBookingRepository()
        val authRepository = FakePaymentAuthRepository(authenticated = true)
        val viewModel = PaymentViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
            businessInfoRepository = FakePaymentBusinessInfoRepository(),
        )

        viewModel.loadPayments()
        runCurrent()
        authRepository.setSessionState(AuthSessionState.Restoring)
        repository.result.complete(
            BookingHistoryResult.Success(BookingHistory(listOf(historyReservation()))),
        )
        runCurrent()

        assertIs<PaymentUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, repository.historyCalls)
    }

    @Test
    fun loadPaymentsMapsRestoreFailureWhenSessionChangesDuringHistoryLoad() = runTest {
        val repository = DeferredPaymentBookingRepository()
        val authRepository = FakePaymentAuthRepository(authenticated = true)
        val viewModel = PaymentViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
            businessInfoRepository = FakePaymentBusinessInfoRepository(),
        )

        viewModel.loadPayments()
        runCurrent()
        authRepository.setSessionState(
            AuthSessionState.RestoreFailed(AuthError.Backend("Falha ao validar sessão.")),
        )
        repository.result.complete(
            BookingHistoryResult.Success(BookingHistory(listOf(historyReservation()))),
        )
        runCurrent()

        val error = assertIs<PaymentUiState.Error>(viewModel.uiState.value)
        assertEquals("Falha ao validar sessão.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(1, repository.historyCalls)
    }

    @Test
    fun loadBusinessInfoKeepsFallbackForBackendFailure() = runTest {
        val viewModel = PaymentViewModel(
            bookingRepository = FakePaymentBookingRepository(BookingHistoryResult.Success(BookingHistory(emptyList()))),
            authRepository = FakePaymentAuthRepository(authenticated = true),
            businessInfoRepository = FakePaymentBusinessInfoRepository(
                BusinessInfoResult.Failure(com.sudsmobile.data.business.BusinessInfoError.Unavailable("business down")),
            ),
        )

        viewModel.loadBusinessInfo()
        runCurrent()

        val error = assertIs<PaymentBusinessInfoUiState.Error>(viewModel.businessInfoState.value)
        assertEquals(DefaultBusinessInfo.phone, error.fallbackInfo.phone)
        assertEquals(true, error.retryable)
    }
}

private class DeferredPaymentBookingRepository : BookingRepository {
    val result = CompletableDeferred<BookingHistoryResult>()
    var historyCalls = 0
        private set

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        error("Not used")
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        error("Not used")
    }

    override suspend fun getMyBookings(): BookingHistoryResult {
        historyCalls += 1
        return result.await()
    }
}

private class FakePaymentBookingRepository(
    private val historyResult: BookingHistoryResult,
) : BookingRepository {
    var historyCalls = 0

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

private class FakePaymentAuthRepository(
    authenticated: Boolean,
    initialState: AuthSessionState? = null,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        initialState ?: if (authenticated) {
            AuthSessionState.Authenticated(session())
        } else {
            AuthSessionState.Unauthenticated
        },
    )
    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    fun setSessionState(nextState: AuthSessionState) {
        mutableSessionState.value = nextState
    }

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
    }

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

    override fun signOut() = Unit
}

private class FakePaymentBusinessInfoRepository(
    private val result: BusinessInfoResult = BusinessInfoResult.Success(DefaultBusinessInfo),
) : BusinessInfoRepository {
    override suspend fun getBusinessInfo(): BusinessInfoResult = result
}

private fun session(): AuthSession = AuthSession(
    user = AuthUser(
        uid = "uid-1",
        email = "user@example.com",
        displayName = "User",
        phoneNumber = "913005855",
    ),
    idToken = "id-token-1",
    refreshToken = "refresh-token-1",
    expiresInSeconds = 3600,
)

private fun historyReservation(
    id: String = "reservation-1",
    reservationCode: String = "SS-ABCDEFGH",
    slotStartIso: String = "2026-05-22T10:00:00.000Z",
    slotEndIso: String = "2026-05-22T10:45:00.000Z",
    paymentStatus: String = "pending",
    priceCents: Int? = 3200,
    upcoming: Boolean = true,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = reservationCode,
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = slotEndIso,
    status = "pending",
    paymentStatus = paymentStatus,
    vehicleType = "suv",
    vehicleLabel = "BMW 320d",
    priceCents = priceCents,
    upcoming = upcoming,
)
