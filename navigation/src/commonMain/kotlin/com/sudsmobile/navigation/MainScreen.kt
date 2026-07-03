package com.sudsmobile.navigation

import androidx.compose.runtime.Composable

@Composable
fun MainScreen(
    onRequestSignIn: () -> Unit,
    pendingNotificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {},
) {
    MainNavigation(
        onRequestSignIn = onRequestSignIn,
        pendingNotificationRoute = pendingNotificationRoute,
        onNotificationRouteConsumed = onNotificationRouteConsumed,
    )
}
