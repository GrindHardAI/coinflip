package com.coinflipper.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coinflipper.app.data.Side

/**
 * A realistic coin that tumbles end over end (around its horizontal axis), like a real toss.
 *
 * @param angle rotation in degrees. 0 (mod 360) shows heads, 180 shows tails.
 * @param toss  0..1 progress through the toss; drives the height and scale arc.
 */
@Composable
fun CoinView(
    angle: Float,
    toss: Float,
    metal: Metal,
    headsLabel: String,
    tailsLabel: String,
    modifier: Modifier = Modifier,
    tossHeight: Dp = 88.dp,
) {
    val normalized = ((angle % 360f) + 360f) % 360f
    val showingTails = normalized > 90f && normalized < 270f
    // Parabola: 0 at start/end, 1 at the peak.
    val arc = 4f * toss * (1f - toss)

    Box(modifier) {
        // Soft contact shadow that shrinks as the coin rises.
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawOval(
                color = Color.Black.copy(alpha = 0.45f * (1f - 0.6f * arc)),
                topLeft = Offset(center.x - r * 0.8f * (1f - 0.3f * arc), size.height - r * 0.06f),
                size = androidx.compose.ui.geometry.Size(r * 1.6f * (1f - 0.3f * arc), r * 0.18f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = angle
                    cameraDistance = 14f * density
                    translationY = -arc * tossHeight.toPx()
                    val s = 1f + 0.2f * arc
                    scaleX = s
                    scaleY = s
                },
        ) {
            RealCoinFace(
                side = if (showingTails) Side.TAILS else Side.HEADS,
                metal = metal,
                label = if (showingTails) tailsLabel else headsLabel,
                // The back face is seen upside down; flip it back so the design reads correctly.
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { if (showingTails) rotationX = 180f },
            )
        }
    }
}
