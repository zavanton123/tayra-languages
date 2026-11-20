package com.idiomiko.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppAlpha

/** What the picture on the Next up card shows on its front card. */
internal enum class ArtFace {
    /** Hills under the sun with a tower: the flashcards. */
    LANDSCAPE,

    /** Hills with a flag on the top: somewhere to start. */
    SUMMIT,

    /** The kind's icon over two lines of text: a lesson or a book. */
    PAGE,
}

/**
 * The Next up card's picture: a fan of three cards on a soft wash of [tint], the front one with [face] on it, and a
 * little burst of light at its corner. Drawn, not an image, so it takes the theme's surface and any accent.
 */
@Composable
internal fun NextUpArt(face: ArtFace, tint: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outlineVariant
    val painter = rememberVectorPainter(icon)
    Canvas(modifier.testTag("next-up-art")) {
        val u = size.minDimension / 100f
        val card = Size(52 * u, 64 * u)
        // The wash behind, a rounded square turned a little.
        rotate(-14f, Offset(size.width * 0.44f, size.height * 0.56f)) {
            drawRoundRect(tint.copy(alpha = AppAlpha.soft), Offset(size.width * 0.44f - 40 * u, size.height * 0.56f - 38 * u), Size(80 * u, 76 * u), CornerRadius(18 * u))
        }
        val pivot = Offset(size.width * 0.42f, size.height * 0.5f + card.height / 2)
        val topLeft = Offset(pivot.x - card.width / 2 + 6 * u, pivot.y - card.height)
        val back = lerp(tint, surface, 0.1f)
        val middle = lerp(tint, surface, 0.55f)
        for ((angle, fill) in listOf(-30f to back, -16f to middle)) {
            rotate(angle, pivot) { drawRoundRect(fill, topLeft, card, CornerRadius(7 * u)) }
        }
        rotate(-2f, pivot) {
            val front = Rect(topLeft + Offset(8 * u, 0f), card)
            drawRoundRect(surface, front.topLeft, front.size, CornerRadius(7 * u))
            drawRoundRect(outline, front.topLeft, front.size, CornerRadius(7 * u), style = Stroke(0.8f * u))
            val inner = Rect(front.left + 5 * u, front.top + 5 * u, front.right - 5 * u, front.bottom - 5 * u)
            val shape = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(inner, CornerRadius(4 * u))) }
            clipPath(shape) { face(face, inner, tint, surface, painter, u) }
        }
        // The burst, above the fan's top corner.
        val burst = Offset(pivot.x + card.width * 0.62f, pivot.y - card.height - 4 * u)
        for ((from, to) in listOf(Offset(-6f, -2f) to Offset(-9f, -12f), Offset(-1f, 1f) to Offset(6f, -7f), Offset(2f, 6f) to Offset(12f, 3f))) {
            drawLine(AppAccents.amber, burst + from * u, burst + to * u, 2.2f * u, StrokeCap.Round)
        }
    }
}

private operator fun Offset.times(u: Float) = Offset(x * u, y * u)

private fun DrawScope.face(face: ArtFace, r: Rect, tint: Color, surface: Color, painter: VectorPainter, u: Float) {
    if (face == ArtFace.PAGE) {
        drawRect(tint.copy(alpha = AppAlpha.soft), r.topLeft, r.size)
        val s = r.width * 0.5f
        translate(r.center.x - s / 2, r.top + r.height * 0.18f) {
            with(painter) { draw(Size(s, s), colorFilter = ColorFilter.tint(tint)) }
        }
        for ((i, w) in listOf(0.7f, 0.5f).withIndex()) {
            drawRoundRect(tint.copy(alpha = AppAlpha.disabled), Offset(r.left + r.width * 0.15f, r.top + r.height * (0.72f + i * 0.12f)), Size(r.width * w, 3 * u), CornerRadius(1.5f * u))
        }
        return
    }
    drawRect(lerp(tint, surface, 0.82f), r.topLeft, r.size)
    drawCircle(AppAccents.amber.copy(alpha = AppAlpha.strong), 5 * u, Offset(r.right - 9 * u, r.top + 11 * u))
    val far = Path().apply {
        moveTo(r.left, r.bottom - r.height * 0.32f)
        cubicTo(r.left + r.width * 0.3f, r.bottom - r.height * 0.55f, r.left + r.width * 0.55f, r.bottom - r.height * 0.3f, r.right, r.bottom - r.height * 0.45f)
        lineTo(r.right, r.bottom); lineTo(r.left, r.bottom); close()
    }
    drawPath(far, lerp(tint, surface, 0.45f))
    val peak = Offset(r.left + r.width * 0.42f, r.bottom - r.height * 0.42f)
    val near = Path().apply {
        moveTo(r.left, r.bottom - r.height * 0.12f)
        quadraticTo(peak.x - r.width * 0.2f, peak.y, peak.x, peak.y)
        quadraticTo(peak.x + r.width * 0.25f, peak.y, r.right, r.bottom - r.height * 0.18f)
        lineTo(r.right, r.bottom); lineTo(r.left, r.bottom); close()
    }
    drawPath(near, tint)
    if (face == ArtFace.LANDSCAPE) {
        // A tower on the hill.
        val w = 4 * u
        drawRect(tint, Offset(peak.x - w / 2, peak.y - 14 * u), Size(w, 14 * u))
        drawRect(tint, Offset(peak.x - w, peak.y - 16 * u), Size(w * 2, 3 * u))
        drawRect(tint, Offset(peak.x - 8 * u, peak.y - 11 * u), Size(16 * u, 2.5f * u))
    } else {
        drawLine(lerp(tint, AppAccents.ink, 0.3f), peak, peak + Offset(0f, -16 * u), 1.5f * u)
        val flag = Path().apply {
            moveTo(peak.x, peak.y - 16 * u); lineTo(peak.x + 10 * u, peak.y - 12.5f * u); lineTo(peak.x, peak.y - 9 * u); close()
        }
        drawPath(flag, AppAccents.orange)
    }
}
