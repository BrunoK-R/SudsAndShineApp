package com.sudsmobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person

val mainTabDestinations = listOf(
    MainNavDestination(Routes.Home, "Início", Icons.Filled.Home),
    MainNavDestination(Routes.Cart, "Marcações", Icons.Filled.History, compactLabel = "Agenda"),
    MainNavDestination(Routes.Loyalty, "Recompensas", Icons.Filled.CardGiftcard, compactLabel = "Prémios"),
    MainNavDestination(Routes.Profile, "Perfil", Icons.Filled.Person),
)

val bookingDestination = MainNavDestination(
    route = Routes.Products,
    label = "Marcar lavagem",
    icon = Icons.Filled.CalendarMonth,
    compactLabel = "Marcar",
)

/** Visual order in the shell, including the raised booking action. */
val mainDestinations = listOf(
    mainTabDestinations[0],
    mainTabDestinations[1],
    bookingDestination,
    mainTabDestinations[2],
    mainTabDestinations[3],
)

enum class MainNavigationSelection {
    Home,
    Bookings,
    Booking,
    Rewards,
    Profile,
}

enum class NavigationTransitionDirection {
    Backward,
    None,
    Forward,
}

fun mainNavigationSelection(route: String?): MainNavigationSelection? = when (route) {
    Routes.Home -> MainNavigationSelection.Home
    Routes.Cart -> MainNavigationSelection.Bookings
    Routes.Products -> MainNavigationSelection.Booking
    Routes.Loyalty -> MainNavigationSelection.Rewards
    Routes.Profile -> MainNavigationSelection.Profile
    else -> null
}

fun isMainDestinationRoute(route: String?): Boolean = mainNavigationSelection(route) != null

fun shouldRestoreMainDestinationState(route: String): Boolean = route != Routes.Home

internal fun navigationIndicatorOffset(
    totalWidth: Float,
    visualSlot: Int,
    indicatorSize: Float,
): Float {
    val slotWidth = totalWidth / mainDestinations.size
    return (slotWidth * visualSlot) + ((slotWidth - indicatorSize) / 2f)
}

internal fun navigationIndicatorVerticalOffset(
    totalHeight: Float,
    topPadding: Float,
    bottomPadding: Float,
    iconSize: Float,
    labelLineHeight: Float,
    indicatorSize: Float,
): Float {
    val availableHeight = totalHeight - topPadding - bottomPadding
    val contentHeight = iconSize + labelLineHeight
    val iconTop = topPadding + ((availableHeight - contentHeight) / 2f)
    val iconCenter = iconTop + (iconSize / 2f)
    return iconCenter - (indicatorSize / 2f)
}

fun navigationTransitionDirection(
    fromRoute: String?,
    toRoute: String?,
): NavigationTransitionDirection {
    val fromIndex = mainDestinations.indexOfFirst { it.route == fromRoute }
    val toIndex = mainDestinations.indexOfFirst { it.route == toRoute }
    if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
        return NavigationTransitionDirection.None
    }
    return if (toIndex > fromIndex) {
        NavigationTransitionDirection.Forward
    } else {
        NavigationTransitionDirection.Backward
    }
}
