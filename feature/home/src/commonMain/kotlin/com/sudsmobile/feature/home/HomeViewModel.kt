package com.sudsmobile.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudsmobile.data.auth.AuthError
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.auth.AuthUser
import com.sudsmobile.data.booking.BookingChangeNotifier
import com.sudsmobile.data.booking.BookingHistory
import com.sudsmobile.data.booking.BookingHistoryError
import com.sudsmobile.data.booking.BookingHistoryReservation
import com.sudsmobile.data.booking.BookingHistoryResult
import com.sudsmobile.data.booking.BookingReservationStatus
import com.sudsmobile.data.booking.BookingRepository
import com.sudsmobile.data.booking.toLoyaltyProgress as toBackendLoyaltyProgress
import com.sudsmobile.data.booking.isCancelledReservation
import com.sudsmobile.data.booking.isCompletedReservation
import com.sudsmobile.data.booking.toBookingReservationStatus
import com.sudsmobile.data.business.BusinessInfo
import com.sudsmobile.data.business.BusinessInfoError
import com.sudsmobile.data.business.BusinessInfoRepository
import com.sudsmobile.data.business.BusinessInfoResult
import com.sudsmobile.data.business.BusinessStat
import com.sudsmobile.data.business.DefaultBusinessInfo
import com.sudsmobile.data.catalog.ServiceCatalog
import com.sudsmobile.data.catalog.ServiceCatalogError
import com.sudsmobile.data.catalog.ServiceCatalogRepository
import com.sudsmobile.data.catalog.ServiceCatalogResult
import com.sudsmobile.data.catalog.ServiceCatalogService
import com.sudsmobile.shared.loyalty.LoyaltyProgress
import com.sudsmobile.shared.loyalty.toLoyaltyProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class HomeIdentityUi(
    val greeting: String,
    val subtitle: String,
    val initials: String,
)

internal data class HomeBookingUi(
    val id: String,
    val service: String,
    val date: String,
    val time: String,
    val location: String,
    val vehicle: String,
    val price: String,
    val statusLabel: String,
    val icon: ImageVector,
)

internal typealias HomeLoyaltyUi = LoyaltyProgress

internal data class HomeFeaturedServiceUi(
    val id: String,
    val name: String,
    val price: String,
    val duration: String,
    val icon: ImageVector,
    val popular: Boolean,
)

internal data class HomeStatUi(
    val value: String,
    val label: String,
    val icon: ImageVector,
)

internal sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState

    data class Unauthenticated(
        val identity: HomeIdentityUi,
        val featuredServices: List<HomeFeaturedServiceUi>,
        val stats: List<HomeStatUi>,
        val warningMessage: String? = null,
        val warningRetryable: Boolean = false,
        val statsWarningMessage: String? = null,
        val statsWarningRetryable: Boolean = false,
    ) : HomeUiState

    data class Empty(
        val identity: HomeIdentityUi,
        val loyalty: HomeLoyaltyUi,
        val featuredServices: List<HomeFeaturedServiceUi>,
        val stats: List<HomeStatUi>,
        val warningMessage: String? = null,
        val warningRetryable: Boolean = false,
        val statsWarningMessage: String? = null,
        val statsWarningRetryable: Boolean = false,
    ) : HomeUiState

    data class Loaded(
        val identity: HomeIdentityUi,
        val nextBooking: HomeBookingUi?,
        val loyalty: HomeLoyaltyUi,
        val featuredServices: List<HomeFeaturedServiceUi>,
        val stats: List<HomeStatUi>,
        val warningMessage: String? = null,
        val warningRetryable: Boolean = false,
        val statsWarningMessage: String? = null,
        val statsWarningRetryable: Boolean = false,
    ) : HomeUiState

    data class Error(
        val identity: HomeIdentityUi,
        val message: String,
        val retryable: Boolean,
        val featuredServices: List<HomeFeaturedServiceUi>,
        val stats: List<HomeStatUi>,
        val warningMessage: String? = null,
        val warningRetryable: Boolean = false,
        val statsWarningMessage: String? = null,
        val statsWarningRetryable: Boolean = false,
    ) : HomeUiState
}

internal class HomeViewModel(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository,
    private val serviceCatalogRepository: ServiceCatalogRepository,
    private val businessInfoRepository: BusinessInfoRepository,
    private val bookingChangeNotifier: BookingChangeNotifier,
) : ViewModel() {
    val sessionState: StateFlow<AuthSessionState> = authRepository.sessionState
    val bookingRevision: StateFlow<Long> = bookingChangeNotifier.revision

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadedSessionKey: String? = null
    private var loadedRevision: Long? = null

    fun refreshForSession(force: Boolean = false) {
        when (val sessionState = sessionState.value) {
            AuthSessionState.Restoring -> {
                loadedSessionKey = null
                loadedRevision = null
                _uiState.value = HomeUiState.Loading
            }

            is AuthSessionState.RestoreFailed -> {
                loadedSessionKey = null
                loadedRevision = null
                _uiState.value = HomeUiState.Error(
                    identity = GuestIdentity,
                    message = sessionState.error.message,
                    retryable = sessionState.error.isRetryable(),
                    featuredServices = emptyList(),
                    stats = DefaultHomeStats,
                )
            }

            AuthSessionState.Unauthenticated -> {
                loadedRevision = null
                if (!force && loadedSessionKey == GuestSessionKey && _uiState.value is HomeUiState.Unauthenticated) {
                    return
                }
                loadGuestHome()
            }

            is AuthSessionState.Authenticated -> {
                val uid = sessionState.session.user.uid
                val revision = bookingRevision.value
                val hasReusableState = _uiState.value is HomeUiState.Loaded ||
                    _uiState.value is HomeUiState.Empty
                if (!force && loadedSessionKey == uid && loadedRevision == revision && hasReusableState) {
                    return
                }
                loadAuthenticatedHome(
                    user = sessionState.session.user,
                    requestedUid = uid,
                    requestedRevision = revision,
                )
            }
        }
    }

    fun retry() {
        loadedSessionKey = null
        loadedRevision = null
        refreshForSession(force = true)
    }

    private fun loadGuestHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val catalog = serviceCatalogRepository.getServiceCatalog().toFeaturedServices()
            val businessInfo = businessInfoRepository.getBusinessInfo().toHomeStats()
            if (authRepository.sessionState.value == AuthSessionState.Unauthenticated) {
                loadedSessionKey = GuestSessionKey
                _uiState.value = HomeUiState.Unauthenticated(
                    identity = GuestIdentity,
                    featuredServices = catalog.services,
                    stats = businessInfo.stats,
                    warningMessage = catalog.warningMessage,
                    warningRetryable = catalog.warningRetryable,
                    statsWarningMessage = businessInfo.warningMessage,
                    statsWarningRetryable = businessInfo.warningRetryable,
                )
            }
        }
    }

    private fun loadAuthenticatedHome(
        user: AuthUser,
        requestedUid: String,
        requestedRevision: Long,
    ) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val nextState = coroutineScope {
                val catalogDeferred = async { serviceCatalogRepository.getServiceCatalog().toFeaturedServices() }
                val historyDeferred = async { bookingRepository.getMyBookings() }
                val businessInfoDeferred = async { businessInfoRepository.getBusinessInfo().toHomeStats() }
                buildAuthenticatedState(
                    identity = user.toHomeIdentity(),
                    catalog = catalogDeferred.await(),
                    historyResult = historyDeferred.await(),
                    businessInfo = businessInfoDeferred.await(),
                )
            }

            val currentUid = (authRepository.sessionState.value as? AuthSessionState.Authenticated)
                ?.session
                ?.user
                ?.uid
            if (currentUid == requestedUid) {
                loadedSessionKey = requestedUid
                loadedRevision = requestedRevision
                _uiState.value = nextState
            } else {
                loadedSessionKey = null
                loadedRevision = null
                _uiState.value = HomeUiState.Unauthenticated(
                    identity = GuestIdentity,
                    featuredServices = emptyList(),
                    stats = DefaultHomeStats,
                )
            }
        }
    }
}

internal fun HomeUiState.identityOrDefault(): HomeIdentityUi {
    return when (this) {
        HomeUiState.Idle,
        HomeUiState.Loading -> LoadingIdentity
        is HomeUiState.Unauthenticated -> identity
        is HomeUiState.Empty -> identity
        is HomeUiState.Loaded -> identity
        is HomeUiState.Error -> identity
    }
}

internal fun HomeUiState.featuredServicesOrEmpty(): List<HomeFeaturedServiceUi> {
    return when (this) {
        HomeUiState.Idle,
        HomeUiState.Loading -> emptyList()
        is HomeUiState.Unauthenticated -> featuredServices
        is HomeUiState.Empty -> featuredServices
        is HomeUiState.Loaded -> featuredServices
        is HomeUiState.Error -> featuredServices
    }
}

internal fun HomeUiState.statsOrDefault(): List<HomeStatUi> {
    return when (this) {
        HomeUiState.Idle,
        HomeUiState.Loading -> DefaultHomeStats
        is HomeUiState.Unauthenticated -> stats
        is HomeUiState.Empty -> stats
        is HomeUiState.Loaded -> stats
        is HomeUiState.Error -> stats
    }
}

internal fun HomeUiState.warningMessageOrNull(): String? {
    return when (this) {
        HomeUiState.Idle,
        HomeUiState.Loading -> null
        is HomeUiState.Unauthenticated -> warningMessage
        is HomeUiState.Empty -> warningMessage
        is HomeUiState.Loaded -> warningMessage
        is HomeUiState.Error -> warningMessage
    }
}

internal fun HomeUiState.warningRetryableOrFalse(): Boolean {
    return when (this) {
        HomeUiState.Idle,
        HomeUiState.Loading -> false
        is HomeUiState.Unauthenticated -> warningRetryable
        is HomeUiState.Empty -> warningRetryable
        is HomeUiState.Loaded -> warningRetryable
        is HomeUiState.Error -> warningRetryable
    }
}

internal fun HomeUiState.statsWarningMessageOrNull(): String? {
    return when (this) {
        HomeUiState.Idle,
        HomeUiState.Loading -> null
        is HomeUiState.Unauthenticated -> statsWarningMessage
        is HomeUiState.Empty -> statsWarningMessage
        is HomeUiState.Loaded -> statsWarningMessage
        is HomeUiState.Error -> statsWarningMessage
    }
}

internal fun HomeUiState.statsWarningRetryableOrFalse(): Boolean {
    return when (this) {
        HomeUiState.Idle,
        HomeUiState.Loading -> false
        is HomeUiState.Unauthenticated -> statsWarningRetryable
        is HomeUiState.Empty -> statsWarningRetryable
        is HomeUiState.Loaded -> statsWarningRetryable
        is HomeUiState.Error -> statsWarningRetryable
    }
}

private data class FeaturedServicesResult(
    val services: List<HomeFeaturedServiceUi>,
    val warningMessage: String? = null,
    val warningRetryable: Boolean = false,
)

private data class HomeBusinessInfoResult(
    val stats: List<HomeStatUi>,
    val location: String,
    val warningMessage: String? = null,
    val warningRetryable: Boolean = false,
)

private fun buildAuthenticatedState(
    identity: HomeIdentityUi,
    catalog: FeaturedServicesResult,
    historyResult: BookingHistoryResult,
    businessInfo: HomeBusinessInfoResult,
): HomeUiState {
    return when (historyResult) {
        is BookingHistoryResult.Success -> historyResult.history.toHomeState(
            identity = identity,
            catalog = catalog,
            businessInfo = businessInfo,
        )
        is BookingHistoryResult.Failure -> historyResult.error.toHomeErrorState(
            identity = identity,
            catalog = catalog,
            businessInfo = businessInfo,
        )
    }
}

private fun BookingHistory.toHomeState(
    identity: HomeIdentityUi,
    catalog: FeaturedServicesResult,
    businessInfo: HomeBusinessInfoResult,
): HomeUiState {
    val validReservations = reservations.filter { it.id.isNotBlank() && it.slotStartIso.isNotBlank() }
    val completedWashCount = validReservations.count { it.isCompletedReservation() }
    val loyaltyProgress = this.loyalty?.toBackendLoyaltyProgress() ?: completedWashCount.toLoyaltyUi()
    val nextBooking = validReservations
        .filter { it.upcoming && !it.isCancelledReservation() }
        .minByOrNull { it.slotStartIso }
        ?.toHomeBookingUi(location = businessInfo.location)

    return if (validReservations.isEmpty()) {
        HomeUiState.Empty(
            identity = identity,
            loyalty = loyaltyProgress,
            featuredServices = catalog.services,
            stats = businessInfo.stats,
            warningMessage = catalog.warningMessage,
            warningRetryable = catalog.warningRetryable,
            statsWarningMessage = businessInfo.warningMessage,
            statsWarningRetryable = businessInfo.warningRetryable,
        )
    } else {
        HomeUiState.Loaded(
            identity = identity,
            nextBooking = nextBooking,
            loyalty = loyaltyProgress,
            featuredServices = catalog.services,
            stats = businessInfo.stats,
            warningMessage = catalog.warningMessage,
            warningRetryable = catalog.warningRetryable,
            statsWarningMessage = businessInfo.warningMessage,
            statsWarningRetryable = businessInfo.warningRetryable,
        )
    }
}

private fun BookingHistoryError.toHomeErrorState(
    identity: HomeIdentityUi,
    catalog: FeaturedServicesResult,
    businessInfo: HomeBusinessInfoResult,
): HomeUiState {
    return when (this) {
        is BookingHistoryError.Unauthenticated -> HomeUiState.Unauthenticated(
            identity = GuestIdentity,
            featuredServices = catalog.services,
            stats = businessInfo.stats,
            warningMessage = catalog.warningMessage,
            warningRetryable = catalog.warningRetryable,
            statsWarningMessage = businessInfo.warningMessage,
            statsWarningRetryable = businessInfo.warningRetryable,
        )
        is BookingHistoryError.Permission -> HomeUiState.Error(
            identity = identity,
            message = message,
            retryable = false,
            featuredServices = catalog.services,
            stats = businessInfo.stats,
            warningMessage = catalog.warningMessage,
            warningRetryable = catalog.warningRetryable,
            statsWarningMessage = businessInfo.warningMessage,
            statsWarningRetryable = businessInfo.warningRetryable,
        )
        is BookingHistoryError.Unavailable,
        is BookingHistoryError.Backend -> HomeUiState.Error(
            identity = identity,
            message = message,
            retryable = true,
            featuredServices = catalog.services,
            stats = businessInfo.stats,
            warningMessage = catalog.warningMessage,
            warningRetryable = catalog.warningRetryable,
            statsWarningMessage = businessInfo.warningMessage,
            statsWarningRetryable = businessInfo.warningRetryable,
        )
    }
}

private fun ServiceCatalogResult.toFeaturedServices(): FeaturedServicesResult {
    return when (this) {
        is ServiceCatalogResult.Success -> FeaturedServicesResult(
            services = catalog.toFeaturedServices(),
        )
        is ServiceCatalogResult.Failure -> FeaturedServicesResult(
            services = emptyList(),
            warningMessage = error.message,
            warningRetryable = error.isRetryable(),
        )
    }
}

private fun ServiceCatalog.toFeaturedServices(): List<HomeFeaturedServiceUi> {
    return services
        .mapNotNull { it.toHomeFeaturedServiceOrNull() }
        .sortedWith(compareByDescending<HomeFeaturedServiceUi> { it.popular }.thenBy { it.name.lowercase() })
        .take(2)
}

private fun ServiceCatalogService.toHomeFeaturedServiceOrNull(): HomeFeaturedServiceUi? {
    if (id.isBlank() || name.isBlank() || durationMinutes <= 0) return null

    return HomeFeaturedServiceUi(
        id = id,
        name = name,
        price = passengerPriceCents.toEuroLabel(),
        duration = "$durationMinutes min",
        icon = iconKey.toServiceIcon(),
        popular = popular,
    )
}

private fun BookingHistoryReservation.toHomeBookingUi(location: String): HomeBookingUi {
    return HomeBookingUi(
        id = id,
        service = serviceName.ifBlank { "Serviço" },
        date = slotStartIso.toDateLabel(),
        time = slotStartIso.toTimeLabel(),
        location = location,
        vehicle = vehicleLabel?.takeIf { it.isNotBlank() } ?: vehicleType.toVehicleLabel(),
        price = priceCents?.toEuroLabel() ?: "A confirmar",
        statusLabel = status.toStatusLabel(),
        icon = serviceIcon(),
    )
}

private fun AuthUser.toHomeIdentity(): HomeIdentityUi {
    val name = resolvedDisplayName
    return HomeIdentityUi(
        greeting = "Olá, ${name.firstNameOrFullName()}!",
        subtitle = "Bem-vindo de volta",
        initials = name.initialsFromNameOrEmail(email),
    )
}

private fun AuthError.isRetryable(): Boolean {
    return this is AuthError.Unavailable || this is AuthError.Backend
}

private fun BusinessInfoError.isRetryable(): Boolean {
    return this is BusinessInfoError.Unavailable || this is BusinessInfoError.Backend
}

private fun ServiceCatalogError.isRetryable(): Boolean {
    return this is ServiceCatalogError.Unavailable || this is ServiceCatalogError.Backend
}

private fun BookingHistoryReservation.serviceIcon(): ImageVector {
    val key = "$serviceId $serviceName".lowercase()
    return if ("premium" in key || "detalh" in key) {
        Icons.Filled.AutoAwesome
    } else {
        Icons.Filled.DirectionsCar
    }
}

private fun String.toServiceIcon(): ImageVector = when (lowercase()) {
    "sparkles", "auto_awesome", "premium" -> Icons.Filled.AutoAwesome
    "water", "water_drop", "droplets", "exterior" -> Icons.Filled.WaterDrop
    "sofa", "weekend", "interior" -> Icons.Filled.Weekend
    else -> Icons.Filled.DirectionsCar
}

private fun BusinessInfoResult.toHomeStats(): HomeBusinessInfoResult {
    return when (this) {
        is BusinessInfoResult.Success -> HomeBusinessInfoResult(
            stats = info.toHomeStats(),
            location = info.toHomeLocationLabel(),
        )
        is BusinessInfoResult.Failure -> HomeBusinessInfoResult(
            stats = DefaultHomeStats,
            location = DefaultBusinessInfo.toHomeLocationLabel(),
            warningMessage = error.message,
            warningRetryable = error.isRetryable(),
        )
    }
}

private fun BusinessInfo.toHomeStats(): List<HomeStatUi> {
    return stats
        .mapIndexedNotNull { index, stat -> stat.toHomeStatOrNull(index) }
        .ifEmpty { DefaultHomeStats }
        .take(MaxHomeStats)
}

private fun BusinessInfo.toHomeLocationLabel(): String {
    return listOf(addressLine1, addressLine2)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = ", ")
        .ifBlank { DefaultBusinessInfo.addressLine1 }
}

private fun BusinessStat.toHomeStatOrNull(index: Int): HomeStatUi? {
    val statValue = value.trim()
    val statLabel = label.trim()
    if (statValue.isBlank() || statLabel.isBlank()) return null

    return HomeStatUi(
        value = statValue,
        label = statLabel,
        icon = statLabel.toStatIcon(index),
    )
}

private fun String.toStatIcon(index: Int): ImageVector {
    val normalized = lowercase()
    return when {
        "avalia" in normalized || "rating" in normalized -> Icons.Filled.Star
        "ano" in normalized || "exper" in normalized -> Icons.Filled.EmojiEvents
        "carro" in normalized || "vehicle" in normalized -> Icons.Filled.DirectionsCar
        "premium" in normalized || "qualidade" in normalized -> Icons.Filled.Shield
        else -> DefaultStatIcons[index % DefaultStatIcons.size]
    }
}

private fun String.toStatusLabel(): String {
    return when (toBookingReservationStatus()) {
        BookingReservationStatus.Pending -> "Novo"
        BookingReservationStatus.Confirmed -> "Confirmado"
        BookingReservationStatus.InProgress -> "Em execução"
        BookingReservationStatus.Completed -> "Concluído"
        BookingReservationStatus.Cancelled -> "Cancelado"
        BookingReservationStatus.Unknown -> "A atualizar"
    }
}

private fun Int.toLoyaltyUi(): HomeLoyaltyUi = toLoyaltyProgress()

private fun String.toVehicleLabel(): String = when (lowercase()) {
    "suv" -> "SUV"
    "passageiros", "passenger" -> "Passageiros"
    else -> replaceFirstChar { it.titlecase() }
}

private fun String.toDateLabel(): String {
    val date = substringBefore("T")
    val parts = date.split("-")
    if (parts.size != 3) return date.ifBlank { "Data a confirmar" }
    val year = parts[0]
    val month = parts[1].toIntOrNull()?.let { monthNames.getOrNull(it - 1) } ?: return date
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "$day de $month, $year"
}

private fun String.toTimeLabel(): String {
    val time = substringAfter("T", missingDelimiterValue = "")
    return time.takeIf { it.length >= 5 }?.take(5) ?: "Hora a confirmar"
}

private fun Int.toEuroLabel(): String {
    val euros = this / 100
    val remainder = this % 100
    return "$euros,${remainder.toString().padStart(2, '0')}€"
}

private fun String.firstNameOrFullName(): String {
    return trim().split(" ").firstOrNull { it.isNotBlank() } ?: this
}

private fun String.initialsFromNameOrEmail(email: String): String {
    val parts = trim().split(" ").filter { it.isNotBlank() }
    val initials = when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}"
        parts.size == 1 -> parts.first().take(2)
        else -> email.substringBefore("@").take(2)
    }
    return initials.uppercase().ifBlank { "SS" }
}

private val GuestIdentity = HomeIdentityUi(
    greeting = "Olá!",
    subtitle = "Entre para acompanhar marcações",
    initials = "SS",
)

private val LoadingIdentity = HomeIdentityUi(
    greeting = "Olá!",
    subtitle = "A preparar a sua experiência",
    initials = "SS",
)

private const val MaxHomeStats = 3
private const val GuestSessionKey = "guest"
private val DefaultStatIcons = listOf(
    Icons.Filled.DirectionsCar,
    Icons.Filled.Star,
    Icons.Filled.EmojiEvents,
)
private val DefaultHomeStats = DefaultBusinessInfo.stats
    .mapIndexedNotNull { index, stat -> stat.toHomeStatOrNull(index) }
    .take(MaxHomeStats)
private val monthNames = listOf(
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
