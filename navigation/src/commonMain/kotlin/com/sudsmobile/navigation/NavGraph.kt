package com.sudsmobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sudsmobile.feature.auth.AuthScreen
import com.sudsmobile.feature.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

@Composable
fun SetupNavGraph(
    showOnboarding: Boolean = false,
    visualFixtureEnabled: Boolean = false,
    onCompleteOnboarding: suspend () -> Unit = {},
    onResetOnboardingPreference: suspend () -> Unit = {},
    pendingNotificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {},
    renderOnboarding: @Composable (
        actionsEnabled: Boolean,
        onSkip: () -> Unit,
        onComplete: () -> Unit,
    ) -> Unit = { _, _, _ -> },
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    fun completeOnboarding() {
        coroutineScope.launch {
            onCompleteOnboarding()
            navController.navigate(Routes.Main) {
                popUpTo(Routes.Onboarding) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) Routes.Onboarding else Routes.Main,
    ) {
        composable(Routes.Onboarding) {
            renderOnboarding(
                true,
                {
                    completeOnboarding()
                },
                { completeOnboarding() },
            )
        }

        composable(Routes.Auth) {
            AuthScreen(
                onLoginSuccess = { navController.popBackStack() },
                onLoginCancelled = { navController.popBackStack() },
            )
        }

        composable(Routes.Main) {
            MainScreen(
                onRequestSignIn = { navController.navigate(Routes.Auth) },
                visualFixtureEnabled = visualFixtureEnabled,
                pendingNotificationRoute = pendingNotificationRoute,
                onNotificationRouteConsumed = onNotificationRouteConsumed,
            )
        }
    }
}

@Composable
fun DefaultOnboardingScreen(
    actionsEnabled: Boolean,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
) {
    OnboardingScreen(
        actionsEnabled = actionsEnabled,
        onSkip = onSkip,
        onComplete = onComplete,
    )
}
