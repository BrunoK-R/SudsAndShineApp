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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sudsmobile.feature.blog.BlogScreen
import com.sudsmobile.feature.cart.CartScreen
import com.sudsmobile.feature.home.HomeScreen
import com.sudsmobile.feature.payment.PaymentScreen
import com.sudsmobile.feature.products.ProductsScreen
import com.sudsmobile.feature.profile.ProfileScreen

@Composable
fun MainNavigation(
    onRequestSignIn: () -> Unit,
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        bottomBar = {
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
                        label = { Text(destination.label) },
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
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
        ) {
            composable(Routes.Home) {
                HomeScreen(contentPadding = paddingValues)
            }
            composable(Routes.Products) {
                ProductsScreen(contentPadding = paddingValues)
            }
            composable(Routes.Cart) {
                CartScreen(contentPadding = paddingValues)
            }
            composable(Routes.Profile) {
                ProfileScreen(
                    contentPadding = paddingValues,
                    onRequestSignIn = onRequestSignIn,
                )
            }
            composable(Routes.Blog) {
                BlogScreen(contentPadding = paddingValues)
            }
            composable(Routes.Payment) {
                PaymentScreen(contentPadding = paddingValues)
            }
        }
    }
}
