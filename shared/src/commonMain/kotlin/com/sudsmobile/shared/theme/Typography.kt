package com.sudsmobile.shared.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

private val baseline = Typography()

internal val SudsTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = FontFamily.SansSerif),
    displayMedium = baseline.displayMedium.copy(fontFamily = FontFamily.SansSerif),
    displaySmall = baseline.displaySmall.copy(fontFamily = FontFamily.SansSerif),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = FontFamily.SansSerif),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = FontFamily.SansSerif),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = FontFamily.SansSerif),
    titleLarge = baseline.titleLarge.copy(fontFamily = FontFamily.SansSerif),
    titleMedium = baseline.titleMedium.copy(fontFamily = FontFamily.SansSerif),
    titleSmall = baseline.titleSmall.copy(fontFamily = FontFamily.SansSerif),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = baseline.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = baseline.labelLarge.copy(fontFamily = FontFamily.SansSerif),
    labelMedium = baseline.labelMedium.copy(fontFamily = FontFamily.SansSerif),
    labelSmall = baseline.labelSmall.copy(fontFamily = FontFamily.SansSerif),
)
