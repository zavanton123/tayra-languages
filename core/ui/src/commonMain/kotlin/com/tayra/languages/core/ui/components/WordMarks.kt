package com.tayra.languages.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/*
 * Words drawn on a colour of their own, as the reader colours them by status: a rounded mark around
 * the letters rather than a span background, which fills the line's full height and runs into the
 * lines above and below.
 */

/** Marks the text it covers to be drawn on a colour; the value is the colour, as ARGB. */
const val FILL_ANNOTATION = "fill"

/** Marks the text from [start] to [end] (exclusive) to be drawn on [color]. */
fun AnnotatedString.Builder.addFill(color: Color, start: Int, end: Int) = addStringAnnotation(FILL_ANNOTATION, color.toArgb().toString(), start, end)

/** Marks the text appended until the matching pop() to be drawn on [color]. */
fun AnnotatedString.Builder.pushFill(color: Color) = pushStringAnnotation(FILL_ANNOTATION, color.toArgb().toString())

/**
 * Draws the marks of [text]'s filled words behind it, from [layout], read as it is drawn; a layout
 * of older text is skipped. [fontSize] is the text's, which the marks are sized by.
 */
fun Modifier.wordMarks(layout: () -> TextLayoutResult?, text: AnnotatedString, fontSize: TextUnit): Modifier = drawWithCache {
    val result = layout()
    val marks = if (result != null && result.layoutInput.text == text) result.marks(fontSize.toPx(), MARK_SIDE.toPx()) else emptyList()
    val corner = CornerRadius(MARK_CORNER.toPx())
    onDrawBehind { marks.forEach { (rect, color) -> drawRoundRect(color, rect.topLeft, rect.size, corner) } }
}

/** [text] with its filled words on rounded marks, as the reader draws them; for previews of reading and sentences on cards. */
@Composable
fun MarkedText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(text, modifier.wordMarks({ layout }, text, style.fontSize), style = style, maxLines = maxLines, overflow = overflow, onTextLayout = { layout = it })
}

/** Where each filled word's mark goes, a piece for each line it is on, and its colour. */
private fun TextLayoutResult.marks(fontPx: Float, side: Float): List<Pair<Rect, Color>> {
    val text = layoutInput.text
    return text.getStringAnnotations(FILL_ANNOTATION, 0, text.length).flatMap { fill ->
        val color = Color(fill.item.toInt())
        if (fill.end <= fill.start) return@flatMap emptyList()
        (getLineForOffset(fill.start)..getLineForOffset(fill.end - 1)).mapNotNull { line ->
            val from = maxOf(fill.start, getLineStart(line))
            val to = minOf(fill.end, getLineEnd(line))
            if (to <= from) return@mapNotNull null
            val box = getPathForRange(from, to).getBounds()
            val baseline = getLineBaseline(line)
            val top = maxOf(box.top, baseline - fontPx * MARK_ABOVE)
            val bottom = minOf(box.bottom, baseline + fontPx * MARK_BELOW)
            Rect(box.left - side, top, box.right + side, bottom) to color
        }
    }
}

/** How far a mark reaches above its baseline and below it, in ems: round its letters, short of the line's full height, so lines stay apart. */
private const val MARK_ABOVE = 0.98f
private const val MARK_BELOW = 0.32f
private val MARK_CORNER = 4.dp

/** How far a mark reaches past its first and last letters, short of the space beside it. */
private val MARK_SIDE = 2.dp
