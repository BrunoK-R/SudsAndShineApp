package com.sudsmobile.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 0.dp,
                ) {
                    mainDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.tertiaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.outline,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            label = {
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    softWrap = false,
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                        )
                    }
                }
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
            composable(Routes.Blog) {
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
