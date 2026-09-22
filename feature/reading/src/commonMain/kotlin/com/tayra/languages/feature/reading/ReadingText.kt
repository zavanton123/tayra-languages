package com.tayra.languages.feature.reading

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.render.RenderedPage
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.ui.theme.AppTheme

/** Callbacks from the text to the screen. Item indexes refer to [RenderedPage.items]. */
class ReadingTextCallbacks(
    val onClick: (itemIndex: Int, shift: Boolean) -> Unit,
    val onTap: (itemIndex: Int) -> Unit,
    val onLongPress: (itemIndex: Int) -> Unit,
    val onHover: (itemIndex: Int?) -> Unit,
    val onDragStart: (itemIndex: Int) -> Unit,
    val onDrag: (itemIndex: Int) -> Unit,
    val onDragEnd: (itemIndex: Int, shift: Boolean) -> Unit,
    val popupContent: @Composable (itemIndex: Int, anchorBottom: androidx.compose.ui.unit.IntOffset) -> Unit,
)

/** Where each item was placed within a paragraph's annotated string. */
private class Span(val start: Int, val end: Int, val itemIndex: Int)

/**
 * Renders a page as selectable, colour-coded text. Each paragraph is one [Text] so that
 * wrapping and right-to-left layout come for free; token positions are resolved from
 * the text layout when the user interacts.
 */
@Composable
fun ReadingText(
    page: RenderedPage,
    theme: AppTheme,
    showHighlights: Boolean,
    marked: Set<Int>,
    hovered: Int?,
    selection: IntRange?,
    popupItem: Int?,
    fontScale: Float,
    lineHeight: Float,
    rightToLeft: Boolean,
    callbacks: ReadingTextCallbacks,
    modifier: Modifier = Modifier,
) {
    var itemOffset = 0
    Column(modifier) {
        page.paragraphs.forEach { paragraph ->
            val paragraphItems = paragraph.sentences.flatMap { it.items }
            val first = itemOffset
            itemOffset += paragraphItems.size
            ParagraphText(
                items = paragraphItems,
                firstItemIndex = first,
                theme = theme,
                showHighlights = showHighlights,
                marked = marked,
                hovered = hovered,
                selection = selection,
                popupItem = popupItem,
                fontScale = fontScale,
                lineHeight = lineHeight,
                rightToLeft = rightToLeft,
                callbacks = callbacks,
            )
        }
    }
}

@Composable
private fun ParagraphText(
    items: List<TextItem>,
    firstItemIndex: Int,
    theme: AppTheme,
    showHighlights: Boolean,
    marked: Set<Int>,
    hovered: Int?,
    selection: IntRange?,
    popupItem: Int?,
    fontScale: Float,
    lineHeight: Float,
    rightToLeft: Boolean,
    callbacks: ReadingTextCallbacks,
) {
    val spans = remember(items) { mutableListOf<Span>() }
    val text = remember(items, theme, showHighlights, marked, hovered, selection, firstItemIndex) {
        spans.clear()
        buildParagraph(items, firstItemIndex, theme, showHighlights, marked, hovered, selection, spans)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    fun itemAt(position: Offset): Int? {
        val result = layout ?: return null
        if (position.y < 0 || position.y > result.size.height) return null
        val offset = result.getOffsetForPosition(position)
        val line = result.getLineForOffset(offset)
        if (position.x < result.getLineLeft(line) - 4 || position.x > result.getLineRight(line) + 4) return null
        val span = spans.firstOrNull { offset >= it.start && offset < it.end } ?: spans.lastOrNull { offset == it.end } ?: return null
        return span.itemIndex
    }

    Box(Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = (18 * fontScale).sp,
                lineHeight = (18 * fontScale * lineHeight).sp,
                color = theme.readingText,
                textDirection = if (rightToLeft) TextDirection.Rtl else TextDirection.Ltr,
                fontFamily = FontFamily.Serif,
            ),
            onTextLayout = { layout = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .pointerInput(callbacks) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            when (event.type) {
                                PointerEventType.Move -> if (event.changes.firstOrNull()?.type == PointerType.Mouse) {
                                    callbacks.onHover(itemAt(event.changes.first().position))
                                }
                                PointerEventType.Exit -> callbacks.onHover(null)
                            }
                        }
                    }
                }
                .pointerInput(callbacks) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startItem = itemAt(down.position)
                        val isMouse = down.type == PointerType.Mouse
                        if (isMouse) {
                            val shift = currentEvent.keyboardModifiers.isShiftPressed
                            if (startItem == null) return@awaitEachGesture
                            var dragged = false
                            var lastItem = startItem
                            val finished = drag(down.id) { change ->
                                val item = itemAt(change.position)
                                if (item != null && item != lastItem) {
                                    if (!dragged) {
                                        dragged = true
                                        callbacks.onDragStart(startItem)
                                    }
                                    lastItem = item
                                    callbacks.onDrag(item)
                                }
                                change.consume()
                            }
                            if (dragged) {
                                callbacks.onDragEnd(lastItem, finished && currentEvent.keyboardModifiers.isShiftPressed || shift)
                            } else {
                                callbacks.onClick(startItem, shift)
                            }
                        } else {
                            // Touch: a release before the long-press timeout is a tap; holding is a long press;
                            // a cancelled gesture (the parent scrolled) is ignored.
                            var timedOut = false
                            val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() }
                                ?: run { timedOut = currentEvent.changes.any { it.pressed }; null }
                            when {
                                up != null && startItem != null -> callbacks.onTap(startItem)
                                timedOut && startItem != null -> callbacks.onLongPress(startItem)
                            }
                        }
                    }
                },
        )
        if (popupItem != null) {
            val span = spans.firstOrNull { it.itemIndex == popupItem }
            val result = layout
            if (span != null && result != null) {
                val box = result.getBoundingBox(span.start)
                callbacks.popupContent(popupItem, androidx.compose.ui.unit.IntOffset(box.left.toInt(), (box.bottom + 8).toInt()))
            }
        }
    }
    if (items.isEmpty()) Text(" ", style = MaterialTheme.typography.bodyLarge)
}

private fun buildParagraph(
    items: List<TextItem>,
    firstItemIndex: Int,
    theme: AppTheme,
    showHighlights: Boolean,
    marked: Set<Int>,
    hovered: Int?,
    selection: IntRange?,
    spans: MutableList<Span>,
): AnnotatedString = buildAnnotatedString {
    val colors = theme.statusColors
    items.forEachIndexed { i, item ->
        val itemIndex = firstItemIndex + i
        val start = length
        val inSelection = selection != null && item.index in selection
        val isMarked = itemIndex in marked
        val isHovered = itemIndex == hovered
        var style = SpanStyle()
        if (item.isWord) {
            val status = item.status
            val highlight = showHighlights || isHovered || isMarked
            if (highlight && status != TermStatus.WELL_KNOWN && status != TermStatus.IGNORED) {
                val background = colors.background(status)
                if (status == TermStatus.UNKNOWN && colors.unknownAsText) {
                    style = style.copy(color = background)
                } else if (background != Color.Transparent) {
                    style = style.copy(background = background, color = if (colors.onHighlight != Color.Unspecified) colors.onHighlight else Color.Unspecified)
                }
            }
            if (isHovered || isMarked) style = style.copy(textDecoration = TextDecoration.Underline)
            if (isMarked) style = style.copy(background = theme.markedUnderline.copy(alpha = 0.35f))
        }
        if (inSelection) style = style.copy(background = theme.selectionBackground)
        withStyle(style) {
            if (item.isOverlapped) append("⁺")
            append(item.renderText)
        }
        spans.add(Span(start, length, itemIndex))
    }
}
