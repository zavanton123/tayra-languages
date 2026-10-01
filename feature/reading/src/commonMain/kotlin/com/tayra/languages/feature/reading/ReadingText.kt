package com.tayra.languages.feature.reading

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import com.tayra.languages.core.ui.components.AppIcons
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithContent
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
    /** Draws the popup for the item; [word] is the item's rectangle in the paragraph's coordinates. */
    val popupContent: @Composable (itemIndex: Int, word: androidx.compose.ui.unit.IntRect) -> Unit,
)

/** Where each item was placed within a paragraph's annotated string. */
/** [underlined] words are selected; their underline is drawn separately, as text decorations are too thin. */
private class Span(val start: Int, val end: Int, val itemIndex: Int, val underlined: Boolean = false)

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
                        // A sentence on its own line starts at its first word, not at the space that follows the previous one.
                        trimLeadingSpace = perSentence,
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
                // The button is centred on its sentence's text, so a translation under it stays out of the row.
                val withButton: @Composable (Modifier) -> Unit = { rowModifier ->
                    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
                        PlayButton(sentenceText.takeIf { speakable }, sentenceText == playingSentence, fontScale, onSpeakSentence!!)
                        Box(Modifier.weight(1f)) { sentence() }
                    }
                }
                if (twoColumns) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.Top) {
                        if (onSpeakSentence != null) withButton(Modifier.weight(1f)) else Box(Modifier.weight(1f)) { sentence() }
                        Box(Modifier.width(24.dp))
                        Box(Modifier.weight(1f)) {
                            if (translated) TranslationLine(translations[sentenceText], theme, fontScale, lineHeight, large = true)
                        }
                    }
                } else if (perSentence && onSpeakSentence != null) {
                    withButton(Modifier.fillMaxWidth())
                    if (translated) Box(Modifier.padding(start = playGutter(fontScale))) { TranslationLine(translations[sentenceText], theme, fontScale, lineHeight) }
                } else {
                    sentence()
                    if (translated) TranslationLine(translations[sentenceText], theme, fontScale, lineHeight)
                }
            }
        }
    }
}

/**
 * The button before a sentence, in a gutter with room before the text; an empty slot of the
 * same width keeps sentences without words aligned.
 */
@Composable
private fun PlayButton(text: String?, playing: Boolean, fontScale: Float, onSpeak: (String) -> Unit) {
    Box(Modifier.padding(start = PLAY_GUTTER_START, end = PLAY_GUTTER_END).size((PLAY_BUTTON_SIZE * fontScale).dp)) {
        if (text != null) SpeakerCircle(playing, Modifier.fillMaxSize()) { onSpeak(text) }
    }
}

/** How far the text of a sentence with a play button starts from the edge. */
private fun playGutter(fontScale: Float) = (PLAY_BUTTON_SIZE * fontScale).dp + PLAY_GUTTER_START + PLAY_GUTTER_END

private const val PLAY_BUTTON_SIZE = 32
private val PLAY_GUTTER_START = 8.dp

// The reading column's own margin already sits before the button, so the text needs more room after it to look even.
private val PLAY_GUTTER_END = 20.dp

/** The speaker icon in a soft circle; a stop square while the sentence is being read. */
@Composable
private fun SpeakerCircle(playing: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier.clip(CircleShape).background(primary.copy(alpha = 0.12f)).clickable(onClick = onClick)
            .semantics { contentDescription = if (playing) "Stop" else "Play sentence" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (playing) AppIcons.Stop else AppIcons.VolumeUp, contentDescription = null, tint = primary, modifier = Modifier.fillMaxSize(0.58f))
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
    trimLeadingSpace: Boolean = false,
) {
    val spans = remember(items) { mutableListOf<Span>() }
    val text = remember(items, theme, showHighlights, marked, hovered, selection, firstItemIndex, inlinePlay.keys, trimLeadingSpace) {
        spans.clear()
        buildParagraph(items, firstItemIndex, theme, showHighlights, marked, hovered, selection, spans, inlinePlay.keys, trimLeadingSpace)
    }
    val primary = MaterialTheme.colorScheme.primary
    val inlineContent = remember(inlinePlay, onSpeakSentence, primary, playingSentence) {
        if (onSpeakSentence == null) emptyMap() else inlinePlay.entries.associate { (position, sentence) ->
            val playing = sentence == playingSentence
            // The circle fills the placeholder's height; the extra width is the gap before the sentence.
            "play-$position" to InlineTextContent(Placeholder(1.85.em, 1.35.em, PlaceholderVerticalAlign.TextCenter)) {
                Box(Modifier.fillMaxSize()) {
                    SpeakerCircle(playing, Modifier.fillMaxHeight().aspectRatio(1f).align(Alignment.CenterStart)) { onSpeakSentence(sentence) }
                }
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
                .drawWithContent {
                    drawContent()
                    val result = layout ?: return@drawWithContent
                    // A layout from before the text changed would put the underlines in the wrong places.
                    if (result.layoutInput.text == text) drawSelectionUnderlines(spans, result, theme.readingText)
                }
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
                val bounds = result.getPathForRange(span.start, span.end).getBounds()
                // The text sits below the paragraph's top padding, so its rectangle moves down by it.
                val top = with(LocalDensity.current) { 6.dp.toPx() }
                callbacks.popupContent(
                    popupItem,
                    androidx.compose.ui.unit.IntRect(bounds.left.roundToInt(), (bounds.top + top).roundToInt(), bounds.right.roundToInt(), (bounds.bottom + top).roundToInt()),
                )
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
    trimLeadingSpace: Boolean = false,
): AnnotatedString = buildAnnotatedString {
    val colors = theme.statusColors
    // Leading blank items still get (empty) spans, so item positions stay as they are.
    val firstShown = if (trimLeadingSpace) items.indexOfFirst { it.renderText.isNotBlank() }.coerceAtLeast(0) else 0
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
            // The word under the mouse turns plain gray, whatever its status colour, so it reads as "pointed at".
            if (isHovered && !isMarked) style = SpanStyle(background = theme.readingText.copy(alpha = HOVER_ALPHA))
        }
        // Selected words and phrases, including one still being dragged over, are underlined and keep their colours.
        val underlined = (isMarked && item.isWord) || inSelection
        withStyle(style) {
            if (underlined) pushStringAnnotation(SELECTED_ANNOTATION, "")
            if (item.isOverlapped) append("⁺")
            if (i >= firstShown) append(item.renderText)
            if (underlined) pop()
        }
        spans.add(Span(start, length, itemIndex, underlined))
    }
}

/** Marks the selected characters in the text, for tests and accessibility tools. */
internal const val SELECTED_ANNOTATION = "selected"

/**
 * Underlines the selected words, one line per text line they cover. Neighbouring underlined
 * spans, such as the words and spaces of a selected phrase, share one line.
 */
private fun DrawScope.drawSelectionUnderlines(spans: List<Span>, layout: TextLayoutResult, color: Color) {
    val fontSize = layout.layoutInput.style.fontSize.toPx()
    val thickness = UNDERLINE_THICKNESS.toPx()
    var i = 0
    while (i < spans.size) {
        if (!spans[i].underlined) {
            i++
            continue
        }
        var last = i
        while (last + 1 < spans.size && spans[last + 1].underlined && spans[last + 1].start == spans[last].end) last++
        val from = spans[i].start
        val to = spans[last].end
        if (to > from) {
            for (line in layout.getLineForOffset(from)..layout.getLineForOffset(to - 1)) {
                val start = maxOf(from, layout.getLineStart(line))
                val stop = minOf(to, layout.getLineEnd(line, visibleEnd = true))
                if (stop <= start) continue
                val first = layout.getBoundingBox(start)
                val end = layout.getBoundingBox(stop - 1)
                val left = minOf(first.left, end.left)
                val right = maxOf(first.right, end.right)
                drawRect(color, Offset(left, layout.getLineBaseline(line) + fontSize * UNDERLINE_OFFSET_EM), Size(right - left, thickness))
            }
        }
        i = last + 1
    }
}

private val UNDERLINE_THICKNESS = 2.dp

/** How far below the baseline the underline starts, in multiples of the font size. */
private const val UNDERLINE_OFFSET_EM = 0.12f

/** How strongly the hovered word is tinted with the text colour: light gray on light themes, soft gray on dark ones. */
private const val HOVER_ALPHA = 0.22f
