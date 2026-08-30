package com.sudsmobile.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import com.sudsmobile.shared.theme.SudsMotion

internal fun mainEnterTransition(
    fromRoute: String?,
    toRoute: String?,
    reduceMotion: Boolean,
    distancePx: Int,
): EnterTransition {
    val direction = navigationTransitionDirection(fromRoute, toRoute)
    if (direction == NavigationTransitionDirection.None) return EnterTransition.None
    val duration = if (reduceMotion) SudsMotion.quick else SudsMotion.emphasized
    val fade = fadeIn(
        animationSpec = tween(durationMillis = duration, easing = SudsMotion.standardEasing),
    )
    if (reduceMotion) return fade
    val signedDistance = when (direction) {
        NavigationTransitionDirection.Forward -> distancePx
        NavigationTransitionDirection.Backward -> -distancePx
        NavigationTransitionDirection.None -> 0
    }
    return fade + slideInHorizontally(
        animationSpec = tween(durationMillis = duration, easing = SudsMotion.standardEasing),
        initialOffsetX = { signedDistance },
    )
}

internal fun mainExitTransition(
    fromRoute: String?,
    toRoute: String?,
    reduceMotion: Boolean,
    distancePx: Int,
): ExitTransition {
    val direction = navigationTransitionDirection(fromRoute, toRoute)
    if (direction == NavigationTransitionDirection.None) return ExitTransition.None
    val duration = if (reduceMotion) SudsMotion.quick else SudsMotion.emphasized
    val fade = fadeOut(
        animationSpec = tween(durationMillis = duration, easing = SudsMotion.exitEasing),
    )
    if (reduceMotion) return fade
    val signedDistance = when (direction) {
        NavigationTransitionDirection.Forward -> -distancePx
        NavigationTransitionDirection.Backward -> distancePx
        NavigationTransitionDirection.None -> 0
    }
    return fade + slideOutHorizontally(
        animationSpec = tween(durationMillis = duration, easing = SudsMotion.exitEasing),
        targetOffsetX = { signedDistance },
    )
}
