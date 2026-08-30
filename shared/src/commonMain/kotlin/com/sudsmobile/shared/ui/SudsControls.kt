package com.sudsmobile.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.LocalSudsMotionPreferences
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsShapes
import com.sudsmobile.shared.theme.SudsSpacing

@Composable
fun SudsPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val reduceMotion = LocalSudsMotionPreferences.current.reduceMotion
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 56.dp)
            .then(
                if (loading) Modifier.semantics { stateDescription = "A carregar" }
                else Modifier,
            ),
        enabled = enabled && !loading,
        shape = SudsShapes.capsule,
        colors = ButtonDefaults.buttonColors(
            containerColor = SudsColors.cyan,
            contentColor = SudsColors.onAction,
            disabledContainerColor = SudsColors.glassStrong,
            disabledContentColor = SudsColors.onBrandMuted,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        when {
            loading && reduceMotion -> Text(
                text = "…",
                style = MaterialTheme.typography.titleMedium,
            )

            loading -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = SudsColors.onAction,
                strokeWidth = 2.dp,
            )

            else -> {
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(Modifier.width(SudsSpacing.xs))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

enum class SudsProgressSegmentState {
    Completed,
    Active,
    Upcoming,
}

fun progressSegmentStates(
    currentStepIndex: Int,
    totalSteps: Int,
): List<SudsProgressSegmentState> {
    if (totalSteps <= 0) return emptyList()
    val activeIndex = currentStepIndex.coerceIn(0, totalSteps - 1)
    return List(totalSteps) { index ->
        when {
            index < activeIndex -> SudsProgressSegmentState.Completed
            index == activeIndex -> SudsProgressSegmentState.Active
            else -> SudsProgressSegmentState.Upcoming
        }
    }
}

@Composable
fun SudsProgressSegments(
    currentStepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    if (totalSteps <= 0) return
    val states = progressSegmentStates(currentStepIndex, totalSteps)
    val semanticProgress = (currentStepIndex.coerceIn(0, totalSteps - 1) + 1).toFloat()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = semanticProgress,
                    range = 1f..totalSteps.toFloat(),
                    steps = (totalSteps - 2).coerceAtLeast(0),
                )
            },
        horizontalArrangement = Arrangement.spacedBy(SudsSpacing.xs),
    ) {
        states.forEach { state ->
            val color = when (state) {
                SudsProgressSegmentState.Completed -> SudsColors.cyanMuted
                SudsProgressSegmentState.Active -> SudsColors.cyan
                SudsProgressSegmentState.Upcoming -> SudsColors.glassStrong
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
