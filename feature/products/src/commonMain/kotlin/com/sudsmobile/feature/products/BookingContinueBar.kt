package com.sudsmobile.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsShapes
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsAutomotivePhoto
import com.sudsmobile.shared.ui.automotivePhotoKindForKey

@Composable
internal fun ContinueBar(
    enabled: Boolean,
    onClick: () -> Unit,
    label: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    summaryTitle: String? = null,
    summaryDetail: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = SudsSpacing.sm)
            .padding(bottom = contentPadding.calculateBottomPadding() + SudsSpacing.xs),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (summaryTitle == null) 76.dp else 90.dp),
            shape = SudsShapes.card,
            color = SudsColors.ink.copy(alpha = 0.98f),
            border = BorderStroke(SudsSpacing.hairline, SudsColors.glassBorder),
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
        ) {
            Row(
                modifier = Modifier.padding(SudsSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (summaryTitle != null) {
                    SudsAutomotivePhoto(
                        kind = automotivePhotoKindForKey(summaryTitle),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(SudsShapes.control),
                        contentDescription = null,
                    )
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = summaryTitle,
                            color = SudsColors.onBrand,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (summaryDetail != null) {
                            Text(
                                text = summaryDetail.bookingCompactPrice(),
                                color = SudsColors.onBrandMuted,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                Surface(
                    modifier = Modifier
                        .width(150.dp)
                        .then(if (summaryTitle == null) Modifier.weight(1f) else Modifier)
                        .height(64.dp)
                        .semantics { role = Role.Button }
                        .clickable(enabled = enabled, onClick = onClick),
                    shape = SudsShapes.capsule,
                    color = if (enabled) SudsColors.transparent else SudsColors.glassStrong,
                    contentColor = if (enabled) SudsColors.onAction else SudsColors.onBrandMuted,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (enabled) {
                                    Modifier.background(
                                        Brush.horizontalGradient(SudsColors.actionGradient),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .padding(horizontal = SudsSpacing.lg),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(SudsSpacing.xs))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(SudsSpacing.xl),
                        )
                    }
                }
            }
        }
    }
}
