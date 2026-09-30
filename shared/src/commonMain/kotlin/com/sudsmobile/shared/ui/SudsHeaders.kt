package com.sudsmobile.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsCustomerTheme
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.platformName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudsCompactTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    centered: Boolean = false,
    customTitle: (@Composable () -> Unit)? = null,
) {
    val isIos = platformName() == "iOS"
    val titleContent: @Composable () -> Unit = {
        if (customTitle != null) {
            customTitle()
        } else {
            Text(
                text = title,
                style = if (isIos) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
    }
    val navigationIcon: @Composable () -> Unit = { leadingContent?.invoke() }
    val actions: @Composable RowScope.() -> Unit = { trailingContent?.invoke(this) }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    )
    if (isIos || centered) {
        CenterAlignedTopAppBar(
            title = titleContent,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = WindowInsets(0),
            colors = colors,
        )
    } else {
        TopAppBar(
            title = titleContent,
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = WindowInsets(0),
            colors = colors,
        )
    }
}

@Composable
fun SudsSecondaryTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    SudsCompactTopBar(
        title = title,
        modifier = modifier.statusBarsPadding(),
        leadingContent = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                )
            }
        },
        trailingContent = trailingContent,
    )
}

@Composable
fun SudsAdminHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    SudsCustomerTheme {
        Column(Modifier.fillMaxWidth().background(SudsColors.navy)) {
            SudsSecondaryTopBar(title = title, onBack = onBack)
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(
                    start = SudsSpacing.contentGutter,
                    end = SudsSpacing.contentGutter,
                    bottom = SudsSpacing.md,
                ),
            )
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
