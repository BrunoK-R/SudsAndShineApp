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
    fun rejectsInvalidAvailabilitySlotIntervalBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.getAvailability(
            BookingAvailabilityRequest(
                serviceDurationMinutes = 30,
                slotIntervalMinutes = 241,
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
    fun rejectsInvalidCreateSlotBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createBooking(
            validRequest().copy(slotStartIso = "2026-02-31T09:30:00.000Z"),
        )

        assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Validation>(result.error)
        assertEquals(0, api.calls)
    }

    @Test
    fun rejectsCreateSlotEndBeforeStartBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createBooking(
            validRequest().copy(
                slotStartIso = "2026-05-20T10:30:00.000Z",
                slotEndIso = "2026-05-20T10:00:00.000Z",
            ),
        )

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
                slotStartIso = " 2026-05-20T09:30:00.000Z ",
                slotEndIso = " 2026-05-20T10:15:00.000Z ",
                notes = "  Sem ambientador  ",
            ),
        )

        assertIs<BookingCreateResult.Success>(result)
        assertEquals(1, api.calls)
        assertEquals("Bruno Ribeiro", api.lastRequest?.customerName)
        assertEquals("bruno@example.com", api.lastRequest?.customerEmail)
        assertEquals("2026-05-20T09:30:00.000Z", api.lastRequest?.slotStartIso)
        assertEquals("2026-05-20T10:15:00.000Z", api.lastRequest?.slotEndIso)
        assertEquals("Sem ambientador", api.lastRequest?.notes)
        assertEquals("id-token-1", api.lastIdToken)
    }

    @Test
    fun normalizesLoyaltyRewardCodeBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createBooking(
            validRequest().copy(loyaltyRewardCode = " ss-free-uid1-0001 "),
        )

        assertIs<BookingCreateResult.Success>(result)
        assertEquals(1, api.calls)
        assertEquals("SS-FREE-UID1-0001", api.lastRequest?.loyaltyRewardCode)
        assertEquals("id-token-1", api.lastIdToken)
    }

    @Test
    fun normalizesSavedVehicleBeforeCallingApiWithAuthenticatedSession() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createBooking(
            validRequest().copy(
                userVehicleId = " vehicle-1 ",
                vehicleLabel = "  BMW 320d  ",
            ),
        )

        assertIs<BookingCreateResult.Success>(result)
        assertEquals(1, api.calls)
        assertEquals("vehicle-1", api.lastRequest?.userVehicleId)
        assertEquals("BMW 320d", api.lastRequest?.vehicleLabel)
        assertEquals("id-token-1", api.lastIdToken)
    }

    @Test
    fun normalizesSelectedExtraIdsBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createBooking(
            validRequest().copy(extraIds = listOf(" wax ", "vacuum", "WAX", "")),
        )

        assertIs<BookingCreateResult.Success>(result)
        assertEquals(1, api.calls)
        assertEquals(listOf("wax", "vacuum"), api.lastRequest?.extraIds)
    }

    @Test
    fun rejectsInvalidSelectedExtraIdsBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createBooking(validRequest().copy(extraIds = listOf("service_extras/wax")))

        assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Validation>(result.error)
        assertEquals(0, api.calls)
    }

    @Test
    fun rejectsLoyaltyRewardCodeWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.createBooking(validRequest().copy(loyaltyRewardCode = "SS-FREE-UID1-0001"))

        assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Unauthenticated>(result.error)
        assertEquals(0, api.calls)
    }

    @Test
    fun rejectsSavedVehicleWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.createBooking(validRequest().copy(userVehicleId = "vehicle-1"))

        assertIs<BookingCreateResult.Failure>(result)
        val error = assertIs<BookingCreateError.Unauthenticated>(result.error)
        assertEquals("Inicie sessão para usar este veículo guardado.", error.message)
        assertEquals(0, api.calls)
    }

    @Test
    fun rejectsInvalidLoyaltyRewardCodeBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.createBooking(validRequest().copy(loyaltyRewardCode = "users/other/reward"))

        assertIs<BookingCreateResult.Failure>(result)
        assertIs<BookingCreateError.Validation>(result.error)
        assertEquals(0, api.calls)
    }

    @Test
    fun successfulBookingNotifiesBookingHistoryObservers() = runTest {
        val api = RecordingBookingFunctionsApi()
        val changeNotifier = MutableBookingChangeNotifier()
        val repository = FirebaseBookingRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
            bookingChangeNotifier = changeNotifier,
        )

        val result = repository.createBooking(validRequest())

        assertIs<BookingCreateResult.Success>(result)
        assertEquals(1L, changeNotifier.revision.value)
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

    @Test
    fun rejectsLoyaltyWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.getMyLoyalty()

        assertIs<BookingLoyaltyResult.Failure>(result)
        assertIs<BookingLoyaltyError.Unauthenticated>(result.error)
        assertEquals(0, api.loyaltyCalls)
    }

    @Test
    fun loadsLoyaltyWithAuthenticatedIdToken() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.getMyLoyalty()

        assertIs<BookingLoyaltyResult.Success>(result)
        assertEquals(1, api.loyaltyCalls)
        assertEquals("id-token-1", api.lastLoyaltyIdToken)
    }

    @Test
    fun rejectsReviewWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.submitReview(validReviewRequest())

        assertIs<BookingReviewResult.Failure>(result)
        assertIs<BookingReviewError.Unauthenticated>(result.error)
        assertEquals(0, api.reviewCalls)
    }

    @Test
    fun normalizesReviewBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.submitReview(
            validReviewRequest().copy(
                tags = listOf(" Qualidade ", "qualidade", "Rápido"),
                comment = "  Ficou impecável.  ",
            ),
        )

        assertIs<BookingReviewResult.Success>(result)
        assertEquals(1, api.reviewCalls)
        assertEquals("reservation-1", api.lastReviewRequest?.reservationId)
        assertEquals(listOf("Qualidade", "Rápido"), api.lastReviewRequest?.tags)
        assertEquals("Ficou impecável.", api.lastReviewRequest?.comment)
        assertEquals("id-token-1", api.lastReviewIdToken)
    }

    @Test
    fun successfulReviewNotifiesBookingHistoryObservers() = runTest {
        val api = RecordingBookingFunctionsApi()
        val changeNotifier = MutableBookingChangeNotifier()
        val repository = FirebaseBookingRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
            bookingChangeNotifier = changeNotifier,
        )

        val result = repository.submitReview(validReviewRequest())

        assertIs<BookingReviewResult.Success>(result)
        assertEquals(1L, changeNotifier.revision.value)
    }

    @Test
    fun rejectsCancelWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.cancelBooking(validCancelRequest())

        assertIs<BookingCancelResult.Failure>(result)
        assertIs<BookingCancelError.Unauthenticated>(result.error)
        assertEquals(0, api.cancelCalls)
    }

    @Test
    fun normalizesCancelBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.cancelBooking(BookingCancelRequest(" reservation-1 "))

        assertIs<BookingCancelResult.Success>(result)
        assertEquals(1, api.cancelCalls)
        assertEquals("reservation-1", api.lastCancelRequest?.reservationId)
        assertEquals("id-token-1", api.lastCancelIdToken)
    }

    @Test
    fun successfulCancelNotifiesBookingHistoryObservers() = runTest {
        val api = RecordingBookingFunctionsApi()
        val changeNotifier = MutableBookingChangeNotifier()
        val repository = FirebaseBookingRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
            bookingChangeNotifier = changeNotifier,
        )

        val result = repository.cancelBooking(validCancelRequest())

        assertIs<BookingCancelResult.Success>(result)
        assertEquals(1L, changeNotifier.revision.value)
    }

    @Test
    fun rejectsRescheduleWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.rescheduleBooking(validRescheduleRequest())

        assertIs<BookingRescheduleResult.Failure>(result)
        assertIs<BookingRescheduleError.Unauthenticated>(result.error)
        assertEquals(0, api.rescheduleCalls)
    }

    @Test
    fun normalizesRescheduleBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.rescheduleBooking(
            BookingRescheduleRequest(
                reservationId = " reservation-1 ",
                slotStartIso = " 2026-05-22T11:00:00.000Z ",
                slotEndIso = " 2026-05-22T11:45:00.000Z ",
            ),
        )

        assertIs<BookingRescheduleResult.Success>(result)
        assertEquals(1, api.rescheduleCalls)
        assertEquals("reservation-1", api.lastRescheduleRequest?.reservationId)
        assertEquals("2026-05-22T11:00:00.000Z", api.lastRescheduleRequest?.slotStartIso)
        assertEquals("2026-05-22T11:45:00.000Z", api.lastRescheduleRequest?.slotEndIso)
        assertEquals("id-token-1", api.lastRescheduleIdToken)
    }

    @Test
    fun rejectsInvalidRescheduleSlotBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.rescheduleBooking(
            validRescheduleRequest().copy(slotEndIso = "2026-05-22T10:00:00.000Z"),
        )

        assertIs<BookingRescheduleResult.Failure>(result)
        assertIs<BookingRescheduleError.Validation>(result.error)
        assertEquals(0, api.rescheduleCalls)
    }

    @Test
    fun successfulRescheduleNotifiesBookingHistoryObservers() = runTest {
        val api = RecordingBookingFunctionsApi()
        val changeNotifier = MutableBookingChangeNotifier()
        val repository = FirebaseBookingRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
            bookingChangeNotifier = changeNotifier,
        )

        val result = repository.rescheduleBooking(validRescheduleRequest())

        assertIs<BookingRescheduleResult.Success>(result)
        assertEquals(1L, changeNotifier.revision.value)
    }

    @Test
    fun rejectsRewardRedemptionWhenUnauthenticatedBeforeCallingApi() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.redeemLoyaltyReward()

        assertIs<BookingRewardRedemptionResult.Failure>(result)
        assertIs<BookingRewardRedemptionError.Unauthenticated>(result.error)
        assertEquals(0, api.rewardRedemptionCalls)
    }

    @Test
    fun redeemsRewardWithAuthenticatedIdToken() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.redeemLoyaltyReward()

        assertIs<BookingRewardRedemptionResult.Success>(result)
        assertEquals(1, api.rewardRedemptionCalls)
        assertEquals("id-token-1", api.lastRewardRedemptionIdToken)
    }

    @Test
    fun successfulRewardRedemptionNotifiesBookingHistoryObservers() = runTest {
        val api = RecordingBookingFunctionsApi()
        val changeNotifier = MutableBookingChangeNotifier()
        val repository = FirebaseBookingRepository(
            api = api,
            authRepository = FakeAuthRepository(authenticated = true),
            bookingChangeNotifier = changeNotifier,
        )

        val result = repository.redeemLoyaltyReward()

        assertIs<BookingRewardRedemptionResult.Success>(result)
        assertEquals(1L, changeNotifier.revision.value)
    }

    @Test
    fun bookingPresetsRequireAuthentication() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository())

        val result = repository.getMyBookingPresets()

        val failure = assertIs<BookingPresetListResult.Failure>(result)
        assertIs<BookingPresetError.Unauthenticated>(failure.error)
        assertEquals(0, api.presetListCalls)
    }

    @Test
    fun normalizesBookingPresetBeforeSavingWithIdToken() = runTest {
        val api = RecordingBookingFunctionsApi()
        val repository = FirebaseBookingRepository(api, FakeAuthRepository(authenticated = true))

        val result = repository.saveBookingPreset(
            BookingPresetUpsertRequest(
                label = "  BMW habitual  ",
                serviceId = " premium ",
                extraIds = listOf(" wax ", "wax", " vacuum "),
                userVehicleId = " vehicle-1 ",
                vehicleType = " PASSAGEIROS ",
                vehicleLabel = "  BMW 320d  ",
            ),
        )

        assertIs<BookingPresetSaveResult.Success>(result)
        assertEquals("BMW habitual", api.lastPresetRequest?.label)
        assertEquals("premium", api.lastPresetRequest?.serviceId)
        assertEquals(listOf("wax", "vacuum"), api.lastPresetRequest?.extraIds)
        assertEquals("vehicle-1", api.lastPresetRequest?.userVehicleId)
        assertEquals("passenger", api.lastPresetRequest?.vehicleType)
        assertEquals("id-token-1", api.lastPresetIdToken)
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
    var loyaltyCalls: Int = 0
        private set
    var lastLoyaltyIdToken: String? = null
        private set
    var reviewCalls: Int = 0
        private set
    var lastReviewRequest: BookingReviewRequest? = null
        private set
    var lastReviewIdToken: String? = null
        private set
    var cancelCalls: Int = 0
        private set
    var lastCancelRequest: BookingCancelRequest? = null
        private set
    var lastCancelIdToken: String? = null
        private set
    var rescheduleCalls: Int = 0
        private set
    var lastRescheduleRequest: BookingRescheduleRequest? = null
        private set
    var lastRescheduleIdToken: String? = null
        private set
    var rewardRedemptionCalls: Int = 0
        private set
    var lastRewardRedemptionIdToken: String? = null
        private set
    var presetListCalls: Int = 0
        private set
    var lastPresetRequest: BookingPresetUpsertRequest? = null
        private set
    var lastPresetIdToken: String? = null
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

    override suspend fun getMyLoyalty(idToken: String): BookingLoyaltyResult {
        loyaltyCalls += 1
        lastLoyaltyIdToken = idToken
        return BookingLoyaltyResult.Success(
            BookingLoyalty(
                summary = BookingLoyaltySummary(
                    totalWashes = 0,
                    currentWashes = 0,
                    targetWashes = 10,
                    remainingWashes = 10,
                    progress = 0f,
                    rewardReady = false,
                    completedRewards = 0,
                    claimedRewards = 0,
                    availableRewards = 0,
                ),
                stampHistory = emptyList(),
                redemptions = emptyList(),
            ),
        )
    }

    override suspend fun getMyBookingPresets(idToken: String): BookingPresetListResult {
        presetListCalls += 1
        lastPresetIdToken = idToken
        return BookingPresetListResult.Success(BookingPresetList(emptyList()))
    }

    override suspend fun upsertMyBookingPreset(
        request: BookingPresetUpsertRequest,
        idToken: String,
    ): BookingPresetSaveResult {
        lastPresetRequest = request
        lastPresetIdToken = idToken
        return BookingPresetSaveResult.Success(
            BookingPreset(
                id = "preset-1",
                label = request.label,
                serviceId = request.serviceId,
                extraIds = request.extraIds,
                userVehicleId = request.userVehicleId,
                vehicleType = request.vehicleType,
                vehicleLabel = request.vehicleLabel,
            ),
        )
    }

    override suspend fun submitReservationReview(request: BookingReviewRequest, idToken: String): BookingReviewResult {
        reviewCalls += 1
        lastReviewRequest = request
        lastReviewIdToken = idToken
        return BookingReviewResult.Success(
            BookingReviewReceipt(
                reviewId = "review-1",
                reservationId = request.reservationId,
            ),
        )
    }

    override suspend fun cancelMyReservation(request: BookingCancelRequest, idToken: String): BookingCancelResult {
        cancelCalls += 1
        lastCancelRequest = request
        lastCancelIdToken = idToken
        return BookingCancelResult.Success(
            BookingCancelReceipt(
                reservationId = request.reservationId,
                status = "cancelled",
            ),
        )
    }

    override suspend fun rescheduleMyReservation(
        request: BookingRescheduleRequest,
        idToken: String,
    ): BookingRescheduleResult {
        rescheduleCalls += 1
        lastRescheduleRequest = request
        lastRescheduleIdToken = idToken
        return BookingRescheduleResult.Success(
            BookingRescheduleReceipt(
                reservationId = request.reservationId,
                status = "pending",
                slotStartIso = request.slotStartIso,
                slotEndIso = request.slotEndIso,
            ),
        )
    }

    override suspend fun redeemMyLoyaltyReward(idToken: String): BookingRewardRedemptionResult {
        rewardRedemptionCalls += 1
        lastRewardRedemptionIdToken = idToken
        return BookingRewardRedemptionResult.Success(
            BookingRewardRedemptionReceipt(
                redemptionId = "reward-0001",
                rewardCode = "SS-FREE-UID1-0001",
                rewardNumber = 1,
                status = "issued",
                loyalty = BookingLoyaltySummary(
                    totalWashes = 10,
                    currentWashes = 0,
                    targetWashes = 10,
                    remainingWashes = 10,
                    progress = 0f,
                    rewardReady = false,
                    completedRewards = 1,
                    claimedRewards = 1,
                    availableRewards = 0,
                ),
            ),
        )
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

private fun validReviewRequest(): BookingReviewRequest = BookingReviewRequest(
    reservationId = "reservation-1",
    rating = 5,
    tags = listOf("Qualidade"),
    comment = "Muito bom.",
)

private fun validCancelRequest(): BookingCancelRequest = BookingCancelRequest(
    reservationId = "reservation-1",
)

private fun validRescheduleRequest(): BookingRescheduleRequest = BookingRescheduleRequest(
    reservationId = "reservation-1",
    slotStartIso = "2026-05-22T11:00:00.000Z",
    slotEndIso = "2026-05-22T11:45:00.000Z",
)
