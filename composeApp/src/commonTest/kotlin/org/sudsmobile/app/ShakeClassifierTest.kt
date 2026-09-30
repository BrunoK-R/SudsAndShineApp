package org.sudsmobile.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShakeClassifierTest {
    @Test
    fun requiresTwoDistinctImpulsesWithinWindow() {
        val classifier = ShakeClassifier()
        assertFalse(classifier.onSample(3.0, 0.0, 0.0, 100))
        assertFalse(classifier.onSample(3.0, 0.0, 0.0, 110))
        assertFalse(classifier.onSample(1.0, 0.0, 0.0, 120))
        assertTrue(classifier.onSample(3.0, 0.0, 0.0, 400))
    }

    @Test
    fun ignoresOldImpulsesAndAppliesCooldown() {
        val classifier = ShakeClassifier()
        assertFalse(classifier.onSample(3.0, 0.0, 0.0, 100))
        classifier.onSample(1.0, 0.0, 0.0, 200)
        assertFalse(classifier.onSample(3.0, 0.0, 0.0, 800))
        classifier.onSample(1.0, 0.0, 0.0, 900)
        assertTrue(classifier.onSample(3.0, 0.0, 0.0, 1000))
        classifier.onSample(1.0, 0.0, 0.0, 1100)
        assertFalse(classifier.onSample(3.0, 0.0, 0.0, 1200))
    }
}
