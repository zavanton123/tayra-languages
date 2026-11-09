package com.tayra.languages.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tayra.languages.core.ui.theme.AppAccents
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Fireworks of confetti over the whole window: bursts one after another, each flying out all round,
 * slowing, drifting down and fading, a few seconds in all, over everything, a dialog too. Opened from inside a dialog, it stays above it on every platform.
 */
@Composable
fun Confetti() {
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) { time.animateTo(1f, tween(CONFETTI_MILLIS, easing = LinearEasing)) }
    if (time.value >= 1f) return
    Popup(popupPositionProvider = WindowOrigin, properties = PopupProperties(focusable = false)) {
        ConfettiPieces(time.value, Modifier.fillMaxSize())
    }
}

/** Places a popup at the window's corner, whatever opened it. */
private object WindowOrigin : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) = IntOffset.Zero
}

@Composable
private fun ConfettiPieces(progress: Float, modifier: Modifier) {
    val bursts = remember { List(BURSTS) { Burst(it, Random.Default) } }
    val palette = AppAccents.sequence
    Canvas(modifier.testTag("confetti")) {
        val now = progress * CONFETTI_MILLIS / 1000f
        // Distances go by the window's shorter side, so a burst is round on any window.
        val reach = minOf(size.width, size.height)
        val unit = PIECE_LENGTH.toPx()
        bursts.forEach { burst ->
            val t = now - burst.delay
            if (t <= 0f || t >= LIFE) return@forEach
            val centre = Offset(size.width * burst.x, size.height * burst.y)
            // The first moment is a bright flash where the burst opens.
            if (t < FLASH) drawCircle(palette[burst.colors.first() % palette.size], radius = reach * FLASH_REACH * (t / FLASH), center = centre, alpha = 1f - t / FLASH)
            // Air slows the pieces fast, then they drift down and fade.
            val travelled = (1f - exp(-DRAG * t)) / DRAG
            val alpha = ((LIFE - t) / (LIFE * FADE)).coerceIn(0f, 1f)
            burst.pieces.forEach { piece ->
                val x = centre.x + piece.dx * reach * travelled
                val y = centre.y + piece.dy * reach * travelled + reach * GRAVITY * t * t / 2
                val w = unit * piece.scale
                val h = w * piece.ratio
                val color = palette[burst.colors[piece.color] % palette.size]
                rotate(piece.angle + piece.spin * t, pivot = Offset(x, y)) {
                    drawRect(color, topLeft = Offset(x - w / 2, y - h / 2), size = Size(w, h), alpha = alpha)
                }
            }
        }
    }
}

/** One firework: where and when it bursts, its two colours, and its pieces flying out all round. */
private class Burst(index: Int, random: Random) {
    val x = 0.15f + random.nextFloat() * 0.7f
    val y = 0.12f + random.nextFloat() * 0.38f
    // One after another, the first at once.
    val delay = index * BURST_GAP + random.nextFloat() * BURST_GAP / 2
    val colors = List(2) { random.nextInt(0, 64) }
    val pieces = List(PIECES_PER_BURST) { Piece(random) }
}

/** A piece of a burst: the way it flies, as a share of the window each second, and how it looks. */
private class Piece(random: Random) {
    private val heading = random.nextFloat() * 2 * PI.toFloat()
    // Most pieces fly near the edge of the burst, which keeps its ring.
    private val speed = SPEED * (0.55f + 0.45f * sqrt(random.nextFloat()))
    val dx = cos(heading) * speed
    val dy = sin(heading) * speed
    val angle = random.nextFloat() * 360f
    val spin = (random.nextFloat() - 0.5f) * 900f
    val scale = 0.5f + random.nextFloat() * 0.7f
    val ratio = 0.4f + random.nextFloat() * 0.3f
    val color = random.nextInt(0, 2)
}

private const val BURSTS = 7
private const val PIECES_PER_BURST = 80
private const val BURST_GAP = 0.12f
private const val LIFE = 2.6f
private const val FLASH = 0.18f
private const val FLASH_REACH = 0.035f
private const val SPEED = 1.2f
private const val DRAG = 3.2f
private const val GRAVITY = 0.12f
private const val FADE = 0.45f
private const val CONFETTI_MILLIS = 3_500
private val PIECE_LENGTH = 14.dp
