package com.coinflipper.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** "Vault" palette: deep charcoal with warm metal accents. */
object VaultColors {
    val BackgroundTop = Color(0xFF1A1F2B)
    val BackgroundBottom = Color(0xFF0B0D12)
    val Surface = Color(0xFF171B23)
    val Card = Color(0x0FFFFFFF)
    val CardBorder = Color(0x1AFFFFFF)
    val TextPrimary = Color(0xFFEDEAE4)
    val TextSecondary = Color(0xFF9AA0AB)
}

private val VaultScheme = darkColorScheme(
    primary = Color(0xFFFFDE7A),
    onPrimary = Color(0xFF3A2A00),
    background = VaultColors.BackgroundBottom,
    onBackground = VaultColors.TextPrimary,
    surface = VaultColors.Surface,
    onSurface = VaultColors.TextPrimary,
    onSurfaceVariant = VaultColors.TextSecondary,
    surfaceContainerLow = VaultColors.Surface,
    surfaceContainer = VaultColors.Surface,
    surfaceContainerHigh = Color(0xFF1F2430),
    outline = Color(0xFF3A404C),
)

@Composable
fun CoinFlipTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VaultScheme, content = content)
}
