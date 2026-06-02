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
import com.sudsmobile.data.booking.BookingLoyalty
import com.sudsmobile.data.booking.BookingLoyaltyError
import com.sudsmobile.data.booking.BookingLoyaltyRedemption
import com.sudsmobile.data.booking.BookingLoyaltyResult
import com.sudsmobile.data.booking.BookingLoyaltyStamp
import com.sudsmobile.data.booking.BookingLoyaltySummary
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.business.BusinessFaq
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.BusinessOpeningHours
import com.sudsmobile.data.business.BusinessStat
import com.sudsmobile.data.business.DefaultBusinessInfo
import com.sudsmobile.data.profile.UserProfile
import com.sudsmobile.data.profile.UserProfileError
import com.sudsmobile.data.profile.UserProfileMutationResult
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.profile.UserProfileResult
import com.sudsmobile.data.profile.UserProfileSaveRequest
import com.sudsmobile.data.auth.AuthActionResult
import com.sudsmobile.data.auth.AuthError
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
        assertEquals(null, repository.lastAvailabilityRequest?.slotIntervalMinutes)
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
    fun availabilitySlotCapacityLabelMapsBackendCapacity() {
        assertEquals(
            "1 vaga",
            BookingAvailabilitySlot(time = "09:00", available = true, remainingCapacity = 1).capacityLabel(),
        )
        assertEquals(
            "3 vagas",
            BookingAvailabilitySlot(time = "10:00", available = true, remainingCapacity = 3).capacityLabel(),
        )
        assertEquals(
            "Cheio",
            BookingAvailabilitySlot(time = "11:00", available = false, remainingCapacity = 0).capacityLabel(),
        )
        assertEquals(
            "Indisponível",
            BookingAvailabilitySlot(time = "12:00", available = false, remainingCapacity = 2).capacityLabel(),
        )
    }

    @Test
    fun loadAvailabilityAcceptsNewMonthWhilePreviousRequestIsLoading() = runTest {
        val firstResult = CompletableDeferred<BookingAvailabilityResult>()
        val secondResult = CompletableDeferred<BookingAvailabilityResult>()
        val repository = DeferredAvailabilityBookingRepository(firstResult, secondResult)
        val viewModel = productsBookingViewModel(repository)

        viewModel.loadAvailability(serviceDurationMinutes = 30, anchorDate = "2026-05-01")
        runCurrent()

        assertIs<BookingAvailabilityUiState.Loading>(viewModel.availabilityState.value)
        assertEquals("2026-05-01", repository.requests.single().anchorDate)

        viewModel.loadAvailability(serviceDurationMinutes = 45, anchorDate = "2026-06-01")
        runCurrent()

        assertEquals(2, repository.requests.size)
        assertEquals("2026-06-01", repository.requests[1].anchorDate)
        assertEquals(45, repository.requests[1].serviceDurationMinutes)

        secondResult.complete(
            BookingAvailabilityResult.Success(availableMonth("junho 2026", "2026-06-01")),
        )
        runCurrent()

        val loaded = assertIs<BookingAvailabilityUiState.Loaded>(viewModel.availabilityState.value)
        assertEquals("junho 2026", loaded.month.monthTitle)

        firstResult.complete(
            BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
        )
        runCurrent()

        val stillLoaded = assertIs<BookingAvailabilityUiState.Loaded>(viewModel.availabilityState.value)
        assertEquals("junho 2026", stillLoaded.month.monthTitle)
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
    fun loadVehiclesWaitsWhileSessionIsRestoringWithoutRepositoryCall() = runTest {
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(listOf(userVehicle())),
        )
        val viewModel = productsBookingViewModel(
            authRepository = FakeProductsAuthRepository(initialState = AuthSessionState.Restoring),
            vehicleRepository = vehicleRepository,
        )

        viewModel.loadVehicles()
        runCurrent()

        assertIs<BookingVehiclesUiState.Loading>(viewModel.vehiclesState.value)
        assertEquals(0, vehicleRepository.listCalls)
    }

    @Test
    fun loadVehiclesMapsRestoreFailureToRetryableState() = runTest {
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(listOf(userVehicle())),
        )
        val viewModel = productsBookingViewModel(
            authRepository = FakeProductsAuthRepository(
                initialState = AuthSessionState.RestoreFailed(
                    AuthError.Unavailable("Sessão indisponível."),
                ),
            ),
            vehicleRepository = vehicleRepository,
        )

        viewModel.loadVehicles()
        runCurrent()

        val error = assertIs<BookingVehiclesUiState.Error>(viewModel.vehiclesState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(0, vehicleRepository.listCalls)
    }

    @Test
    fun loadVehiclesMapsSavedVehiclesToBookingOptions() = runTest {
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(
                listOf(userVehicle(id = "vehicle-1", brand = "BMW", type = "suv", isDefault = true)),
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
        assertEquals(true, loaded.vehicles.single().isDefault)
    }

    @Test
    fun loadVehiclesSortsDefaultVehicleFirst() = runTest {
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(
                listOf(
                    userVehicle(id = "vehicle-1", brand = "Audi"),
                    userVehicle(id = "vehicle-2", brand = "BMW", isDefault = true),
                ),
            ),
        )
        val viewModel = productsBookingViewModel(vehicleRepository = vehicleRepository)

        viewModel.loadVehicles()
        runCurrent()

        val loaded = assertIs<BookingVehiclesUiState.Loaded>(viewModel.vehiclesState.value)
        assertEquals("saved:vehicle-2", loaded.vehicles.first().id)
        assertEquals(true, loaded.vehicles.first().isDefault)
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
    fun loadVehiclesKeepsLatestRevisionWhenOlderRequestCompletesLast() = runTest {
        val vehicleChangeNotifier = MutableUserVehicleChangeNotifier()
        val firstResult = CompletableDeferred<UserVehicleListResult>()
        val secondResult = CompletableDeferred<UserVehicleListResult>()
        val vehicleRepository = DeferredProductsVehicleRepository(firstResult, secondResult)
        val viewModel = productsBookingViewModel(
            vehicleRepository = vehicleRepository,
            userVehicleChangeNotifier = vehicleChangeNotifier,
        )

        viewModel.loadVehicles()
        runCurrent()

        assertIs<BookingVehiclesUiState.Loading>(viewModel.vehiclesState.value)
        assertEquals(1, vehicleRepository.listCalls)

        vehicleChangeNotifier.notifyVehiclesChanged()
        viewModel.refreshVehiclesForSession()
        runCurrent()

        assertEquals(2, vehicleRepository.listCalls)

        secondResult.complete(
            UserVehicleListResult.Success(listOf(userVehicle(id = "vehicle-2", brand = "Mercedes"))),
        )
        runCurrent()

        val loaded = assertIs<BookingVehiclesUiState.Loaded>(viewModel.vehiclesState.value)
        assertEquals("saved:vehicle-2", loaded.vehicles.single().id)
        assertEquals("Mercedes 320d", loaded.vehicles.single().name)

        firstResult.complete(
            UserVehicleListResult.Success(listOf(userVehicle(id = "vehicle-1", brand = "BMW"))),
        )
        runCurrent()

        val stillLoaded = assertIs<BookingVehiclesUiState.Loaded>(viewModel.vehiclesState.value)
        assertEquals("saved:vehicle-2", stillLoaded.vehicles.single().id)
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
    fun loadContactProfileWaitsWhileSessionIsRestoringWithoutRepositoryCall() = runTest {
        val profileRepository = FakeProductsProfileRepository(
            profileResult = UserProfileResult.Success(userProfile()),
        )
        val viewModel = productsBookingViewModel(
            authRepository = FakeProductsAuthRepository(initialState = AuthSessionState.Restoring),
            profileRepository = profileRepository,
        )

        viewModel.loadContactProfile()
        runCurrent()

        assertIs<BookingContactProfileUiState.Loading>(viewModel.contactProfileState.value)
        assertEquals(0, profileRepository.getCalls)
    }

    @Test
    fun loadContactProfileMapsRestoreFailureToRetryableState() = runTest {
        val profileRepository = FakeProductsProfileRepository(
            profileResult = UserProfileResult.Success(userProfile()),
        )
        val viewModel = productsBookingViewModel(
            authRepository = FakeProductsAuthRepository(
                initialState = AuthSessionState.RestoreFailed(
                    AuthError.Backend("Falha ao validar sessão."),
                ),
            ),
            profileRepository = profileRepository,
        )

        viewModel.loadContactProfile()
        runCurrent()

        val error = assertIs<BookingContactProfileUiState.Error>(viewModel.contactProfileState.value)
        assertEquals("Falha ao validar sessão.", error.message)
        assertEquals(true, error.retryable)
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
    fun refreshContactProfileLoadsNewUserWhenOlderRequestIsInFlight() = runTest {
        val authRepository = FakeProductsAuthRepository(authenticated = true)
        val firstResult = CompletableDeferred<UserProfileResult>()
        val secondResult = CompletableDeferred<UserProfileResult>()
        val profileRepository = DeferredProductsProfileRepository(firstResult, secondResult)
        val viewModel = productsBookingViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )

        viewModel.loadContactProfile()
        runCurrent()

        assertIs<BookingContactProfileUiState.Loading>(viewModel.contactProfileState.value)
        assertEquals(1, profileRepository.getCalls)

        authRepository.authenticate(uid = "uid-2")
        viewModel.refreshContactProfileForSession()
        runCurrent()

        assertEquals(2, profileRepository.getCalls)

        secondResult.complete(UserProfileResult.Success(userProfile(uid = "uid-2", email = "ana@example.com")))
        runCurrent()

        val loaded = assertIs<BookingContactProfileUiState.Loaded>(viewModel.contactProfileState.value)
        assertEquals("uid-2", loaded.profile.uid)
        assertEquals("ana@example.com", loaded.profile.email)

        firstResult.complete(UserProfileResult.Success(userProfile(uid = "uid-1", email = "bruno@example.com")))
        runCurrent()

        val stillLoaded = assertIs<BookingContactProfileUiState.Loaded>(viewModel.contactProfileState.value)
        assertEquals("uid-2", stillLoaded.profile.uid)
        assertEquals("ana@example.com", stillLoaded.profile.email)
    }

    @Test
    fun refreshContactProfileInvalidatesInFlightRequestAfterSignOut() = runTest {
        val authRepository = FakeProductsAuthRepository(authenticated = true)
        val firstResult = CompletableDeferred<UserProfileResult>()
        val secondResult = CompletableDeferred<UserProfileResult>()
        val profileRepository = DeferredProductsProfileRepository(firstResult, secondResult)
        val viewModel = productsBookingViewModel(
            authRepository = authRepository,
            profileRepository = profileRepository,
        )

        viewModel.loadContactProfile()
        runCurrent()

        authRepository.signOut()
        viewModel.refreshContactProfileForSession()
        runCurrent()

        assertIs<BookingContactProfileUiState.Unauthenticated>(viewModel.contactProfileState.value)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshContactProfileForSession()
        runCurrent()

        assertEquals(2, profileRepository.getCalls)

        secondResult.complete(UserProfileResult.Success(userProfile(uid = "uid-1", phoneNumber = "+351900000002")))
        runCurrent()

        val loaded = assertIs<BookingContactProfileUiState.Loaded>(viewModel.contactProfileState.value)
        assertEquals("+351900000002", loaded.profile.phoneNumber)

        firstResult.complete(UserProfileResult.Success(userProfile(uid = "uid-1", phoneNumber = "+351900000001")))
        runCurrent()

        val stillLoaded = assertIs<BookingContactProfileUiState.Loaded>(viewModel.contactProfileState.value)
        assertEquals("+351900000002", stillLoaded.profile.phoneNumber)
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
    fun loadBusinessInfoMapsBackendContactDetailsForBookingSummary() = runTest {
        val businessRepository = FakeProductsBusinessInfoRepository(
            result = BusinessInfoResult.Success(
                businessInfo(
                    phone = "244 000 111",
                    addressLine1 = "Rua Nova 10",
                    addressLine2 = "Leiria",
                    openingHours = listOf(
                        BusinessOpeningHours(
                            dayLabel = "Dias úteis",
                            hoursLabel = "10:00 - 18:00",
                            closed = false,
                        ),
                        BusinessOpeningHours(
                            dayLabel = "Domingo",
                            hoursLabel = "Encerrado",
                            closed = true,
                        ),
                    ),
                ),
            ),
        )
        val viewModel = productsBookingViewModel(businessRepository = businessRepository)

        viewModel.loadBusinessInfo()
        runCurrent()

        val loaded = assertIs<BookingBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("244 000 111", loaded.info.phone)
        assertEquals("Rua Nova 10", loaded.info.addressLine1)
        assertEquals("Leiria", loaded.info.addressLine2)
        assertEquals("Dias úteis", loaded.info.openingHours.first().dayLabel)
        assertEquals("10:00 - 18:00", loaded.info.openingHours.first().hoursLabel)
        assertEquals(true, loaded.info.openingHours.last().closed)
        assertEquals(1, businessRepository.calls)
    }

    @Test
    fun loadBusinessInfoUsesDefaultFallbackOnRetryableBackendError() = runTest {
        val viewModel = productsBookingViewModel(
            businessRepository = FakeProductsBusinessInfoRepository(
                result = BusinessInfoResult.Failure(
                    BusinessInfoError.Unavailable("O serviço de contactos está indisponível."),
                ),
            ),
        )

        viewModel.loadBusinessInfo()
        runCurrent()

        val error = assertIs<BookingBusinessInfoUiState.Error>(viewModel.businessInfoState.value)
        assertEquals("O serviço de contactos está indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(DefaultBusinessInfo.phone, error.fallbackInfo.phone)
        assertEquals(DefaultBusinessInfo.addressLine1, error.fallbackInfo.addressLine1)
        assertEquals(DefaultBusinessInfo.openingHours.first().dayLabel, error.fallbackInfo.openingHours.first().dayLabel)
    }

    @Test
    fun forcedBusinessInfoRefreshKeepsLatestBookingSummaryResponse() = runTest {
        val oldResult = CompletableDeferred<BusinessInfoResult>()
        val newResult = CompletableDeferred<BusinessInfoResult>()
        val businessRepository = DeferredProductsBusinessInfoRepository(oldResult, newResult)
        val viewModel = productsBookingViewModel(businessRepository = businessRepository)

        viewModel.loadBusinessInfo()
        runCurrent()
        viewModel.loadBusinessInfo(force = true)
        runCurrent()

        assertIs<BookingBusinessInfoUiState.Loading>(viewModel.businessInfoState.value)
        assertEquals(2, businessRepository.calls)

        newResult.complete(
            BusinessInfoResult.Success(businessInfo(phone = "244 000 222")),
        )
        runCurrent()

        val latest = assertIs<BookingBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("244 000 222", latest.info.phone)

        oldResult.complete(
            BusinessInfoResult.Success(businessInfo(phone = "244 000 111")),
        )
        runCurrent()

        val stillLatest = assertIs<BookingBusinessInfoUiState.Loaded>(viewModel.businessInfoState.value)
        assertEquals("244 000 222", stillLatest.info.phone)
    }

    @Test
    fun loadRewardsRequiresAuthenticatedSession() = runTest {
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
            loyaltyResult = BookingLoyaltyResult.Success(bookingLoyalty()),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = FakeProductsAuthRepository(authenticated = false),
        )

        viewModel.loadRewards()
        runCurrent()

        assertIs<BookingRewardsUiState.Unauthenticated>(viewModel.rewardsState.value)
        assertEquals(0, repository.loyaltyCalls)
    }

    @Test
    fun loadRewardsWaitsWhileSessionIsRestoringWithoutRepositoryCall() = runTest {
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
            loyaltyResult = BookingLoyaltyResult.Success(bookingLoyalty()),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = FakeProductsAuthRepository(initialState = AuthSessionState.Restoring),
        )

        viewModel.loadRewards()
        runCurrent()

        assertIs<BookingRewardsUiState.Loading>(viewModel.rewardsState.value)
        assertEquals(0, repository.loyaltyCalls)
    }

    @Test
    fun loadRewardsMapsRestoreFailureToRetryableState() = runTest {
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
            loyaltyResult = BookingLoyaltyResult.Success(bookingLoyalty()),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = FakeProductsAuthRepository(
                initialState = AuthSessionState.RestoreFailed(
                    AuthError.Unavailable("Sessão indisponível."),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val error = assertIs<BookingRewardsUiState.Error>(viewModel.rewardsState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(0, repository.loyaltyCalls)
    }

    @Test
    fun loadRewardsShowsOnlyIssuedRewardCodes() = runTest {
        val viewModel = productsBookingViewModel(
            bookingRepository = FakeBookingRepository(
                availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
                loyaltyResult = BookingLoyaltyResult.Success(
                    bookingLoyalty(
                        redemptions = listOf(
                            bookingRedemption(
                                id = "reward-1",
                                rewardCode = "SS-FREE-UID1-0001",
                                status = "issued",
                                createdAtIso = "2026-05-20T10:00:00.000Z",
                            ),
                            bookingRedemption(
                                id = "reward-2",
                                rewardCode = "SS-FREE-UID1-0002",
                                status = "redeemed",
                                createdAtIso = "2026-05-21T10:00:00.000Z",
                            ),
                        ),
                    ),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val loaded = assertIs<BookingRewardsUiState.Loaded>(viewModel.rewardsState.value)
        assertEquals(1, loaded.rewardCodes.size)
        assertEquals("SS-FREE-UID1-0001", loaded.rewardCodes.single().code)
        assertEquals("Disponível", loaded.rewardCodes.single().statusLabel)
        assertEquals("Emitida em 20 de maio, 2026", loaded.rewardCodes.single().issuedAt)
    }

    @Test
    fun refreshRewardsForSessionReloadsAfterRevisionChangesAndClearsAfterSignOut() = runTest {
        val authRepository = FakeProductsAuthRepository(authenticated = true)
        val bookingChangeNotifier = MutableBookingChangeNotifier()
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
            loyaltyResult = BookingLoyaltyResult.Success(bookingLoyalty(redemptions = listOf(bookingRedemption()))),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
            bookingChangeNotifier = bookingChangeNotifier,
        )

        viewModel.refreshRewardsForSession()
        runCurrent()

        assertIs<BookingRewardsUiState.Loaded>(viewModel.rewardsState.value)
        assertEquals(1, repository.loyaltyCalls)

        viewModel.refreshRewardsForSession()
        runCurrent()

        assertEquals(1, repository.loyaltyCalls)

        bookingChangeNotifier.notifyBookingsChanged()
        viewModel.refreshRewardsForSession()
        runCurrent()

        assertIs<BookingRewardsUiState.Loaded>(viewModel.rewardsState.value)
        assertEquals(2, repository.loyaltyCalls)

        authRepository.signOut()
        viewModel.refreshRewardsForSession()
        runCurrent()

        assertIs<BookingRewardsUiState.Unauthenticated>(viewModel.rewardsState.value)
    }

    @Test
    fun loadRewardsMapsBackendErrorToRetryableState() = runTest {
        val viewModel = productsBookingViewModel(
            bookingRepository = FakeBookingRepository(
                availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
                loyaltyResult = BookingLoyaltyResult.Failure(
                    BookingLoyaltyError.Unavailable("Não foi possível carregar recompensas."),
                ),
            ),
        )

        viewModel.loadRewards()
        runCurrent()

        val error = assertIs<BookingRewardsUiState.Error>(viewModel.rewardsState.value)
        assertEquals("Não foi possível carregar recompensas.", error.message)
        assertEquals(true, error.retryable)
    }

    @Test
    fun userSpecificLoadsKeepRestoringStateWhenSessionChangesDuringRequests() = runTest {
        val authRepository = FakeProductsAuthRepository(authenticated = true)
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(listOf(userVehicle())),
        )
        val profileRepository = FakeProductsProfileRepository(
            profileResult = UserProfileResult.Success(userProfile()),
        )
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
            loyaltyResult = BookingLoyaltyResult.Success(bookingLoyalty(redemptions = listOf(bookingRedemption()))),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
            vehicleRepository = vehicleRepository,
            profileRepository = profileRepository,
        )

        viewModel.loadVehicles()
        viewModel.loadContactProfile()
        viewModel.loadRewards()
        authRepository.setSessionState(AuthSessionState.Restoring)
        runCurrent()

        assertIs<BookingVehiclesUiState.Loading>(viewModel.vehiclesState.value)
        assertIs<BookingContactProfileUiState.Loading>(viewModel.contactProfileState.value)
        assertIs<BookingRewardsUiState.Loading>(viewModel.rewardsState.value)
        assertEquals(1, vehicleRepository.listCalls)
        assertEquals(1, profileRepository.getCalls)
        assertEquals(1, repository.loyaltyCalls)
    }

    @Test
    fun userSpecificRefreshesLoadAfterRestoringSessionCompletes() = runTest {
        val authRepository = FakeProductsAuthRepository(initialState = AuthSessionState.Restoring)
        val vehicleRepository = FakeProductsVehicleRepository(
            listResult = UserVehicleListResult.Success(listOf(userVehicle())),
        )
        val profileRepository = FakeProductsProfileRepository(
            profileResult = UserProfileResult.Success(userProfile()),
        )
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
            loyaltyResult = BookingLoyaltyResult.Success(bookingLoyalty(redemptions = listOf(bookingRedemption()))),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
            vehicleRepository = vehicleRepository,
            profileRepository = profileRepository,
        )

        viewModel.refreshVehiclesForSession()
        viewModel.refreshContactProfileForSession()
        viewModel.refreshRewardsForSession()

        assertIs<BookingVehiclesUiState.Loading>(viewModel.vehiclesState.value)
        assertIs<BookingContactProfileUiState.Loading>(viewModel.contactProfileState.value)
        assertIs<BookingRewardsUiState.Loading>(viewModel.rewardsState.value)
        assertEquals(0, vehicleRepository.listCalls)
        assertEquals(0, profileRepository.getCalls)
        assertEquals(0, repository.loyaltyCalls)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshVehiclesForSession()
        viewModel.refreshContactProfileForSession()
        viewModel.refreshRewardsForSession()
        runCurrent()

        assertIs<BookingVehiclesUiState.Loaded>(viewModel.vehiclesState.value)
        assertIs<BookingContactProfileUiState.Loaded>(viewModel.contactProfileState.value)
        assertIs<BookingRewardsUiState.Loaded>(viewModel.rewardsState.value)
        assertEquals(1, vehicleRepository.listCalls)
        assertEquals(1, profileRepository.getCalls)
        assertEquals(1, repository.loyaltyCalls)
    }

    @Test
    fun submitBookingWaitsForRestoringSessionBeforeRepositoryCall() = runTest {
        val authRepository = FakeProductsAuthRepository(initialState = AuthSessionState.Restoring)
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
        )

        viewModel.submitBooking(validDraft())
        runCurrent()

        assertIs<BookingSubmitUiState.Loading>(viewModel.submitState.value)
        assertEquals(0, repository.createCalls)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshSubmitForSession()
        runCurrent()

        assertIs<BookingSubmitUiState.Success>(viewModel.submitState.value)
        assertEquals(1, repository.createCalls)
    }

    @Test
    fun submitBookingMapsRestoreFailureWithoutRepositoryCall() = runTest {
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = FakeProductsAuthRepository(
                initialState = AuthSessionState.RestoreFailed(
                    AuthError.Unavailable("Sessão indisponível."),
                ),
            ),
        )

        viewModel.submitBooking(validDraft())
        runCurrent()

        val error = assertIs<BookingSubmitUiState.Error>(viewModel.submitState.value)
        assertEquals("Sessão indisponível.", error.message)
        assertEquals(true, error.retryable)
        assertEquals(BookingSubmitResolution.Retry, error.resolution)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun submitBookingRequiresSignInForSavedVehicleWhenUnauthenticated() = runTest {
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = FakeProductsAuthRepository(authenticated = false),
        )

        viewModel.submitBooking(
            validDraft().copy(
                userVehicleId = "vehicle-1",
                vehicleLabel = "BMW Serie 1",
            ),
        )
        runCurrent()

        val error = assertIs<BookingSubmitUiState.Error>(viewModel.submitState.value)
        assertEquals(BookingSubmitResolution.SignIn, error.resolution)
        assertEquals(false, error.retryable)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun submitBookingDefersIfSessionStartsRestoringBeforeCreateRuns() = runTest {
        val authRepository = FakeProductsAuthRepository(authenticated = true)
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
        )

        viewModel.submitBooking(validDraft())
        authRepository.setSessionState(AuthSessionState.Restoring)
        runCurrent()

        assertIs<BookingSubmitUiState.Loading>(viewModel.submitState.value)
        assertEquals(0, repository.createCalls)

        authRepository.authenticate(uid = "uid-1")
        viewModel.refreshSubmitForSession()
        runCurrent()

        assertIs<BookingSubmitUiState.Success>(viewModel.submitState.value)
        assertEquals(1, repository.createCalls)
    }

    @Test
    fun submitBookingRejectsSessionUserChangeBeforeCreateRuns() = runTest {
        val authRepository = FakeProductsAuthRepository(initialState = authenticatedSession(uid = "uid-1"))
        val repository = FakeBookingRepository(
            availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
        )
        val viewModel = productsBookingViewModel(
            bookingRepository = repository,
            authRepository = authRepository,
        )

        viewModel.submitBooking(validDraft())
        authRepository.authenticate(uid = "uid-2")
        runCurrent()

        val error = assertIs<BookingSubmitUiState.Error>(viewModel.submitState.value)
        assertEquals(BookingSubmitResolution.Retry, error.resolution)
        assertEquals(true, error.retryable)
        assertEquals(0, repository.createCalls)
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

    @Test
    fun submitBookingMapsRewardAuthErrorToSignInResolution() = runTest {
        val viewModel = productsBookingViewModel(
            bookingRepository = FakeBookingRepository(
                availabilityResult = BookingAvailabilityResult.Success(availableMonth("maio 2026", "2026-05-01")),
                createResult = BookingCreateResult.Failure(
                    BookingCreateError.Unauthenticated("Inicie sessão para aplicar esta recompensa."),
                ),
            ),
        )

        viewModel.submitBooking(validDraft().copy(loyaltyRewardCode = "SS-FREE-UID1-0001"))
        runCurrent()

        val error = assertIs<BookingSubmitUiState.Error>(viewModel.submitState.value)
        assertEquals(BookingSubmitResolution.SignIn, error.resolution)
        assertEquals(false, error.retryable)
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
    businessRepository: BusinessInfoRepository = FakeProductsBusinessInfoRepository(
        result = BusinessInfoResult.Success(businessInfo()),
    ),
    userVehicleChangeNotifier: MutableUserVehicleChangeNotifier = MutableUserVehicleChangeNotifier(),
    bookingChangeNotifier: MutableBookingChangeNotifier = MutableBookingChangeNotifier(),
): ProductsBookingViewModel = ProductsBookingViewModel(
    bookingRepository = bookingRepository,
    authRepository = authRepository,
    userVehicleRepository = vehicleRepository,
    userProfileRepository = profileRepository,
    businessInfoRepository = businessRepository,
    userVehicleChangeNotifier = userVehicleChangeNotifier,
    bookingChangeNotifier = bookingChangeNotifier,
)

private class FakeBookingRepository(
    private val availabilityResult: BookingAvailabilityResult,
    private val createResult: BookingCreateResult = BookingCreateResult.Success(
        BookingReceipt(
            reservationId = "reservation-1",
            reservationCode = "SS-ABCDEFGH",
        ),
    ),
    private val loyaltyResult: BookingLoyaltyResult = BookingLoyaltyResult.Success(bookingLoyalty()),
) : BookingRepository {
    var lastAvailabilityRequest: BookingAvailabilityRequest? = null
        private set
    var loyaltyCalls: Int = 0
        private set
    var createCalls: Int = 0
        private set

    override suspend fun getAvailability(request: BookingAvailabilityRequest): BookingAvailabilityResult {
        lastAvailabilityRequest = request
        return availabilityResult
    }

    override suspend fun createBooking(request: BookingCreateRequest): BookingCreateResult {
        createCalls += 1
        return createResult
    }

    override suspend fun getMyBookings(): BookingHistoryResult {
        error("Not used")
    }

    override suspend fun getMyLoyalty(): BookingLoyaltyResult {
        loyaltyCalls += 1
        return loyaltyResult
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

private class DeferredProductsVehicleRepository(
    vararg results: CompletableDeferred<UserVehicleListResult>,
) : UserVehicleRepository {
    private val pendingResults = results.toMutableList()
    var listCalls: Int = 0
        private set

    override suspend fun getMyVehicles(): UserVehicleListResult {
        listCalls += 1
        return pendingResults.removeAt(0).await()
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

private class DeferredProductsProfileRepository(
    vararg results: CompletableDeferred<UserProfileResult>,
) : UserProfileRepository {
    private val pendingResults = results.toMutableList()
    var getCalls: Int = 0
        private set

    override suspend fun getMyProfile(): UserProfileResult {
        getCalls += 1
        return pendingResults.removeAt(0).await()
    }

    override suspend fun updateMyProfile(request: UserProfileSaveRequest): UserProfileMutationResult {
        error("Not used")
    }
}

private class FakeProductsBusinessInfoRepository(
    var result: BusinessInfoResult,
) : BusinessInfoRepository {
    var calls: Int = 0
        private set

    override suspend fun getBusinessInfo(): BusinessInfoResult {
        calls += 1
        return result
    }
}

private class DeferredProductsBusinessInfoRepository(
    vararg results: CompletableDeferred<BusinessInfoResult>,
) : BusinessInfoRepository {
    private val pendingResults = results.toMutableList()
    var calls: Int = 0
        private set

    override suspend fun getBusinessInfo(): BusinessInfoResult {
        calls += 1
        return pendingResults.removeAt(0).await()
    }
}

private class FakeProductsAuthRepository(
    authenticated: Boolean = true,
    initialState: AuthSessionState? = null,
) : AuthRepository {
    private val mutableSessionState = MutableStateFlow(
        initialState ?: if (authenticated) {
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

    fun setSessionState(state: AuthSessionState) {
        mutableSessionState.value = state
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
    isDefault: Boolean = false,
): UserVehicle = UserVehicle(
    id = id,
    brand = brand,
    model = "320d",
    plate = "AA-00-BB",
    color = "Preto",
    type = type,
    isDefault = isDefault,
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

private fun businessInfo(
    phone: String = "913 005 855",
    addressLine1: String = "Shopping Norte Sul, Piso -1",
    addressLine2: String = "Leiria, Portugal",
    openingHours: List<BusinessOpeningHours> = listOf(
        BusinessOpeningHours(dayLabel = "Segunda a Sexta", hoursLabel = "09:00 - 19:00", closed = false),
    ),
): BusinessInfo = BusinessInfo(
    phone = phone,
    phoneUri = "tel:${phone.filter { it.isDigit() }}",
    email = "info@sudsshine.pt",
    emailUri = "mailto:info@sudsshine.pt",
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    mapsUri = "https://maps.example.test",
    whatsappUri = "https://wa.me/351913005855",
    openingHours = openingHours,
    faq = listOf(BusinessFaq(question = "Pergunta?", answer = "Resposta.")),
    stats = listOf(BusinessStat(value = "500+", label = "Carros")),
    socialLinks = emptyList(),
)

private fun bookingLoyalty(
    redemptions: List<BookingLoyaltyRedemption> = emptyList(),
): BookingLoyalty = BookingLoyalty(
    summary = BookingLoyaltySummary(
        totalWashes = 10,
        currentWashes = 0,
        targetWashes = 10,
        remainingWashes = 10,
        progress = 0f,
        rewardReady = false,
        completedRewards = 1,
        claimedRewards = redemptions.size,
        availableRewards = 0,
    ),
    stampHistory = emptyList<BookingLoyaltyStamp>(),
    redemptions = redemptions,
)

private fun bookingRedemption(
    id: String = "reward-1",
    rewardCode: String = "SS-FREE-UID1-0001",
    status: String = "issued",
    createdAtIso: String = "2026-05-20T10:00:00.000Z",
): BookingLoyaltyRedemption = BookingLoyaltyRedemption(
    id = id,
    rewardCode = rewardCode,
    rewardNumber = 1,
    status = status,
    createdAtIso = createdAtIso,
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
