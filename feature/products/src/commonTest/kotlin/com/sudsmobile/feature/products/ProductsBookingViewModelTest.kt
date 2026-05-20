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
import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthResult
import com.sudsmobile.data.auth.AuthSession
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.vehicle.UserVehicle
import com.sudsmobile.data.vehicle.UserVehicleDeleteResult
import com.sudsmobile.data.vehicle.UserVehicleListResult
import com.sudsmobile.data.vehicle.UserVehicleMutationResult
import com.sudsmobile.data.vehicle.UserVehicleRepository
import com.sudsmobile.data.vehicle.UserVehicleSaveRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
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
        val viewModel = productsBookingViewModel(repository)

        viewModel.loadAvailability(serviceDurationMinutes = 45, anchorDate = "2026-06-01")
        runCurrent()

        assertEquals("2026-06-01", repository.lastAvailabilityRequest?.anchorDate)
        assertEquals(45, repository.lastAvailabilityRequest?.serviceDurationMinutes)
        assertIs<BookingAvailabilityUiState.Loaded>(viewModel.availabilityState.value)
    }

    @Test
    fun loadAvailabilityKeepsEmptyMonthForNavigation() = runTest {
        val emptyMonth = emptyMonth("maio 2026", "2026-05-01")
        val viewModel = productsBookingViewModel(
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

    @Test
    fun loadVehiclesRequiresAuthenticatedSession() = runTest {
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(listOf(userVehicle())),
        )
        val viewModel = productsBookingViewModel(
            authRepository = FakeProductsAuthRepository(authenticated = false),
            vehicleRepository = vehicleRepository,
        )

        viewModel.loadVehicles()
        runCurrent()

        assertIs<BookingVehiclesUiState.Unauthenticated>(viewModel.vehiclesState.value)
        assertEquals(0, vehicleRepository.listCalls)
    }

    @Test
    fun loadVehiclesMapsSavedVehiclesToBookingOptions() = runTest {
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(
                listOf(userVehicle(id = "vehicle-1", brand = "BMW", type = "suv")),
            ),
        )
        val viewModel = productsBookingViewModel(vehicleRepository = vehicleRepository)

        viewModel.loadVehicles()
        runCurrent()

        val loaded = assertIs<BookingVehiclesUiState.Loaded>(viewModel.vehiclesState.value)
        assertEquals("saved:vehicle-1", loaded.vehicles.single().id)
        assertEquals("BMW 320d", loaded.vehicles.single().name)
        assertEquals("suv", loaded.vehicles.single().type)
        assertEquals("vehicle-1", loaded.vehicles.single().userVehicleId)
    }
}

private fun productsBookingViewModel(
    bookingRepository: BookingRepository = FakeBookingRepository(
        availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
    ),
    authRepository: AuthRepository = FakeProductsAuthRepository(authenticated = true),
    vehicleRepository: UserVehicleRepository = FakeProductsVehicleRepository(
        listResult = UserVehicleListResult.Success(emptyList()),
    ),
): ProductsBookingViewModel = ProductsBookingViewModel(
    bookingRepository = bookingRepository,
    authRepository = authRepository,
    userVehicleRepository = vehicleRepository,
)

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

private class FakeProductsVehicleRepository(
    private val listResult: UserVehicleListResult,
) : UserVehicleRepository {
    var listCalls: Int = 0
        private set

    override suspend fun getMyVehicles(): UserVehicleListResult {
        listCalls += 1
        return listResult
    }

    override suspend fun createVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        error("Not used")
    }

    override suspend fun updateVehicle(request: UserVehicleSaveRequest): UserVehicleMutationResult {
        error("Not used")
    }

    override suspend fun deleteVehicle(vehicleId: String): UserVehicleDeleteResult {
        error("Not used")
    }
}

private class FakeProductsAuthRepository(
    authenticated: Boolean,
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

private fun userVehicle(
    id: String = "vehicle-1",
    brand: String = "BMW",
    type: String = "passenger",
): UserVehicle = UserVehicle(
    id = id,
    brand = brand,
    model = "320d",
    plate = "AA-00-BB",
    color = "Preto",
    type = type,
)
