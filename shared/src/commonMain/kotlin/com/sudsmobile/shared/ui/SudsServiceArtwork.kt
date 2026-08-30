package com.sudsmobile.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sudsmobile.shared.theme.SudsColors
import com.sudsmobile.shared.theme.SudsShapes

enum class SudsServiceArtworkStyle {
    Wash,
    Interior,
    Premium,
}

fun serviceArtworkStyleForKey(key: String): SudsServiceArtworkStyle {
    val normalized = key.lowercase()
    return when {
        normalized.contains("interior") || normalized.contains("inside") -> SudsServiceArtworkStyle.Interior
        normalized.contains("premium") || normalized.contains("detail") -> SudsServiceArtworkStyle.Premium
        else -> SudsServiceArtworkStyle.Wash
    }
}

@Composable
fun SudsServiceArtwork(
    modifier: Modifier = Modifier,
    style: SudsServiceArtworkStyle = SudsServiceArtworkStyle.Wash,
    size: Dp = 152.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .aspectRatio(1f)
            .clip(SudsShapes.hero)
            .background(SudsColors.glass),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = SudsColors.cyan.copy(alpha = 0.16f),
                radius = this.size.minDimension * 0.42f,
                center = Offset(this.size.width * 0.55f, this.size.height * 0.46f),
            )
            drawCarSilhouette(style)
            when (style) {
                SudsServiceArtworkStyle.Wash -> drawBubbles()
                SudsServiceArtworkStyle.Interior -> drawInteriorLines()
                SudsServiceArtworkStyle.Premium -> drawSparkles()
            }
        }
    }
}

private fun DrawScope.drawCarSilhouette(style: SudsServiceArtworkStyle) {
    val bodyColor = when (style) {
        SudsServiceArtworkStyle.Wash -> SudsColors.cyanMuted
        SudsServiceArtworkStyle.Interior -> SudsColors.onBrand
        SudsServiceArtworkStyle.Premium -> SudsColors.champagne
    }
    val left = size.width * 0.17f
    val top = size.height * 0.43f
    val width = size.width * 0.66f
    val height = size.height * 0.25f
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height * 0.34f),
        style = Stroke(width = size.minDimension * 0.035f),
    )
    val roof = Path().apply {
        moveTo(size.width * 0.31f, top)
        quadraticTo(size.width * 0.4f, size.height * 0.27f, size.width * 0.58f, size.height * 0.3f)
        quadraticTo(size.width * 0.69f, size.height * 0.33f, size.width * 0.75f, top)
    }
    drawPath(
        path = roof,
        color = bodyColor,
        style = Stroke(width = size.minDimension * 0.035f),
    )
    drawCircle(bodyColor, size.minDimension * 0.055f, Offset(size.width * 0.33f, size.height * 0.68f))
    drawCircle(bodyColor, size.minDimension * 0.055f, Offset(size.width * 0.69f, size.height * 0.68f))
}

private fun DrawScope.drawBubbles() {
    listOf(
        Triple(0.19f, 0.24f, 0.055f),
        Triple(0.73f, 0.21f, 0.075f),
        Triple(0.84f, 0.38f, 0.035f),
    ).forEach { (x, y, radius) ->
        drawCircle(
            color = SudsColors.cyan,
            radius = size.minDimension * radius,
            center = Offset(size.width * x, size.height * y),
            style = Stroke(width = size.minDimension * 0.018f),
        )
    }
}

private fun DrawScope.drawInteriorLines() {
    drawLine(
        color = SudsColors.cyan,
        start = Offset(size.width * 0.39f, size.height * 0.34f),
        end = Offset(size.width * 0.39f, size.height * 0.5f),
        strokeWidth = size.minDimension * 0.02f,
    )
    drawLine(
        color = SudsColors.cyan,
        start = Offset(size.width * 0.62f, size.height * 0.34f),
        end = Offset(size.width * 0.62f, size.height * 0.5f),
        strokeWidth = size.minDimension * 0.02f,
    )
}

private fun DrawScope.drawSparkles() {
    drawSparkle(Offset(size.width * 0.78f, size.height * 0.22f), size.minDimension * 0.09f)
    drawSparkle(Offset(size.width * 0.21f, size.height * 0.3f), size.minDimension * 0.055f)
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.25f, center.y - radius * 0.25f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * 0.25f, center.y + radius * 0.25f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.25f, center.y + radius * 0.25f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * 0.25f, center.y - radius * 0.25f)
        close()
    }
    drawPath(path, SudsColors.champagne)
}
