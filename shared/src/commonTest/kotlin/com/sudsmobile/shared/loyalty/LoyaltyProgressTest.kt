package com.sudsmobile.shared.loyalty

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoyaltyProgressTest {
    @Test
    fun progressStartsEmpty() {
        val progress = 0.toLoyaltyProgress()

        assertEquals(0, progress.totalWashes)
        assertEquals(0, progress.currentWashes)
        assertEquals(10, progress.remainingWashes)
        assertEquals(0f, progress.progress)
        assertFalse(progress.rewardReady)
    }

    @Test
    fun progressTracksCurrentRewardCycle() {
        val progress = 7.toLoyaltyProgress()

        assertEquals(7, progress.totalWashes)
        assertEquals(7, progress.currentWashes)
        assertEquals(3, progress.remainingWashes)
        assertEquals(0.7f, progress.progress)
        assertFalse(progress.rewardReady)
    }

    @Test
    fun exactRewardBoundaryMarksRewardReady() {
        val progress = 10.toLoyaltyProgress()

        assertEquals(10, progress.totalWashes)
        assertEquals(10, progress.currentWashes)
        assertEquals(0, progress.remainingWashes)
        assertEquals(1.0f, progress.progress)
        assertEquals(1, progress.completedRewards)
        assertTrue(progress.rewardReady)
    }

    @Test
    fun claimedRewardResetsProgressTowardNextCycle() {
        val progress = 10.toLoyaltyProgress(claimedRewards = 1)

        assertEquals(10, progress.totalWashes)
        assertEquals(0, progress.currentWashes)
        assertEquals(10, progress.remainingWashes)
        assertEquals(0f, progress.progress)
        assertEquals(1, progress.completedRewards)
        assertEquals(1, progress.claimedRewards)
        assertEquals(0, progress.availableRewards)
        assertFalse(progress.rewardReady)
    }

    @Test
    fun progressStaysRewardReadyAfterBoundaryUntilClaimed() {
        val progress = 12.toLoyaltyProgress()

        assertEquals(12, progress.totalWashes)
        assertEquals(10, progress.currentWashes)
        assertEquals(0, progress.remainingWashes)
        assertEquals(1.0f, progress.progress)
        assertEquals(1, progress.completedRewards)
        assertEquals(1, progress.availableRewards)
        assertTrue(progress.rewardReady)
    }

    @Test
    fun progressContinuesAfterClaimedRewardBoundary() {
        val progress = 12.toLoyaltyProgress(claimedRewards = 1)

        assertEquals(12, progress.totalWashes)
        assertEquals(2, progress.currentWashes)
        assertEquals(8, progress.remainingWashes)
        assertEquals(0.2f, progress.progress)
        assertEquals(1, progress.completedRewards)
        assertEquals(0, progress.availableRewards)
        assertFalse(progress.rewardReady)
    }
}
