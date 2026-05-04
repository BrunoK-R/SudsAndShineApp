package org.sudsmobile.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.sudsmobile.navigation.SetupNavGraph
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        SetupNavGraph(
            showOnboarding = false,
            renderOnboarding = { _, onSkip, _ ->
                onSkip()
            },
        )
    }
}
