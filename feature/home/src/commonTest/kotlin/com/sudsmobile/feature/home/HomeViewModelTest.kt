package com.sudsmobile.feature.home

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
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.business.BusinessFaq
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.BusinessOpeningHours
import com.sudsmobile.data.business.BusinessStat
import com.sudsmobile.data.business.DefaultBusinessInfo
import com.sudsmobile.data.catalog.ServiceCatalog
import com.sudsmobile.data.catalog.ServiceCatalogRepository
import com.sudsmobile.data.catalog.ServiceCatalogResult
import com.sudsmobile.data.catalog.ServiceCatalogService
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
        val businessInfoRepository = FakeHomeBusinessInfoRepository(
            BusinessInfoResult.Success(
                businessInfo(
                    stats = listOf(
                        BusinessStat(value = "900+", label = "Carros Tratados"),
                        BusinessStat(value = "4.8", label = "Avaliação Média"),
                    ),
                ),
            ),
        )
        val viewModel = homeViewModel(
            authRepository = FakeHomeAuthRepository(authenticated = false),
            bookingRepository = bookingRepository,
            businessInfoRepository = businessInfoRepository,
            catalogRepository = FakeHomeCatalogRepository(
                ServiceCatalogResult.Success(ServiceCatalog(listOf(service("premium", popular = true)))),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val unauthenticated = assertIs<HomeUiState.Unauthenticated>(viewModel.uiState.value)
        assertEquals("Olá!", unauthenticated.identity.greeting)
        assertEquals("Lavagem Premium", unauthenticated.featuredServices.single().name)
        assertEquals("900+", unauthenticated.stats.first().value)
        assertEquals("Carros Tratados", unauthenticated.stats.first().label)
        assertEquals(0, bookingRepository.historyCalls)
        assertEquals(1, businessInfoRepository.calls)
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
            businessInfoRepository = FakeHomeBusinessInfoRepository(
                BusinessInfoResult.Success(
                    businessInfo(
                        addressLine1 = "Suds Norte",
                        addressLine2 = "Piso -1",
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
        assertEquals("Suds Norte, Piso -1", loaded.nextBooking?.location)
        assertEquals("BMW 320d", loaded.nextBooking?.vehicle)
        assertEquals(3, loaded.loyalty.completedWashes)
        assertEquals(7, loaded.loyalty.remainingWashes)
        assertEquals(false, loaded.loyalty.rewardReady)
        assertEquals("Lavagem Premium", loaded.featuredServices.first().name)
        assertEquals(DefaultBusinessInfo.stats.first().value, loaded.stats.first().value)
    }

    @Test
    fun homeKeepsFallbackStatsWhenBusinessInfoFails() = runTest {
        val viewModel = homeViewModel(
            businessInfoRepository = FakeHomeBusinessInfoRepository(
                BusinessInfoResult.Failure(BusinessInfoError.Unavailable("Dados institucionais indisponíveis.")),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val empty = assertIs<HomeUiState.Empty>(viewModel.uiState.value)
        assertEquals(DefaultBusinessInfo.stats.map { it.value }, empty.stats.map { it.value })
        assertEquals("Dados institucionais indisponíveis.", empty.statsWarningMessage)
        assertEquals(true, empty.statsWarningRetryable)
    }

    @Test
    fun authenticatedHomeKeepsFallbackBookingLocationWhenBusinessInfoFails() = runTest {
        val viewModel = homeViewModel(
            bookingRepository = FakeHomeBookingRepository(
                historyResult = BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            homeReservation(
                                id = "upcoming-1",
                                slotStartIso = "2026-05-22T10:00:00.000Z",
                                upcoming = true,
                            ),
                        ),
                    ),
                ),
            ),
            businessInfoRepository = FakeHomeBusinessInfoRepository(
                BusinessInfoResult.Failure(BusinessInfoError.Unavailable("Dados institucionais indisponíveis.")),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val loaded = assertIs<HomeUiState.Loaded>(viewModel.uiState.value)
        assertEquals(
            "${DefaultBusinessInfo.addressLine1}, ${DefaultBusinessInfo.addressLine2}",
            loaded.nextBooking?.location,
        )
        assertEquals("Dados institucionais indisponíveis.", loaded.statsWarningMessage)
        assertEquals(true, loaded.statsWarningRetryable)
    }

    @Test
    fun authenticatedHomeMarksLoyaltyRewardReadyOnTenthCompletedWash() = runTest {
        val viewModel = homeViewModel(
            bookingRepository = FakeHomeBookingRepository(
                historyResult = BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = (1..10).map {
                            homeReservation(
                                id = "completed-$it",
                                slotStartIso = "2026-05-${it.toString().padStart(2, '0')}T09:00:00.000Z",
                                upcoming = false,
                            )
                        },
                    ),
                ),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val loaded = assertIs<HomeUiState.Loaded>(viewModel.uiState.value)
        assertEquals(10, loaded.loyalty.completedWashes)
        assertEquals(0, loaded.loyalty.remainingWashes)
        assertEquals(1.0f, loaded.loyalty.progress)
        assertEquals(true, loaded.loyalty.rewardReady)
    }

    @Test
    fun authenticatedHomeShowsWebsiteStatusLabelAndCountsOnlyCompletedWashes() = runTest {
        val viewModel = homeViewModel(
            bookingRepository = FakeHomeBookingRepository(
                historyResult = BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            homeReservation(
                                id = "running",
                                slotStartIso = "2026-05-22T10:00:00.000Z",
                                upcoming = true,
                                status = "em_execucao",
                            ),
                            homeReservation(
                                id = "done",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                upcoming = false,
                                status = "concluido",
                            ),
                            homeReservation(
                                id = "cancelled",
                                slotStartIso = "2026-05-16T10:00:00.000Z",
                                upcoming = false,
                                status = "cancelado",
                            ),
                        ),
                    ),
                ),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val loaded = assertIs<HomeUiState.Loaded>(viewModel.uiState.value)
        assertEquals("running", loaded.nextBooking?.id)
        assertEquals("Em execução", loaded.nextBooking?.statusLabel)
        assertEquals(1, loaded.loyalty.completedWashes)
        assertEquals(9, loaded.loyalty.remainingWashes)
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

    @Test
    fun authenticatedHomeKeepsLatestRevisionWhenRequestsCompleteOutOfOrder() = runTest {
        val bookingChangeNotifier = MutableBookingChangeNotifier()
        val bookingRepository = DeferredHomeBookingRepository()
        val viewModel = homeViewModel(
            bookingRepository = bookingRepository,
            bookingChangeNotifier = bookingChangeNotifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<HomeUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, bookingRepository.historyCalls)

        bookingChangeNotifier.notifyBookingsChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertEquals(2, bookingRepository.historyCalls)

        bookingRepository.requests[1].complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    listOf(
                        homeReservation(
                            id = "new-booking",
                            slotStartIso = "2026-06-02T10:00:00.000Z",
                            upcoming = true,
                        ),
                    ),
                ),
            ),
        )
        runCurrent()

        val loaded = assertIs<HomeUiState.Loaded>(viewModel.uiState.value)
        assertEquals("new-booking", loaded.nextBooking?.id)

        bookingRepository.requests[0].complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    listOf(
                        homeReservation(
                            id = "stale-booking",
                            slotStartIso = "2026-05-30T10:00:00.000Z",
                            upcoming = true,
                        ),
                    ),
                ),
            ),
        )
        runCurrent()

        val stillLoaded = assertIs<HomeUiState.Loaded>(viewModel.uiState.value)
        assertEquals("new-booking", stillLoaded.nextBooking?.id)
    }

    @Test
    fun authenticatedHomeKeepsRestoringStateWhenSessionChangesDuringLoad() = runTest {
        val bookingRepository = DeferredHomeBookingRepository()
        val authRepository = FakeHomeAuthRepository(authenticated = true)
        val viewModel = homeViewModel(
            authRepository = authRepository,
            bookingRepository = bookingRepository,
        )

        viewModel.refreshForSession()
        runCurrent()
        authRepository.setSessionState(AuthSessionState.Restoring)
        bookingRepository.requests.single().complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    listOf(
                        homeReservation(
                            id = "pending-booking",
                            slotStartIso = "2026-06-02T10:00:00.000Z",
                            upcoming = true,
                        ),
                    ),
                ),
            ),
        )
        runCurrent()

        assertIs<HomeUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, bookingRepository.historyCalls)
    }

    @Test
    fun authenticatedHomeMapsRestoreFailureWhenSessionChangesDuringLoad() = runTest {
        val bookingRepository = DeferredHomeBookingRepository()
        val authRepository = FakeHomeAuthRepository(authenticated = true)
        val viewModel = homeViewModel(
            authRepository = authRepository,
            bookingRepository = bookingRepository,
        )

        viewModel.refreshForSession()
        runCurrent()
        authRepository.setSessionState(
            AuthSessionState.RestoreFailed(AuthError.Backend("Falha ao validar sessão.")),
        )
        bookingRepository.requests.single().complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    listOf(
                        homeReservation(
                            id = "pending-booking",
                            slotStartIso = "2026-06-02T10:00:00.000Z",
                            upcoming = true,
                        ),
                    ),
                ),
            ),
        )
        runCurrent()

        val error = assertIs<HomeUiState.Error>(viewModel.uiState.value)
        assertEquals("Falha ao validar sessão.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(1, bookingRepository.historyCalls)
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
    businessInfoRepository: BusinessInfoRepository = FakeHomeBusinessInfoRepository(
        BusinessInfoResult.Success(businessInfo()),
    ),
    bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
): HomeViewModel = HomeViewModel(
    authRepository = authRepository,
    bookingRepository = bookingRepository,
    serviceCatalogRepository = catalogRepository,
    businessInfoRepository = businessInfoRepository,
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

private class DeferredHomeBookingRepository : BookingRepository {
    val requests = mutableListOf<CompletableDeferred<BookingHistoryResult>>()
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
        val request = CompletableDeferred<BookingHistoryResult>()
        requests += request
        return request.await()
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

private class FakeHomeBusinessInfoRepository(
    private val result: BusinessInfoResult,
) : BusinessInfoRepository {
    var calls: Int = 0
        private set

    override suspend fun getBusinessInfo(): BusinessInfoResult {
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

    fun setSessionState(nextState: AuthSessionState) {
        mutableSessionState.value = nextState
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

private fun businessInfo(
    stats: List<BusinessStat> = DefaultBusinessInfo.stats,
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
    stats = stats,
)

private fun homeReservation(
    id: String,
    slotStartIso: String,
    upcoming: Boolean,
    priceCents: Int? = 3200,
    vehicleLabel: String? = null,
    status: String = if (upcoming) "confirmed" else "completed",
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = "2026-05-22T10:45:00.000Z",
    status = status,
    vehicleType = "passageiros",
    vehicleLabel = vehicleLabel,
    priceCents = priceCents,
    upcoming = upcoming,
)
