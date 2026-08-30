package com.sudsmobile.feature.products

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.sudsmobile.shared.theme.SudsColors

private val BookingColorScheme = darkColorScheme(
    primary = SudsColors.cyan,
    onPrimary = SudsColors.onAction,
    primaryContainer = SudsColors.glassStrong,
    onPrimaryContainer = SudsColors.onBrand,
    secondary = SudsColors.cyanMuted,
    onSecondary = SudsColors.onAction,
    secondaryContainer = SudsColors.glassStrong,
    onSecondaryContainer = SudsColors.onBrand,
    tertiary = SudsColors.champagne,
    onTertiary = SudsColors.onAction,
    tertiaryContainer = SudsColors.champagne,
    onTertiaryContainer = SudsColors.onAction,
    error = SudsColors.error,
    onError = SudsColors.ink,
    errorContainer = SudsColors.error.copy(alpha = 0.16f),
    onErrorContainer = SudsColors.error,
    background = SudsColors.ink,
    onBackground = SudsColors.onBrand,
    surface = SudsColors.navy,
    onSurface = SudsColors.onBrand,
    surfaceVariant = SudsColors.navyElevated,
    onSurfaceVariant = SudsColors.onBrandMuted,
    outline = SudsColors.onBrandMuted,
    outlineVariant = SudsColors.glassBorder,
    scrim = SudsColors.scrim,
    inverseSurface = SudsColors.cyan,
    inverseOnSurface = SudsColors.onAction,
    inversePrimary = SudsColors.navy,
    surfaceDim = SudsColors.ink,
    surfaceBright = SudsColors.navyElevated,
    surfaceContainerLowest = SudsColors.navyElevated,
    surfaceContainerLow = SudsColors.navy,
    surfaceContainer = SudsColors.navyElevated,
    surfaceContainerHigh = SudsColors.glassStrong,
    surfaceContainerHighest = SudsColors.glassStrong,
)

@Composable
internal fun BookingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BookingColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}
