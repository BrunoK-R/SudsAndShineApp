package org.sudsmobile.app

import androidx.compose.runtime.Composable
import com.sudsmobile.navigation.SetupNavGraph
import com.sudsmobile.shared.theme.SudsAndShineTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    SudsAndShineTheme {
        SetupNavGraph(
            showOnboarding = false,
            renderOnboarding = { _, onSkip, _ ->
                onSkip()
            },
        )
    }
}
