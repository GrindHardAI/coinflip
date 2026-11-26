package com.coinflipper.app.data

import java.security.SecureRandom

enum class Side { HEADS, TAILS }

/** Immutable flip statistics. All logic is pure so it can be unit tested. */
data class FlipStats(
    val heads: Int = 0,
    val tails: Int = 0,
    val streakSide: Side? = null,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    /** Most recent first, capped at [MAX_HISTORY]. */
    val history: List<Side> = emptyList(),
) {
    val total: Int get() = heads + tails
    val last: Side? get() = history.firstOrNull()

    fun percent(side: Side): Int {
        if (total == 0) return 0
        val count = if (side == Side.HEADS) heads else tails
        return Math.round(count * 100f / total)
    }

    fun record(side: Side): FlipStats {
        val newStreak = if (side == streakSide) streak + 1 else 1
        return copy(
            heads = heads + if (side == Side.HEADS) 1 else 0,
            tails = tails + if (side == Side.TAILS) 1 else 0,
            streakSide = side,
            streak = newStreak,
            longestStreak = maxOf(longestStreak, newStreak),
            history = (listOf(side) + history).take(MAX_HISTORY),
        )
    }

    companion object {
        const val MAX_HISTORY = 30
    }
}

/** Fair 50/50 coin using a cryptographically strong RNG. */
class Coin(private val random: SecureRandom = SecureRandom()) {
    fun flip(): Side = if (random.nextBoolean()) Side.HEADS else Side.TAILS
}
