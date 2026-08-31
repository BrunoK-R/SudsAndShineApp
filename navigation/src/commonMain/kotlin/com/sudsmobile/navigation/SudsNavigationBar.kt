package com.sudsmobile.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudsmobile.shared.theme.LocalSudsMotionPreferences
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsMotion
import com.sudsmobile.shared.theme.SudsShapes
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsBookingNavigationMark

object SudsNavigationBarDefaults {
    val shellHeight = 96.dp
    val contentClearance = SudsSpacing.navigationClearance
}

@Composable
fun SudsNavigationBar(
    currentRoute: String?,
    onDestinationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalSudsMotionPreferences.current.reduceMotion
    val hapticFeedback = LocalHapticFeedback.current
    val selectedTabIndex = mainTabDestinations.indexOfFirst { it.route == currentRoute }
    val selectedVisualSlot = when (selectedTabIndex) {
        0, 1 -> selectedTabIndex
        2, 3 -> selectedTabIndex + 1
        else -> -1
    }
    val selectedIsBooking = currentRoute == bookingDestination.route

    fun selectRoute(route: String) {
        if (route == currentRoute) return
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onDestinationClick(route)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(SudsNavigationBarDefaults.shellHeight)
            .padding(
                start = SudsSpacing.sm,
                end = SudsSpacing.sm,
                bottom = SudsSpacing.xs,
            ),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp),
            shape = SudsShapes.capsule,
            color = SudsColors.ink.copy(alpha = 0.96f),
            border = BorderStroke(SudsSpacing.hairline, SudsColors.glassBorder),
            shadowElevation = 14.dp,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val indicatorSize = 38.dp
                val tabTopPadding = SudsSpacing.xs
                val tabBottomPadding = SudsSpacing.xxs
                val tabIconSize = 22.dp
                val tabLabelLineHeight = 12.dp
                val compact = maxWidth < 330.dp
                val indicatorSlot = selectedVisualSlot.takeIf { it >= 0 } ?: 2
                val targetOffset = navigationIndicatorOffset(
                    totalWidth = maxWidth.value,
                    visualSlot = indicatorSlot,
                    indicatorSize = indicatorSize.value,
                ).dp
                val indicatorVerticalOffset = navigationIndicatorVerticalOffset(
                    totalHeight = maxHeight.value,
                    topPadding = tabTopPadding.value,
                    bottomPadding = tabBottomPadding.value,
                    iconSize = tabIconSize.value,
                    labelLineHeight = tabLabelLineHeight.value,
                    indicatorSize = indicatorSize.value,
                ).dp
                val indicatorOffset = animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = if (reduceMotion) {
                        snap()
                    } else {
                        tween(
                            durationMillis = SudsMotion.emphasized,
                            easing = SudsMotion.standardEasing,
                        )
                    },
                    label = "navigation indicator offset",
                )
                val indicatorAlpha = animateFloatAsState(
                    targetValue = if (selectedVisualSlot >= 0) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = if (reduceMotion) 0 else SudsMotion.quick,
                    ),
                    label = "navigation indicator alpha",
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset.value, y = indicatorVerticalOffset)
                        .size(indicatorSize)
                        .alpha(indicatorAlpha.value)
                        .background(
                            SudsColors.cyan.copy(alpha = 0.08f),
                            SudsShapes.capsule,
                        ),
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    mainTabDestinations.take(2).forEach { destination ->
                        SudsNavigationTab(
                            destination = destination,
                            selected = destination.route == currentRoute,
                            compact = compact,
                            reduceMotion = reduceMotion,
                            onClick = { selectRoute(destination.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    mainTabDestinations.drop(2).forEach { destination ->
                        SudsNavigationTab(
                            destination = destination,
                            selected = destination.route == currentRoute,
                            compact = compact,
                            reduceMotion = reduceMotion,
                            onClick = { selectRoute(destination.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        SudsBookingNavigationAction(
            selected = selectedIsBooking,
            reduceMotion = reduceMotion,
            onClick = { selectRoute(bookingDestination.route) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun SudsNavigationTab(
    destination: MainNavDestination,
    selected: Boolean,
    compact: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconScale = animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = if (reduceMotion) snap() else SudsMotion.selectionSpring(),
        label = "${destination.route} icon scale",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = SudsSpacing.minimumTouchTarget)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
            )
            .semantics { contentDescription = destination.label }
            .padding(top = SudsSpacing.xs, bottom = SudsSpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = if (selected) SudsColors.cyan else SudsColors.onBrandMuted,
            modifier = Modifier
                .size(22.dp)
                .scale(iconScale.value),
        )
        Text(
            text = if (compact) destination.compactLabel else destination.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 12.sp,
                letterSpacing = (-0.1).sp,
            ),
            color = if (selected) SudsColors.onBrand else SudsColors.onBrandMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
        )
    }
}

@Composable
private fun SudsBookingNavigationAction(
    selected: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = if (reduceMotion) snap() else SudsMotion.selectionSpring(),
        label = "booking action scale",
    )

    Column(
        modifier = modifier
            .widthIn(min = 72.dp)
            .heightIn(min = SudsSpacing.minimumTouchTarget)
            .clickable(
                onClick = onClick,
                role = Role.Button,
            )
            .semantics {
                contentDescription = bookingDestination.label
                this.selected = selected
                stateDescription = if (selected) "Selecionado" else "Não selecionado"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
    ) {
        Surface(
            modifier = Modifier
                .size(58.dp)
                .scale(scale.value)
                .graphicsLayer {
                    shadowElevation = 10.dp.toPx()
                    shape = CircleShape
                    clip = false
                },
            shape = CircleShape,
            color = SudsColors.cyan,
            contentColor = SudsColors.onAction,
            border = BorderStroke(
                width = 2.dp,
                color = if (selected) SudsColors.cyanMuted else SudsColors.onBrand,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                SudsBookingNavigationMark(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                )
            }
        }
    }
}
