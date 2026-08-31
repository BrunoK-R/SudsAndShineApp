package com.sudsmobile.shared.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class AccessibilityTokensTest {

    @Test
    fun bodyTextMeetsWcagAaAcrossCustomerSurfaces() {
        assertContrast(SudsColors.onBrand, SudsColors.ink, minimum = 4.5)
        assertContrast(SudsColors.onBrand, SudsColors.navyElevated, minimum = 4.5)
        assertContrast(SudsColors.onBrandMuted, SudsColors.navyElevated, minimum = 4.5)

        val strongestGlassSurface = SudsColors.glassStrong.opaqueOver(SudsColors.navyElevated)
        assertContrast(SudsColors.onBrandMuted, strongestGlassSurface, minimum = 4.5)
    }

    @Test
    fun actionsStatusesAndEssentialIconsMeetContrastGate() {
        assertContrast(SudsColors.onAction, SudsColors.cyan, minimum = 4.5)
        assertContrast(SudsColors.onAction, SudsColors.champagne, minimum = 4.5)
        assertContrast(SudsColors.cyan, SudsColors.navy, minimum = 3.0)
        assertContrast(SudsColors.champagne, SudsColors.navy, minimum = 3.0)
        assertContrast(SudsColors.success, SudsColors.ink, minimum = 3.0)
        assertContrast(SudsColors.warning, SudsColors.ink, minimum = 3.0)
        assertContrast(SudsColors.error, SudsColors.ink, minimum = 3.0)
    }

    @Test
    fun sharedTouchTargetTokenIsAtLeastFortyEightDp() {
        assertTrue(SudsSpacing.minimumTouchTarget.value >= 48f)
    }
}

private fun assertContrast(
    foreground: Color,
    background: Color,
    minimum: Double,
) {
    val opaqueBackground = background.opaqueOver(SudsColors.ink)
    val opaqueForeground = foreground.opaqueOver(opaqueBackground)
    val ratio = contrastRatio(opaqueForeground, opaqueBackground)
    assertTrue(
        ratio >= minimum,
        "Expected contrast >= $minimum, but was $ratio for $foreground on $background",
    )
}

private fun Color.opaqueOver(background: Color): Color {
    if (alpha >= 1f) return copy(alpha = 1f)
    val inverseAlpha = 1f - alpha
    val outputAlpha = alpha + (background.alpha * inverseAlpha)
    if (outputAlpha <= 0f) return Color.Transparent
    return Color(
        red = ((red * alpha) + (background.red * background.alpha * inverseAlpha)) / outputAlpha,
        green = ((green * alpha) + (background.green * background.alpha * inverseAlpha)) / outputAlpha,
        blue = ((blue * alpha) + (background.blue * background.alpha * inverseAlpha)) / outputAlpha,
        alpha = outputAlpha,
    )
}

private fun contrastRatio(first: Color, second: Color): Double {
    val firstLuminance = first.relativeLuminance()
    val secondLuminance = second.relativeLuminance()
    val lighter = maxOf(firstLuminance, secondLuminance)
    val darker = minOf(firstLuminance, secondLuminance)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun Color.relativeLuminance(): Double =
    (0.2126 * red.linearized()) +
        (0.7152 * green.linearized()) +
        (0.0722 * blue.linearized())

private fun Float.linearized(): Double {
    val channel = toDouble()
    return if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
