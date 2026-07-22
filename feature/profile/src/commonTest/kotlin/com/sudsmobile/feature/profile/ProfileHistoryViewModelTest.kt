package com.sudsmobile.feature.profile

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
import com.sudsmobile.data.booking.BookingReservationExtra
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
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
    fun refreshForSessionWaitsWhileSessionIsRestoring() = runTest {
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = repository,
            authRepository = FakeProfileHistoryAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileHistoryUiState.Loading>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun refreshForSessionMapsRestoreFailureWithoutHistoryCall() = runTest {
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = repository,
            authRepository = FakeProfileHistoryAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.RestoreFailed(AuthError.Backend("Falha ao validar sessão.")),
            ),
        )

        viewModel.refreshForSession()
        runCurrent()

        val error = assertIs<ProfileHistoryUiState.Error>(viewModel.uiState.value)
        assertEquals("Falha ao validar sessão.", error.message)
        assertEquals(true, error.retryable)
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
                                reviewed = true,
                                reviewRating = 5,
                                reviewTags = listOf(" Qualidade ", "qualidade", "Rápido"),
                                reviewComment = "  Ficou impecável.\nVoltava a reservar.  ",
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
                                id = "confirmed-past",
                                slotStartIso = "2026-05-21T09:00:00.000Z",
                                upcoming = false,
                                status = "confirmed",
                                priceCents = 3200,
                            ),
                            historyReservation(
                                id = "running-past",
                                slotStartIso = "2026-05-21T10:00:00.000Z",
                                upcoming = false,
                                status = "in_progress",
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
        assertEquals(listOf("completed-1", "completed-2", "cancelled-1"), loaded.items.map { it.id })
        assertEquals("18 de maio, 2026", loaded.items.first().date)
        assertEquals("10:00", loaded.items.first().time)
        assertEquals("SS-completed-1", loaded.items.first().reference)
        assertEquals("BMW 320d", loaded.items.first().vehicle)
        assertEquals(true, loaded.items.first().reviewed)
        assertEquals(false, loaded.items.first().reviewable)
        assertEquals(5, loaded.items.first().reviewRating)
        assertEquals(listOf("Qualidade", "Rápido"), loaded.items.first().reviewTags)
        assertEquals("Ficou impecável. Voltava a reservar.", loaded.items.first().reviewComment)
        assertEquals("premium", loaded.items.first().rebookPreset?.serviceId)
        assertEquals(true, loaded.items[1].reviewable)
        assertEquals(ProfileHistoryStatusUi.Cancelled, loaded.items[2].status)
        assertEquals(false, loaded.items[2].reviewable)
    }

    @Test
    fun loadHistoryMapsReservationDetailMetadata() = runTest {
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "completed-1",
                                reservationCode = "SS-DETAIL",
                                slotStartIso = "2026-05-18T10:30:00.000Z",
                                upcoming = false,
                                priceCents = 3950,
                                paymentStatus = "covered_by_loyalty",
                                userVehicleId = "vehicle-1",
                                vehicleLabel = "BMW 320d",
                                extras = listOf(
                                    BookingReservationExtra(id = "wax", name = " Cera premium ", priceCents = 700),
                                    BookingReservationExtra(id = "vacuum", name = "Aspiração", priceCents = 0),
                                    BookingReservationExtra(id = "blank", name = " ", priceCents = 500),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            authRepository = FakeProfileHistoryAuthRepository(authenticated = true),
        )

        viewModel.loadHistory()
        runCurrent()

        val item = assertIs<ProfileHistoryUiState.Loaded>(viewModel.uiState.value).items.single()
        assertEquals("SS-DETAIL", item.reference)
        assertEquals("10:30", item.time)
        assertEquals("Recompensa", item.paymentStatus)
        assertEquals("Lavagem Premium + 2 extras", item.service)
        assertEquals(
            listOf(
                ProfileHistoryExtraUi(name = "Cera premium", price = "7,00€"),
                ProfileHistoryExtraUi(name = "Aspiração", price = "Incluído"),
            ),
            item.extras,
        )
        assertEquals("premium", item.rebookPreset?.serviceId)
        assertEquals(listOf("wax", "vacuum", "blank"), item.rebookPreset?.extraIds)
        assertEquals("vehicle-1", item.rebookPreset?.userVehicleId)
        assertEquals("BMW 320d", item.rebookPreset?.vehicleLabel)
    }

    @Test
    fun loadHistoryMapsRebookActionOnlyWhenServiceIdIsPresent() = runTest {
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "completed-1",
                                serviceId = " premium ",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                            ),
                            historyReservation(
                                id = "completed-2",
                                serviceId = " ",
                                slotStartIso = "2026-05-19T10:00:00.000Z",
                                upcoming = false,
                                priceCents = 2500,
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
        assertEquals("premium", loaded.items[0].rebookPreset?.serviceId)
        assertEquals(null, loaded.items[1].rebookPreset)
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
    fun loadHistoryUsesWebsiteCompletedStatusAndSkipsClosedNonCompletedItems() = runTest {
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "done-1",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                upcoming = false,
                                status = "concluido",
                                priceCents = 3200,
                            ),
                            historyReservation(
                                id = "cancelled-1",
                                slotStartIso = "2026-05-17T10:00:00.000Z",
                                upcoming = false,
                                status = "cancelado",
                                priceCents = 3200,
                            ),
                            historyReservation(
                                id = "running-1",
                                slotStartIso = "2026-05-19T10:00:00.000Z",
                                upcoming = true,
                                status = "em_execucao",
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
        assertEquals(listOf("done-1", "cancelled-1"), loaded.items.map { it.id })
        assertEquals(ProfileHistoryStatusUi.Completed, loaded.items[0].status)
        assertEquals(ProfileHistoryStatusUi.Cancelled, loaded.items[1].status)
        assertEquals("1", loaded.summary.washCount)
    }

    @Test
    fun loadHistoryMapsRescheduleAuditForCompletedReservations() = runTest {
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "completed-1",
                                slotStartIso = "2026-05-22T11:00:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                                previousSlotStartIso = "2026-05-21T10:00:00.000Z",
                                previousSlotEndIso = "2026-05-21T10:45:00.000Z",
                                rescheduleCount = 1,
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
        val auditNote = loaded.items.single().auditNotes.single()
        assertEquals("Remarcada", auditNote.title)
        assertEquals(
            "De 21 de maio, 2026 às 10:00 para 22 de maio, 2026 às 11:00.",
            auditNote.body,
        )
    }

    @Test
    fun loadHistoryMapsCancellationAuditForCancelledReservations() = runTest {
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "cancelled-1",
                                slotStartIso = "2026-05-22T11:00:00.000Z",
                                upcoming = false,
                                status = "cancelado",
                                priceCents = 3200,
                                cancelledAtIso = "2026-05-21T15:30:00.000Z",
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
        val item = loaded.items.single()
        val auditNote = item.auditNotes.single()
        assertEquals(ProfileHistoryStatusUi.Cancelled, item.status)
        assertEquals("0", loaded.summary.washCount)
        assertEquals("0,00€", loaded.summary.totalSpent)
        assertEquals("Cancelada", auditNote.title)
        assertEquals("Cancelada em 21 de maio, 2026 às 15:30.", auditNote.body)
        assertEquals(ProfileHistoryAuditToneUi.Warning, auditNote.tone)
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

    @Test
    fun refreshForSessionReloadsWhenBookingRevisionChanges() = runTest {
        val bookingChangeNotifier = MutableBookingChangeNotifier()
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
            authRepository = FakeProfileHistoryAuthRepository(authenticated = true),
            bookingChangeNotifier = bookingChangeNotifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileHistoryUiState.Loaded>(viewModel.uiState.value)
        assertEquals(1, repository.historyCalls)

        viewModel.refreshForSession()
        runCurrent()

        assertEquals(1, repository.historyCalls)

        bookingChangeNotifier.notifyBookingsChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileHistoryUiState.Loaded>(viewModel.uiState.value)
        assertEquals(2, repository.historyCalls)
    }

    @Test
    fun refreshForSessionKeepsLatestBookingRevisionWhenHistoryLoadIsInFlight() = runTest {
        val bookingChangeNotifier = MutableBookingChangeNotifier()
        val firstResult = CompletableDeferred<BookingHistoryResult>()
        val secondResult = CompletableDeferred<BookingHistoryResult>()
        val repository = DeferredHistoryBookingRepository(firstResult, secondResult)
        val viewModel = ProfileHistoryViewModel(
            bookingRepository = repository,
            authRepository = FakeProfileHistoryAuthRepository(authenticated = true),
            bookingChangeNotifier = bookingChangeNotifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileHistoryUiState.Loading>(viewModel.uiState.value)
        assertEquals(1, repository.historyCalls)

        bookingChangeNotifier.notifyBookingsChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertEquals(2, repository.historyCalls)

        secondResult.complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    reservations = listOf(
                        historyReservation(
                            id = "latest-completed",
                            slotStartIso = "2026-05-23T10:00:00.000Z",
                            upcoming = false,
                            priceCents = 3200,
                        ),
                    ),
                ),
            ),
        )
        runCurrent()

        val loaded = assertIs<ProfileHistoryUiState.Loaded>(viewModel.uiState.value)
        assertEquals("latest-completed", loaded.items.single().id)

        firstResult.complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    reservations = listOf(
                        historyReservation(
                            id = "stale-completed",
                            slotStartIso = "2026-05-21T10:00:00.000Z",
                            upcoming = false,
                            priceCents = 3200,
                        ),
                    ),
                ),
            ),
        )
        runCurrent()

        val stillLoaded = assertIs<ProfileHistoryUiState.Loaded>(viewModel.uiState.value)
        assertEquals("latest-completed", stillLoaded.items.single().id)
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

private class DeferredHistoryBookingRepository(
    vararg results: CompletableDeferred<BookingHistoryResult>,
) : BookingRepository {
    private val pendingResults = results.toMutableList()
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
        return pendingResults.removeAt(0).await()
    }
}

private class FakeProfileHistoryAuthRepository(
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
    reservationCode: String = "SS-$id",
    serviceId: String = "premium",
    slotStartIso: String,
    upcoming: Boolean,
    status: String = if (upcoming) "pending" else "completed",
    paymentStatus: String = "paid",
    priceCents: Int?,
    userVehicleId: String? = null,
    vehicleLabel: String? = null,
    extras: List<BookingReservationExtra> = emptyList(),
    reviewed: Boolean = false,
    reviewRating: Int? = null,
    reviewTags: List<String> = emptyList(),
    reviewComment: String = "",
    rescheduledAtIso: String? = null,
    cancelledAtIso: String? = null,
    previousSlotStartIso: String? = null,
    previousSlotEndIso: String? = null,
    rescheduleCount: Int = 0,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = reservationCode,
    serviceId = serviceId,
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = slotStartIso,
    status = status,
    paymentStatus = paymentStatus,
    vehicleType = "suv",
    userVehicleId = userVehicleId,
    vehicleLabel = vehicleLabel,
    priceCents = priceCents,
    upcoming = upcoming,
    reviewed = reviewed,
    reviewRating = reviewRating,
    reviewTags = reviewTags,
    reviewComment = reviewComment,
    extras = extras,
    cancelledAtIso = cancelledAtIso,
    rescheduledAtIso = rescheduledAtIso,
    previousSlotStartIso = previousSlotStartIso,
    previousSlotEndIso = previousSlotEndIso,
    rescheduleCount = rescheduleCount,
)
