package com.coinflipper.app.data

import android.content.Context
import androidx.core.content.edit

enum class CoinSkin { GOLD, SILVER, BRONZE, ROSE }

enum class LabelPreset { HEADS_TAILS, YES_NO, CUSTOM }

data class AppSettings(
    val skin: CoinSkin = CoinSkin.GOLD,
    val labels: LabelPreset = LabelPreset.HEADS_TAILS,
    val customHeads: String = "",
    val customTails: String = "",
    val sound: Boolean = true,
    val shake: Boolean = true,
) {
    companion object {
        const val MAX_LABEL_LENGTH = 14
    }
}

/** Stores user preferences locally on the device. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        skin = enumOrDefault(prefs.getString(KEY_SKIN, null), CoinSkin.GOLD),
        labels = enumOrDefault(prefs.getString(KEY_LABELS, null), LabelPreset.HEADS_TAILS),
        customHeads = prefs.getString(KEY_CUSTOM_HEADS, "").orEmpty(),
        customTails = prefs.getString(KEY_CUSTOM_TAILS, "").orEmpty(),
        sound = prefs.getBoolean(KEY_SOUND, true),
        shake = prefs.getBoolean(KEY_SHAKE, true),
    )

    fun save(s: AppSettings) = prefs.edit {
        putString(KEY_SKIN, s.skin.name)
        putString(KEY_LABELS, s.labels.name)
        putString(KEY_CUSTOM_HEADS, s.customHeads)
        putString(KEY_CUSTOM_TAILS, s.customTails)
        putBoolean(KEY_SOUND, s.sound)
        putBoolean(KEY_SHAKE, s.shake)
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    private companion object {
        const val KEY_SKIN = "skin"
        const val KEY_LABELS = "labels"
        const val KEY_CUSTOM_HEADS = "custom_heads"
        const val KEY_CUSTOM_TAILS = "custom_tails"
        const val KEY_SOUND = "sound"
        const val KEY_SHAKE = "shake"
    }
}
