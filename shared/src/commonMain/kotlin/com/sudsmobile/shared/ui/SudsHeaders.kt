package com.sudsmobile.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsSpacing

@Composable
fun SudsCompactTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = SudsSpacing.contentGutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(Modifier.width(SudsSpacing.sm))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    color = SudsColors.cyanMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                color = SudsColors.onBrand,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(SudsSpacing.sm))
            trailingContent()
        }
    }
}

@Composable
fun SudsCollapsingHeader(
    title: String,
    collapseProgress: Float,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    compactTrailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    val progress = collapseProgress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = SudsSpacing.contentGutter, vertical = SudsSpacing.xl)
                .graphicsLayer {
                    translationY = -24.dp.toPx() * progress
                }
                .alpha(1f - progress)
                .then(
                    if (progress >= 0.5f) Modifier.clearAndSetSemantics { }
                    else Modifier,
                ),
            verticalArrangement = Arrangement.spacedBy(SudsSpacing.xs),
        ) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    color = SudsColors.cyanMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = title,
                color = SudsColors.onBrand,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = SudsColors.onBrandMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        SudsCompactTopBar(
            title = title,
            eyebrow = eyebrow,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .alpha(progress)
                .then(
                    if (progress < 0.5f) Modifier.clearAndSetSemantics { }
                    else Modifier,
                ),
            trailingContent = if (progress >= 0.5f) compactTrailingContent else null,
        )
    }
}

@Composable
fun SudsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
        ) {
            Text(
                text = title,
                color = SudsColors.onBrand,
                style = MaterialTheme.typography.titleLarge,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    color = SudsColors.onBrandMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.width(SudsSpacing.md))
            action()
        }
    }
}
