package com.coinflipper.app.monetization

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdPacingTest {
    private fun pacing() = AdPacing(sessionStartMs = 0, flipsBetween = 3, minIntervalMs = 1000, graceMs = 500)

    @Test
    fun needsEnoughFlips() {
        val p = pacing()
        repeat(2) { p.recordFlip() }
        assertFalse(p.shouldShow(10_000))
        p.recordFlip()
        assertTrue(p.shouldShow(10_000))
    }

    @Test
    fun respectsGracePeriodAfterLaunch() {
        val p = pacing()
        repeat(5) { p.recordFlip() }
        assertFalse(p.shouldShow(400))
        assertTrue(p.shouldShow(600))
    }

    @Test
    fun respectsMinimumIntervalAndResetsCount() {
        val p = pacing()
        repeat(3) { p.recordFlip() }
        p.markShown(1_000)
        repeat(3) { p.recordFlip() }
        assertFalse(p.shouldShow(1_500))
        assertTrue(p.shouldShow(2_000))
    }
}
