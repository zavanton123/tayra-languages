package com.tayra.languages.feature.reading

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import com.tayra.languages.core.ui.components.AppIcons
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.Placeholder
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import com.tayra.languages.core.domain.service.SentenceTranslation
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
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
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
    /** Right mouse button on a word. */
    val onSecondaryClick: (itemIndex: Int) -> Unit = {},
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
    /** Lay each sentence out on its own line instead of flowing a paragraph together. */
    splitSentences: Boolean = false,
    /** Translations shown under each sentence, keyed by [RenderedSentence.displayText]; null hides them. */
    translations: Map<String, SentenceTranslation>? = null,
    /** With translations, put each sentence in a left column and its translation in a right one. */
    sideBySide: Boolean = false,
    /** Reads a sentence aloud; a play button precedes every sentence when set. */
    onSpeakSentence: ((String) -> Unit)? = null,
    /** The sentence being read aloud, whose button shows Stop. */
    playingSentence: String? = null,
) {
    var itemOffset = 0
    val perSentence = splitSentences || translations != null
    val twoColumns = translations != null && sideBySide
    Column(modifier) {
        page.paragraphs.forEach { paragraph ->
            // A run of items that shares one Text: the whole paragraph, or one sentence each.
            val runs = if (perSentence) paragraph.sentences.map { it.items to it.displayText } else listOf(paragraph.sentences.flatMap { it.items } to "")
            // In a flowing paragraph the play buttons sit inline, before the first item of each sentence.
            val inlinePlay: Map<Int, String> = if (perSentence || onSpeakSentence == null) emptyMap() else buildMap {
                var position = 0
                paragraph.sentences.forEach { sentence ->
                    if (sentence.displayText.any { it.isLetter() }) {
                        val lead = sentence.items.indexOfFirst { !it.isParagraphMark && it.renderText.isNotBlank() }
                        if (lead >= 0) put(position + lead, sentence.displayText)
                    }
                    position += sentence.items.size
                }
            }
            runs.forEach { (runItems, sentenceText) ->
                val first = itemOffset
                itemOffset += runItems.size
                val speakable = perSentence && onSpeakSentence != null && sentenceText.any { it.isLetter() }
                val sentence: @Composable () -> Unit = {
                    ParagraphText(
                        items = runItems,
                        inlinePlay = inlinePlay,
                        onSpeakSentence = onSpeakSentence,
                        playingSentence = playingSentence,
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
                val translated = translations != null && sentenceText.any { it.isLetter() }
                if (twoColumns) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.Top) {
                        if (onSpeakSentence != null) PlayButton(sentenceText.takeIf { speakable }, sentenceText == playingSentence, fontScale, lineHeight, onSpeakSentence)
                        Box(Modifier.weight(1f)) { sentence() }
                        Box(Modifier.width(24.dp))
                        Box(Modifier.weight(1f)) {
                            if (translated) TranslationLine(translations[sentenceText], theme, fontScale, lineHeight, large = true)
                        }
                    }
                } else if (perSentence && onSpeakSentence != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        PlayButton(sentenceText.takeIf { speakable }, sentenceText == playingSentence, fontScale, lineHeight, onSpeakSentence)
                        Column(Modifier.weight(1f)) {
                            sentence()
                            if (translated) TranslationLine(translations[sentenceText], theme, fontScale, lineHeight)
                        }
                    }
                } else {
                    sentence()
                    if (translated) TranslationLine(translations[sentenceText], theme, fontScale, lineHeight)
                }
            }
        }
    }
}

/** The button before a sentence; an empty slot of the same width keeps sentences without words aligned. */
@Composable
private fun PlayButton(text: String?, playing: Boolean, fontScale: Float, lineHeight: Float, onSpeak: (String) -> Unit) {
    val size = (22 * fontScale).dp
    // Centred on the first line of the sentence, whatever the font size and line height.
    val top = 6.dp + ((18 * fontScale * lineHeight) - 22 * fontScale).coerceAtLeast(0f).dp / 2
    Box(Modifier.padding(top = top, end = 8.dp).size(size), contentAlignment = Alignment.Center) {
        if (text != null) {
            Icon(
                if (playing) AppIcons.Stop else AppIcons.PlayArrow,
                contentDescription = if (playing) "Stop" else "Play sentence",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { onSpeak(text) },
            )
        }
    }
}

/** The translation under a sentence: a spinner while it loads, nothing when the service has none. */
@Composable
private fun TranslationLine(translation: SentenceTranslation?, theme: AppTheme, fontScale: Float, lineHeight: Float, large: Boolean = false) {
    val color = theme.readingText.copy(alpha = if (large) 0.7f else 0.55f)
    // In the side-by-side layout the translation mirrors the original's size and line height so the rows line up.
    val size = if (large) 18 * fontScale else 14 * fontScale
    val spacing = if (large) lineHeight else 1.4f
    when (translation) {
        is SentenceTranslation.Done -> Text(
            translation.text,
            style = TextStyle(fontSize = size.sp, lineHeight = (size * spacing).sp, color = color),
            modifier = Modifier.fillMaxWidth().padding(start = if (large) 0.dp else 12.dp, bottom = if (large) 0.dp else 10.dp),
        )
        SentenceTranslation.Loading, null -> Row(
            Modifier.padding(start = 12.dp, top = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = color)
            Text("Translating\u2026", style = TextStyle(fontSize = (13 * fontScale).sp, color = color))
        }
        SentenceTranslation.Unavailable -> Spacer(Modifier.height(6.dp))
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
    /** Local item positions that start a sentence, with the sentence to read, for inline play buttons. */
    inlinePlay: Map<Int, String> = emptyMap(),
    onSpeakSentence: ((String) -> Unit)? = null,
    playingSentence: String? = null,
) {
    val spans = remember(items) { mutableListOf<Span>() }
    val text = remember(items, theme, showHighlights, marked, hovered, selection, firstItemIndex, inlinePlay.keys) {
        spans.clear()
        buildParagraph(items, firstItemIndex, theme, showHighlights, marked, hovered, selection, spans, inlinePlay.keys)
    }
    val primary = MaterialTheme.colorScheme.primary
    val inlineContent = remember(inlinePlay, onSpeakSentence, primary, playingSentence) {
        if (onSpeakSentence == null) emptyMap() else inlinePlay.entries.associate { (position, sentence) ->
            val playing = sentence == playingSentence
            "play-$position" to InlineTextContent(Placeholder(1.25.em, 1.em, PlaceholderVerticalAlign.TextCenter)) {
                Icon(
                    if (playing) AppIcons.Stop else AppIcons.PlayArrow,
                    contentDescription = if (playing) "Stop" else "Play sentence",
                    tint = primary,
                    modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { onSpeakSentence(sentence) },
                )
            }
        }
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
            inlineContent = inlineContent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .pointerInput(callbacks) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> if (event.changes.firstOrNull()?.type == PointerType.Mouse) {
                                    callbacks.onHover(itemAt(event.changes.first().position))
                                }
                                PointerEventType.Exit -> callbacks.onHover(null)
                            }
                        }
                    }
                }
                .pointerInput(callbacks) {
                    awaitEachGesture {
                        // A secondary mouse button never sets `pressed`, so wait for the raw press event
                        // rather than a "down" change; primary presses and touches arrive the same way.
                        var press = awaitPointerEvent()
                        while (press.type != PointerEventType.Press) press = awaitPointerEvent()
                        val down = press.changes.first()
                        // A press an inline play button already took is not a press on the text.
                        if (down.isConsumed) return@awaitEachGesture
                        val startItem = itemAt(down.position)
                        val isMouse = down.type == PointerType.Mouse
                        if (isMouse) {
                            val shift = press.keyboardModifiers.isShiftPressed
                            val secondary = press.buttons.isSecondaryPressed || !press.buttons.isPrimaryPressed
                            if (startItem == null) return@awaitEachGesture
                            if (secondary) {
                                var release = awaitPointerEvent()
                                while (release.type != PointerEventType.Release) release = awaitPointerEvent()
                                if (itemAt(release.changes.first().position) == startItem) callbacks.onSecondaryClick(startItem)
                                return@awaitEachGesture
                            }
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
    playBefore: Set<Int> = emptySet(),
): AnnotatedString = buildAnnotatedString {
    val colors = theme.statusColors
    items.forEachIndexed { i, item ->
        val itemIndex = firstItemIndex + i
        if (i in playBefore) appendInlineContent("play-$i", "\u25B6")
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
