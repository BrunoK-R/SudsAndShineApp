package com.sudsmobile.feature.cart

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
    fun loadBookingsMapsUnauthenticatedState() = runTest {
        val viewModel = CartBookingsViewModel(
            FakeBookingRepository(
                BookingHistoryResult.Failure(
                    BookingHistoryError.Unauthenticated("Inicie sessão."),
                ),
            ),
        )

        viewModel.loadBookings()
        runCurrent()

        assertIs<CartBookingsUiState.Unauthenticated>(viewModel.uiState.value)
    }

    @Test
    fun loadBookingsSplitsUpcomingAndCompletedReservations() = runTest {
        val viewModel = CartBookingsViewModel(
            FakeBookingRepository(
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
        )

        viewModel.loadBookings()
        runCurrent()

        val loaded = assertIs<CartBookingsUiState.Loaded>(viewModel.uiState.value)
        assertEquals("upcoming-1", loaded.upcoming.single().id)
        assertEquals("21 de maio, 2026", loaded.upcoming.single().date)
        assertEquals("34,00€", loaded.upcoming.single().price)
        assertEquals("completed-1", loaded.completed.single().id)
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
    slotEndIso: String,
    upcoming: Boolean,
    priceCents: Int?,
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = slotStartIso,
    slotEndIso = slotEndIso,
    status = if (upcoming) "pending" else "completed",
    vehicleType = "suv",
    priceCents = priceCents,
    upcoming = upcoming,
)
