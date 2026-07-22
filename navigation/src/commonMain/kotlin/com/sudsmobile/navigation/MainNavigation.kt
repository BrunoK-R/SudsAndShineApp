package com.sudsmobile.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.savedstate.read
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.data.auth.AuthRepository
import com.sudsmobile.data.auth.AuthSessionState
import com.sudsmobile.data.booking.MutableBookingChangeNotifier
import com.sudsmobile.data.booking.BookingSelectionPreset
import com.sudsmobile.data.notification.NotificationDeviceRegistrar
import com.sudsmobile.data.notification.NotificationRepository
import com.sudsmobile.feature.blog.BlogScreen
import com.sudsmobile.feature.cart.CartScreen
import com.sudsmobile.feature.cart.RatingScreen
import com.sudsmobile.feature.home.HomeScreen
import com.sudsmobile.feature.payment.PaymentScreen
import com.sudsmobile.feature.profile.AdminAvailabilityScreen
import com.sudsmobile.feature.profile.AdminBookingPolicyScreen
import com.sudsmobile.feature.profile.AdminBusinessInfoScreen
import com.sudsmobile.feature.profile.AdminBookingsScreen
import com.sudsmobile.feature.profile.AdminLoyaltySettingsScreen
import com.sudsmobile.feature.profile.AdminNotificationCampaignDraftsScreen
import com.sudsmobile.feature.profile.AdminNotificationSettingsScreen
import com.sudsmobile.feature.profile.AdminServiceCatalogScreen
import com.sudsmobile.feature.profile.AdminServiceExtrasScreen
import com.sudsmobile.feature.profile.ContactScreen
import com.sudsmobile.feature.profile.HistoryScreen
import com.sudsmobile.feature.profile.NotificationPreferencesScreen
import com.sudsmobile.feature.profile.PersonalDataScreen
import com.sudsmobile.feature.products.ProductsScreen
import com.sudsmobile.feature.products.ServicesScreen
import com.sudsmobile.feature.profile.ProfileScreen
import com.sudsmobile.feature.profile.VehiclesScreen
import org.koin.compose.koinInject

@Composable
fun MainNavigation(
    onRequestSignIn: () -> Unit,
    pendingNotificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val bookingChangeNotifier: MutableBookingChangeNotifier = koinInject()
    val authRepository: AuthRepository = koinInject()
    val notificationDeviceRegistrar: NotificationDeviceRegistrar = koinInject()
    val notificationRepository: NotificationRepository = koinInject()
    val sessionState by authRepository.sessionState.collectAsStateWithLifecycle()
    val latestOnNotificationRouteConsumed by rememberUpdatedState(onNotificationRouteConsumed)
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomBar = mainDestinations.any { it.route == currentRoute }
    var initialBookingServiceId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialBookingSelectionPreset by remember { mutableStateOf<BookingSelectionPreset?>(null) }
    var initialBookingRequestKey by rememberSaveable { mutableStateOf(0L) }

    fun navigateToBooking(serviceId: String? = null) {
        initialBookingServiceId = serviceId
        initialBookingSelectionPreset = null
        initialBookingRequestKey += 1
        navController.navigate(Routes.Products)
    }

    fun navigateToBookingFromLeaf(serviceId: String? = null) {
        initialBookingServiceId = serviceId
        initialBookingSelectionPreset = null
        initialBookingRequestKey += 1
        navController.navigate(Routes.Products) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToBookingPresetFromLeaf(preset: BookingSelectionPreset) {
        initialBookingServiceId = null
        initialBookingSelectionPreset = preset
        initialBookingRequestKey += 1
        navController.navigate(Routes.Products) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(pendingNotificationRoute) {
        val route = pendingNotificationRoute?.trim()?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (route.invalidatesBookingsFromNotification()) {
            bookingChangeNotifier.notifyBookingsChanged()
        }
        navController.navigateToNotificationRoute(route)
        latestOnNotificationRouteConsumed()
    }

    LaunchedEffect((sessionState as? AuthSessionState.Authenticated)?.session?.user?.uid) {
        val uid = (sessionState as? AuthSessionState.Authenticated)?.session?.user?.uid
            ?.takeIf { it.isNotBlank() }
            ?: return@LaunchedEffect
        registerNotificationDeviceIfAllowed(
            userUid = uid,
            notificationDeviceRegistrar = notificationDeviceRegistrar,
            notificationRepository = notificationRepository,
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SudsBottomBar(
                    currentRoute = currentRoute,
                    onDestinationClick = { route ->
                        if (route == Routes.Products) {
                            initialBookingServiceId = null
                            initialBookingSelectionPreset = null
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    contentPadding = paddingValues,
                    onBookService = { navigateToBooking() },
                    onBookSelectedService = { serviceId -> navigateToBooking(serviceId) },
                    onViewServices = { navController.navigate(Routes.Services) },
                    onViewBookings = { navController.navigate(Routes.Cart) },
                    onOpenRewards = { navController.navigate(Routes.Loyalty) },
                    onOpenProfile = { navController.navigate(Routes.Profile) },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.Services) {
                ServicesScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onBookService = { serviceId -> navigateToBooking(serviceId) },
                )
            }
            composable(Routes.Products) {
                ProductsScreen(
                    contentPadding = paddingValues,
                    initialServiceId = initialBookingServiceId,
                    initialSelectionPreset = initialBookingSelectionPreset,
                    initialServiceRequestKey = initialBookingRequestKey,
                    onBack = { navController.popBackStack() },
                    onViewBooking = {
                        navController.navigate(Routes.Cart) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onHome = {
                        navController.navigate(Routes.Home) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenPayment = { reservationId ->
                        navController.navigate(
                            reservationId
                                ?.takeIf { it.isNotBlank() }
                                ?.let(Routes::payment)
                                ?: Routes.Payment,
                        )
                    },
                    onRequestSignIn = onRequestSignIn,
                    onManageVehicles = { navController.navigate(Routes.Vehicles) },
                )
            }
            composable(Routes.Cart) {
                CartScreen(
                    contentPadding = paddingValues,
                    onRateService = { reservationId -> navController.navigate(Routes.rating(reservationId)) },
                    onRequestSignIn = onRequestSignIn,
                    onOpenPayment = { reservationId -> navController.navigate(Routes.payment(reservationId)) },
                )
            }
            composable(Routes.Rating) { backStackEntry ->
                RatingScreen(
                    reservationId = backStackEntry.arguments
                        ?.read { getStringOrNull(Routes.RatingReservationIdArg) }
                        .orEmpty(),
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                    onHome = {
                        navController.navigate(Routes.Home) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.Profile) {
                ProfileScreen(
                    contentPadding = paddingValues,
                    onRequestSignIn = onRequestSignIn,
                    onOpenPersonalData = { navController.navigate(Routes.PersonalData) },
                    onOpenNotificationPreferences = { navController.navigate(Routes.NotificationPreferences) },
                    onManageVehicles = { navController.navigate(Routes.Vehicles) },
                    onOpenHistory = { navController.navigate(Routes.History) },
                    onOpenContact = { navController.navigate(Routes.Contact) },
                    onOpenRewards = { navController.navigate(Routes.Loyalty) },
                    onOpenAdminBookings = { navController.navigate(Routes.AdminBookings) },
                    onOpenAdminAvailability = { navController.navigate(Routes.AdminAvailability) },
                    onOpenAdminBookingPolicy = { navController.navigate(Routes.AdminBookingPolicy) },
                    onOpenAdminBusinessInfo = { navController.navigate(Routes.AdminBusinessInfo) },
                    onOpenAdminLoyaltySettings = { navController.navigate(Routes.AdminLoyaltySettings) },
                    onOpenAdminNotificationSettings = { navController.navigate(Routes.AdminNotificationSettings) },
                    onOpenAdminNotificationCampaignDrafts = {
                        navController.navigate(Routes.AdminNotificationCampaignDrafts)
                    },
                    onOpenAdminServiceCatalog = { navController.navigate(Routes.AdminServiceCatalog) },
                    onOpenAdminServiceExtras = { navController.navigate(Routes.AdminServiceExtras) },
                )
            }
            composable(Routes.AdminBookings) {
                AdminBookingsScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                    onOpenNotificationPreferences = { navController.navigate(Routes.NotificationPreferences) },
                )
            }
            composable(Routes.AdminAvailability) {
                AdminAvailabilityScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.AdminBookingPolicy) {
                AdminBookingPolicyScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.AdminLoyaltySettings) {
                AdminLoyaltySettingsScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.AdminNotificationSettings) {
                AdminNotificationSettingsScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.AdminNotificationCampaignDrafts) {
                AdminNotificationCampaignDraftsScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.AdminBusinessInfo) {
                AdminBusinessInfoScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.AdminServiceCatalog) {
                AdminServiceCatalogScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.AdminServiceExtras) {
                AdminServiceExtrasScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.PersonalData) {
                PersonalDataScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.NotificationPreferences) {
                NotificationPreferencesScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.Vehicles) {
                VehiclesScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.History) {
                HistoryScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                    onRateService = { reservationId -> navController.navigate(Routes.rating(reservationId)) },
                    onBookAgain = { preset -> navigateToBookingPresetFromLeaf(preset) },
                )
            }
            composable(Routes.Contact) {
                ContactScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onBookWash = {
                        navigateToBookingFromLeaf()
                    },
                )
            }
            composable(Routes.Loyalty) {
                BlogScreen(
                    contentPadding = paddingValues,
                    onRequestSignIn = onRequestSignIn,
                    onBookWash = {
                        navigateToBookingFromLeaf()
                    },
                )
            }
            composable(Routes.Payment) {
                PaymentScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                    onBookWash = { navigateToBookingFromLeaf() },
                )
            }
            composable(Routes.PaymentReservation) { backStackEntry ->
                PaymentScreen(
                    targetReservationId = backStackEntry.arguments
                        ?.read { getStringOrNull(Routes.PaymentReservationIdArg) }
                        .orEmpty(),
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = onRequestSignIn,
                    onBookWash = { navigateToBookingFromLeaf() },
                )
            }
        }
    }
}

private fun NavHostController.navigateToNotificationRoute(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = mainDestinations.any { it.route == route }
    }
}

internal fun String.invalidatesBookingsFromNotification(): Boolean {
    val route = trim()
    return route == Routes.Cart ||
        route == Routes.AdminBookings ||
        route == Routes.Loyalty ||
        route == Routes.History ||
        route.startsWith("rating/")
}

@Composable
private fun SudsBottomBar(
    currentRoute: String?,
    onDestinationClick: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                mainDestinations.forEach { destination ->
                    SudsBottomBarItem(
                        destination = destination,
                        selected = currentRoute == destination.route,
                        onClick = { onDestinationClick(destination.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SudsBottomBarItem(
    destination: MainNavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .semantics {
                contentDescription = destination.label
            }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
        )
    }
}
