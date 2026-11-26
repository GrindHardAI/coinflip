package com.coinflipper.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.coinflipper.app.R

/** Short, low-latency coin sounds (original, synthesized audio in res/raw). */
class CoinSounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val flipId = pool.load(context, R.raw.coin_flip, 1)
    private val landId = pool.load(context, R.raw.coin_land, 1)

    fun playFlip() {
        pool.play(flipId, 0.6f, 0.6f, 1, 0, 1f)
    }

    fun playLand() {
        pool.play(landId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}
