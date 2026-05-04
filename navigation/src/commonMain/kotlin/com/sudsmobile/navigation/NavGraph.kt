package com.sudsmobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sudsmobile.feature.auth.AuthScreen

@Composable
fun SetupNavGraph(
    showOnboarding: Boolean = false,
    onCompleteOnboarding: suspend () -> Unit = {},
    onResetOnboardingPreference: suspend () -> Unit = {},
    renderOnboarding: @Composable (
        actionsEnabled: Boolean,
        onSkip: () -> Unit,
        onComplete: () -> Unit,
    ) -> Unit = { _, _, _ -> },
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (showOnboarding) Routes.Onboarding else Routes.Main,
    ) {
        composable(Routes.Onboarding) {
            renderOnboarding(
                true,
                {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                },
                {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                },
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
            )
        }
    }
}
