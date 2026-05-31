package com.sudsmobile.feature.cart

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthError
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
import com.sudsmobile.data.booking.BookingAvailabilityDay
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilityResult
import com.sudsmobile.data.booking.BookingAvailabilitySlot
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
import com.sudsmobile.data.booking.BookingRescheduleError
import com.sudsmobile.data.booking.BookingRescheduleReceipt
import com.sudsmobile.data.booking.BookingRescheduleRequest
import com.sudsmobile.data.booking.BookingRescheduleResult
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
    fun refreshForSessionWaitsWhileSessionIsRestoring() = runTest {
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<CartBookingsUiState.Loading>(viewModel.uiState.value)
        assertEquals(0, repository.historyCalls)
    }

    @Test
    fun refreshForSessionMapsRestoreFailureWithoutBookingCall() = runTest {
        val repository = FakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.RestoreFailed(AuthError.Unavailable("Sessão indisponível.")),
            ),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.refreshForSession()
        runCurrent()

        val error = assertIs<CartBookingsUiState.Error>(viewModel.uiState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
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
                                reviewTags = listOf(" Qualidade ", "qualidade", "Rápido"),
                                reviewComment = "  Ficou impecável.\nVoltava a reservar.  ",
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
        assertEquals(listOf("Qualidade", "Rápido"), loaded.completed.single().reviewTags)
        assertEquals("Ficou impecável. Voltava a reservar.", loaded.completed.single().reviewComment)
    }

    @Test
    fun loadBookingsPreservesBackendStatusLifecycleAndActions() = runTest {
        val viewModel = CartBookingsViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "new-1",
                                slotStartIso = "2026-05-21T10:00:00.000Z",
                                slotEndIso = "2026-05-21T10:45:00.000Z",
                                upcoming = true,
                                priceCents = 3400,
                                status = "novo",
                                paymentStatus = "pending",
                            ),
                            historyReservation(
                                id = "running-1",
                                slotStartIso = "2026-05-21T12:00:00.000Z",
                                slotEndIso = "2026-05-21T12:45:00.000Z",
                                upcoming = true,
                                priceCents = 3400,
                                status = "em_execucao",
                            ),
                            historyReservation(
                                id = "done-1",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                slotEndIso = "2026-05-18T10:45:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                                status = "concluido",
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
        assertEquals(BookingStatusUi.Pending, loaded.upcoming[0].status)
        assertEquals(true, loaded.upcoming[0].cancelable)
        assertEquals(false, loaded.upcoming[0].requiresPayment)
        assertEquals("Pagamento pendente", loaded.upcoming[0].paymentLabel)
        assertEquals(BookingStatusUi.InProgress, loaded.upcoming[1].status)
        assertEquals(false, loaded.upcoming[1].cancelable)
        assertEquals(false, loaded.upcoming[1].requiresPayment)
        assertEquals(BookingStatusUi.Completed, loaded.completed.single().status)
        assertEquals(true, loaded.completed.single().reviewable)
    }

    @Test
    fun loadBookingsMapsReservationAuditNotes() = runTest {
        val viewModel = CartBookingsViewModel(
            bookingRepository = FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "rescheduled-1",
                                slotStartIso = "2026-05-22T11:00:00.000Z",
                                slotEndIso = "2026-05-22T11:45:00.000Z",
                                upcoming = true,
                                priceCents = 3400,
                                previousSlotStartIso = "2026-05-21T10:00:00.000Z",
                                previousSlotEndIso = "2026-05-21T10:45:00.000Z",
                                rescheduleCount = 2,
                            ),
                            historyReservation(
                                id = "cancelled-1",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                slotEndIso = "2026-05-18T10:45:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
                                status = "cancelled",
                                cancelledAtIso = "2026-05-17T16:30:00.000Z",
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
        val rescheduledNote = loaded.upcoming.single().auditNotes.single()
        assertEquals("Marcação remarcada 2 vezes", rescheduledNote.title)
        assertEquals(
            "De 21 de maio, 2026 às 10:00 para 22 de maio, 2026 às 11:00.",
            rescheduledNote.body,
        )

        val cancelledNote = loaded.completed.single().auditNotes.single()
        assertEquals("Marcação cancelada", cancelledNote.title)
        assertEquals("Cancelada em 17 de maio, 2026 às 16:30.", cancelledNote.body)
        assertEquals(BookingAuditToneUi.Warning, cancelledNote.tone)
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
    fun refreshForSessionKeepsLatestBookingRevisionWhenHistoryLoadIsInFlight() = runTest {
        val bookingChangeNotifier = MutableBookingChangeNotifier()
        val firstResult = CompletableDeferred<BookingHistoryResult>()
        val secondResult = CompletableDeferred<BookingHistoryResult>()
        val repository = DeferredHistoryBookingRepository(firstResult, secondResult)
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
            bookingChangeNotifier = bookingChangeNotifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<CartBookingsUiState.Loading>(viewModel.uiState.value)
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
                            id = "latest-upcoming",
                            slotStartIso = "2026-05-23T10:00:00.000Z",
                            slotEndIso = "2026-05-23T10:45:00.000Z",
                            upcoming = true,
                            priceCents = 3400,
                        ),
                    ),
                ),
            ),
        )
        runCurrent()

        val loaded = assertIs<CartBookingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals("latest-upcoming", loaded.upcoming.single().id)

        firstResult.complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    reservations = listOf(
                        historyReservation(
                            id = "stale-upcoming",
                            slotStartIso = "2026-05-21T10:00:00.000Z",
                            slotEndIso = "2026-05-21T10:45:00.000Z",
                            upcoming = true,
                            priceCents = 3400,
                        ),
                    ),
                ),
            ),
        )
        runCurrent()

        val stillLoaded = assertIs<CartBookingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals("latest-upcoming", stillLoaded.upcoming.single().id)
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
    fun cancelBookingWaitsForValidatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.cancelBooking("reservation-1")
        runCurrent()

        val error = assertIs<BookingCancellationUiState.Error>(viewModel.cancellationState.value)
        assertEquals("reservation-1", error.reservationId)
        assertEquals(true, error.retryable)
        assertEquals(0, repository.cancelCalls)
    }

    @Test
    fun cancelBookingRejectsSessionUserChangeBeforeRepositoryCall() = runTest {
        val authRepository = FakeCartAuthRepository(authenticated = true)
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.cancelBooking("reservation-1")
        authRepository.authenticate(uid = "uid-2")
        runCurrent()

        val error = assertIs<BookingCancellationUiState.Error>(viewModel.cancellationState.value)
        assertEquals("reservation-1", error.reservationId)
        assertEquals(false, error.retryable)
        assertEquals(0, repository.cancelCalls)
    }

    @Test
    fun loadRescheduleAvailabilityRequestsBackendDurationAndAnchor() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
            availabilityResult = BookingAvailabilityResult.Success(availabilityMonth()),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.loadRescheduleAvailability(
            serviceDurationMinutes = 45,
            anchorDate = "2026-05-01",
        )
        runCurrent()

        val loaded = assertIs<BookingRescheduleAvailabilityUiState.Loaded>(
            viewModel.rescheduleAvailabilityState.value,
        )
        assertEquals("maio 2026", loaded.month.monthTitle)
        assertEquals(1, repository.availabilityCalls)
        assertEquals(
            BookingAvailabilityRequest(
                anchorDate = "2026-05-01",
                serviceDurationMinutes = 45,
            ),
            repository.lastAvailabilityRequest,
        )
    }

    @Test
    fun loadRescheduleAvailabilityAcceptsNewMonthWhilePreviousRequestIsLoading() = runTest {
        val firstResult = CompletableDeferred<BookingAvailabilityResult>()
        val secondResult = CompletableDeferred<BookingAvailabilityResult>()
        val repository = DeferredAvailabilityBookingRepository(firstResult, secondResult)
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.loadRescheduleAvailability(serviceDurationMinutes = 30, anchorDate = "2026-05-01")
        runCurrent()

        assertIs<BookingRescheduleAvailabilityUiState.Loading>(viewModel.rescheduleAvailabilityState.value)
        assertEquals("2026-05-01", repository.requests.single().anchorDate)

        viewModel.loadRescheduleAvailability(serviceDurationMinutes = 45, anchorDate = "2026-06-01")
        runCurrent()

        assertEquals(2, repository.requests.size)
        assertEquals("2026-06-01", repository.requests[1].anchorDate)
        assertEquals(45, repository.requests[1].serviceDurationMinutes)

        secondResult.complete(
            BookingAvailabilityResult.Success(availabilityMonth(monthTitle = "junho 2026", dayId = "2026-06-22")),
        )
        runCurrent()

        val loaded = assertIs<BookingRescheduleAvailabilityUiState.Loaded>(
            viewModel.rescheduleAvailabilityState.value,
        )
        assertEquals("junho 2026", loaded.month.monthTitle)

        firstResult.complete(
            BookingAvailabilityResult.Success(availabilityMonth(monthTitle = "maio 2026", dayId = "2026-05-22")),
        )
        runCurrent()

        val stillLoaded = assertIs<BookingRescheduleAvailabilityUiState.Loaded>(
            viewModel.rescheduleAvailabilityState.value,
        )
        assertEquals("junho 2026", stillLoaded.month.monthTitle)
    }

    @Test
    fun rescheduleBookingBuildsRequestAndPublishesSuccessState() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
            rescheduleResult = BookingRescheduleResult.Success(
                BookingRescheduleReceipt(
                    reservationId = "reservation-1",
                    status = "pending",
                    slotStartIso = "2026-05-22T11:00:00.000Z",
                    slotEndIso = "2026-05-22T11:45:00.000Z",
                ),
            ),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.rescheduleBooking(
            BookingRescheduleDraft(
                reservationId = "reservation-1",
                dateId = "2026-05-22",
                time = "11:00",
                durationMinutes = 45,
            ),
        )
        runCurrent()

        val success = assertIs<BookingRescheduleUiState.Success>(viewModel.rescheduleState.value)
        assertEquals("reservation-1", success.reservationId)
        assertEquals(1, repository.rescheduleCalls)
        assertEquals(
            BookingRescheduleRequest(
                reservationId = "reservation-1",
                slotStartIso = "2026-05-22T11:00:00.000Z",
                slotEndIso = "2026-05-22T11:45:00.000Z",
            ),
            repository.lastRescheduleRequest,
        )
    }

    @Test
    fun rescheduleBookingMapsConflictToChangeSlotError() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
            rescheduleResult = BookingRescheduleResult.Failure(
                BookingRescheduleError.Conflict("Este horário deixou de estar disponível."),
            ),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.rescheduleBooking(
            BookingRescheduleDraft(
                reservationId = "reservation-1",
                dateId = "2026-05-22",
                time = "11:00",
                durationMinutes = 45,
            ),
        )
        runCurrent()

        val error = assertIs<BookingRescheduleUiState.Error>(viewModel.rescheduleState.value)
        assertEquals("reservation-1", error.reservationId)
        assertEquals("Este horário deixou de estar disponível.", error.message)
        assertEquals(false, error.retryable)
        assertEquals(true, error.changeSlot)
    }

    @Test
    fun rescheduleBookingWaitsForValidatedSessionBeforeRepositoryCall() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(
                authenticated = false,
                initialState = AuthSessionState.Restoring,
            ),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.rescheduleBooking(
            BookingRescheduleDraft(
                reservationId = "reservation-1",
                dateId = "2026-05-22",
                time = "11:00",
                durationMinutes = 45,
            ),
        )
        runCurrent()

        val error = assertIs<BookingRescheduleUiState.Error>(viewModel.rescheduleState.value)
        assertEquals("reservation-1", error.reservationId)
        assertEquals(true, error.retryable)
        assertEquals(false, error.changeSlot)
        assertEquals(0, repository.rescheduleCalls)
    }

    @Test
    fun rescheduleBookingRejectsSessionUserChangeBeforeRepositoryCall() = runTest {
        val authRepository = FakeCartAuthRepository(authenticated = true)
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.rescheduleBooking(
            BookingRescheduleDraft(
                reservationId = "reservation-1",
                dateId = "2026-05-22",
                time = "11:00",
                durationMinutes = 45,
            ),
        )
        authRepository.authenticate(uid = "uid-2")
        runCurrent()

        val error = assertIs<BookingRescheduleUiState.Error>(viewModel.rescheduleState.value)
        assertEquals("reservation-1", error.reservationId)
        assertEquals(false, error.retryable)
        assertEquals(false, error.changeSlot)
        assertEquals(0, repository.rescheduleCalls)
    }

    @Test
    fun rescheduleBookingRejectsIncompleteDraftBeforeRepositoryCall() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.rescheduleBooking(
            BookingRescheduleDraft(
                reservationId = "reservation-1",
                dateId = "2026-05-22",
                time = "",
                durationMinutes = 45,
            ),
        )

        val error = assertIs<BookingRescheduleUiState.Error>(viewModel.rescheduleState.value)
        assertEquals("Escolha uma nova data e hora para remarcar.", error.message)
        assertEquals(0, repository.rescheduleCalls)
    }

    @Test
    fun rescheduleBookingRejectsImpossibleCalendarDateBeforeRepositoryCall() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.rescheduleBooking(
            BookingRescheduleDraft(
                reservationId = "reservation-1",
                dateId = "2026-02-29",
                time = "11:00",
                durationMinutes = 45,
            ),
        )

        val error = assertIs<BookingRescheduleUiState.Error>(viewModel.rescheduleState.value)
        assertEquals("Escolha uma nova data e hora para remarcar.", error.message)
        assertEquals(0, repository.rescheduleCalls)
    }

    @Test
    fun rescheduleBookingAllowsLeapDayDraft() = runTest {
        val repository = FakeBookingRepository(
            historyResult = BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val viewModel = CartBookingsViewModel(
            bookingRepository = repository,
            authRepository = FakeCartAuthRepository(authenticated = true),
            businessInfoRepository = FakeBusinessInfoRepository(),
        )

        viewModel.rescheduleBooking(
            BookingRescheduleDraft(
                reservationId = "reservation-1",
                dateId = "2028-02-29",
                time = "11:00",
                durationMinutes = 45,
            ),
        )
        runCurrent()

        assertIs<BookingRescheduleUiState.Success>(viewModel.rescheduleState.value)
        assertEquals(1, repository.rescheduleCalls)
        assertEquals("2028-02-29T11:00:00.000Z", repository.lastRescheduleRequest?.slotStartIso)
        assertEquals("2028-02-29T11:45:00.000Z", repository.lastRescheduleRequest?.slotEndIso)
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
    private val availabilityResult: BookingAvailabilityResult = BookingAvailabilityResult.Success(availabilityMonth()),
    private val cancelResult: BookingCancelResult = BookingCancelResult.Success(
        BookingCancelReceipt(
            reservationId = "reservation-1",
            status = "cancelled",
        ),
    ),
    private val rescheduleResult: BookingRescheduleResult = BookingRescheduleResult.Success(
        BookingRescheduleReceipt(
            reservationId = "reservation-1",
            status = "pending",
            slotStartIso = "2026-05-22T11:00:00.000Z",
            slotEndIso = "2026-05-22T11:45:00.000Z",
        ),
    ),
) : BookingRepository {
    var historyCalls: Int = 0
        private set
    var availabilityCalls: Int = 0
        private set
    var lastAvailabilityRequest: BookingAvailabilityRequest? = null
        private set
    var cancelCalls: Int = 0
        private set
    var lastCancelRequest: BookingCancelRequest? = null
        private set
    var rescheduleCalls: Int = 0
        private set
    var lastRescheduleRequest: BookingRescheduleRequest? = null
        private set

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        availabilityCalls += 1
        lastAvailabilityRequest = request
        return availabilityResult
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

    override suspend fun rescheduleBooking(request: BookingRescheduleRequest): BookingRescheduleResult {
        rescheduleCalls += 1
        lastRescheduleRequest = request
        return rescheduleResult
    }
}

private class DeferredAvailabilityBookingRepository(
    vararg results: CompletableDeferred<BookingAvailabilityResult>,
) : BookingRepository {
    private val pendingResults = results.toMutableList()
    val requests: MutableList<BookingAvailabilityRequest> = mutableListOf()

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        requests += request
        return pendingResults.removeAt(0).await()
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        error("Not used")
    }

    override suspend fun getMyBookings(): BookingHistoryResult {
        error("Not used")
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
    slotStartIso: String,
    slotEndIso: String,
    upcoming: Boolean,
    priceCents: Int?,
    vehicleLabel: String? = null,
    reviewed: Boolean = false,
    reviewRating: Int? = null,
    reviewTags: List<String> = emptyList(),
    reviewComment: String = "",
    status: String = if (upcoming) "pending" else "completed",
    paymentStatus: String = "",
    cancelledAtIso: String? = null,
    rescheduledAtIso: String? = null,
    previousSlotStartIso: String? = null,
    previousSlotEndIso: String? = null,
    rescheduleCount: Int = 0,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = slotEndIso,
    status = status,
    paymentStatus = paymentStatus,
    vehicleType = "suv",
    vehicleLabel = vehicleLabel,
    priceCents = priceCents,
    upcoming = upcoming,
    reviewed = reviewed,
    reviewRating = reviewRating,
    reviewTags = reviewTags,
    reviewComment = reviewComment,
    cancelledAtIso = cancelledAtIso,
    rescheduledAtIso = rescheduledAtIso,
    previousSlotStartIso = previousSlotStartIso,
    previousSlotEndIso = previousSlotEndIso,
    rescheduleCount = rescheduleCount,
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
    socialLinks = emptyList(),
)

private fun availabilityMonth(
    monthTitle: String = "maio 2026",
    dayId: String = "2026-05-22",
): BookingAvailabilityMonth = BookingAvailabilityMonth(
    monthTitle = monthTitle,
    leadingEmptyCells = 4,
    days = listOf(
        BookingAvailabilityDay(
            id = dayId,
            dayOfMonth = dayId.substringAfterLast("-").toIntOrNull() ?: 22,
            dateLabel = "Sexta, 22 maio",
            summaryLabel = "Sex",
            available = true,
            slots = listOf(
                BookingAvailabilitySlot(
                    time = "11:00",
                    available = true,
                    remainingCapacity = 2,
                ),
            ),
        ),
    ),
)
