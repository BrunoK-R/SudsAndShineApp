package com.sudsmobile.feature.products

import com.sudsmobile.data.booking.BookingAvailabilityDay
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilityRequest
import com.sudsmobile.data.booking.BookingAvailabilityResult
import com.sudsmobile.data.booking.BookingAvailabilitySlot
import com.sudsmobile.data.booking.BookingCreateRequest
import com.sudsmobile.data.booking.BookingCreateResult
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsBookingViewModelTest {
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
    fun loadAvailabilityRequestsSelectedMonthAnchor() = runTest {
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("junho 2026", "2026-06-01")),
        )
        val viewModel = ProductsBookingViewModel(repository)

        viewModel.loadAvailability(serviceDurationMinutes = 45, anchorDate = "2026-06-01")
        runCurrent()

        assertEquals("2026-06-01", repository.lastAvailabilityRequest?.anchorDate)
        assertEquals(45, repository.lastAvailabilityRequest?.serviceDurationMinutes)
        assertIs<BookingAvailabilityUiState.Loaded>(viewModel.availabilityState.value)
    }

    @Test
    fun loadAvailabilityKeepsEmptyMonthForNavigation() = runTest {
        val emptyMonth = emptyMonth("maio 2026", "2026-05-01")
        val viewModel = ProductsBookingViewModel(
            FakeBookingRepository(
                availabilityResult = BookingAvailabilityResult.Success(emptyMonth),
            ),
        )

        viewModel.loadAvailability(serviceDurationMinutes = 30)
        runCurrent()

        val empty = assertIs<BookingAvailabilityUiState.Empty>(viewModel.availabilityState.value)
        assertEquals(emptyMonth, empty.month)
        assertEquals("2026-05-01", empty.month.monthAnchorDate())
    }

    @Test
    fun shiftsMonthAnchorAcrossYearBoundaries() {
        assertEquals("2027-01-01", shiftMonthAnchorDate("2026-12-20", monthOffset = 1))
        assertEquals("2026-12-01", shiftMonthAnchorDate("2027-01-01", monthOffset = -1))
        assertNull(shiftMonthAnchorDate("2026/12/20", monthOffset = 1))
    }
}

private class FakeBookingRepository(
    private val availabilityResult: BookingAvailabilityResult,
) : BookingRepository {
    var lastAvailabilityRequest: BookingAvailabilityRequest? = null
        private set

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        lastAvailabilityRequest = request
        return availabilityResult
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        error("Not used")
    }

    override suspend fun getMyBookings(): BookingHistoryResult {
        error("Not used")
    }
}

private fun availableMonth(monthTitle: String, firstDateId: String): BookingAvailabilityMonth {
    return BookingAvailabilityMonth(
        monthTitle = monthTitle,
        leadingEmptyCells = 0,
        days = listOf(
            availabilityDay(
                id = firstDateId,
                available = true,
                slots = listOf(
                    BookingAvailabilitySlot(
                        time = "09:00",
                        available = true,
                        remainingCapacity = 1,
                    ),
                ),
            ),
        ),
    )
}

private fun emptyMonth(monthTitle: String, firstDateId: String): BookingAvailabilityMonth {
    return BookingAvailabilityMonth(
        monthTitle = monthTitle,
        leadingEmptyCells = 0,
        days = listOf(
            availabilityDay(
                id = firstDateId,
                available = false,
                slots = emptyList(),
            ),
        ),
    )
}

private fun availabilityDay(
    id: String,
    available: Boolean,
    slots: List<BookingAvailabilitySlot>,
): BookingAvailabilityDay {
    return BookingAvailabilityDay(
        id = id,
        dayOfMonth = id.takeLast(2).toInt(),
        dateLabel = id,
        summaryLabel = id,
        available = available,
        slots = slots,
    )
}
