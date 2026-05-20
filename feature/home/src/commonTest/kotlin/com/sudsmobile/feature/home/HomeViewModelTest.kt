package com.sudsmobile.feature.home

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
import com.sudsmobile.data.catalog.ServiceCatalog
import com.sudsmobile.data.catalog.ServiceCatalogRepository
import com.sudsmobile.data.catalog.ServiceCatalogResult
import com.sudsmobile.data.catalog.ServiceCatalogService
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
class HomeViewModelTest {
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
    fun guestHomeLoadsPublicCatalogWithoutCallingBookingHistory() = runTest {
        val bookingRepository = FakeHomeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = homeViewModel(
            authRepository = FakeHomeAuthRepository(authenticated = false),
            bookingRepository = bookingRepository,
            catalogRepository = FakeHomeCatalogRepository(
                ServiceCatalogResult.Success(ServiceCatalog(listOf(service("premium", popular = true)))),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val unauthenticated = assertIs<HomeUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals("Olá!", unauthenticated.identity.greeting)
        assertEquals("Lavagem Premium", unauthenticated.featuredServices.single().name)
        assertEquals(0, bookingRepository.historyCalls)
    }

    @Test
    fun authenticatedHomeMapsNextBookingAndLoyaltyFromUserHistory() = runTest {
        val viewModel = homeViewModel(
            bookingRepository = FakeHomeBookingRepository(
                historyResult = BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            homeReservation(
                                id = "completed-1",
                                slotStartIso = "2026-05-10T09:00:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                            ),
                            homeReservation(
                                id = "completed-2",
                                slotStartIso = "2026-05-11T09:00:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                            ),
                            homeReservation(
                                id = "completed-3",
                                slotStartIso = "2026-05-12T09:00:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                            ),
                            homeReservation(
                                id = "upcoming-later",
                                slotStartIso = "2026-05-24T14:30:00.000Z",
                                upcoming = true,
                                priceCents = 3400,
                                vehicleLabel = "VW Golf",
                            ),
                            homeReservation(
                                id = "upcoming-next",
                                slotStartIso = "2026-05-22T10:00:00.000Z",
                                upcoming = true,
                                priceCents = 3200,
                                vehicleLabel = "BMW 320d",
                            ),
                        ),
                    ),
                ),
            ),
            catalogRepository = FakeHomeCatalogRepository(
                ServiceCatalogResult.Success(
                    ServiceCatalog(
                        listOf(
                            service("standard", name = "Lavagem Standard"),
                            service("premium", name = "Lavagem Premium", popular = true),
                        ),
                    ),
                ),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val loaded = assertIs<HomeUiState.Loaded>(viewModel.uiState.value)
        assertEquals("Olá, Bruno!", loaded.identity.greeting)
        assertEquals("upcoming-next", loaded.nextBooking?.id)
        assertEquals("22 de maio, 2026", loaded.nextBooking?.date)
        assertEquals("10:00", loaded.nextBooking?.time)
        assertEquals("BMW 320d", loaded.nextBooking?.vehicle)
        assertEquals(3, loaded.loyalty.completedWashes)
        assertEquals(7, loaded.loyalty.remainingWashes)
        assertEquals("Lavagem Premium", loaded.featuredServices.first().name)
    }

    @Test
    fun authenticatedHomeMapsHistoryFailureToRetryableErrorWithFeaturedServices() = runTest {
        val viewModel = homeViewModel(
            bookingRepository = FakeHomeBookingRepository(
                historyResult = BookingHistoryResult.Failure(
                    BookingHistoryError.Unavailable("Marcações indisponíveis."),
                ),
            ),
            catalogRepository = FakeHomeCatalogRepository(
                ServiceCatalogResult.Success(ServiceCatalog(listOf(service("premium", popular = true)))),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val error = assertIs<HomeUiState.Error>(viewModel.uiState.value)
        assertEquals("Marcações indisponíveis.", error.message)
        assertEquals(true, error.retryable)
        assertEquals("Lavagem Premium", error.featuredServices.single().name)
    }

    @Test
    fun refreshForSessionReloadsWhenBookingRevisionChanges() = runTest {
        val bookingChangeNotifier = MutableBookingChangeNotifier()
        val bookingRepository = FakeHomeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = homeViewModel(
            bookingRepository = bookingRepository,
            bookingChangeNotifier = bookingChangeNotifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<HomeUiState.Empty>(viewModel.uiState.value)
        assertEquals(1, bookingRepository.historyCalls)

        viewModel.refreshForSession()
        runCurrent()

        assertEquals(1, bookingRepository.historyCalls)

        bookingChangeNotifier.notifyBookingsChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<HomeUiState.Empty>(viewModel.uiState.value)
        assertEquals(2, bookingRepository.historyCalls)
    }
}

private fun homeViewModel(
    authRepository: AuthRepository = FakeHomeAuthRepository(authenticated = true),
    bookingRepository: BookingRepository = FakeHomeBookingRepository(
        historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
    ),
    catalogRepository: ServiceCatalogRepository = FakeHomeCatalogRepository(
        ServiceCatalogResult.Success(ServiceCatalog(listOf(service("premium", popular = true)))),
    ),
    bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
): HomeViewModel = HomeViewModel(
    authRepository = authRepository,
    bookingRepository = bookingRepository,
    serviceCatalogRepository = catalogRepository,
    bookingChangeNotifier = bookingChangeNotifier,
)

private class FakeHomeBookingRepository(
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

private class FakeHomeCatalogRepository(
    private val result: ServiceCatalogResult,
) : ServiceCatalogRepository {
    var calls: Int = 0
        private set

    override suspend fun getServiceCatalog(): ServiceCatalogResult {
        calls += 1
        return result
    }
}

private class FakeHomeAuthRepository(
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
                displayName = "Bruno Ribeiro",
                phoneNumber = "",
            ),
            idToken = "id-token-$uid",
            refreshToken = "refresh-token-$uid",
            expiresInSeconds = 3600,
        ),
    )
}

private fun service(
    id: String,
    name: String = "Lavagem Premium",
    popular: Boolean = false,
): ServiceCatalogService = ServiceCatalogService(
    id = id,
    name = name,
    description = "Lavagem detalhada",
    durationMinutes = 45,
    passengerPriceCents = 3200,
    suvPriceCents = 3400,
    iconKey = "sparkles",
    popular = popular,
)

private fun homeReservation(
    id: String,
    slotStartIso: String,
    upcoming: Boolean,
    priceCents: Int?,
    vehicleLabel: String? = null,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = "2026-05-22T10:45:00.000Z",
    status = if (upcoming) "confirmed" else "completed",
    vehicleType = "passageiros",
    vehicleLabel = vehicleLabel,
    priceCents = priceCents,
    upcoming = upcoming,
)
