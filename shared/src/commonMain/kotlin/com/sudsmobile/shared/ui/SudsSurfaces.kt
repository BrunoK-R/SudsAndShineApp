package com.sudsmobile.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsShapes
import com.sudsmobile.shared.theme.SudsSpacing

@Composable
fun SudsBrandBackground(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(SudsColors.ink, SudsColors.navy, SudsColors.navyElevated),
            ),
        ),
        contentAlignment = contentAlignment,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = SudsColors.cyan.copy(alpha = 0.08f),
                radius = size.minDimension * 0.58f,
                center = Offset(size.width * 0.94f, size.height * 0.08f),
            )
            drawCircle(
                color = SudsColors.champagne.copy(alpha = 0.05f),
                radius = size.minDimension * 0.38f,
                center = Offset(size.width * 0.04f, size.height * 0.72f),
            )
        }
        content()
    }
}

@Composable
fun SudsGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = SudsShapes.card,
    contentPadding: PaddingValues = PaddingValues(SudsSpacing.lg),
    containerColor: Color = SudsColors.glass,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(SudsSpacing.hairline, SudsColors.glassBorder, shape)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
internal fun SudsStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color),
    )
}
