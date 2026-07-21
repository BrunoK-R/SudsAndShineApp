package com.sudsmobile.feature.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileAvatarCropMathTest {
    @Test
    fun clampsScaleAndOffsets() {
        val clamped = clampProfileAvatarCropTransform(
            imageWidthPx = 1200,
            imageHeightPx = 800,
            viewportSizePx = 400,
            transform = ProfileAvatarCropTransform(scale = 8f, offsetX = 5000f, offsetY = -5000f),
        )

        assertEquals(PROFILE_AVATAR_MAX_SCALE, clamped.scale)
        assertEquals(1000f, clamped.offsetX)
        assertEquals(-600f, clamped.offsetY)
    }

    @Test
    fun resolvesCenteredCropForDefaultTransform() {
        val rect = resolveProfileAvatarSourceCropRect(
            imageWidthPx = 1200,
            imageHeightPx = 800,
            viewportSizePx = 400,
            transform = ProfileAvatarCropTransform(),
        )

        assertEquals(200, rect.left)
        assertEquals(0, rect.top)
        assertEquals(800, rect.size)
    }

    @Test
    fun resolvesPanAndZoomDeterministically() {
        val rect = resolveProfileAvatarSourceCropRect(
            imageWidthPx = 1200,
            imageHeightPx = 800,
            viewportSizePx = 400,
            transform = ProfileAvatarCropTransform(scale = 2f, offsetX = 50f, offsetY = -30f),
        )

        assertEquals(350, rect.left)
        assertEquals(230, rect.top)
        assertEquals(400, rect.size)
    }

    @Test
    fun invalidInputsReturnSafeBounds() {
        val rect = resolveProfileAvatarSourceCropRect(
            imageWidthPx = 0,
            imageHeightPx = -1,
            viewportSizePx = 0,
            transform = ProfileAvatarCropTransform(scale = 0.1f, offsetX = 99f, offsetY = 99f),
        )

        assertEquals(ProfileAvatarSourceCropRect(left = 0, top = 0, size = 1), rect)
        val clamped = clampProfileAvatarCropTransform(500, 500, 300, ProfileAvatarCropTransform(scale = 0.2f))
        assertEquals(PROFILE_AVATAR_MIN_SCALE, clamped.scale)
        assertTrue(clamped.offsetX == 0f && clamped.offsetY == 0f)
    }
}
