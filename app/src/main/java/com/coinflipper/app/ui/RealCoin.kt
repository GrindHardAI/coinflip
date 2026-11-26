package com.coinflipper.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.coinflipper.app.R
import com.coinflipper.app.data.CoinSkin
import com.coinflipper.app.data.Side
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Five tones of one metal, from brightest highlight to deepest shadow. */
data class Metal(
    val highlight: Color,
    val light: Color,
    val base: Color,
    val dark: Color,
    val deep: Color,
)

val CoinSkin.metal: Metal
    get() = when (this) {
        CoinSkin.GOLD -> Metal(Color(0xFFFFF6D5), Color(0xFFFFDE7A), Color(0xFFE2B33C), Color(0xFFAE7F12), Color(0xFF5E4200))
        CoinSkin.SILVER -> Metal(Color(0xFFFFFFFF), Color(0xFFE9ECF1), Color(0xFFBCC3CC), Color(0xFF7F8792), Color(0xFF3A4048))
        CoinSkin.BRONZE -> Metal(Color(0xFFFFE6CC), Color(0xFFEDB27E), Color(0xFFC17B3F), Color(0xFF8A4F1E), Color(0xFF45250B))
        CoinSkin.ROSE -> Metal(Color(0xFFFFEDE8), Color(0xFFF6C3B4), Color(0xFFD9917F), Color(0xFFA25D4D), Color(0xFF552B22))
    }

val CoinSkin.nameRes: Int
    get() = when (this) {
        CoinSkin.GOLD -> R.string.skin_gold
        CoinSkin.SILVER -> R.string.skin_silver
        CoinSkin.BRONZE -> R.string.skin_bronze
        CoinSkin.ROSE -> R.string.skin_rose
    }

/** One face of a realistic, original-design coin (crown on heads, laurel wreath on tails). */
@Composable
fun RealCoinFace(side: Side, metal: Metal, label: String, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawRealCoin(side, metal, label) }
}

fun DrawScope.drawRealCoin(side: Side, metal: Metal, label: String) {
    val r = size.minDimension / 2f
    val c = center

    // 1. Outer edge with fine ridges (reeded edge)
    drawCircle(
        brush = Brush.linearGradient(
            listOf(metal.light, metal.dark, metal.base, metal.deep),
            start = Offset(c.x - r, c.y - r),
            end = Offset(c.x + r, c.y + r),
        ),
        radius = r,
    )
    val ridges = 150
    val ridgeWidth = (r * 0.011f).coerceAtLeast(1f)
    for (i in 0 until ridges) {
        val a = ((i + 0.5) * 2 * PI / ridges).toFloat()
        drawLine(
            color = metal.deep.copy(alpha = 0.5f),
            start = Offset(c.x + cos(a) * r * 0.935f, c.y + sin(a) * r * 0.935f),
            end = Offset(c.x + cos(a) * r * 0.995f, c.y + sin(a) * r * 0.995f),
            strokeWidth = ridgeWidth,
        )
    }

    // 2. Raised rim: lit from the top-left
    drawCircle(
        brush = Brush.linearGradient(
            listOf(metal.highlight, metal.base, metal.deep),
            start = Offset(c.x - r, c.y - r),
            end = Offset(c.x + r, c.y + r),
        ),
        radius = r * 0.92f,
    )

    // 3. Recessed field: shaded the opposite way, plus a soft sheen
    drawCircle(
        brush = Brush.linearGradient(
            listOf(metal.dark, metal.base, metal.light),
            start = Offset(c.x - r * 0.8f, c.y - r * 0.8f),
            end = Offset(c.x + r * 0.8f, c.y + r * 0.8f),
        ),
        radius = r * 0.84f,
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(metal.highlight.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(c.x - r * 0.3f, c.y - r * 0.35f),
            radius = r * 0.9f,
        ),
        radius = r * 0.84f,
    )

    // 4. Beaded inner ring
    val beads = 72
    val beadR = r * 0.013f
    val shade = r * 0.006f
    for (i in 0 until beads) {
        val a = (i * 2 * PI / beads).toFloat()
        val p = Offset(c.x + cos(a) * r * 0.795f, c.y + sin(a) * r * 0.795f)
        drawCircle(metal.deep.copy(alpha = 0.7f), radius = beadR, center = Offset(p.x + shade, p.y + shade))
        drawCircle(metal.highlight.copy(alpha = 0.9f), radius = beadR, center = Offset(p.x - shade * 0.5f, p.y - shade * 0.5f))
        drawCircle(metal.base, radius = beadR * 0.8f, center = p)
    }

    // 5. Central engraving
    val emblem = if (side == Side.HEADS) {
        crownPath(c.x, c.y + r * 0.1f, r * 0.34f)
    } else {
        wreathPath(c.x, c.y + r * 0.08f, r * 0.36f)
    }
    embossPath(emblem, metal, r)

    // 6. Label around the top, three small stars at the bottom
    if (label.isNotBlank()) drawArcLabel(label.uppercase(), metal, c, r)
    for (deg in listOf(72.0, 90.0, 108.0)) {
        val a = Math.toRadians(deg)
        val p = Offset(c.x + cos(a).toFloat() * r * 0.68f, c.y + sin(a).toFloat() * r * 0.68f)
        embossPath(starPath(p.x, p.y, r * 0.055f, r * 0.022f), metal, r)
    }
}

/** Draws a path so it looks raised: light edge top-left, shadow bottom-right. */
private fun DrawScope.embossPath(path: Path, metal: Metal, r: Float) {
    val d = r * 0.012f
    translate(left = -d, top = -d) { drawPath(path, metal.highlight.copy(alpha = 0.9f)) }
    translate(left = d, top = d) { drawPath(path, metal.deep.copy(alpha = 0.8f)) }
    drawPath(
        path,
        Brush.linearGradient(
            listOf(metal.light, metal.base, metal.dark),
            start = Offset(center.x - r * 0.4f, center.y - r * 0.4f),
            end = Offset(center.x + r * 0.4f, center.y + r * 0.4f),
        ),
    )
}

private fun DrawScope.drawArcLabel(text: String, metal: Metal, c: Offset, r: Float) {
    val rr = r * 0.6f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textSize = r * 0.15f
        letterSpacing = 0.12f
    }
    val arcLength = (PI * rr).toFloat()
    val maxWidth = arcLength * 0.62f
    val natural = paint.measureText(text)
    if (natural > maxWidth) paint.textSize *= maxWidth / natural
    val hOffset = (arcLength - paint.measureText(text)) / 2f
    val path = android.graphics.Path().apply {
        addArc(c.x - rr, c.y - rr, c.x + rr, c.y + rr, 180f, 180f)
    }
    val d = r * 0.009f
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        paint.color = metal.highlight.toArgb()
        nc.drawTextOnPath(text, path, hOffset - d, -d, paint)
        paint.color = metal.deep.copy(alpha = 0.85f).toArgb()
        nc.drawTextOnPath(text, path, hOffset + d, d, paint)
        paint.color = metal.dark.toArgb()
        nc.drawTextOnPath(text, path, hOffset, 0f, paint)
    }
}

private fun crownPath(cx: Float, cy: Float, s: Float): Path = Path().apply {
    fun p(x: Float, y: Float) = Offset(cx + x * s, cy + y * s)
    val pts = listOf(
        p(-0.8f, 0.45f), p(-0.92f, -0.35f), p(-0.45f, 0.05f), p(0f, -0.55f),
        p(0.45f, 0.05f), p(0.92f, -0.35f), p(0.8f, 0.45f),
    )
    moveTo(pts[0].x, pts[0].y)
    pts.drop(1).forEach { lineTo(it.x, it.y) }
    close()
    addRect(Rect(cx - 0.84f * s, cy + 0.52f * s, cx + 0.84f * s, cy + 0.76f * s))
    addOval(Rect(center = p(-0.92f, -0.47f), radius = 0.11f * s))
    addOval(Rect(center = p(0f, -0.68f), radius = 0.12f * s))
    addOval(Rect(center = p(0.92f, -0.47f), radius = 0.11f * s))
}

private fun wreathPath(cx: Float, cy: Float, s: Float): Path {
    val path = Path()
    val leafLength = 0.34f * s
    val leafWidth = 0.13f * s
    val leaves = 8
    for (mirror in listOf(1f, -1f)) {
        for (i in 0 until leaves) {
            // From the bottom of the wreath, up around the side, toward the top.
            val deg = 108.0 + i * (130.0 / (leaves - 1))
            val a = Math.toRadians(deg)
            val px = cx + mirror * (cos(a) * 0.88 * s).toFloat()
            val py = cy + (sin(a) * 0.88 * s).toFloat()
            val tangent = Math.toRadians(deg + 90.0 + 22.0)
            val phi = if (mirror > 0) tangent else PI - tangent
            addEllipse(path, px, py, leafLength / 2f, leafWidth / 2f, phi)
        }
    }
    path.addPath(starPath(cx, cy - 0.04f * s, 0.42f * s, 0.17f * s))
    return path
}

private fun addEllipse(path: Path, x: Float, y: Float, rx: Float, ry: Float, angle: Double) {
    val steps = 18
    val ca = cos(angle).toFloat()
    val sa = sin(angle).toFloat()
    for (k in 0..steps) {
        val t = (2 * PI * k / steps).toFloat()
        val lx = rx * cos(t)
        val ly = ry * sin(t)
        val px = x + lx * ca - ly * sa
        val py = y + lx * sa + ly * ca
        if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
}

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val a = Math.toRadians(-90.0 + i * 36.0)
        val x = cx + (cos(a) * radius).toFloat()
        val y = cy + (sin(a) * radius).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
