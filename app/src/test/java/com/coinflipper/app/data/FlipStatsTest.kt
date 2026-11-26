package com.coinflipper.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipStatsTest {
    @Test
    fun recordsCountsAndStreaks() {
        val s = FlipStats()
            .record(Side.HEADS)
            .record(Side.HEADS)
            .record(Side.HEADS)
            .record(Side.TAILS)

        assertEquals(3, s.heads)
        assertEquals(1, s.tails)
        assertEquals(Side.TAILS, s.streakSide)
        assertEquals(1, s.streak)
        assertEquals(3, s.longestStreak)
        assertEquals(listOf(Side.TAILS, Side.HEADS, Side.HEADS, Side.HEADS), s.history)
        assertEquals(75, s.percent(Side.HEADS))
    }

    @Test
    fun historyIsCapped() {
        var s = FlipStats()
        repeat(FlipStats.MAX_HISTORY + 10) { s = s.record(Side.TAILS) }
        assertEquals(FlipStats.MAX_HISTORY, s.history.size)
    }

    @Test
    fun coinIsRoughlyFair() {
        val coin = Coin()
        val n = 100_000
        val heads = (1..n).count { coin.flip() == Side.HEADS }
        // ~6 standard deviations either side of 50%.
        assertTrue("heads=$heads", heads in 49_000..51_000)
    }
}

class AppSettingsTest {
    @Test
    fun premiumSkinsLockedUntilUnlockedOrAdsRemoved() {
        val s = AppSettings()
        assertTrue(s.isLocked(CoinSkin.BRONZE))
        assertTrue(!s.isLocked(CoinSkin.SILVER))
        assertTrue(!s.copy(unlockedSkins = setOf(CoinSkin.BRONZE)).isLocked(CoinSkin.BRONZE))
        assertTrue(!s.copy(adsRemoved = true).isLocked(CoinSkin.ROSE))
    }
}
