package com.sudsmobile.feature.products

import com.sudsmobile.data.booking.BookingAvailabilityDay
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilityRequest
import com.sudsmobile.data.booking.BookingAvailabilityResult
import com.sudsmobile.data.booking.BookingAvailabilitySlot
import com.sudsmobile.data.booking.BookingCreateRequest
import com.sudsmobile.data.booking.BookingCreateError
import com.sudsmobile.data.booking.BookingCreateResult
import com.sudsmobile.data.booking.BookingReceipt
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.profile.UserProfile
import com.sudsmobile.data.profile.UserProfileError
import com.sudsmobile.data.profile.UserProfileMutationResult
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.profile.UserProfileResult
import com.sudsmobile.data.profile.UserProfileSaveRequest
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
import com.sudsmobile.data.vehicle.MutableUserVehicleChangeNotifier
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

    @Test
    fun refreshVehiclesForSessionLoadsAfterSignInAndClearsAfterSignOut() = runTest {
        val authRepository = FakeProductsAuthRepository(authenticated = false)
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(
                listOf(userVehicle(id = "vehicle-1", brand = "BMW", type = "suv")),
            ),
        )
        val viewModel = productsBookingViewModel(
            authRepository = authRepository,
            vehicleRepository = vehicleRepository,
        )

        viewModel.refreshVehiclesForSession()
        runCurrent()

        assertIs<BookingVehiclesUiState.Unauthenticated>(viewModel.vehiclesState.value)
        assertEquals(0, vehicleRepository.listCalls)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshVehiclesForSession()
        runCurrent()

        assertIs<BookingVehiclesUiState.Loaded>(viewModel.vehiclesState.value)
        assertEquals(1, vehicleRepository.listCalls)

        authRepository.signOut()
        viewModel.refreshVehiclesForSession()
        runCurrent()

        assertIs<BookingVehiclesUiState.Unauthenticated>(viewModel.vehiclesState.value)
    }

    @Test
    fun refreshVehiclesForSessionReloadsWhenVehicleRevisionChanges() = runTest {
        val vehicleChangeNotifier = MutableUserVehicleChangeNotifier()
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(
                listOf(userVehicle(id = "vehicle-1", brand = "BMW")),
            ),
        )
        val viewModel = productsBookingViewModel(
            vehicleRepository = vehicleRepository,
            userVehicleChangeNotifier = vehicleChangeNotifier,
        )

        viewModel.refreshVehiclesForSession()
        runCurrent()

        assertIs<BookingVehiclesUiState.Loaded>(viewModel.vehiclesState.value)
        assertEquals(1, vehicleRepository.listCalls)

        viewModel.refreshVehiclesForSession()
        runCurrent()

        assertEquals(1, vehicleRepository.listCalls)

        vehicleChangeNotifier.notifyVehiclesChanged()
        viewModel.refreshVehiclesForSession()
        runCurrent()

        assertIs<BookingVehiclesUiState.Loaded>(viewModel.vehiclesState.value)
        assertEquals(2, vehicleRepository.listCalls)
    }

    @Test
    fun loadContactProfileRequiresAuthenticatedSession() = runTest {
        val profileRepository = FakeProductsProfileRepository(
            profileResult = UserProfileResult.Success(userProfile()),
        )
        val viewModel = productsBookingViewModel(
            authRepository = FakeProductsAuthRepository(authenticated = false),
            profileRepository = profileRepository,
        )

        viewModel.loadContactProfile()
        runCurrent()

        assertIs<BookingContactProfileUiState.Unauthenticated>(viewModel.contactProfileState.value)
        assertEquals(0, profileRepository.getCalls)
    }

    @Test
    fun loadContactProfileMapsAuthenticatedProfile() = runTest {
        val profileRepository = FakeProductsProfileRepository(
            profileResult = UserProfileResult.Success(
                userProfile(
                    displayName = "Bruno Ribeiro",
                    phoneNumber = "+351913005855",
                ),
            ),
        )
        val viewModel = productsBookingViewModel(profileRepository = profileRepository)

        viewModel.loadContactProfile()
        runCurrent()

        val loaded = assertIs<BookingContactProfileUiState.Loaded>(viewModel.contactProfileState.value)
        assertEquals("uid-1", loaded.profile.uid)
        assertEquals("Bruno Ribeiro", loaded.profile.displayName)
        assertEquals("bruno@example.com", loaded.profile.email)
        assertEquals("+351913005855", loaded.profile.phoneNumber)
        assertEquals(1, profileRepository.getCalls)
    }

    @Test
    fun refreshContactProfileReloadsWhenAuthenticatedUserChanges() = runTest {
        val authRepository = FakeProductsAuthRepository(authenticated = true)
        val profileRepository = FakeProductsProfileRepository(
            profileResult = UserProfileResult.Success(userProfile(uid = "uid-1")),
        )
        val viewModel = productsBookingViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )

        viewModel.refreshContactProfileForSession()
        runCurrent()
        assertIs<BookingContactProfileUiState.Loaded>(viewModel.contactProfileState.value)

        profileRepository.profileResult = UserProfileResult.Success(userProfile(uid = "uid-2", email = "ana@example.com"))
        authRepository.authenticate(uid = "uid-2")
        viewModel.refreshContactProfileForSession()
        runCurrent()

        val loaded = assertIs<BookingContactProfileUiState.Loaded>(viewModel.contactProfileState.value)
        assertEquals("uid-2", loaded.profile.uid)
        assertEquals("ana@example.com", loaded.profile.email)
        assertEquals(2, profileRepository.getCalls)
    }

    @Test
    fun loadContactProfileMapsBackendErrorToRetryableState() = runTest {
        val viewModel = productsBookingViewModel(
            profileRepository = FakeProductsProfileRepository(
                profileResult = UserProfileResult.Failure(
                    UserProfileError.Unavailable("Não foi possível carregar os dados pessoais."),
                ),
            ),
        )

        viewModel.loadContactProfile()
        runCurrent()

        val error = assertIs<BookingContactProfileUiState.Error>(viewModel.contactProfileState.value)
        assertEquals("Não foi possível carregar os dados pessoais.", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun submitBookingMapsConflictToChangeSlotResolution() = runTest {
        val viewModel = productsBookingViewModel(
            bookingRepository = FakeBookingRepository(
                availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
                createResult = BookingCreateResult.Failure(
                    BookingCreateError.Conflict("Este horário deixou de estar disponível."),
                ),
            ),
        )

        viewModel.submitBooking(validDraft())
        runCurrent()

        val error = assertIs<BookingSubmitUiState.Error>(viewModel.submitState.value)
        assertEquals(BookingSubmitResolution.ChangeSlot, error.resolution)
        assertEquals(false, error.retryable)
    }

    @Test
    fun submitBookingMapsUnavailableBackendToRetryResolution() = runTest {
        val viewModel = productsBookingViewModel(
            bookingRepository = FakeBookingRepository(
                availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
                createResult = BookingCreateResult.Failure(
                    BookingCreateError.Unavailable("Não foi possível contactar o serviço de marcações."),
                ),
            ),
        )

        viewModel.submitBooking(validDraft())
        runCurrent()

        val error = assertIs<BookingSubmitUiState.Error>(viewModel.submitState.value)
        assertEquals(BookingSubmitResolution.Retry, error.resolution)
        assertEquals(true, error.retryable)
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
    profileRepository: UserProfileRepository = FakeProductsProfileRepository(
        profileResult = UserProfileResult.Success(userProfile()),
    ),
    userVehicleChangeNotifier: MutableUserVehicleChangeNotifier = MutableUserVehicleChangeNotifier(),
): ProductsBookingViewModel = ProductsBookingViewModel(
    bookingRepository = bookingRepository,
    authRepository = authRepository,
    userVehicleRepository = vehicleRepository,
    userProfileRepository = profileRepository,
    userVehicleChangeNotifier = userVehicleChangeNotifier,
)

private class FakeBookingRepository(
    private val availabilityResult: BookingAvailabilityResult,
    private val createResult: BookingCreateResult = BookingCreateResult.Success(
        BookingReceipt(
            reservationId = "reservation-1",
            reservationCode = "SS-ABCDEFGH",
        ),
    ),
) : BookingRepository {
    var lastAvailabilityRequest: BookingAvailabilityRequest? = null
        private set

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        lastAvailabilityRequest = request
        return availabilityResult
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        return createResult
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

private class FakeProductsProfileRepository(
    var profileResult: UserProfileResult,
) : UserProfileRepository {
    var getCalls: Int = 0
        private set

    override suspend fun getMyProfile(): UserProfileResult {
        getCalls += 1
        return profileResult
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        error("Not used")
    }
}

private class FakeProductsAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) {
            authenticatedSession()
        } else {
            AuthSessionState.Unauthenticated
        },
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

private fun userProfile(
    uid: String = "uid-1",
    email: String = "bruno@example.com",
    displayName: String = "Bruno",
    phoneNumber: String = "",
): UserProfile = UserProfile(
    uid = uid,
    email = email,
    displayName = displayName,
    phoneNumber = phoneNumber,
    marketingOptIn = false,
)

private fun validDraft(): ProductsBookingDraft = ProductsBookingDraft(
    customerName = "Bruno Ribeiro",
    customerEmail = "bruno@example.com",
    customerPhone = "+351913005855",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    dateId = "2026-05-20",
    time = "09:30",
    serviceDurationMinutes = 45,
    vehicleType = "passenger",
    gdprConsent = true,
    notes = "",
)
