package com.coinflipper.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.coinflipper.app.monetization.Monetization
import com.coinflipper.app.monetization.NoMonetization
import com.coinflipper.app.monetization.PlayMonetization
import com.coinflipper.app.ui.CoinFlipScreen
import com.coinflipper.app.ui.CoinFlipTheme

class MainActivity : ComponentActivity() {
    private val vm: CoinFlipViewModel by viewModels()
    private var playMonetization: PlayMonetization? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app always uses its dark "vault" look, so keep system bar icons light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Automated screenshot runs start the app with this flag so no test ads appear.
        val screenshotMode = intent.getBooleanExtra(EXTRA_SCREENSHOT_MODE, false)
        val monetization: Monetization = if (screenshotMode) {
            NoMonetization
        } else {
            PlayMonetization(this, onAdsRemovedChanged = vm::setAdsRemoved).also {
                playMonetization = it
                it.start()
            }
        }

        setContent {
            CoinFlipTheme {
                CoinFlipScreen(vm = vm, monetization = monetization)
            }
        }
    }

    override fun onDestroy() {
        playMonetization?.stop()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SCREENSHOT_MODE = "screenshot_mode"
    }
}
