package org.sudsmobile.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sudsmobile.feature.onboarding.SplashScreen
import com.sudsmobile.navigation.DefaultOnboardingScreen
import com.sudsmobile.navigation.SetupNavGraph
import com.sudsmobile.shared.theme.SudsAndShineTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    SudsAndShineTheme {
        var splashComplete by rememberSaveable { mutableStateOf(false) }

        if (!splashComplete) {
            SplashScreen(onFinished = { splashComplete = true })
        } else {
            SetupNavGraph(
                showOnboarding = true,
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
}
