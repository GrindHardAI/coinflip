package com.coinflipper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.coinflipper.app.ui.CoinFlipScreen
import com.coinflipper.app.ui.CoinFlipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoinFlipTheme {
                CoinFlipScreen()
            }
        }
    }
}
