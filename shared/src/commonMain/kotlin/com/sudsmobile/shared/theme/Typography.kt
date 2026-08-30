package com.sudsmobile.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
        displayLarge = baseline.displayLarge.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 52.sp,
            lineHeight = 56.sp,
            letterSpacing = (-1.4).sp,
        ),
        displayMedium = baseline.displayMedium.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 42.sp,
            lineHeight = 46.sp,
            letterSpacing = (-1).sp,
        ),
        displaySmall = baseline.displaySmall.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 39.sp,
            letterSpacing = (-0.6).sp,
        ),
        headlineLarge = baseline.headlineLarge.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            lineHeight = 35.sp,
            letterSpacing = (-0.4).sp,
        ),
        headlineMedium = baseline.headlineMedium.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 31.sp,
            letterSpacing = (-0.2).sp,
        ),
        headlineSmall = baseline.headlineSmall.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        titleLarge = baseline.titleLarge.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = baseline.titleMedium.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        titleSmall = baseline.titleSmall.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodyLarge = baseline.bodyLarge.copy(
            fontFamily = manrope,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyMedium = baseline.bodyMedium.copy(
            fontFamily = manrope,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        ),
        bodySmall = baseline.bodySmall.copy(
            fontFamily = manrope,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        ),
        labelLarge = baseline.labelLarge.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelMedium = baseline.labelMedium.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        ),
        labelSmall = baseline.labelSmall.copy(
            fontFamily = manrope,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        ),
    )
}
