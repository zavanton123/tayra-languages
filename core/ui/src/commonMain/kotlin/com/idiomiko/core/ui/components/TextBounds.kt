package com.idiomiko.core.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign

/**
 * The box around the text from [start] to [end] (exclusive), where it is drawn.
 *
 * Android draws a justified line stretched to the full width but reports its characters' positions
 * as if the line were not stretched, so boxes fall further behind their words along the line. When a
 * justified line's characters span less than the width, the missing width is shared out over the
 * line's spaces, as Android shares it out when it draws, and the box moves and widens with it.
 */
fun TextLayoutResult.textBounds(start: Int, end: Int): Rect {
    val last = getLineForOffset(maxOf(start, end - 1))
    val pieces = (getLineForOffset(start)..last).mapNotNull { line ->
        val from = maxOf(start, getLineStart(line))
        val to = minOf(end, getLineEnd(line))
        if (to > from) lineBounds(line, from, to) else null
    }
    return pieces.reduceOrNull { a, b -> Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom)) }
        ?: getPathForRange(start, end).getBounds()
}

private fun TextLayoutResult.lineBounds(line: Int, from: Int, to: Int): Rect {
    val box = getPathForRange(from, to).getBounds()
    val perSpace = justifiedSpaceWidth(line) ?: return box
    val text = layoutInput.text.text
    val before = text.spaces(getLineStart(line), from) * perSpace
    val within = text.spaces(from, to) * perSpace
    return if (getParagraphDirection(from) == ResolvedTextDirection.Rtl) {
        Rect(box.left - before - within, box.top, box.right - before, box.bottom)
    } else {
        Rect(box.left + before, box.top, box.right + before + within, box.bottom)
    }
}

/**
 * How much wider each space of [line] is drawn than its characters' positions say, or null when the
 * positions are where the line is drawn: the line is not justified (or ends its paragraph), or the
 * layout already reports its characters spread out.
 */
private fun TextLayoutResult.justifiedSpaceWidth(line: Int): Float? {
    if (layoutInput.style.textAlign != TextAlign.Justify || line >= lineCount - 1) return null
    val text = layoutInput.text.text
    val start = getLineStart(line)
    val end = getLineEnd(line)
    if (end > 0 && text[end - 1] == '\n') return null
    val visibleEnd = getLineEnd(line, visibleEnd = true)
    val spaces = text.spaces(start, visibleEnd)
    if (visibleEnd <= start || spaces == 0) return null
    val missing = size.width - getPathForRange(start, visibleEnd).getBounds().width
    return if (missing > MISSING_WIDTH_TOLERANCE) missing / spaces else null
}

/** Only plain spaces are stretched when a line is justified. */
private fun String.spaces(from: Int, to: Int): Int = (from until minOf(to, length)).count { this[it] == ' ' }

/** Less missing width than this, in pixels, is rounding, not an unstretched line. */
private const val MISSING_WIDTH_TOLERANCE = 1f
