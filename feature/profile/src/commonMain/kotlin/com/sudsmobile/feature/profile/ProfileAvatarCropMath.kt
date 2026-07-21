package com.sudsmobile.feature.profile

import kotlin.math.max
import kotlin.math.roundToInt

internal const val PROFILE_AVATAR_MIN_SCALE = 1f
internal const val PROFILE_AVATAR_MAX_SCALE = 4f

internal data class ProfileAvatarCropTransform(
    val scale: Float = PROFILE_AVATAR_MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

internal data class ProfileAvatarSourceCropRect(
    val left: Int,
    val top: Int,
    val size: Int,
)

internal fun clampProfileAvatarCropTransform(
    imageWidthPx: Int,
    imageHeightPx: Int,
    viewportSizePx: Int,
    transform: ProfileAvatarCropTransform,
): ProfileAvatarCropTransform {
    if (imageWidthPx <= 0 || imageHeightPx <= 0 || viewportSizePx <= 0) {
        return ProfileAvatarCropTransform()
    }
    val clampedScale = transform.scale.coerceIn(PROFILE_AVATAR_MIN_SCALE, PROFILE_AVATAR_MAX_SCALE)
    val baseScale = profileAvatarBaseScale(imageWidthPx, imageHeightPx, viewportSizePx)
    val displayedWidth = imageWidthPx * baseScale * clampedScale
    val displayedHeight = imageHeightPx * baseScale * clampedScale
    val maxOffsetX = ((displayedWidth - viewportSizePx) / 2f).coerceAtLeast(0f)
    val maxOffsetY = ((displayedHeight - viewportSizePx) / 2f).coerceAtLeast(0f)
    return ProfileAvatarCropTransform(
        scale = clampedScale,
        offsetX = transform.offsetX.coerceIn(-maxOffsetX, maxOffsetX),
        offsetY = transform.offsetY.coerceIn(-maxOffsetY, maxOffsetY),
    )
}

internal fun resolveProfileAvatarSourceCropRect(
    imageWidthPx: Int,
    imageHeightPx: Int,
    viewportSizePx: Int,
    transform: ProfileAvatarCropTransform,
): ProfileAvatarSourceCropRect {
    if (imageWidthPx <= 0 || imageHeightPx <= 0 || viewportSizePx <= 0) {
        return ProfileAvatarSourceCropRect(left = 0, top = 0, size = 1)
    }

    val clampedTransform = clampProfileAvatarCropTransform(
        imageWidthPx = imageWidthPx,
        imageHeightPx = imageHeightPx,
        viewportSizePx = viewportSizePx,
        transform = transform,
    )
    val totalScale = profileAvatarBaseScale(imageWidthPx, imageHeightPx, viewportSizePx) * clampedTransform.scale
    val displayedWidth = imageWidthPx * totalScale
    val displayedHeight = imageHeightPx * totalScale
    val imageTopLeftX = ((viewportSizePx - displayedWidth) / 2f) + clampedTransform.offsetX
    val imageTopLeftY = ((viewportSizePx - displayedHeight) / 2f) + clampedTransform.offsetY
    val sourceSizeFloat = (viewportSizePx / totalScale).coerceIn(
        minimumValue = 1f,
        maximumValue = minOf(imageWidthPx, imageHeightPx).toFloat(),
    )
    val sourceLeftFloat = ((0f - imageTopLeftX) / totalScale).coerceAtLeast(0f)
    val sourceTopFloat = ((0f - imageTopLeftY) / totalScale).coerceAtLeast(0f)
    val size = sourceSizeFloat.roundToInt().coerceAtLeast(1).coerceAtMost(minOf(imageWidthPx, imageHeightPx))
    return ProfileAvatarSourceCropRect(
        left = sourceLeftFloat.roundToInt().coerceIn(0, imageWidthPx - size),
        top = sourceTopFloat.roundToInt().coerceIn(0, imageHeightPx - size),
        size = size,
    )
}

private fun profileAvatarBaseScale(
    imageWidthPx: Int,
    imageHeightPx: Int,
    viewportSizePx: Int,
): Float = max(
    viewportSizePx.toFloat() / imageWidthPx.toFloat(),
    viewportSizePx.toFloat() / imageHeightPx.toFloat(),
)
