package com.coinflipper.app.monetization

/**
 * Decides when a full-screen ad may appear, so ads stay occasional:
 * after at least [flipsBetween] flips, no more than once per [minIntervalMs],
 * and never within [graceMs] of the app being opened.
 */
class AdPacing(
    private val sessionStartMs: Long,
    private val flipsBetween: Int = 15,
    private val minIntervalMs: Long = 3 * 60_000L,
    private val graceMs: Long = 60_000L,
) {
    private var flipsSinceAd = 0
    private var lastShownMs: Long? = null

    fun recordFlip() {
        flipsSinceAd++
    }

    fun shouldShow(nowMs: Long): Boolean {
        if (flipsSinceAd < flipsBetween) return false
        if (nowMs - sessionStartMs < graceMs) return false
        val last = lastShownMs ?: return true
        return nowMs - last >= minIntervalMs
    }

    fun markShown(nowMs: Long) {
        flipsSinceAd = 0
        lastShownMs = nowMs
    }
}
