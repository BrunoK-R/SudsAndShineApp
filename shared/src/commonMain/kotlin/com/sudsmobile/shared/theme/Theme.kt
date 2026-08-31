package com.sudsmobile.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

private val CustomerColorScheme = darkColorScheme(
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
    inverseSurface = SudsColors.navyElevated,
    inverseOnSurface = SudsColors.onBrand,
    inversePrimary = SudsColors.cyan,
    surfaceDim = SudsColors.ink,
    surfaceBright = SudsColors.navyElevated,
    surfaceContainerLowest = SudsColors.navyElevated,
    surfaceContainerLow = SudsColors.navy,
    surfaceContainer = SudsColors.navyElevated,
    surfaceContainerHigh = SudsColors.glassStrong,
    surfaceContainerHighest = SudsColors.glassStrong,
)

/** Brand theme for customer destinations while legacy and admin screens migrate independently. */
@Composable
fun SudsCustomerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CustomerColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

@Composable
fun SudsAndShineTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    motionPreferences: SudsMotionPreferences? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> if (darkTheme) DarkColorScheme else LightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val resolvedMotionPreferences = motionPreferences ?: rememberSudsMotionPreferences()

    CompositionLocalProvider(LocalSudsMotionPreferences provides resolvedMotionPreferences) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = sudsTypography(),
            shapes = SudsShapes.material,
            content = content,
        )
    }
}
