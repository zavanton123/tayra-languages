package com.tayra.languages.core.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.isSpecified

/** A background behind the characters [start] until [end] of a laid-out text. */
class TextBackground(val start: Int, val end: Int, val color: Color)

/**
 * Moves the span backgrounds out of the text, since those can only be square, so that
 * [drawTextBackgrounds] can draw them rounded instead.
 */
fun AnnotatedString.splitBackgrounds(): Pair<AnnotatedString, List<TextBackground>> {
    val backgrounds = spanStyles
        .filter { it.item.background.isSpecified && it.item.background != Color.Transparent && it.end > it.start }
        .map { TextBackground(it.start, it.end, it.item.background) }
    if (backgrounds.isEmpty()) return this to emptyList()
    val plain = AnnotatedString.Builder(text.length).apply {
        append(text)
        for (span in spanStyles) addStyle(span.item.copy(background = Color.Unspecified), span.start, span.end)
        for (paragraph in paragraphStyles) addStyle(paragraph.item, paragraph.start, paragraph.end)
    }.toAnnotatedString()
    return plain to backgrounds
}

/**
 * Draws [backgrounds] as rounded boxes around the glyphs, one per line a range covers, in the
 * order given. Neighbouring ranges of the same colour, such as the words and spaces of a
 * selection, share one box.
 */
fun DrawScope.drawTextBackgrounds(backgrounds: List<TextBackground>, layout: TextLayoutResult) {
    val fontSize = layout.layoutInput.style.fontSize.takeIf { it.isSpecified }?.toPx() ?: return
    val radius = CornerRadius(fontSize * CORNER_RADIUS_EM)
    val padding = fontSize * PADDING_EM
    val length = layout.layoutInput.text.length
    var i = 0
    while (i < backgrounds.size) {
        val color = backgrounds[i].color
        var last = i
        while (last + 1 < backgrounds.size && backgrounds[last + 1].color == color && backgrounds[last + 1].start == backgrounds[last].end) last++
        val from = backgrounds[i].start.coerceIn(0, length)
        val to = backgrounds[last].end.coerceIn(0, length)
        if (color != Color.Transparent && to > from) {
            for (line in layout.getLineForOffset(from)..layout.getLineForOffset(to - 1)) {
                val start = maxOf(from, layout.getLineStart(line))
                val stop = minOf(to, layout.getLineEnd(line, visibleEnd = true))
                if (stop <= start) continue
                val first = layout.getBoundingBox(start)
                val end = layout.getBoundingBox(stop - 1)
                val left = minOf(first.left, end.left)
                val right = maxOf(first.right, end.right)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left - padding, layout.getLineBaseline(line) - fontSize * ASCENT_EM),
                    size = Size(right - left + padding * 2, fontSize * (ASCENT_EM + DESCENT_EM)),
                    cornerRadius = radius,
                )
            }
        }
        i = last + 1
    }
}

/** The box around a word, in multiples of the font size: from above the capitals to below the descenders. */
private const val ASCENT_EM = 0.98f
private const val DESCENT_EM = 0.32f
private const val PADDING_EM = 0.04f
private const val CORNER_RADIUS_EM = 0.24f
