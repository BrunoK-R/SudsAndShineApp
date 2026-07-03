package com.sudsmobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.booking.toLoyaltyProgress as toBackendLoyaltyProgress
import com.sudsmobile.data.booking.isCompletedReservation
import com.sudsmobile.data.notification.NotificationDeviceRegistrar
import com.sudsmobile.data.notification.NotificationRepository
import com.sudsmobile.data.notification.NotificationTokenDeleteRequest
import com.sudsmobile.data.notification.NotificationTokenDeleteResult
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
import kotlinx.coroutines.withTimeoutOrNull

internal data class ProfileStatsUi(
    val washCount: String,
    val loyaltyRemaining: String,
    val vehicleCount: String,
    val rewardReady: Boolean = false,
    val availableRewards: Int = 0,
    val rewardDescription: String = "1 lavagem grátis",
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
    val photoUrl: String,
    val marketingOptIn: Boolean,
    val appointmentReminderOptIn: Boolean,
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
    private val notificationRepository: NotificationRepository? = null,
    private val notificationDeviceRegistrar: NotificationDeviceRegistrar? = null,
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
    private var loadingStatsUid: String? = null
    private var loadingStatsBookingRevision: Long? = null
    private var loadingStatsVehicleRevision: Long? = null
    private var statsRequestSequence: Long = 0
    private var loadedPreferencesUid: String? = null
    private var loadedProfileRevision: Long? = null
    private var loadingPreferencesUid: String? = null
    private var loadingPreferencesProfileRevision: Long? = null
    private var preferencesRequestSequence: Long = 0
    private var signOutInProgress: Boolean = false

    fun loadStats() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedStats()
                _statsState.value = ProfileStatsUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedStats()
                _statsState.value = currentSessionState.error.toProfileStatsErrorState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedStats()
                _statsState.value = ProfileStatsUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }
        val requestedUid = session.session.user.uid
        val requestedBookingRevision = bookingRevision.value
        val requestedVehicleRevision = vehicleRevision.value
        val sameRequestInFlight = loadingStatsUid == requestedUid &&
            loadingStatsBookingRevision == requestedBookingRevision &&
            loadingStatsVehicleRevision == requestedVehicleRevision
        if (sameRequestInFlight) return

        val requestSequence = ++statsRequestSequence
        loadingStatsUid = requestedUid
        loadingStatsBookingRevision = requestedBookingRevision
        loadingStatsVehicleRevision = requestedVehicleRevision

        viewModelScope.launch {
            try {
                _statsState.value = ProfileStatsUiState.Loading
                val nextState = coroutineScope {
                    val historyDeferred = async { bookingRepository.getMyBookings() }
                    val vehiclesDeferred = async { userVehicleRepository.getMyVehicles() }
                    buildProfileStatsState(
                        historyResult = historyDeferred.await(),
                        vehiclesResult = vehiclesDeferred.await(),
                    )
                }
                if (requestSequence != statsRequestSequence) return@launch

                when (val currentSessionState = sessionState.value) {
                    AuthSessionState.Restoring -> {
                        clearLoadedStats()
                        _statsState.value = ProfileStatsUiState.Loading
                    }
                    is AuthSessionState.RestoreFailed -> {
                        clearLoadedStats()
                        _statsState.value = currentSessionState.error.toProfileStatsErrorState()
                    }
                    AuthSessionState.Unauthenticated -> {
                        clearLoadedStats()
                        _statsState.value = ProfileStatsUiState.Unauthenticated
                    }
                    is AuthSessionState.Authenticated -> {
                        if (currentSessionState.session.user.uid == requestedUid) {
                            loadedUid = requestedUid
                            loadedBookingRevision = requestedBookingRevision
                            loadedVehicleRevision = requestedVehicleRevision
                            _statsState.value = nextState
                        } else {
                            clearLoadedStats()
                            _statsState.value = ProfileStatsUiState.Unauthenticated
                        }
                    }
                }
            } finally {
                if (requestSequence == statsRequestSequence) {
                    clearLoadingStatsRequest()
                }
            }
        }
    }

    fun loadPreferences() {
        if (_preferencesState.value is ProfilePreferencesUiState.Saving) {
            return
        }

        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedPreferences()
                _preferencesState.value = ProfilePreferencesUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedPreferences()
                _preferencesState.value = currentSessionState.error.toProfilePreferencesErrorState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedPreferences()
                _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
        }
        val requestedUid = session.session.user.uid
        val requestedProfileRevision = profileRevision.value
        val sameRequestInFlight = loadingPreferencesUid == requestedUid &&
            loadingPreferencesProfileRevision == requestedProfileRevision
        if (sameRequestInFlight) return

        val requestSequence = ++preferencesRequestSequence
        loadingPreferencesUid = requestedUid
        loadingPreferencesProfileRevision = requestedProfileRevision

        viewModelScope.launch {
            try {
                _preferencesState.value = ProfilePreferencesUiState.Loading
                val nextState = when (val result = userProfileRepository.getMyProfile()) {
                    is UserProfileResult.Success -> ProfilePreferencesUiState.Loaded(result.profile.toPreferencesUi())
                    is UserProfileResult.Failure -> result.error.toPreferencesState()
                }
                if (requestSequence != preferencesRequestSequence) return@launch

                when (val currentSessionState = sessionState.value) {
                    AuthSessionState.Restoring -> {
                        clearLoadedPreferences()
                        _preferencesState.value = ProfilePreferencesUiState.Loading
                    }
                    is AuthSessionState.RestoreFailed -> {
                        clearLoadedPreferences()
                        _preferencesState.value = currentSessionState.error.toProfilePreferencesErrorState()
                    }
                    AuthSessionState.Unauthenticated -> {
                        clearLoadedPreferences()
                        _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
                    }
                    is AuthSessionState.Authenticated -> {
                        if (currentSessionState.session.user.uid == requestedUid) {
                            loadedPreferencesUid = requestedUid
                            loadedProfileRevision = requestedProfileRevision
                            _preferencesState.value = nextState
                        } else {
                            clearLoadedPreferences()
                            _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
                        }
                    }
                }
            } finally {
                if (requestSequence == preferencesRequestSequence) {
                    clearLoadingPreferencesRequest()
                }
            }
        }
    }

    fun updateMarketingOptIn(marketingOptIn: Boolean) {
        updatePreferences { it.copy(marketingOptIn = marketingOptIn) }
    }

    fun updateAppointmentReminderOptIn(appointmentReminderOptIn: Boolean) {
        updatePreferences { it.copy(appointmentReminderOptIn = appointmentReminderOptIn) }
    }

    fun updateProfilePhotoUrl(photoUrl: String) {
        updatePreferences { it.copy(photoUrl = photoUrl) }
    }

    fun retryPreferenceSave() {
        updatePreferences { it }
    }

    private fun updatePreferences(
        transform: (ProfilePreferencesUi) -> ProfilePreferencesUi,
    ) {
        if (_preferencesState.value is ProfilePreferencesUiState.Saving) return

        val currentPreferences = _preferencesState.value.preferencesOrNull() ?: return
        val requestedPreferences = transform(currentPreferences)
        val validationError = requestedPreferences.profileSaveValidationError()
        if (validationError != null) {
            _preferencesState.value = ProfilePreferencesUiState.SaveError(
                preferences = requestedPreferences,
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

        viewModelScope.launch {
            _preferencesState.value = ProfilePreferencesUiState.Saving(requestedPreferences)
            if (!preferencesSessionStillMatches(requestedUid)) {
                handlePreferencesSessionChangedDuringMutation()
                return@launch
            }

            val result = userProfileRepository.updateMyProfile(
                UserProfileSaveRequest(
                    displayName = requestedPreferences.displayName,
                    phoneNumber = requestedPreferences.phoneNumber,
                    marketingOptIn = requestedPreferences.marketingOptIn,
                    appointmentReminderOptIn = requestedPreferences.appointmentReminderOptIn,
                    photoUrl = requestedPreferences.photoUrl,
                ),
            )
            if (!preferencesSessionStillMatches(requestedUid)) {
                handlePreferencesSessionChangedDuringMutation()
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
                    _preferencesState.value = result.error.toPreferenceSaveState(requestedPreferences)
                }
            }
        }
    }

    fun refreshForSession() {
        val session = when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedStats()
                clearLoadedPreferences()
                _statsState.value = ProfileStatsUiState.Loading
                _preferencesState.value = ProfilePreferencesUiState.Loading
                return
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedStats()
                clearLoadedPreferences()
                _statsState.value = currentSessionState.error.toProfileStatsErrorState()
                _preferencesState.value = currentSessionState.error.toProfilePreferencesErrorState()
                return
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedStats()
                clearLoadedPreferences()
                _statsState.value = ProfileStatsUiState.Unauthenticated
                _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
                return
            }
            is AuthSessionState.Authenticated -> currentSessionState
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
        clearLoadedStats()
        clearLoadedPreferences()
        _statsState.value = ProfileStatsUiState.Unauthenticated
        _preferencesState.value = ProfilePreferencesUiState.Unauthenticated

        val uid = (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid
        if (uid == null) {
            authRepository.signOut()
            return
        }
        if (signOutInProgress) return

        signOutInProgress = true
        viewModelScope.launch {
            try {
                withTimeoutOrNull(SIGN_OUT_NOTIFICATION_REVOCATION_TIMEOUT_MS) {
                    revokeNotificationTokenForSignOut(uid)
                }
            } finally {
                authRepository.signOut()
                signOutInProgress = false
            }
        }
    }

    private suspend fun revokeNotificationTokenForSignOut(uid: String) {
        val notificationRepository = notificationRepository ?: return
        val notificationDeviceRegistrar = notificationDeviceRegistrar ?: return
        val tokenId = runCatching {
            notificationDeviceRegistrar.currentState(uid).registeredTokenId
        }.getOrNull()?.takeUnless { it.isBlank() } ?: return

        val result = runCatching {
            notificationRepository.deleteNotificationToken(NotificationTokenDeleteRequest(tokenId))
        }.getOrNull()
        if (result is NotificationTokenDeleteResult.Success) {
            runCatching {
                notificationDeviceRegistrar.markDeleted(uid, result.tokenId)
            }
        }
    }

    private fun clearLoadedStats() {
        loadedUid = null
        loadedBookingRevision = null
        loadedVehicleRevision = null
        clearLoadingStatsRequest()
        statsRequestSequence += 1
    }

    private fun clearLoadingStatsRequest() {
        loadingStatsUid = null
        loadingStatsBookingRevision = null
        loadingStatsVehicleRevision = null
    }

    private fun clearLoadedPreferences() {
        loadedPreferencesUid = null
        loadedProfileRevision = null
        clearLoadingPreferencesRequest()
        preferencesRequestSequence += 1
    }

    private fun clearLoadingPreferencesRequest() {
        loadingPreferencesUid = null
        loadingPreferencesProfileRevision = null
    }

    private fun preferencesSessionStillMatches(uid: String): Boolean {
        return (sessionState.value as? AuthSessionState.Authenticated)?.session?.user?.uid == uid
    }

    private fun handlePreferencesSessionChangedDuringMutation() {
        when (val currentSessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                clearLoadedPreferences()
                _preferencesState.value = ProfilePreferencesUiState.Loading
            }
            is AuthSessionState.RestoreFailed -> {
                clearLoadedPreferences()
                _preferencesState.value = currentSessionState.error.toProfilePreferencesErrorState()
            }
            AuthSessionState.Unauthenticated -> {
                clearLoadedPreferences()
                _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
            }
            is AuthSessionState.Authenticated -> {
                clearLoadedPreferences()
                _preferencesState.value = ProfilePreferencesUiState.Unauthenticated
            }
        }
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
    val completedWashCount = reservations.count { it.isCompletedReservation() }
    val loyaltyProgress = this.loyalty?.toBackendLoyaltyProgress() ?: completedWashCount.toLoyaltyProgress()

    return ProfileStatsUi(
        washCount = completedWashCount.toString(),
        loyaltyRemaining = loyaltyProgress.remainingWashes.toString(),
        vehicleCount = vehicleCount.toString(),
        rewardReady = loyaltyProgress.rewardReady,
        availableRewards = loyaltyProgress.availableRewards,
        rewardDescription = loyalty?.rewardDescription?.trim()?.ifBlank { null } ?: "1 lavagem grátis",
    )
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
    photoUrl = photoUrl,
    marketingOptIn = marketingOptIn,
    appointmentReminderOptIn = appointmentReminderOptIn,
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
        photoUrl.isNotBlank() && !photoUrl.isValidProfilePhotoUrl() ->
            "Indique uma URL de fotografia válida."
        else -> null
    }
}

private fun String.isValidProfilePhotoUrl(): Boolean {
    val value = trim()
    if (value.length !in 1..2048) return false
    if (value.any { it.isWhitespace() || it.isISOControl() }) return false
    return value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("http://", ignoreCase = true)
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

private fun AuthError.toProfileStatsErrorState(): ProfileStatsUiState.Error {
    return ProfileStatsUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.toProfilePreferencesErrorState(): ProfilePreferencesUiState.Error {
    return ProfilePreferencesUiState.Error(message = message, retryable = isRetryableSessionError())
}

private fun AuthError.isRetryableSessionError(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private const val SIGN_OUT_NOTIFICATION_REVOCATION_TIMEOUT_MS = 2_000L
