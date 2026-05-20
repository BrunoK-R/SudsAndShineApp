package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.profile.MutableUserProfileChangeNotifier
import com.sudsmobile.data.profile.UserProfile
import com.sudsmobile.data.profile.UserProfileChangeNotifier
import com.sudsmobile.data.profile.UserProfileError
import com.sudsmobile.data.profile.UserProfileMutationResult
import com.sudsmobile.data.profile.UserProfileRepository
import com.sudsmobile.data.profile.UserProfileResult
import com.sudsmobile.data.profile.UserProfileSaveRequest
import com.sudsmobile.data.vehicle.MutableUserVehicleChangeNotifier
import com.sudsmobile.data.vehicle.UserVehicle
import com.sudsmobile.data.vehicle.UserVehicleChangeNotifier
import com.sudsmobile.data.vehicle.UserVehicleError
import com.sudsmobile.data.vehicle.UserVehicleListResult
import com.sudsmobile.data.vehicle.UserVehicleRepository
import com.sudsmobile.shared.loyalty.toLoyaltyProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ProfileStatsUi(
    val washCount: String,
    val loyaltyRemaining: String,
    val vehicleCount: String,
)

internal sealed interface ProfileStatsUiState {
    data object Idle : ProfileStatsUiState
    data object Loading : ProfileStatsUiState
    data object Unauthenticated : ProfileStatsUiState
    data class Loaded(
        val stats: ProfileStatsUi,
        val warningMessage: String? = null,
        val warningRetryable: Boolean = false,
    ) : ProfileStatsUiState
    data class Error(val message: String, val retryable: Boolean) : ProfileStatsUiState
}

internal data class ProfilePreferencesUi(
    val displayName: String,
    val phoneNumber: String,
    val marketingOptIn: Boolean,
)

internal sealed interface ProfilePreferencesUiState {
    data object Idle : ProfilePreferencesUiState
    data object Loading : ProfilePreferencesUiState
    data object Unauthenticated : ProfilePreferencesUiState
    data class Loaded(val preferences: ProfilePreferencesUi) : ProfilePreferencesUiState
    data class Saving(val preferences: ProfilePreferencesUi) : ProfilePreferencesUiState
    data class Saved(val preferences: ProfilePreferencesUi, val message: String) : ProfilePreferencesUiState
    data class Error(val message: String, val retryable: Boolean) : ProfilePreferencesUiState
    data class SaveError(
        val preferences: ProfilePreferencesUi,
        val message: String,
        val retryable: Boolean,
    ) : ProfilePreferencesUiState
}

internal class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository,
    private val userVehicleRepository: UserVehicleRepository,
    private val userProfileRepository: UserProfileRepository,
    private val bookingChangeNotifier: BookingChangeNotifier = MutableBookingChangeNotifier(),
    private val userVehicleChangeNotifier: UserVehicleChangeNotifier = MutableUserVehicleChangeNotifier(),
    private val userProfileChangeNotifier: UserProfileChangeNotifier = MutableUserProfileChangeNotifier(),
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision
    val vehicleRevision: StateFlow<Long> = userVehicleChangeNotifier.revision
    val profileRevision: StateFlow<Long> = userProfileChangeNotifier.revision
    private val _statsState = MutableStateFlow<ProfileStatsUiState>(ProfileStatsUiState.Idle)
    val statsState: StateFlow<ProfileStatsUiState> = _statsState.asStateFlow()
    private val _preferencesState = MutableStateFlow<ProfilePreferencesUiState>(ProfilePreferencesUiState.Idle)
    val preferencesState: StateFlow<ProfilePreferencesUiState> = _preferencesState.asStateFlow()
    private var loadedUid: String? = null
    private var loadedBookingRevision: Long? = null
    private var loadedVehicleRevision: Long? = null
    private var loadedPreferencesUid: String? = null
    private var loadedProfileRevision: Long? = null

    fun loadStats() {
        if (_statsState.value is ProfileStatsUiState.Loading) return

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            loadedBookingRevision = null
            loadedVehicleRevision = null
            _statsState.value = ProfileStatsUiState.Unauthenticated
            return
        }
        val requestedUid = session.session.user.uid
        val requestedBookingRevision = bookingRevision.value
        val requestedVehicleRevision = vehicleRevision.value

        viewModelScope.launch {
            _statsState.value = ProfileStatsUiState.Loading
            val nextState = coroutineScope {
                val historyDeferred = async { bookingRepository.getMyBookings() }
                val vehiclesDeferred = async { userVehicleRepository.getMyVehicles() }
                buildProfileStatsState(
                    historyResult = historyDeferred.await(),
                    vehiclesResult = vehiclesDeferred.await(),
                )
            }
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedUid = requestedUid
                loadedBookingRevision = requestedBookingRevision
                loadedVehicleRevision = requestedVehicleRevision
                _statsState.value = nextState
            } else {
                loadedUid = null
                loadedBookingRevision = null
                loadedVehicleRevision = null
            }
        }
    }

    fun loadPreferences() {
        if (_preferencesState.value is ProfilePreferencesUiState.Loading ||
            _preferencesState.value is ProfilePreferencesUiState.Saving
        ) {
            return
        }

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedPreferencesUid = null
            loadedProfileRevision = null
            _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
            return
        }
        val requestedUid = session.session.user.uid
        val requestedProfileRevision = profileRevision.value

        viewModelScope.launch {
            _preferencesState.value = ProfilePreferencesUiState.Loading
            val nextState = when (val result = userProfileRepository.getMyProfile()) {
                is UserProfileResult.Success -> ProfilePreferencesUiState.Loaded(result.profile.toPreferencesUi())
                is UserProfileResult.Failure -> result.error.toPreferencesState()
            }
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid == requestedUid) {
                loadedPreferencesUid = requestedUid
                loadedProfileRevision = requestedProfileRevision
                _preferencesState.value = nextState
            } else {
                loadedPreferencesUid = null
                loadedProfileRevision = null
                _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
            }
        }
    }

    fun updateMarketingOptIn(marketingOptIn: Boolean) {
        if (_preferencesState.value is ProfilePreferencesUiState.Saving) return

        val currentPreferences = _preferencesState.value.preferencesOrNull() ?: return
        val validationError = currentPreferences.profileSaveValidationError()
        if (validationError != null) {
            _preferencesState.value = ProfilePreferencesUiState.SaveError(
                preferences = currentPreferences,
                message = validationError,
                retryable = false,
            )
            return
        }

        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedPreferencesUid = null
            loadedProfileRevision = null
            _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
            return
        }
        val requestedUid = session.session.user.uid
        val requestedPreferences = currentPreferences.copy(marketingOptIn = marketingOptIn)

        viewModelScope.launch {
            _preferencesState.value = ProfilePreferencesUiState.Saving(requestedPreferences)
            val result = userProfileRepository.updateMyProfile(
                UserProfileSaveRequest(
                    displayName = currentPreferences.displayName,
                    phoneNumber = currentPreferences.phoneNumber,
                    marketingOptIn = marketingOptIn,
                ),
            )
            val currentUid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
            if (currentUid != requestedUid) {
                loadedPreferencesUid = null
                loadedProfileRevision = null
                _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
                return@launch
            }

            when (result) {
                is UserProfileMutationResult.Success -> {
                    loadedPreferencesUid = requestedUid
                    loadedProfileRevision = profileRevision.value
                    _preferencesState.value = ProfilePreferencesUiState.Saved(
                        preferences = result.profile.toPreferencesUi(),
                        message = "Preferências atualizadas.",
                    )
                }
                is UserProfileMutationResult.Failure -> {
                    _preferencesState.value = result.error.toPreferenceSaveState(currentPreferences)
                }
            }
        }
    }

    fun refreshForSession() {
        val session = sessionState.value as? AuthSessionState.Authenticated
        if (session == null) {
            loadedUid = null
            loadedBookingRevision = null
            loadedVehicleRevision = null
            loadedPreferencesUid = null
            loadedProfileRevision = null
            _statsState.value = ProfileStatsUiState.Unauthenticated
            _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
            return
        }

        refreshStatsForSession(session)
        refreshPreferencesForSession(session)
    }

    private fun refreshStatsForSession(session: AuthSessionState.Authenticated) {
        val uid = session.session.user.uid
        val currentBookingRevision = bookingRevision.value
        val currentVehicleRevision = vehicleRevision.value
        val hasReusableState = _statsState.value is ProfileStatsUiState.Loaded
        if (
            loadedUid == uid &&
            loadedBookingRevision == currentBookingRevision &&
            loadedVehicleRevision == currentVehicleRevision &&
            hasReusableState
        ) {
            return
        }

        loadStats()
    }

    private fun refreshPreferencesForSession(session: AuthSessionState.Authenticated) {
        val uid = session.session.user.uid
        val currentProfileRevision = profileRevision.value
        val hasReusableState = _preferencesState.value is ProfilePreferencesUiState.Loaded ||
            _preferencesState.value is ProfilePreferencesUiState.Saved ||
            _preferencesState.value is ProfilePreferencesUiState.SaveError
        if (
            loadedPreferencesUid == uid &&
            loadedProfileRevision == currentProfileRevision &&
            hasReusableState
        ) {
            return
        }

        loadPreferences()
    }

    fun signOut() {
        authRepository.signOut()
        loadedUid = null
        loadedBookingRevision = null
        loadedVehicleRevision = null
        loadedPreferencesUid = null
        loadedProfileRevision = null
        _statsState.value = ProfileStatsUiState.Unauthenticated
        _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
    }
}

private fun buildProfileStatsState(
    historyResult: BookingHistoryResult,
    vehiclesResult: UserVehicleListResult,
): ProfileStatsUiState {
    val history = when (historyResult) {
        is BookingHistoryResult.Success -> historyResult.history
        is BookingHistoryResult.Failure -> return historyResult.error.toProfileStatsState()
    }

    return when (vehiclesResult) {
        is UserVehicleListResult.Success -> ProfileStatsUiState.Loaded(
            stats = history.toProfileStats(vehicleCount = vehiclesResult.vehicles.validVehicleCount()),
        )
        is UserVehicleListResult.Failure -> ProfileStatsUiState.Loaded(
            stats = history.toProfileStats(vehicleCount = 0),
            warningMessage = vehiclesResult.error.message,
            warningRetryable = vehiclesResult.error.isRetryableProfileStatsError(),
        )
    }
}

private fun BookingHistory.toProfileStats(vehicleCount: Int): ProfileStatsUi {
    val completedWashCount = reservations.count { !it.upcoming && !it.isCancelled() }
    val loyaltyProgress = completedWashCount.toLoyaltyProgress()

    return ProfileStatsUi(
        washCount = completedWashCount.toString(),
        loyaltyRemaining = loyaltyProgress.remainingWashes.toString(),
        vehicleCount = vehicleCount.toString(),
    )
}

private fun BookingHistoryReservation.isCancelled(): Boolean {
    val normalized = status.lowercase()
    return normalized in setOf("cancelled", "canceled", "cancelado")
}

private fun List<UserVehicle>.validVehicleCount(): Int {
    return count { vehicle ->
        vehicle.id.isNotBlank() &&
            vehicle.brand.isNotBlank() &&
            vehicle.model.isNotBlank() &&
            vehicle.plate.isNotBlank()
    }
}

private fun BookingHistoryError.toProfileStatsState(): ProfileStatsUiState {
    return when (this) {
        is BookingHistoryError.Unauthenticated -> ProfileStatsUiState.Unauthenticated
        is BookingHistoryError.Permission -> ProfileStatsUiState.Error(message = message, retryable = false)
        is BookingHistoryError.Unavailable,
        is BookingHistoryError.Backend -> ProfileStatsUiState.Error(message = message, retryable = true)
    }
}

private fun UserVehicleError.isRetryableProfileStatsError(): Boolean {
    return when (this) {
        is UserVehicleError.Unavailable,
        is UserVehicleError.Backend,
        is UserVehicleError.NotFound -> true
        is UserVehicleError.Validation,
        is UserVehicleError.Permission,
        is UserVehicleError.Unauthenticated -> false
    }
}

private fun UserProfile.toPreferencesUi(): ProfilePreferencesUi = ProfilePreferencesUi(
    displayName = displayName,
    phoneNumber = phoneNumber,
    marketingOptIn = marketingOptIn,
)

private fun ProfilePreferencesUiState.preferencesOrNull(): ProfilePreferencesUi? {
    return when (this) {
        is ProfilePreferencesUiState.Loaded -> preferences
        is ProfilePreferencesUiState.Saving -> preferences
        is ProfilePreferencesUiState.Saved -> preferences
        is ProfilePreferencesUiState.SaveError -> preferences
        ProfilePreferencesUiState.Idle,
        ProfilePreferencesUiState.Loading,
        ProfilePreferencesUiState.Unauthenticated,
        is ProfilePreferencesUiState.Error -> null
    }
}

private val preferencePhoneSeparators = setOf('+', '-', '(', ')', '.', ' ')

private fun ProfilePreferencesUi.profileSaveValidationError(): String? {
    val phone = phoneNumber.trim()
    return when {
        displayName.isBlank() -> "Complete os dados pessoais antes de atualizar preferências."
        phone.length < 6 -> "Complete o telemóvel nos dados pessoais antes de atualizar preferências."
        phone.length > 32 || !phone.all { it.isDigit() || it in preferencePhoneSeparators } ->
            "Complete o telemóvel nos dados pessoais antes de atualizar preferências."
        else -> null
    }
}

private fun UserProfileError.toPreferencesState(): ProfilePreferencesUiState {
    return when (this) {
        is UserProfileError.Unauthenticated -> ProfilePreferencesUiState.Unauthenticated
        is UserProfileError.Permission,
        is UserProfileError.Validation -> ProfilePreferencesUiState.Error(message = message, retryable = false)
        is UserProfileError.Unavailable,
        is UserProfileError.Backend -> ProfilePreferencesUiState.Error(message = message, retryable = true)
    }
}

private fun UserProfileError.toPreferenceSaveState(
    preferences: ProfilePreferencesUi,
): ProfilePreferencesUiState.SaveError {
    return when (this) {
        is UserProfileError.Validation -> ProfilePreferencesUiState.SaveError(preferences, message, retryable = false)
        is UserProfileError.Unauthenticated -> ProfilePreferencesUiState.SaveError(preferences, message, retryable = false)
        is UserProfileError.Permission -> ProfilePreferencesUiState.SaveError(preferences, message, retryable = false)
        is UserProfileError.Unavailable,
        is UserProfileError.Backend -> ProfilePreferencesUiState.SaveError(preferences, message, retryable = true)
    }
}
