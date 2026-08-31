package com.sudsmobile.navigation

import androidx.compose.runtime.Composable

@Composable
fun MainScreen(
    onRequestSignIn: () -> Unit,
    visualFixtureEnabled: Boolean = false,
    pendingNotificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {},
) {
    MainNavigation(
        onRequestSignIn = onRequestSignIn,
        visualFixtureEnabled = visualFixtureEnabled,
        pendingNotificationRoute = pendingNotificationRoute,
        onNotificationRouteConsumed = onNotificationRouteConsumed,
    )
}
