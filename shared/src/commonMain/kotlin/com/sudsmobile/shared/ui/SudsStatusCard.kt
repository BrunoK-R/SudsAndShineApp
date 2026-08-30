package com.sudsmobile.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsSpacing

enum class SudsStatus {
    Neutral,
    Success,
    Warning,
    Error,
}

private fun SudsStatus.color(): Color = when (this) {
    SudsStatus.Neutral -> SudsColors.cyanMuted
    SudsStatus.Success -> SudsColors.success
    SudsStatus.Warning -> SudsColors.warning
    SudsStatus.Error -> SudsColors.error
}

@Composable
fun SudsStatusCard(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    status: SudsStatus = SudsStatus.Neutral,
    action: (@Composable () -> Unit)? = null,
) {
    SudsGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SudsSpacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            SudsStatusDot(
                color = status.color(),
                modifier = Modifier
                    .padding(top = SudsSpacing.xs)
                    .size(SudsSpacing.xs),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SudsSpacing.xxs),
            ) {
                Text(
                    text = title,
                    color = SudsColors.onBrand,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (message != null) {
                    Text(
                        text = message,
                        color = SudsColors.onBrandMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (action != null) {
                    action()
                }
            }
        }
    }
}
