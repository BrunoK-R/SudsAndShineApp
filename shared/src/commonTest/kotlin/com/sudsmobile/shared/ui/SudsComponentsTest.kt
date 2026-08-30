package com.sudsmobile.shared.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SudsComponentsTest {

    @Test
    fun progressSegmentsRepresentCompletedActiveAndUpcomingSteps() {
        assertEquals(
            listOf(
                SudsProgressSegmentState.Completed,
                SudsProgressSegmentState.Completed,
                SudsProgressSegmentState.Active,
                SudsProgressSegmentState.Upcoming,
            ),
            progressSegmentStates(currentStepIndex = 2, totalSteps = 4),
        )
    }

    @Test
    fun progressSegmentsClampOutOfRangeInput() {
        assertEquals(
            SudsProgressSegmentState.Active,
            progressSegmentStates(currentStepIndex = -2, totalSteps = 4).first(),
        )
        assertEquals(
            SudsProgressSegmentState.Active,
            progressSegmentStates(currentStepIndex = 20, totalSteps = 4).last(),
        )
        assertTrue(progressSegmentStates(currentStepIndex = 0, totalSteps = 0).isEmpty())
    }

    @Test
    fun serviceArtworkMapsCatalogKeysWithoutDependingOnCatalogModels() {
        assertEquals(SudsServiceArtworkStyle.Interior, serviceArtworkStyleForKey("deep-interior"))
        assertEquals(SudsServiceArtworkStyle.Premium, serviceArtworkStyleForKey("premium-detail"))
        assertEquals(SudsServiceArtworkStyle.Wash, serviceArtworkStyleForKey("standard-wash"))
    }
}
