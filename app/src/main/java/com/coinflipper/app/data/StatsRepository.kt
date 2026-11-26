package com.coinflipper.app.data

import android.content.Context
import androidx.core.content.edit

/** Stores stats locally on the device. Nothing ever leaves the phone. */
class StatsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("flip_stats", Context.MODE_PRIVATE)

    fun load(): FlipStats = FlipStats(
        heads = prefs.getInt(KEY_HEADS, 0),
        tails = prefs.getInt(KEY_TAILS, 0),
        streakSide = prefs.getString(KEY_STREAK_SIDE, null)?.let { runCatching { Side.valueOf(it) }.getOrNull() },
        streak = prefs.getInt(KEY_STREAK, 0),
        longestStreak = prefs.getInt(KEY_LONGEST, 0),
        history = prefs.getString(KEY_HISTORY, "").orEmpty().mapNotNull {
            when (it) { 'H' -> Side.HEADS; 'T' -> Side.TAILS; else -> null }
        },
    )

    fun save(stats: FlipStats) = prefs.edit {
        putInt(KEY_HEADS, stats.heads)
        putInt(KEY_TAILS, stats.tails)
        putString(KEY_STREAK_SIDE, stats.streakSide?.name)
        putInt(KEY_STREAK, stats.streak)
        putInt(KEY_LONGEST, stats.longestStreak)
        putString(KEY_HISTORY, stats.history.joinToString("") { if (it == Side.HEADS) "H" else "T" })
    }

    private companion object {
        const val KEY_HEADS = "heads"
        const val KEY_TAILS = "tails"
        const val KEY_STREAK_SIDE = "streak_side"
        const val KEY_STREAK = "streak"
        const val KEY_LONGEST = "longest_streak"
        const val KEY_HISTORY = "history"
    }
}
