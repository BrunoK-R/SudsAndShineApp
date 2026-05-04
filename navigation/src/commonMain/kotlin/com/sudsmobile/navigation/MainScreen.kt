package com.sudsmobile.navigation

import androidx.compose.runtime.Composable

@Composable
fun MainScreen(
    onRequestSignIn: () -> Unit,
) {
    MainNavigation(onRequestSignIn = onRequestSignIn)
}
