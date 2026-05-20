package com.sudsmobile.feature.profile

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
    fun loadHistoryMapsUnauthenticatedState() = runTest {
        val viewModel = ProfileHistoryViewModel(
            FakeBookingRepository(
                BookingHistoryResult.Failure(
                    BookingHistoryError.Unauthenticated("Inicie sessão."),
                ),
            ),
        )

        viewModel.loadHistory()
        runCurrent()

        assertIs<ProfileHistoryUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun loadHistoryBuildsCompletedSummaryFromUserReservations() = runTest {
        val viewModel = ProfileHistoryViewModel(
            FakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            historyReservation(
                                id = "completed-1",
                                slotStartIso = "2026-05-18T10:00:00.000Z",
                                upcoming = false,
                                priceCents = 3200,
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
        )

        viewModel.loadHistory()
        runCurrent()

        val loaded = assertIs<ProfileHistoryUiState.Loaded>(viewModel.uiState.value)
        assertEquals("2", loaded.summary.washCount)
        assertEquals("57,00€", loaded.summary.totalSpent)
        assertEquals(listOf("completed-1", "completed-2"), loaded.items.map { it.id })
        assertEquals("18 de maio, 2026", loaded.items.first().date)
        assertEquals("SUV", loaded.items.first().vehicle)
    }

    @Test
    fun loadHistoryMapsBackendErrorAsRetryable() = runTest {
        val viewModel = ProfileHistoryViewModel(
            FakeBookingRepository(
                BookingHistoryResult.Failure(
                    BookingHistoryError.Unavailable("Serviço indisponível."),
                ),
            ),
        )

        viewModel.loadHistory()
        runCurrent()

        val error = assertIs<ProfileHistoryUiState.Error>(viewModel.uiState.value)
        assertEquals("Serviço indisponível.", error.message)
        assertEquals(true, error.retryable)
    }
}

private class FakeBookingRepository(
    private val historyResult: BookingHistoryResult,
) : BookingRepository {
    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        error("Not used")
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        error("Not used")
    }

    override suspend fun getMyBookings(): BookingHistoryResult = historyResult
}

private fun historyReservation(
    id: String,
    slotStartIso: String,
    upcoming: Boolean,
    status: String = if (upcoming) "pending" else "completed",
    priceCents: Int?,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = slotStartIso,
    status = status,
    vehicleType = "suv",
    priceCents = priceCents,
    upcoming = upcoming,
)
