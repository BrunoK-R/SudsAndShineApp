package com.sudsmobile.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class MotionTest {

    @Test
    fun collapseProgressStartsExpandedAndClampsAtCollapsed() {
        assertEquals(0f, calculateCollapseProgress(0, 0, 200))
        assertEquals(0.5f, calculateCollapseProgress(0, 100, 200))
        assertEquals(1f, calculateCollapseProgress(0, 300, 200))
        assertEquals(1f, calculateCollapseProgress(1, 0, 200))
    }

    @Test
    fun collapseProgressHandlesMissingRange() {
        assertEquals(0f, calculateCollapseProgress(0, 0, 0))
        assertEquals(1f, calculateCollapseProgress(0, 1, 0))
    }

    @Test
    fun interpolationClampsProgress() {
        assertEquals(100, interpolateInt(100, 200, -1f))
        assertEquals(125, interpolateInt(100, 200, 0.25f))
        assertEquals(200, interpolateInt(100, 200, 2f))
    }

    @Test
    fun reducedMotionSelectsImmediateDuration() {
        assertEquals(
            0,
            motionDurationMillis(
                durationMillis = SudsMotion.emphasized,
                preferences = SudsMotionPreferences(reduceMotion = true),
            ),
        )
        assertEquals(
            SudsMotion.emphasized,
            motionDurationMillis(
                durationMillis = SudsMotion.emphasized,
                preferences = SudsMotionPreferences(reduceMotion = false),
            ),
        )
    }
}
