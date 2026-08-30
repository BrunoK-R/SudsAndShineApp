package com.sudsmobile.feature.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class SplashScreenTest {
    @Test
    fun waitsForTheConfiguredMinimumDurationBeforeFinishing() = runTest {
        val requestedDelays = mutableListOf<Long>()

        awaitMinimumSplashDuration { requestedDelays += it }

        assertEquals(listOf(SplashMinimumDurationMillis), requestedDelays)
        assertEquals(1_400L, SplashMinimumDurationMillis)
    }
}
