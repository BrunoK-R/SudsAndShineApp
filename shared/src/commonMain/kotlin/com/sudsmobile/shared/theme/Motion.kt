package com.sudsmobile.shared.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.roundToInt

object SudsMotion {
    const val quick = 120
    const val standard = 200
    const val emphasized = 280
    const val expressive = 420

    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val exitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    fun <T> selectionSpring(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

@Immutable
data class SudsMotionPreferences(
    val reduceMotion: Boolean = false,
)

val LocalSudsMotionPreferences = staticCompositionLocalOf { SudsMotionPreferences() }

@Composable
fun rememberSudsMotionPreferences(): SudsMotionPreferences = SudsMotionPreferences(
    reduceMotion = rememberPlatformReduceMotionEnabled(),
)

@Composable
internal expect fun rememberPlatformReduceMotionEnabled(): Boolean

fun motionDurationMillis(
    durationMillis: Int,
    preferences: SudsMotionPreferences,
): Int = if (preferences.reduceMotion) 0 else durationMillis.coerceAtLeast(0)

fun calculateCollapseProgress(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    collapseRangePx: Int,
): Float {
    if (firstVisibleItemIndex > 0) return 1f
    if (collapseRangePx <= 0) return if (firstVisibleItemScrollOffset > 0) 1f else 0f
    return (firstVisibleItemScrollOffset.toFloat() / collapseRangePx).coerceIn(0f, 1f)
}

fun interpolateInt(
    start: Int,
    end: Int,
    progress: Float,
): Int = (start + ((end - start) * progress.coerceIn(0f, 1f))).roundToInt()
