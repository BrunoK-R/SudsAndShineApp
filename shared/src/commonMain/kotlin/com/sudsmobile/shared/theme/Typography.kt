package com.sudsmobile.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import sudsandshine.shared.generated.resources.Res
import sudsandshine.shared.generated.resources.manrope_bold
import sudsandshine.shared.generated.resources.manrope_extrabold
import sudsandshine.shared.generated.resources.manrope_medium
import sudsandshine.shared.generated.resources.manrope_regular
import sudsandshine.shared.generated.resources.manrope_semibold

private val baseline = Typography()

@Composable
private fun manropeFontFamily(): FontFamily = FontFamily(
    Font(Res.font.manrope_regular, weight = FontWeight.Normal),
    Font(Res.font.manrope_medium, weight = FontWeight.Medium),
    Font(Res.font.manrope_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.manrope_bold, weight = FontWeight.Bold),
    Font(Res.font.manrope_extrabold, weight = FontWeight.ExtraBold),
)

@Composable
internal fun sudsTypography(): Typography {
    val manrope = manropeFontFamily()

    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = manrope),
        displayMedium = baseline.displayMedium.copy(fontFamily = manrope),
        displaySmall = baseline.displaySmall.copy(fontFamily = manrope),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = manrope),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = manrope),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = manrope),
        titleLarge = baseline.titleLarge.copy(fontFamily = manrope),
        titleMedium = baseline.titleMedium.copy(fontFamily = manrope),
        titleSmall = baseline.titleSmall.copy(fontFamily = manrope),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = manrope),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = manrope),
        bodySmall = baseline.bodySmall.copy(fontFamily = manrope),
        labelLarge = baseline.labelLarge.copy(fontFamily = manrope),
        labelMedium = baseline.labelMedium.copy(fontFamily = manrope),
        labelSmall = baseline.labelSmall.copy(fontFamily = manrope),
    )
}
