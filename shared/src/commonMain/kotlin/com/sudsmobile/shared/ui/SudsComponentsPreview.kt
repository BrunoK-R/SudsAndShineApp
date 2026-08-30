package com.sudsmobile.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.sudsmobile.shared.theme.SudsAndShineTheme
import com.sudsmobile.shared.theme.SudsMotionPreferences
import com.sudsmobile.shared.theme.SudsSpacing
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun SudsComponentsNarrowPreview() {
    SudsComponentsPreviewContent()
}

@Preview
@Composable
private fun SudsComponentsTabletPreview() {
    SudsComponentsPreviewContent()
}

@Preview
@Composable
private fun SudsComponentsDarkHostPreview() {
    SudsComponentsPreviewContent(darkTheme = true)
}

@Preview
@Composable
private fun SudsComponentsLargeTextPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 1.3f),
    ) {
        SudsComponentsPreviewContent()
    }
}

@Composable
private fun SudsComponentsPreviewContent(darkTheme: Boolean = false) {
    SudsAndShineTheme(
        darkTheme = darkTheme,
        motionPreferences = SudsMotionPreferences(reduceMotion = true),
    ) {
        SudsBrandBackground(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(SudsSpacing.contentGutter),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.md),
            ) {
                SudsCompactTopBar(
                    title = "O teu carro, impecável",
                    eyebrow = "Suds & Shine",
                )
                SudsProgressSegments(currentStepIndex = 1, totalSteps = 4)
                SudsSectionHeader(
                    title = "Escolhe o serviço",
                    supportingText = "Cuidado profissional, sem complicações.",
                )
                SudsGlassCard {
                    SudsServiceArtwork(
                        modifier = Modifier.fillMaxWidth(),
                        style = SudsServiceArtworkStyle.Premium,
                    )
                }
                SudsStatusCard(
                    title = "Próxima marcação confirmada",
                    message = "Amanhã, 10:30 · Leiria",
                    status = SudsStatus.Success,
                )
                SudsStatusCard(
                    title = "Não foi possível atualizar",
                    message = "Tenta novamente dentro de alguns instantes.",
                    status = SudsStatus.Error,
                )
                SudsPrimaryButton(
                    label = "Marcar lavagem",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                SudsPrimaryButton(
                    label = "A carregar",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    loading = true,
                )
                SudsPrimaryButton(
                    label = "Indisponível",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )
            }
        }
    }
}
