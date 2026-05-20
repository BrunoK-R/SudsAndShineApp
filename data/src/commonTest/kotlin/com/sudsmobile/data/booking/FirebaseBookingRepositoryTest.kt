package com.sudsmobile.data.booking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class FirebaseBookingRepositoryTest {
    @Test
    fun rejectsInvalidAvailabilityRequestBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api)

        val result = repository.getAvailability(
            BookingAvailabilityRequest(
                anchorDate = "2026/05/20",
                serviceDurationMinutes = 30,
            ),
        )

        assertIs<BookingAvailabilityResult.Failure>(result)
        assertIs<BookingAvailabilityError.Validation>(result.error)
        assertEquals(0, api.availabilityCalls)
    }

    @Test
    fun rejectsInvalidEmailBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api)

        val result = repository.createBooking(validRequest().copy(customerEmail = "not-an-email"))

        assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Validation>(result.error)
        assertEquals(0, api.calls)
    }

    @Test
    fun normalizesRequestBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api)

        val result = repository.createBooking(
            validRequest().copy(
                customerName = "  Bruno Ribeiro  ",
                customerEmail = "  BRUNO@EXAMPLE.COM  ",
                notes = "  Sem ambientador  ",
            ),
        )

        assertIs<BookingCreateResult.Success>(result)
        assertEquals(1, api.calls)
        assertEquals("Bruno Ribeiro", api.lastRequest?.customerName)
        assertEquals("bruno@example.com", api.lastRequest?.customerEmail)
        assertEquals("Sem ambientador", api.lastRequest?.notes)
    }
}

private class RecordingBookingFunctionsApi : BookingFunctionsApi {
    var availabilityCalls: Int = 0
        private set
    var calls: Int = 0
        private set
    var lastRequest: BookingCreateRequest? = null
        private set

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        availabilityCalls += 1
        return BookingAvailabilityResult.Success(
            BookingAvailabilityMonth(
                monthTitle = "maio 2026",
                leadingEmptyCells = 4,
                days = emptyList(),
            ),
        )
    }

    override suspend fun createReservation(request: BookingCreateRequest): BookingCreateResult {
        calls += 1
        lastRequest = request
        return BookingCreateResult.Success(
            BookingReceipt(
                reservationId = "reservation-1",
                reservationCode = "SS-ABCDEFGH",
            ),
        )
    }
}

private fun validRequest(): BookingCreateRequest = BookingCreateRequest(
    customerName = "Bruno Ribeiro",
    customerEmail = "bruno@example.com",
    customerPhone = "+351913005855",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = "2026-05-20T09:30:00.000Z",
    slotEndIso = "2026-05-20T10:15:00.000Z",
    vehicleType = "passageiros",
    gdprConsent = true,
    notes = "",
)
