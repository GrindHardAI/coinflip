package com.coinflipper.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.coinflipper.app.data.AppSettings
import com.coinflipper.app.data.Coin
import com.coinflipper.app.data.FlipStats
import com.coinflipper.app.data.SettingsRepository
import com.coinflipper.app.data.Side
import com.coinflipper.app.data.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CoinFlipViewModel(app: Application) : AndroidViewModel(app) {
    private val statsRepo = StatsRepository(app)
    private val settingsRepo = SettingsRepository(app)
    private val coin = Coin()

    private val _stats = MutableStateFlow(statsRepo.load())
    val stats: StateFlow<FlipStats> = _stats.asStateFlow()

    private val _settings = MutableStateFlow(settingsRepo.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** Decide the outcome up front so the animation can land on it. */
    fun nextResult(): Side = coin.flip()

    /** Called once the coin has landed. */
    fun record(side: Side) {
        _stats.update { it.record(side) }
        statsRepo.save(_stats.value)
    }

    fun reset() {
        _stats.value = FlipStats()
        statsRepo.save(_stats.value)
    }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        _settings.update(change)
        settingsRepo.save(_settings.value)
    }
}
