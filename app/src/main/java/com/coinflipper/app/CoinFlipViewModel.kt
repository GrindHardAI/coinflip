package com.coinflipper.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.coinflipper.app.data.Coin
import com.coinflipper.app.data.FlipStats
import com.coinflipper.app.data.Side
import com.coinflipper.app.data.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CoinFlipViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StatsRepository(app)
    private val coin = Coin()

    private val _stats = MutableStateFlow(repo.load())
    val stats: StateFlow<FlipStats> = _stats.asStateFlow()

    /** Decide the outcome up front so the animation can land on it. */
    fun nextResult(): Side = coin.flip()

    /** Called once the coin has landed. */
    fun record(side: Side) {
        _stats.update { it.record(side) }
        repo.save(_stats.value)
    }

    fun reset() {
        _stats.value = FlipStats()
        repo.save(_stats.value)
    }
}
