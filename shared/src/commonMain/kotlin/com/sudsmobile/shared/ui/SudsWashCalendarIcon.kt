package com.sudsmobile.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A compact calendar-and-water-drop mark unique to the wash-booking action. */
@Composable
fun SudsWashCalendarIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    Canvas(modifier.size(size)) {
        val unit = this.size.minDimension / 28f
        val strokeWidth = 2.2f * unit
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * unit, 5f * unit),
            size = Size(22f * unit, 20f * unit),
            cornerRadius = CornerRadius(3f * unit),
            style = stroke,
        )
        drawLine(
            color = tint,
            start = Offset(3f * unit, 10f * unit),
            end = Offset(25f * unit, 10f * unit),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        listOf(8f, 20f).forEach { x ->
            drawLine(
                color = tint,
                start = Offset(x * unit, 2.5f * unit),
                end = Offset(x * unit, 7.5f * unit),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }

        val drop = Path().apply {
            moveTo(14f * unit, 13f * unit)
            cubicTo(13f * unit, 15f * unit, 10f * unit, 18f * unit, 10f * unit, 20f * unit)
            cubicTo(10f * unit, 22.4f * unit, 11.8f * unit, 24f * unit, 14f * unit, 24f * unit)
            cubicTo(16.2f * unit, 24f * unit, 18f * unit, 22.4f * unit, 18f * unit, 20f * unit)
            cubicTo(18f * unit, 18f * unit, 15f * unit, 15f * unit, 14f * unit, 13f * unit)
            close()
        }
        drawPath(path = drop, color = tint, style = stroke)
    }
}
