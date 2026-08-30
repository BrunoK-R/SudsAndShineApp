package com.sudsmobile.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsSpacing
import com.sudsmobile.shared.ui.SudsPrimaryButton

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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = SudsColors.ink,
        border = BorderStroke(SudsSpacing.hairline, SudsColors.glassBorder),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SudsSpacing.contentGutter)
                .padding(
                    top = SudsSpacing.sm,
                    bottom = contentPadding.calculateBottomPadding() + SudsSpacing.sm,
                ),
            verticalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
        ) {
            if (summaryTitle != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SudsSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
                    ) {
                        Text(
                            text = "SELEÇÃO",
                            color = SudsColors.cyanMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = summaryTitle,
                            color = SudsColors.onBrand,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (summaryDetail != null) {
                        Text(
                            text = summaryDetail,
                            color = SudsColors.champagne,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(color = SudsColors.glassBorder)
            }

            SudsPrimaryButton(
                label = label,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
        }
    }
}
