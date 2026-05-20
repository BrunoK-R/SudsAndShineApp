package com.sudsmobile.data.booking

import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FirebaseBookingRepositoryTest {
    @Test
    fun rejectsInvalidAvailabilityRequestBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

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
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.createBooking(validRequest().copy(customerEmail = "not-an-email"))

        assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Validation>(result.error)
        assertEquals(0, api.calls)
    }

    @Test
    fun normalizesRequestBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

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
        assertEquals("id-token-1", api.lastIdToken)
    }

    @Test
    fun rejectsHistoryWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.getMyBookings()

        assertIs<BookingHistoryResult.Failure>(result)
        assertIs<BookingHistoryError.Unauthenticated>(result.error)
        assertEquals(0, api.historyCalls)
    }

    @Test
    fun loadsHistoryWithAuthenticatedIdToken() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.getMyBookings()

        assertIs<BookingHistoryResult.Success>(result)
        assertEquals(1, api.historyCalls)
        assertEquals("id-token-1", api.lastHistoryIdToken)
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

    var historyCalls: Int = 0
        private set
    var lastIdToken: String? = null
        private set
    var lastHistoryIdToken: String? = null
        private set

    override suspend fun createReservation(request: BookingCreateRequest, idToken: String?): BookingCreateResult {
        calls += 1
        lastRequest = request
        lastIdToken = idToken
        return BookingCreateResult.Success(
            BookingReceipt(
                reservationId = "reservation-1",
                reservationCode = "SS-ABCDEFGH",
            ),
        )
    }

    override suspend fun getMyReservations(idToken: String): BookingHistoryResult {
        historyCalls += 1
        lastHistoryIdToken = idToken
        return BookingHistoryResult.Success(BookingHistory(reservations = emptyList()))
    }
}

private class FakeAuthRepository(
    authenticated: Boolean = false,
) : AuthRepository {
    override val sessionState: StateFlow<AuthSessionState> = MutableStateFlow(
        if (authenticated) {
            AuthSessionState.Authenticated(
                AuthSession(
                    user = AuthUser(
                        uid = "uid-1",
                        email = "bruno@example.com",
                        displayName = "Bruno",
                        phoneNumber = "",
                    ),
                    idToken = "id-token-1",
                    refreshToken = "refresh-token-1",
                    expiresInSeconds = 3600,
                ),
            )
        } else {
            AuthSessionState.Unauthenticated
        },
    )

    override suspend fun currentSession(): AuthSession? {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session
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
