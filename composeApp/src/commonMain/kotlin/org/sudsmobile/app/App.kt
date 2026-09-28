package org.sudsmobile.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sudsmobile.feature.onboarding.OnboardingGateUiState
import com.sudsmobile.feature.onboarding.OnboardingGateViewModel
import com.sudsmobile.feature.onboarding.SplashScreen
import com.sudsmobile.navigation.DefaultOnboardingScreen
import com.sudsmobile.navigation.SetupNavGraph
import com.sudsmobile.shared.theme.SudsAndShineTheme
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(
    pendingNotificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {},
) {
    val onboardingGateViewModel: OnboardingGateViewModel = koinViewModel()
    val onboardingGateState by onboardingGateViewModel.uiState.collectAsStateWithLifecycle()

    SudsAndShineTheme {
        AppContent(
            onboardingGateState = onboardingGateState,
            onCompleteOnboarding = onboardingGateViewModel::completeOnboarding,
            onResetOnboardingPreference = onboardingGateViewModel::resetOnboardingPreference,
            pendingNotificationRoute = pendingNotificationRoute,
            onNotificationRouteConsumed = onNotificationRouteConsumed,
        )
    }
}

@Composable
private fun AppContent(
    onboardingGateState: OnboardingGateUiState,
    onCompleteOnboarding: suspend () -> Unit,
    onResetOnboardingPreference: suspend () -> Unit,
    pendingNotificationRoute: String?,
    onNotificationRouteConsumed: () -> Unit,
) {
    var splashComplete by rememberSaveable { mutableStateOf(false) }

    if (!splashComplete || onboardingGateState == OnboardingGateUiState.Loading) {
        SplashScreen(onFinished = { splashComplete = true })
    } else {
        SetupNavGraph(
            showOnboarding = onboardingGateState == OnboardingGateUiState.ShowOnboarding,
            onCompleteOnboarding = onCompleteOnboarding,
            onResetOnboardingPreference = onResetOnboardingPreference,
            pendingNotificationRoute = pendingNotificationRoute,
            onNotificationRouteConsumed = onNotificationRouteConsumed,
            renderOnboarding = { actionsEnabled, onSkip, onComplete ->
                DefaultOnboardingScreen(
                    actionsEnabled = actionsEnabled,
                    onSkip = onSkip,
                    onComplete = onComplete,
                )
            },
        )
    }
}

@Composable
@Preview
private fun AppPreview() {
    SudsAndShineTheme {
        AppContent(
            onboardingGateState = OnboardingGateUiState.ShowOnboarding,
            onCompleteOnboarding = {},
            onResetOnboardingPreference = {},
            pendingNotificationRoute = null,
            onNotificationRouteConsumed = {},
        )
    }
}
