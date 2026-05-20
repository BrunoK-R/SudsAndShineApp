package com.sudsmobile.feature.profile

import com.sudsmobile.data.auth.AuthActionResult
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
import com.sudsmobile.data.profile.MutableUserProfileChangeNotifier
import com.sudsmobile.data.profile.UserProfile
import com.sudsmobile.data.profile.UserProfileMutationResult
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.profile.UserProfileResult
import com.sudsmobile.data.profile.UserProfileSaveRequest
import com.sudsmobile.data.vehicle.MutableUserVehicleChangeNotifier
import com.sudsmobile.data.vehicle.UserVehicle
import com.sudsmobile.data.vehicle.UserVehicleDeleteResult
import com.sudsmobile.data.vehicle.UserVehicleError
import com.sudsmobile.data.vehicle.UserVehicleListResult
import com.sudsmobile.data.vehicle.UserVehicleMutationResult
import com.sudsmobile.data.vehicle.UserVehicleRepository
import com.sudsmobile.data.vehicle.UserVehicleSaveRequest
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
class ProfileViewModelTest {
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
    fun loadStatsRequiresAuthenticatedSession() = runTest {
        val bookingRepository = ProfileStatsFakeBookingRepository(
            BookingHistoryResult.Success(BookingHistory(emptyList())),
        )
        val vehicleRepository = ProfileStatsFakeVehicleRepository(
            UserVehicleListResult.Success(emptyList()),
        )
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = false),
            bookingRepository = bookingRepository,
            userVehicleRepository = vehicleRepository,
            userProfileRepository = ProfileStatsFakeProfileRepository(
                UserProfileResult.Success(profilePreferencesProfile()),
            ),
        )

        viewModel.loadStats()
        runCurrent()

        assertIs<ProfileStatsUiState.Unauthenticated>(viewModel.statsState.value)
        assertEquals(0, bookingRepository.historyCalls)
        assertEquals(0, vehicleRepository.listCalls)
    }

    @Test
    fun loadStatsBuildsWashLoyaltyAndVehicleCountsFromUserData() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(
                            profileStatsHistoryReservation("completed-1", upcoming = false),
                            profileStatsHistoryReservation("completed-2", upcoming = false),
                            profileStatsHistoryReservation("completed-3", upcoming = false),
                            profileStatsHistoryReservation("completed-4", upcoming = false),
                            profileStatsHistoryReservation("completed-5", upcoming = false),
                            profileStatsHistoryReservation("completed-6", upcoming = false),
                            profileStatsHistoryReservation("completed-7", upcoming = false),
                            profileStatsHistoryReservation("upcoming-1", upcoming = true),
                            profileStatsHistoryReservation("cancelled-1", upcoming = false, status = "cancelled"),
                        ),
                    ),
                ),
            ),
            userVehicleRepository = ProfileStatsFakeVehicleRepository(
                UserVehicleListResult.Success(
                    listOf(
                        profileStatsVehicle("vehicle-1"),
                        profileStatsVehicle("vehicle-2"),
                        profileStatsVehicle("invalid-vehicle", plate = ""),
                    ),
                ),
            ),
            userProfileRepository = ProfileStatsFakeProfileRepository(
                UserProfileResult.Success(profilePreferencesProfile()),
            ),
        )

        viewModel.loadStats()
        runCurrent()

        val loaded = assertIs<ProfileStatsUiState.Loaded>(viewModel.statsState.value)
        assertEquals("7", loaded.stats.washCount)
        assertEquals("3", loaded.stats.loyaltyRemaining)
        assertEquals("2", loaded.stats.vehicleCount)
        assertEquals(null, loaded.warningMessage)
    }

    @Test
    fun loadStatsKeepsReservationStatsWhenVehiclesFail() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Success(
                    BookingHistory(
                        reservations = listOf(profileStatsHistoryReservation("completed-1", upcoming = false)),
                    ),
                ),
            ),
            userVehicleRepository = ProfileStatsFakeVehicleRepository(
                UserVehicleListResult.Failure(
                    UserVehicleError.Unavailable("Veículos indisponíveis."),
                ),
            ),
            userProfileRepository = ProfileStatsFakeProfileRepository(
                UserProfileResult.Success(profilePreferencesProfile()),
            ),
        )

        viewModel.loadStats()
        runCurrent()

        val loaded = assertIs<ProfileStatsUiState.Loaded>(viewModel.statsState.value)
        assertEquals("1", loaded.stats.washCount)
        assertEquals("4", loaded.stats.loyaltyRemaining)
        assertEquals("0", loaded.stats.vehicleCount)
        assertEquals("Veículos indisponíveis.", loaded.warningMessage)
        assertEquals(true, loaded.warningRetryable)
    }

    @Test
    fun refreshForSessionReloadsWhenBookingOrVehicleRevisionChanges() = runTest {
        val bookingChangeNotifier = MutableBookingChangeNotifier()
        val vehicleChangeNotifier = MutableUserVehicleChangeNotifier()
        val bookingRepository = ProfileStatsFakeBookingRepository(
            BookingHistoryResult.Success(
                BookingHistory(
                    reservations = listOf(profileStatsHistoryReservation("completed-1", upcoming = false)),
                ),
            ),
        )
        val vehicleRepository = ProfileStatsFakeVehicleRepository(
            UserVehicleListResult.Success(listOf(profileStatsVehicle("vehicle-1"))),
        )
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = bookingRepository,
            userVehicleRepository = vehicleRepository,
            userProfileRepository = ProfileStatsFakeProfileRepository(
                UserProfileResult.Success(profilePreferencesProfile()),
            ),
            bookingChangeNotifier = bookingChangeNotifier,
            userVehicleChangeNotifier = vehicleChangeNotifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileStatsUiState.Loaded>(viewModel.statsState.value)
        assertEquals(1, bookingRepository.historyCalls)
        assertEquals(1, vehicleRepository.listCalls)

        viewModel.refreshForSession()
        runCurrent()

        assertEquals(1, bookingRepository.historyCalls)
        assertEquals(1, vehicleRepository.listCalls)

        bookingChangeNotifier.notifyBookingsChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileStatsUiState.Loaded>(viewModel.statsState.value)
        assertEquals(2, bookingRepository.historyCalls)
        assertEquals(2, vehicleRepository.listCalls)

        vehicleChangeNotifier.notifyVehiclesChanged()
        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfileStatsUiState.Loaded>(viewModel.statsState.value)
        assertEquals(3, bookingRepository.historyCalls)
        assertEquals(3, vehicleRepository.listCalls)
    }

    @Test
    fun loadStatsMapsBackendErrorAsRetryable() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Failure(
                    BookingHistoryError.Unavailable("Serviço indisponível."),
                ),
            ),
            userVehicleRepository = ProfileStatsFakeVehicleRepository(
                UserVehicleListResult.Success(listOf(profileStatsVehicle("vehicle-1"))),
            ),
            userProfileRepository = ProfileStatsFakeProfileRepository(
                UserProfileResult.Success(profilePreferencesProfile()),
            ),
        )

        viewModel.loadStats()
        runCurrent()

        val error = assertIs<ProfileStatsUiState.Error>(viewModel.statsState.value)
        assertEquals("Serviço indisponível.", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun signOutWhileStatsLoadIsInFlightDoesNotPublishOldStats() = runTest {
        val authRepository = ProfileStatsFakeAuthRepository(authenticated = true)
        val bookingRepository = DeferredProfileStatsBookingRepository()
        val viewModel = ProfileViewModel(
            authRepository = authRepository,
            bookingRepository = bookingRepository,
            userVehicleRepository = ProfileStatsFakeVehicleRepository(
                UserVehicleListResult.Success(listOf(profileStatsVehicle("vehicle-1"))),
            ),
            userProfileRepository = ProfileStatsFakeProfileRepository(
                UserProfileResult.Success(profilePreferencesProfile()),
            ),
        )

        viewModel.loadStats()
        runCurrent()

        assertIs<ProfileStatsUiState.Loading>(viewModel.statsState.value)

        viewModel.signOut()
        bookingRepository.result.complete(
            BookingHistoryResult.Success(
                BookingHistory(
                    listOf(profileStatsHistoryReservation("completed-1", upcoming = false)),
                ),
            ),
        )
        runCurrent()

        assertIs<ProfileStatsUiState.Unauthenticated>(viewModel.statsState.value)
    }

    @Test
    fun loadPreferencesRequiresAuthenticatedSession() = runTest {
        val profileRepository = ProfileStatsFakeProfileRepository(
            UserProfileResult.Success(profilePreferencesProfile(marketingOptIn = true)),
        )
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = false),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Success(BookingHistory(emptyList())),
            ),
            userVehicleRepository = ProfileStatsFakeVehicleRepository(UserVehicleListResult.Success(emptyList())),
            userProfileRepository = profileRepository,
        )

        viewModel.loadPreferences()
        runCurrent()

        assertIs<ProfilePreferencesUiState.Unauthenticated>(viewModel.preferencesState.value)
        assertEquals(0, profileRepository.loadCalls)
    }

    @Test
    fun loadPreferencesMapsMarketingOptInFromProfile() = runTest {
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Success(BookingHistory(emptyList())),
            ),
            userVehicleRepository = ProfileStatsFakeVehicleRepository(UserVehicleListResult.Success(emptyList())),
            userProfileRepository = ProfileStatsFakeProfileRepository(
                UserProfileResult.Success(profilePreferencesProfile(marketingOptIn = true)),
            ),
        )

        viewModel.loadPreferences()
        runCurrent()

        val loaded = assertIs<ProfilePreferencesUiState.Loaded>(viewModel.preferencesState.value)
        assertEquals(true, loaded.preferences.marketingOptIn)
    }

    @Test
    fun updateMarketingOptInSavesProfilePreference() = runTest {
        val profileRepository = ProfileStatsFakeProfileRepository(
            profileResult = UserProfileResult.Success(profilePreferencesProfile(marketingOptIn = false)),
            mutationResult = UserProfileMutationResult.Success(profilePreferencesProfile(marketingOptIn = true)),
        )
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Success(BookingHistory(emptyList())),
            ),
            userVehicleRepository = ProfileStatsFakeVehicleRepository(UserVehicleListResult.Success(emptyList())),
            userProfileRepository = profileRepository,
        )

        viewModel.loadPreferences()
        runCurrent()
        viewModel.updateMarketingOptIn(true)
        runCurrent()

        val saved = assertIs<ProfilePreferencesUiState.Saved>(viewModel.preferencesState.value)
        assertEquals(true, saved.preferences.marketingOptIn)
        assertEquals(true, profileRepository.lastRequest?.marketingOptIn)
        assertEquals("Bruno Ribeiro", profileRepository.lastRequest?.displayName)
        assertEquals(1, profileRepository.updateCalls)
    }

    @Test
    fun updateMarketingOptInValidatesIncompleteProfileBeforeSaving() = runTest {
        val profileRepository = ProfileStatsFakeProfileRepository(
            UserProfileResult.Success(profilePreferencesProfile(phoneNumber = "")),
        )
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Success(BookingHistory(emptyList())),
            ),
            userVehicleRepository = ProfileStatsFakeVehicleRepository(UserVehicleListResult.Success(emptyList())),
            userProfileRepository = profileRepository,
        )

        viewModel.loadPreferences()
        runCurrent()
        viewModel.updateMarketingOptIn(true)
        runCurrent()

        val error = assertIs<ProfilePreferencesUiState.SaveError>(viewModel.preferencesState.value)
        assertEquals(false, error.retryable)
        assertEquals(0, profileRepository.updateCalls)
    }

    @Test
    fun refreshForSessionReloadsPreferencesWhenProfileRevisionChanges() = runTest {
        val profileChangeNotifier = MutableUserProfileChangeNotifier()
        val profileRepository = ProfileStatsFakeProfileRepository(
            UserProfileResult.Success(profilePreferencesProfile(marketingOptIn = false)),
        )
        val viewModel = ProfileViewModel(
            authRepository = ProfileStatsFakeAuthRepository(authenticated = true),
            bookingRepository = ProfileStatsFakeBookingRepository(
                BookingHistoryResult.Success(BookingHistory(emptyList())),
            ),
            userVehicleRepository = ProfileStatsFakeVehicleRepository(UserVehicleListResult.Success(emptyList())),
            userProfileRepository = profileRepository,
            userProfileChangeNotifier = profileChangeNotifier,
        )

        viewModel.refreshForSession()
        runCurrent()

        assertIs<ProfilePreferencesUiState.Loaded>(viewModel.preferencesState.value)
        assertEquals(1, profileRepository.loadCalls)

        viewModel.refreshForSession()
        runCurrent()

        assertEquals(1, profileRepository.loadCalls)

        profileRepository.profileResult = UserProfileResult.Success(profilePreferencesProfile(marketingOptIn = true))
        profileChangeNotifier.notifyProfileChanged()
        viewModel.refreshForSession()
        runCurrent()

        val loaded = assertIs<ProfilePreferencesUiState.Loaded>(viewModel.preferencesState.value)
        assertEquals(true, loaded.preferences.marketingOptIn)
        assertEquals(2, profileRepository.loadCalls)
    }
}

private class ProfileStatsFakeBookingRepository(
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

private class DeferredProfileStatsBookingRepository : BookingRepository {
    val result = CompletableDeferred<BookingHistoryResult>()

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        error("Not used")
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        error("Not used")
    }

    override suspend fun getMyBookings(): BookingHistoryResult = result.await()
}

private class ProfileStatsFakeVehicleRepository(
    private val vehicleResult: UserVehicleListResult,
) : UserVehicleRepository {
    var listCalls: Int = 0
        private set

    override suspend fun getMyVehicles(): UserVehicleListResult {
        listCalls += 1
        return vehicleResult
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

private class ProfileStatsFakeProfileRepository(
    var profileResult: UserProfileResult,
    private val mutationResult: UserProfileMutationResult = UserProfileMutationResult.Success(
        profilePreferencesProfile(),
    ),
) : UserProfileRepository {
    var loadCalls: Int = 0
        private set
    var updateCalls: Int = 0
        private set
    var lastRequest: UserProfileSaveRequest? = null
        private set

    override suspend fun getMyProfile(): UserProfileResult {
        loadCalls += 1
        return profileResult
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        updateCalls += 1
        lastRequest = request
        return mutationResult
    }
}

private class ProfileStatsFakeAuthRepository(
    authenticated: Boolean,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        if (authenticated) {
            AuthSessionState.Authenticated(
                AuthSession(
                    user = AuthUser(
                        uid = "uid-1",
                        email = "bruno@example.com",
                        displayName = "Bruno Ribeiro",
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

    override val sessionState: StateFlow<AuthSessionState> = mutableSessionState

    override suspend fun currentSession(): AuthSession? {
        return (mutableSessionState.value as? AuthSessionState.Authenticated)?.session
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

    override fun signOut() {
        mutableSessionState.value = AuthSessionState.Unauthenticated
    }
}

private fun profileStatsHistoryReservation(
    id: String,
    upcoming: Boolean,
    status: String = if (upcoming) "pending" else "completed",
): BookingHistoryReservation = BookingHistoryReservation(
    id = id,
    reservationCode = "SS-$id",
    serviceId = "premium",
    serviceName = "Lavagem Premium",
    slotStartIso = "2026-05-20T09:30:00.000Z",
    slotEndIso = "2026-05-20T10:15:00.000Z",
    status = status,
    vehicleType = "passageiros",
    priceCents = 3200,
    upcoming = upcoming,
)

private fun profileStatsVehicle(
    id: String,
    plate: String = "AA-00-BB",
): UserVehicle = UserVehicle(
    id = id,
    brand = "BMW",
    model = "320d",
    plate = plate,
    color = "Azul",
    type = "passenger",
)

private fun profilePreferencesProfile(
    displayName: String = "Bruno Ribeiro",
    phoneNumber: String = "913005855",
    marketingOptIn: Boolean = false,
): UserProfile = UserProfile(
    uid = "uid-1",
    email = "bruno@example.com",
    displayName = displayName,
    phoneNumber = phoneNumber,
    marketingOptIn = marketingOptIn,
)
