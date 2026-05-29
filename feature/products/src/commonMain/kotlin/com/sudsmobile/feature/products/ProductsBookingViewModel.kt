package com.sudsmobile.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingAvailabilityError
import com.sudsmobile.data.booking.BookingAvailabilityMonth
import com.sudsmobile.data.booking.BookingAvailabilityRequest
import com.sudsmobile.data.booking.BookingAvailabilityResult
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingCreateError
import com.sudsmobile.data.booking.BookingCreateRequest
import com.sudsmobile.data.booking.BookingCreateResult
import com.sudsmobile.data.booking.BookingReceipt
import com.sudsmobile.data.booking.BookingLoyalty
import com.sudsmobile.data.booking.BookingLoyaltyError
import com.sudsmobile.data.booking.BookingLoyaltyRedemption
import com.sudsmobile.data.booking.BookingLoyaltyResult
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.BusinessOpeningHours
import com.sudsmobile.data.business.DefaultBusinessInfo
import com.sudsmobile.data.profile.UserProfile
import com.sudsmobile.data.profile.UserProfileError
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.profile.UserProfileResult
import com.sudsmobile.data.vehicle.UserVehicle
import com.sudsmobile.data.vehicle.UserVehicleChangeNotifier
import com.sudsmobile.data.vehicle.UserVehicleError
import com.sudsmobile.data.vehicle.UserVehicleListResult
import com.sudsmobile.data.vehicle.UserVehicleRepository
import com.sudsmobile.data.vehicle.MutableUserVehicleChangeNotifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductsBookingDraft(
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val serviceId: String,
    val serviceName: String,
    val dateId: String,
    val time: String,
    val serviceDurationMinutes: Int,
    val vehicleType: String,
    val gdprConsent: Boolean,
    val notes: String,
    val userVehicleId: String? = null,
    val vehicleLabel: String? = null,
    val loyaltyRewardCode: String? = null,
    val extraIds: List<String> = emptyList(),
)

data class BookingVehicleUi(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val userVehicleId: String?,
    val vehicleLabel: String?,
    val isDefault: Boolean = false,
)

data class BookingContactProfileUi(
    val uid: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String,
)

data class BookingBusinessInfoUi(
    val phone: String,
    val addressLine1: String,
    val addressLine2: String,
    val openingHours: List<BookingOpeningHoursUi>,
)

data class BookingOpeningHoursUi(
    val dayLabel: String,
    val hoursLabel: String,
    val closed: Boolean,
)

data class BookingRewardCodeUi(
    val id: String,
    val code: String,
    val statusLabel: String,
    val issuedAt: String,
)

sealed interface BookingSubmitUiState {
    data object Idle : BookingSubmitUiState
    data object Loading : BookingSubmitUiState
    data class Success(val receipt: BookingReceipt) : BookingSubmitUiState
    data class Error(
        val message: String,
        val retryable: Boolean,
        val resolution: BookingSubmitResolution,
    ) : BookingSubmitUiState
}

enum class BookingSubmitResolution {
    None,
    Retry,
    ChangeSlot,
    SignIn,
}

sealed interface BookingAvailabilityUiState {
    data object Idle : BookingAvailabilityUiState
    data object Loading : BookingAvailabilityUiState
    data class Loaded(val month: BookingAvailabilityMonth) : BookingAvailabilityUiState
    data class Empty(val month: BookingAvailabilityMonth) : BookingAvailabilityUiState
    data class Error(val message: String, val retryable: Boolean) : BookingAvailabilityUiState
}

sealed interface BookingContactProfileUiState {
    data object Idle : BookingContactProfileUiState
    data object Loading : BookingContactProfileUiState
    data object Unauthenticated : BookingContactProfileUiState
    data object Empty : BookingContactProfileUiState
    data class Loaded(val profile: BookingContactProfileUi) : BookingContactProfileUiState
    data class Error(val message: String, val retryable: Boolean) : BookingContactProfileUiState
}

sealed interface BookingVehiclesUiState {
    data object Idle : BookingVehiclesUiState
    data object Loading : BookingVehiclesUiState
    data object Unauthenticated : BookingVehiclesUiState
    data object Empty : BookingVehiclesUiState
    data class Loaded(val vehicles: List<BookingVehicleUi>) : BookingVehiclesUiState
    data class Error(val message: String, val retryable: Boolean) : BookingVehiclesUiState
}

sealed interface BookingBusinessInfoUiState {
    data object Idle : BookingBusinessInfoUiState
    data object Loading : BookingBusinessInfoUiState
    data class Loaded(val info: BookingBusinessInfoUi) : BookingBusinessInfoUiState
    data class Error(
        val fallbackInfo: BookingBusinessInfoUi,
        val message: String,
        val retryable: Boolean,
    ) : BookingBusinessInfoUiState
}

sealed interface BookingRewardsUiState {
    data object Idle : BookingRewardsUiState
    data object Loading : BookingRewardsUiState
    data object Unauthenticated : BookingRewardsUiState
    data object Empty : BookingRewardsUiState
    data class Loaded(val rewardCodes: List<BookingRewardCodeUi>) : BookingRewardsUiState
    data class Error(val message: String, val retryable: Boolean) : BookingRewardsUiState
}

class ProductsBookingViewModel(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val userVehicleRepository: UserVehicleRepository,
    private val userProfileRepository: UserProfileRepository,
    private val businessInfoRepository: BusinessInfoRepository,
    private val userVehicleChangeNotifier: UserVehicleChangeNotifier = MutableUserVehicleChangeNotifier(),
    private val bookingChangeNotifier: BookingChangeNotifier = MutableBookingChangeNotifier(),
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val vehicleRevision: StateFlow<Long> = userVehicleChangeNotifier.revision
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision
    private val _availabilityState = MutableStateFlow<BookingAvailabilityUiState>(BookingAvailabilityUiState.Idle)
    val availabilityState: StateFlow<BookingAvailabilityUiState> = _availabilityState.asStateFlow()
    private var availabilityRequestRevision: Long = 0

    private val _submitState = MutableStateFlow<BookingSubmitUiState>(BookingSubmitUiState.Idle)
    val submitState: StateFlow<BookingSubmitUiState> = _submitState.asStateFlow()
    private var pendingSubmitRequest: BookingCreateRequest? = null
    private var submitRequestInFlight: Boolean = false

    private val _vehiclesState = MutableStateFlow<BookingVehiclesUiState>(BookingVehiclesUiState.Idle)
    val vehiclesState: StateFlow<BookingVehiclesUiState> = _vehiclesState.asStateFlow()
    private var loadedVehiclesUid: String? = null
    private var loadedVehiclesRevision: Long? = null
    private var vehiclesRequestInFlight: Boolean = false

    private val _contactProfileState = MutableStateFlow<BookingContactProfileUiState>(BookingContactProfileUiState.Idle)
    val contactProfileState: StateFlow<BookingContactProfileUiState> = _contactProfileState.asStateFlow()
    private var loadedContactProfileUid: String? = null
    private var contactProfileRequestInFlight: Boolean = false

    private val _businessInfoState = MutableStateFlow<BookingBusinessInfoUiState>(BookingBusinessInfoUiState.Idle)
    val businessInfoState: StateFlow<BookingBusinessInfoUiState> = _businessInfoState.asStateFlow()

    private val _rewardsState = MutableStateFlow<BookingRewardsUiState>(BookingRewardsUiState.Idle)
    val rewardsState: StateFlow<BookingRewardsUiState> = _rewardsState.asStateFlow()
    private var loadedRewardsUid: String? = null
    private var loadedRewardsRevision: Long? = null
    private var rewardsRequestInFlight: Boolean = false

    fun loadBusinessInfo(force: Boolean = false) {
        if (_businessInfoState.value is BookingBusinessInfoUiState.Loading) return
        if (!force && _businessInfoState.value is BookingBusinessInfoUiState.Loaded) return

        viewModelScope.launch {
            _businessInfoState.value = BookingBusinessInfoUiState.Loading
            _businessInfoState.value = when (val result = businessInfoRepository.getBusinessInfo()) {
                is BusinessInfoResult.Success -> BookingBusinessInfoUiState.Loaded(result.info.toBookingBusinessInfoUi())
                is BusinessInfoResult.Failure -> result.error.toBookingBusinessInfoState()
            }
        }
    }

    fun refreshRewardsForSession() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedRewards()
                _rewardsState.value = BookingRewardsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedRewards()
                _rewardsState.value = currentSessionState.error.toRewardsUiState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedRewards()
                _rewardsState.value = BookingRewardsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val revision = bookingRevision.value
        val alreadyLoadedForUser = loadedRewardsUid == uid &&
            loadedRewardsRevision == revision &&
            (_rewardsState.value is BookingRewardsUiState.Loaded || _rewardsState.value is BookingRewardsUiState.Empty)
        if (alreadyLoadedForUser) return
        loadRewards()
    }

    fun loadRewards() {
        if (rewardsRequestInFlight) return

        val session = when (val currentSessionState = authRepository.sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedRewards()
                _rewardsState.value = BookingRewardsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedRewards()
                _rewardsState.value = currentSessionState.error.toRewardsUiState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedRewards()
                _rewardsState.value = BookingRewardsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val requestedUid = session.session.user.uid
        val requestedRevision = bookingRevision.value
        rewardsRequestInFlight = true
        viewModelScope.launch {
            try {
                _rewardsState.value = BookingRewardsUiState.Loading
                val nextState = when (val result = bookingRepository.getMyLoyalty()) {
                    is BookingLoyaltyResult.Success -> result.loyalty.toBookingRewardsUiState()
                    is BookingLoyaltyResult.Failure -> result.error.toBookingRewardsUiState()
                }
                when (val currentSessionState = authRepository.sessionState.value) {
                    AuthSessionState.Restoring -> {
                        clearLoadedRewards()
                        _rewardsState.value = BookingRewardsUiState.Loading
                    }
                    is AuthSessionState.RestoreFailed -> {
                        clearLoadedRewards()
                        _rewardsState.value = currentSessionState.error.toRewardsUiState()
                    }
                    AuthSessionState.Unauthenticated -> {
                        clearLoadedRewards()
                        _rewardsState.value = BookingRewardsUiState.Unauthenticated
                    }
                    is AuthSessionState.Authenticated -> {
                        if (currentSessionState.session.user.uid == requestedUid) {
                            loadedRewardsUid = requestedUid
                            loadedRewardsRevision = requestedRevision
                            _rewardsState.value = nextState
                        } else {
                            clearLoadedRewards()
                            _rewardsState.value = BookingRewardsUiState.Unauthenticated
                        }
                    }
                }
            } finally {
                rewardsRequestInFlight = false
            }
        }
    }

    fun refreshContactProfileForSession() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedContactProfile()
                _contactProfileState.value = BookingContactProfileUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedContactProfile()
                _contactProfileState.value = currentSessionState.error.toContactProfileUiState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedContactProfile()
                _contactProfileState.value = BookingContactProfileUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val alreadyLoadedForUser = loadedContactProfileUid == uid &&
            (
                _contactProfileState.value is BookingContactProfileUiState.Loaded ||
                    _contactProfileState.value is BookingContactProfileUiState.Empty
                )
        if (alreadyLoadedForUser) return
        loadContactProfile()
    }

    fun loadContactProfile() {
        if (contactProfileRequestInFlight) return

        val session = when (val currentSessionState = authRepository.sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedContactProfile()
                _contactProfileState.value = BookingContactProfileUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedContactProfile()
                _contactProfileState.value = currentSessionState.error.toContactProfileUiState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedContactProfile()
                _contactProfileState.value = BookingContactProfileUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val requestedUid = session.session.user.uid
        contactProfileRequestInFlight = true
        viewModelScope.launch {
            try {
                _contactProfileState.value = BookingContactProfileUiState.Loading
                val nextState = when (val result = userProfileRepository.getMyProfile()) {
                    is UserProfileResult.Success -> result.profile.toContactProfileUiState()
                    is UserProfileResult.Failure -> result.error.toContactProfileUiState()
                }
                when (val currentSessionState = authRepository.sessionState.value) {
                    AuthSessionState.Restoring -> {
                        clearLoadedContactProfile()
                        _contactProfileState.value = BookingContactProfileUiState.Loading
                    }
                    is AuthSessionState.RestoreFailed -> {
                        clearLoadedContactProfile()
                        _contactProfileState.value = currentSessionState.error.toContactProfileUiState()
                    }
                    AuthSessionState.Unauthenticated -> {
                        clearLoadedContactProfile()
                        _contactProfileState.value = BookingContactProfileUiState.Unauthenticated
                    }
                    is AuthSessionState.Authenticated -> {
                        if (currentSessionState.session.user.uid == requestedUid) {
                            loadedContactProfileUid = requestedUid
                            _contactProfileState.value = nextState
                        } else {
                            clearLoadedContactProfile()
                            _contactProfileState.value = BookingContactProfileUiState.Unauthenticated
                        }
                    }
                }
            } finally {
                contactProfileRequestInFlight = false
            }
        }
    }

    fun refreshVehiclesForSession() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedVehicles()
                _vehiclesState.value = BookingVehiclesUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedVehicles()
                _vehiclesState.value = currentSessionState.error.toVehiclesUiState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedVehicles()
                _vehiclesState.value = BookingVehiclesUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val uid = session.session.user.uid
        val revision = vehicleRevision.value
        val alreadyLoadedForUser = loadedVehiclesUid == uid &&
            loadedVehiclesRevision == revision &&
            (_vehiclesState.value is BookingVehiclesUiState.Loaded || _vehiclesState.value is BookingVehiclesUiState.Empty)
        if (alreadyLoadedForUser) return
        loadVehicles()
    }

    fun loadVehicles() {
        if (vehiclesRequestInFlight) return

        val session = when (val currentSessionState = authRepository.sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedVehicles()
                _vehiclesState.value = BookingVehiclesUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedVehicles()
                _vehiclesState.value = currentSessionState.error.toVehiclesUiState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedVehicles()
                _vehiclesState.value = BookingVehiclesUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }

        val requestedUid = session.session.user.uid
        val requestedRevision = vehicleRevision.value
        vehiclesRequestInFlight = true
        viewModelScope.launch {
            try {
                _vehiclesState.value = BookingVehiclesUiState.Loading
                val nextState = when (val result = userVehicleRepository.getMyVehicles()) {
                    is UserVehicleListResult.Success -> result.vehicles.toVehiclesUiState()
                    is UserVehicleListResult.Failure -> result.error.toVehiclesUiState()
                }
                when (val currentSessionState = authRepository.sessionState.value) {
                    AuthSessionState.Restoring -> {
                        clearLoadedVehicles()
                        _vehiclesState.value = BookingVehiclesUiState.Loading
                    }
                    is AuthSessionState.RestoreFailed -> {
                        clearLoadedVehicles()
                        _vehiclesState.value = currentSessionState.error.toVehiclesUiState()
                    }
                    AuthSessionState.Unauthenticated -> {
                        clearLoadedVehicles()
                        _vehiclesState.value = BookingVehiclesUiState.Unauthenticated
                    }
                    is AuthSessionState.Authenticated -> {
                        if (currentSessionState.session.user.uid == requestedUid) {
                            loadedVehiclesUid = requestedUid
                            loadedVehiclesRevision = requestedRevision
                            _vehiclesState.value = nextState
                        } else {
                            clearLoadedVehicles()
                            _vehiclesState.value = BookingVehiclesUiState.Unauthenticated
                        }
                    }
                }
            } finally {
                vehiclesRequestInFlight = false
            }
        }
    }

    fun loadAvailability(serviceDurationMinutes: Int, anchorDate: String? = null) {
        val requestRevision = ++availabilityRequestRevision
        val request = BookingAvailabilityRequest(
            anchorDate = anchorDate,
            serviceDurationMinutes = serviceDurationMinutes,
        )

        _availabilityState.value = BookingAvailabilityUiState.Loading
        viewModelScope.launch {
            val nextState = when (val result = bookingRepository.getAvailability(request)) {
                is BookingAvailabilityResult.Success -> {
                    if (result.month.days.any { it.available }) {
                        BookingAvailabilityUiState.Loaded(result.month)
                    } else {
                        BookingAvailabilityUiState.Empty(result.month)
                    }
                }
                is BookingAvailabilityResult.Failure -> result.error.toUiState()
            }
            if (requestRevision == availabilityRequestRevision) {
                _availabilityState.value = nextState
            }
        }
    }

    fun submitBooking(draft: ProductsBookingDraft?) {
        if (submitRequestInFlight) return

        val request = draft?.toCreateRequest()
        if (request == null) {
            pendingSubmitRequest = null
            _submitState.value = BookingSubmitUiState.Error(
                message = "Complete os dados da marcação antes de confirmar.",
                retryable = false,
                resolution = BookingSubmitResolution.None,
            )
            return
        }

        submitCreateRequest(request)
    }

    fun refreshSubmitForSession() {
        val request = pendingSubmitRequest ?: return
        if (_submitState.value !is BookingSubmitUiState.Loading) return
        submitCreateRequest(request)
    }

    private fun submitCreateRequest(request: BookingCreateRequest) {
        if (submitRequestInFlight) return

        when (val currentSessionState = authRepository.sessionState.value) {
            AuthSessionState.Restoring -> {
                pendingSubmitRequest = request
                _submitState.value = BookingSubmitUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                pendingSubmitRequest = null
                _submitState.value = currentSessionState.error.toSubmitUiState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                if (request.requiresAuthenticatedSession()) {
                    pendingSubmitRequest = null
                    _submitState.value = unauthenticatedSubmitState()
                    return
                }
                launchSubmitRequest(request = request, expectedUid = null)
            }
            is AuthSessionState.Authenticated -> {
                launchSubmitRequest(
                    request = request,
                    expectedUid = currentSessionState.session.user.uid,
                )
            }
        }
    }

    private fun launchSubmitRequest(request: BookingCreateRequest, expectedUid: String?) {
        pendingSubmitRequest = null
        submitRequestInFlight = true
        viewModelScope.launch {
            try {
                _submitState.value = BookingSubmitUiState.Loading
                when (val currentSessionState = authRepository.sessionState.value) {
                    AuthSessionState.Restoring -> {
                        pendingSubmitRequest = request
                        _submitState.value = BookingSubmitUiState.Loading
                        return@launch
                    }
                    is AuthSessionState.RestoreFailed -> {
                        _submitState.value = currentSessionState.error.toSubmitUiState()
                        return@launch
                    }
                    AuthSessionState.Unauthenticated -> {
                        if (expectedUid != null || request.requiresAuthenticatedSession()) {
                            _submitState.value = unauthenticatedSubmitState()
                            return@launch
                        }
                    }
                    is AuthSessionState.Authenticated -> {
                        if (expectedUid != null && currentSessionState.session.user.uid != expectedUid) {
                            _submitState.value = changedSessionSubmitState()
                            return@launch
                        }
                    }
                }

                _submitState.value = when (val result = bookingRepository.createBooking(request)) {
                    is BookingCreateResult.Success -> BookingSubmitUiState.Success(result.receipt)
                    is BookingCreateResult.Failure -> result.error.toUiState()
                }
            } finally {
                submitRequestInFlight = false
            }
        }
    }

    fun clearSubmitError() {
        if (_submitState.value is BookingSubmitUiState.Error) {
            pendingSubmitRequest = null
            _submitState.value = BookingSubmitUiState.Idle
        }
    }

    fun consumeSuccess() {
        if (_submitState.value is BookingSubmitUiState.Success) {
            pendingSubmitRequest = null
            _submitState.value = BookingSubmitUiState.Idle
        }
    }

    private fun BookingCreateError.toUiState(): BookingSubmitUiState.Error {
        return when (this) {
            is BookingCreateError.Conflict -> BookingSubmitUiState.Error(
                message = message,
                retryable = false,
                resolution = BookingSubmitResolution.ChangeSlot,
            )
            is BookingCreateError.Unauthenticated -> BookingSubmitUiState.Error(
                message = message,
                retryable = false,
                resolution = BookingSubmitResolution.SignIn,
            )
            is BookingCreateError.Unavailable,
            is BookingCreateError.Backend -> BookingSubmitUiState.Error(
                message = message,
                retryable = true,
                resolution = BookingSubmitResolution.Retry,
            )
            is BookingCreateError.Validation,
            is BookingCreateError.Permission -> BookingSubmitUiState.Error(
                message = message,
                retryable = false,
                resolution = BookingSubmitResolution.None,
            )
        }
    }

    private fun AuthError.toSubmitUiState(): BookingSubmitUiState.Error {
        return BookingSubmitUiState.Error(
            message = message,
            retryable = isRetryableSessionError(),
            resolution = if (isRetryableSessionError()) {
                BookingSubmitResolution.Retry
            } else {
                BookingSubmitResolution.SignIn
            },
        )
    }

    private fun unauthenticatedSubmitState(): BookingSubmitUiState.Error {
        return BookingSubmitUiState.Error(
            message = "Inicie sessão para confirmar esta marcação com os dados guardados.",
            retryable = false,
            resolution = BookingSubmitResolution.SignIn,
        )
    }

    private fun changedSessionSubmitState(): BookingSubmitUiState.Error {
        return BookingSubmitUiState.Error(
            message = "A sessão mudou antes de confirmarmos a marcação. Reveja os dados antes de continuar.",
            retryable = true,
            resolution = BookingSubmitResolution.Retry,
        )
    }

    private fun BookingAvailabilityError.toUiState(): BookingAvailabilityUiState.Error {
        val retryable = this is BookingAvailabilityError.Unavailable ||
            this is BookingAvailabilityError.Backend
        return BookingAvailabilityUiState.Error(message = message, retryable = retryable)
    }

    private fun UserProfile.toContactProfileUiState(): BookingContactProfileUiState {
        val profile = BookingContactProfileUi(
            uid = uid,
            displayName = displayName.trim(),
            email = email.trim(),
            phoneNumber = phoneNumber.trim(),
        )
        return if (
            profile.displayName.isBlank() &&
            profile.email.isBlank() &&
            profile.phoneNumber.isBlank()
        ) {
            BookingContactProfileUiState.Empty
        } else {
            BookingContactProfileUiState.Loaded(profile)
        }
    }

    private fun UserProfileError.toContactProfileUiState(): BookingContactProfileUiState {
        return when (this) {
            is UserProfileError.Unauthenticated -> BookingContactProfileUiState.Unauthenticated
            is UserProfileError.Permission,
            is UserProfileError.Validation -> BookingContactProfileUiState.Error(message = message, retryable = false)
            is UserProfileError.Unavailable,
            is UserProfileError.Backend -> BookingContactProfileUiState.Error(message = message, retryable = true)
        }
    }

    private fun List<UserVehicle>.toVehiclesUiState(): BookingVehiclesUiState {
        val vehicles = mapNotNull { it.toBookingVehicleUiOrNull() }
            .sortedWith(
                compareByDescending<BookingVehicleUi> { it.isDefault }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.description.lowercase() },
            )
        return if (vehicles.isEmpty()) BookingVehiclesUiState.Empty else BookingVehiclesUiState.Loaded(vehicles)
    }

    private fun UserVehicleError.toVehiclesUiState(): BookingVehiclesUiState {
        return when (this) {
            is UserVehicleError.Unauthenticated -> BookingVehiclesUiState.Unauthenticated
            is UserVehicleError.Permission,
            is UserVehicleError.Validation -> BookingVehiclesUiState.Error(message = message, retryable = false)
            is UserVehicleError.NotFound,
            is UserVehicleError.Unavailable,
            is UserVehicleError.Backend -> BookingVehiclesUiState.Error(message = message, retryable = true)
        }
    }

    private fun clearLoadedRewards() {
        loadedRewardsUid = null
        loadedRewardsRevision = null
    }

    private fun clearLoadedContactProfile() {
        loadedContactProfileUid = null
    }

    private fun clearLoadedVehicles() {
        loadedVehiclesUid = null
        loadedVehiclesRevision = null
    }
}

private fun BookingLoyalty.toBookingRewardsUiState(): BookingRewardsUiState {
    val rewardCodes = redemptions
        .sortedByDescending { it.createdAtIso }
        .mapNotNull { it.toBookingRewardCodeUiOrNull() }
    return if (rewardCodes.isEmpty()) BookingRewardsUiState.Empty else BookingRewardsUiState.Loaded(rewardCodes)
}

private fun BookingLoyaltyError.toBookingRewardsUiState(): BookingRewardsUiState {
    return when (this) {
        is BookingLoyaltyError.Unauthenticated -> BookingRewardsUiState.Unauthenticated
        is BookingLoyaltyError.Permission -> BookingRewardsUiState.Error(message = message, retryable = false)
        is BookingLoyaltyError.Unavailable,
        is BookingLoyaltyError.Backend -> BookingRewardsUiState.Error(message = message, retryable = true)
    }
}

private fun AuthError.toRewardsUiState(): BookingRewardsUiState.Error {
    return BookingRewardsUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.toContactProfileUiState(): BookingContactProfileUiState.Error {
    return BookingContactProfileUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.toVehiclesUiState(): BookingVehiclesUiState.Error {
    return BookingVehiclesUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private fun BookingLoyaltyRedemption.toBookingRewardCodeUiOrNull(): BookingRewardCodeUi? {
    if (id.isBlank() || rewardCode.isBlank() || !status.isIssuedRewardStatus()) return null
    return BookingRewardCodeUi(
        id = id,
        code = rewardCode,
        statusLabel = status.toRewardStatusLabel(),
        issuedAt = createdAtIso.toIssuedAtLabel(),
    )
}

private fun String.isIssuedRewardStatus(): Boolean {
    return trim().lowercase() in setOf("issued", "emitida", "emitted", "available", "disponivel", "disponível")
}

private fun String.toRewardStatusLabel(): String {
    return when (trim().lowercase()) {
        "issued", "emitida", "emitted", "available", "disponivel", "disponível" -> "Disponível"
        else -> replaceFirstChar { it.titlecase() }
    }
}

private fun String.toIssuedAtLabel(): String {
    return if (isBlank()) "Emitida" else toDateLabel()
}

private fun String.toDateLabel(): String {
    val date = substringBefore("T")
    val parts = date.split("-")
    if (parts.size != 3) return date.ifBlank { "Emitida" }
    val year = parts[0]
    val month = parts[1].toIntOrNull()?.let { rewardMonthNames.getOrNull(it - 1) } ?: return date
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "Emitida em $day de $month, $year"
}

private val rewardMonthNames = listOf(
    "janeiro",
    "fevereiro",
    "março",
    "abril",
    "maio",
    "junho",
    "julho",
    "agosto",
    "setembro",
    "outubro",
    "novembro",
    "dezembro",
)

internal fun BookingBusinessInfoUiState.infoOrDefault(): BookingBusinessInfoUi {
    return when (this) {
        BookingBusinessInfoUiState.Idle,
        BookingBusinessInfoUiState.Loading -> DefaultBusinessInfo.toBookingBusinessInfoUi()
        is BookingBusinessInfoUiState.Loaded -> info
        is BookingBusinessInfoUiState.Error -> fallbackInfo
    }
}

private fun BusinessInfoError.toBookingBusinessInfoState(): BookingBusinessInfoUiState.Error {
    val retryable = this is BusinessInfoError.Unavailable || this is BusinessInfoError.Backend
    return BookingBusinessInfoUiState.Error(
        fallbackInfo = DefaultBusinessInfo.toBookingBusinessInfoUi(),
        message = message,
        retryable = retryable,
    )
}

private fun BusinessInfo.toBookingBusinessInfoUi(): BookingBusinessInfoUi = BookingBusinessInfoUi(
    phone = phone.trim().ifBlank { DefaultBusinessInfo.phone },
    addressLine1 = addressLine1.trim().ifBlank { DefaultBusinessInfo.addressLine1 },
    addressLine2 = addressLine2.trim().ifBlank { DefaultBusinessInfo.addressLine2 },
    openingHours = openingHours.mapNotNull { it.toBookingOpeningHoursUiOrNull() }.ifEmpty {
        DefaultBusinessInfo.openingHours.mapNotNull { it.toBookingOpeningHoursUiOrNull() }
    },
)

private fun BusinessOpeningHours.toBookingOpeningHoursUiOrNull(): BookingOpeningHoursUi? {
    val day = dayLabel.trim()
    val hours = hoursLabel.trim()
    if (day.isBlank() || hours.isBlank()) return null
    return BookingOpeningHoursUi(
        dayLabel = day,
        hoursLabel = hours,
        closed = closed,
    )
}

private fun UserVehicle.toBookingVehicleUiOrNull(): BookingVehicleUi? {
    if (id.isBlank() || brand.isBlank() || model.isBlank() || plate.isBlank()) return null
    val label = "$brand $model".trim()
    val normalizedType = type.toBookingVehicleType()
    val colorDetail = color.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()

    return BookingVehicleUi(
        id = "saved:$id",
        name = label,
        description = "$plate$colorDetail • ${normalizedType.toVehicleTypeLabel()}",
        type = normalizedType,
        userVehicleId = id,
        vehicleLabel = label,
        isDefault = isDefault,
    )
}

private fun String.toBookingVehicleType(): String = when (trim().lowercase()) {
    "suv" -> "suv"
    else -> "passenger"
}

private fun String.toVehicleTypeLabel(): String = when (this) {
    "suv" -> "SUV"
    else -> "Passageiros"
}

internal fun ProductsBookingDraft.toCreateRequest(): BookingCreateRequest? {
    val slotStartIso = buildSlotIso(dateId = dateId, time = time) ?: return null
    val slotEndIso = buildSlotEndIso(
        dateId = dateId,
        time = time,
        durationMinutes = serviceDurationMinutes,
    ) ?: return null

    return BookingCreateRequest(
        customerName = customerName,
        customerEmail = customerEmail,
        customerPhone = customerPhone,
        serviceId = serviceId,
        serviceName = serviceName,
        slotStartIso = slotStartIso,
        slotEndIso = slotEndIso,
        vehicleType = when (vehicleType) {
            "passenger" -> "passageiros"
            else -> vehicleType
        },
        userVehicleId = userVehicleId,
        vehicleLabel = vehicleLabel,
        loyaltyRewardCode = loyaltyRewardCode,
        extraIds = extraIds,
        gdprConsent = gdprConsent,
        notes = notes,
    )
}

private fun BookingCreateRequest.requiresAuthenticatedSession(): Boolean {
    return !loyaltyRewardCode.isNullOrBlank() || !userVehicleId.isNullOrBlank()
}

private fun buildSlotIso(dateId: String, time: String): String? {
    val (hour, minute) = parseTime(time) ?: return null
    if (!isValidDateId(dateId)) return null
    return "$dateId" + "T" + hour.twoDigits() + ":" + minute.twoDigits() + ":00.000Z"
}

private fun buildSlotEndIso(dateId: String, time: String, durationMinutes: Int): String? {
    val (hour, minute) = parseTime(time) ?: return null
    if (!isValidDateId(dateId) || durationMinutes <= 0) return null

    val endTotalMinutes = hour * 60 + minute + durationMinutes
    if (endTotalMinutes >= 24 * 60) return null

    val endHour = endTotalMinutes / 60
    val endMinute = endTotalMinutes % 60
    return "$dateId" + "T" + endHour.twoDigits() + ":" + endMinute.twoDigits() + ":00.000Z"
}

private fun parseTime(time: String): Pair<Int, Int>? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

private fun isValidDateId(dateId: String): Boolean {
    if (dateId.length != 10) return false
    if (dateId[4] != '-' || dateId[7] != '-') return false

    val year = dateId.substring(0, 4).toIntOrNull() ?: return false
    val month = dateId.substring(5, 7).toIntOrNull() ?: return false
    val day = dateId.substring(8, 10).toIntOrNull() ?: return false
    return year > 0 && month in 1..12 && day in 1..31
}

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')

internal fun BookingAvailabilityMonth.monthAnchorDate(): String? {
    return days.firstOrNull()?.id.toMonthAnchorDateOrNull()
}

internal fun String?.toMonthAnchorDateOrNull(): String? {
    val dateId = this ?: return null
    if (!isValidDateId(dateId)) return null
    return dateId.substring(0, 8) + "01"
}

internal fun shiftMonthAnchorDate(dateId: String?, monthOffset: Int): String? {
    val monthAnchor = dateId.toMonthAnchorDateOrNull() ?: return null
    val year = monthAnchor.substring(0, 4).toIntOrNull() ?: return null
    val month = monthAnchor.substring(5, 7).toIntOrNull() ?: return null
    val zeroBasedMonth = month - 1
    val shiftedMonthIndex = year * 12 + zeroBasedMonth + monthOffset
    if (shiftedMonthIndex < 12) return null

    val shiftedYear = shiftedMonthIndex / 12
    val shiftedMonth = shiftedMonthIndex % 12 + 1
    return "$shiftedYear-${shiftedMonth.twoDigits()}-01"
}
