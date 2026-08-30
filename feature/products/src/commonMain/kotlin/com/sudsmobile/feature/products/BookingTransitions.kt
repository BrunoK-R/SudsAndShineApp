package com.sudsmobile.feature.products

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.sudsmobile.shared.theme.SudsMotion

internal fun bookingStepTransition(
    from: BookingStep,
    to: BookingStep,
    reduceMotion: Boolean,
    distancePx: Int,
): ContentTransform {
    val direction = bookingStepDirection(from, to)
    val duration = if (reduceMotion) SudsMotion.quick else SudsMotion.emphasized
    val enterFade = fadeIn(
        animationSpec = tween(durationMillis = duration, easing = SudsMotion.standardEasing),
    )
    val exitFade = fadeOut(
        animationSpec = tween(durationMillis = duration, easing = SudsMotion.exitEasing),
    )
    if (reduceMotion || direction == BookingStepDirection.None) {
        return enterFade togetherWith exitFade
    }

    val enterDistance = when (direction) {
        BookingStepDirection.Forward -> distancePx
        BookingStepDirection.Backward -> -distancePx
        BookingStepDirection.None -> 0
    }
    val exitDistance = -enterDistance
    return (enterFade + slideInHorizontally(
        animationSpec = tween(durationMillis = duration, easing = SudsMotion.standardEasing),
        initialOffsetX = { enterDistance },
    )) togetherWith (exitFade + slideOutHorizontally(
        animationSpec = tween(durationMillis = duration, easing = SudsMotion.exitEasing),
        targetOffsetX = { exitDistance },
    ))
}
