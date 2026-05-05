package com.sudsmobile.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sudsmobile.feature.blog.BlogScreen
import com.sudsmobile.feature.cart.CartScreen
import com.sudsmobile.feature.cart.RatingScreen
import com.sudsmobile.feature.home.HomeScreen
import com.sudsmobile.feature.payment.PaymentScreen
import com.sudsmobile.feature.profile.ContactScreen
import com.sudsmobile.feature.profile.HistoryScreen
import com.sudsmobile.feature.products.ProductsScreen
import com.sudsmobile.feature.products.ServicesScreen
import com.sudsmobile.feature.profile.ProfileScreen
import com.sudsmobile.feature.profile.VehiclesScreen

@Composable
fun MainNavigation(
    onRequestSignIn: () -> Unit,
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomBar = currentRoute == Routes.Services || mainDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SudsBottomBar(
                    currentRoute = currentRoute,
                    onDestinationClick = { route ->
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
                    onBookService = { navController.navigate(Routes.Products) },
                    onViewServices = { navController.navigate(Routes.Services) },
                    onViewBookings = { navController.navigate(Routes.Cart) },
                    onOpenRewards = { navController.navigate(Routes.Loyalty) },
                    onOpenProfile = { navController.navigate(Routes.Profile) },
                )
            }
            composable(Routes.Services) {
                ServicesScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onBookService = { navController.navigate(Routes.Products) },
                )
            }
            composable(Routes.Products) {
                ProductsScreen(
                    contentPadding = paddingValues,
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
            composable(Routes.Cart) {
                CartScreen(
                    contentPadding = paddingValues,
                    onRateService = { navController.navigate(Routes.Rating) },
                )
            }
            composable(Routes.Rating) {
                RatingScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
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
                    onManageVehicles = { navController.navigate(Routes.Vehicles) },
                    onOpenHistory = { navController.navigate(Routes.History) },
                    onOpenContact = { navController.navigate(Routes.Contact) },
                    onOpenRewards = { navController.navigate(Routes.Loyalty) },
                )
            }
            composable(Routes.Vehicles) {
                VehiclesScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.History) {
                HistoryScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Contact) {
                ContactScreen(
                    contentPadding = paddingValues,
                    onBack = { navController.popBackStack() },
                    onBookWash = {
                        navController.navigate(Routes.Products) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.Loyalty) {
                BlogScreen(
                    contentPadding = paddingValues,
                    onBookWash = {
                        navController.navigate(Routes.Products) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.Payment) {
                PaymentScreen(contentPadding = paddingValues)
            }
        }
    }
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
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
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
