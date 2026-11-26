package com.coinflipper.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinflipper.app.R
import com.coinflipper.app.data.Side

private val GoldLight = Color(0xFFFFE082)
private val Gold = Color(0xFFF5C542)
private val GoldDark = Color(0xFFB8860B)
private val SilverLight = Color(0xFFF1F3F5)
private val Silver = Color(0xFFC9CED6)
private val SilverDark = Color(0xFF7D8590)

/**
 * A coin that can be rotated around its vertical axis.
 *
 * @param angle rotation in degrees. 0 (mod 360) shows heads, 180 shows tails.
 * @param toss  0..1 progress through the toss; drives the height and scale arc.
 * @param tossHeight how high the coin travels at the top of the arc.
 */
@Composable
fun CoinView(
    angle: Float,
    toss: Float,
    modifier: Modifier = Modifier,
    tossHeight: Dp = 220.dp,
) {
    val normalized = ((angle % 360f) + 360f) % 360f
    val showingTails = normalized > 90f && normalized < 270f
    // Parabola: 0 at start/end, 1 at the peak.
    val arc = 4f * toss * (1f - toss)

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = 14f * density
            translationY = -arc * tossHeight.toPx()
            val s = 1f + 0.2f * arc
            scaleX = s
            scaleY = s
        },
    ) {
        CoinFace(
            side = if (showingTails) Side.TAILS else Side.HEADS,
            // The back face is seen mirrored; flip it back so the text reads correctly.
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { if (showingTails) rotationY = 180f },
        )
    }
}

@Composable
private fun CoinFace(side: Side, modifier: Modifier = Modifier) {
    val heads = side == Side.HEADS
    val light = if (heads) GoldLight else SilverLight
    val mid = if (heads) Gold else Silver
    val dark = if (heads) GoldDark else SilverDark

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            // Rim
            drawCircle(color = dark, radius = r)
            // Face with a soft highlight toward the top-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(light, mid, dark),
                    center = Offset(center.x - r * 0.35f, center.y - r * 0.35f),
                    radius = r * 1.6f,
                ),
                radius = r * 0.9f,
            )
            // Engraved inner ring
            drawCircle(
                color = dark.copy(alpha = 0.6f),
                radius = r * 0.76f,
                style = Stroke(width = r * 0.03f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(if (heads) R.string.heads_short else R.string.tails_short),
                fontSize = 88.sp,
                fontWeight = FontWeight.Black,
                color = dark,
            )
            Text(
                text = stringResource(if (heads) R.string.heads else R.string.tails).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = dark,
            )
        }
    }
}
