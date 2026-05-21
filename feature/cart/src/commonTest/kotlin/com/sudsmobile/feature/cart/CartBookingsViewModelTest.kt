package com.sudsmobile.feature.cart

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.business.BusinessFaq
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.BusinessOpeningHours
import com.sudsmobile.data.business.BusinessStat
import com.sudsmobile.data.business.DefaultBusinessInfo
import com.sudsmobile.data.booking.BookingAvailabilityRequest
import com.sudsmobile.data.booking.BookingAvailabilityResult
import com.sudsmobile.data.booking.BookingCancelError
import com.sudsmobile.data.booking.BookingCancelReceipt
import com.sudsmobile.data.booking.BookingCancelRequest
import com.sudsmobile.data.booking.BookingCancelResult
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
            businessInfoRepository = FakeBusinessInfoRepository(),
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
            businessInfoRepository = FakeBusinessInfoRepository(),
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
            businessInfoRepository = FakeBusinessInfoRepository(),
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
            businessInfoRepository = FakeBusinessInfoRepository(),
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
            businessInfoRepository = FakeBusinessInfoRepository(),
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

    @Test
    fun cancelBookingPublishesSuccessState() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
            cancelResult = BookingCancelResult.Success(
                BookingCancelReceipt(
                    reservationId = "reservation-1",
                    status = "cancelled",
                ),
            ),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.cancelBooking("reservation-1")
        runCurrent()

        val success = assertIs<BookingCancellationUiState.Success>(viewModel.cancellationState.value)
        assertEquals("reservation-1", success.reservationId)
        assertEquals(1, repository.cancelCalls)
        assertEquals("reservation-1", repository.lastCancelRequest?.reservationId)
    }

    @Test
    fun cancelBookingMapsBackendError() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
            cancelResult = BookingCancelResult.Failure(
                BookingCancelError.NotCancelable("Reservation can no longer be cancelled"),
            ),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.cancelBooking("reservation-1")
        runCurrent()

        val error = assertIs<BookingCancellationUiState.Error>(viewModel.cancellationState.value)
        assertEquals("reservation-1", error.reservationId)
        assertEquals("Reservation can no longer be cancelled", error.message)
        assertEquals(false, error.retryable)
    }

    @Test
    fun loadBusinessInfoMapsBackendAddressAndSkipsCachedReload() = runTest {
        val businessRepository = FakeBusinessInfoRepository(
            BusinessInfoResult.Success(
                businessInfo(
                    addressLine1 = "Rua das Lavagens 24",
                    addressLine2 = "Leiria",
                ),
            ),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = FakeBookingRepository(BookingHistoryResult.Success(BookingHistory(emptyList()))),
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = businessRepository,
        )

        viewModel.loadBusinessInfo()
        runCurrent()

        val loaded = assertIs<CartBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("Rua das Lavagens 24", loaded.info.addressLine1)
        assertEquals("Leiria", loaded.info.addressLine2)

        viewModel.loadBusinessInfo()
        runCurrent()

        assertEquals(1, businessRepository.calls)
    }

    @Test
    fun loadBusinessInfoMapsRetryableFailureWithFallbackAddress() = runTest {
        val viewModel = CartBookingsViewModel(
            bookingRepository = FakeBookingRepository(BookingHistoryResult.Success(BookingHistory(emptyList()))),
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(
                BusinessInfoResult.Failure(BusinessInfoError.Unavailable("Contactos indisponíveis")),
            ),
        )

        viewModel.loadBusinessInfo()
        runCurrent()

        val error = assertIs<CartBusinessInfoUiState.Error>(viewModel.businessInfoState.value)
        assertEquals("Contactos indisponíveis", error.message)
        assertEquals(true, error.retryable)
        assertEquals(DefaultBusinessInfo.addressLine1, error.fallbackInfo.addressLine1)
        assertEquals(DefaultBusinessInfo.addressLine2, error.fallbackInfo.addressLine2)
    }
}

private class FakeBookingRepository(
    private val historyResult: BookingHistoryResult,
    private val cancelResult: BookingCancelResult = BookingCancelResult.Success(
        BookingCancelReceipt(
            reservationId = "reservation-1",
            status = "cancelled",
        ),
    ),
) : BookingRepository {
    var historyCalls: Int = 0
        private set
    var cancelCalls: Int = 0
        private set
    var lastCancelRequest: BookingCancelRequest? = null
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

    override suspend fun cancelBooking(request: BookingCancelRequest): BookingCancelResult {
        cancelCalls += 1
        lastCancelRequest = request
        return cancelResult
    }
}

private class FakeBusinessInfoRepository(
    private var result: BusinessInfoResult = BusinessInfoResult.Success(businessInfo()),
) : BusinessInfoRepository {
    var calls: Int = 0
        private set

    override suspend fun getBusinessInfo(): BusinessInfoResult {
        calls += 1
        return result
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

private fun businessInfo(
    addressLine1: String = DefaultBusinessInfo.addressLine1,
    addressLine2: String = DefaultBusinessInfo.addressLine2,
): BusinessInfo = BusinessInfo(
    phone = DefaultBusinessInfo.phone,
    phoneUri = DefaultBusinessInfo.phoneUri,
    email = DefaultBusinessInfo.email,
    emailUri = DefaultBusinessInfo.emailUri,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    mapsUri = DefaultBusinessInfo.mapsUri,
    whatsappUri = DefaultBusinessInfo.whatsappUri,
    openingHours = listOf(
        BusinessOpeningHours(dayLabel = "Segunda", hoursLabel = "09:00 - 19:00", closed = false),
    ),
    faq = listOf(
        BusinessFaq(question = "Onde?", answer = "No centro."),
    ),
    stats = listOf(
        BusinessStat(value = "500+", label = "Carros"),
    ),
)
