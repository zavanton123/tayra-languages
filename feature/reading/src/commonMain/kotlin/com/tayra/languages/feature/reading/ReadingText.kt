package com.tayra.languages.feature.reading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.morphology.Furigana
import com.tayra.languages.core.domain.render.RenderedPage
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.domain.service.SentenceTranslation
import com.tayra.languages.core.ui.components.AppIcons
import com.tayra.languages.core.ui.components.AppSpinner
import com.tayra.languages.core.ui.components.SpinnerSize
import com.tayra.languages.core.ui.components.Tag
import com.tayra.languages.core.ui.components.TagSize
import com.tayra.languages.core.ui.components.softBlurAvailable
import com.tayra.languages.core.ui.components.toneColored
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppMotion
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

/** Callbacks from the text to the screen. Item indexes refer to [RenderedPage.items]. */
data class ReadingTextCallbacks(
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
    /** A tap (touch, not mouse) on the text beside its words. */
    val onBlankTap: () -> Unit = {},
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
    fontFamily: FontFamily = FontFamily.Serif,
    modifier: Modifier = Modifier,
    /** Lay each sentence out on its own line instead of flowing a paragraph together. */
    splitSentences: Boolean = false,
    /** Readings drawn above words (furigana, pinyin), by the word's text; lines get room for them when there are any. */
    readings: Map<String, List<Furigana>> = emptyMap(),
    /** Pinyin readings coloured by tone. */
    toneColors: Boolean = false,
    /** Justify the lines of text rather than aligning them to the start. */
    justify: Boolean = false,
    /** Translations shown under each sentence, keyed by [RenderedSentence.displayText]; null hides them. */
    translations: Map<String, SentenceTranslation>? = null,
    /** With translations, put each sentence in a left column and its translation in a right one. */
    sideBySide: Boolean = false,
    /**
     * Reads a sentence aloud, given its place among the page's spoken sentences (see [isSpoken]); a
     * play button precedes every sentence when set. The same sentence can occur twice on a page,
     * so a place, not a text, says which one is meant here and in [playingSentence] and [readingSentence].
     */
    onSpeakSentence: ((Int) -> Unit)? = null,
    /** The place of the sentence being read aloud, whose button shows Stop. */
    playingSentence: Int? = null,
    /** Sentences whose audio is still being made; their buttons show a spinner. */
    preparingSentences: Set<String> = emptySet(),
    /** The place of the sentence being heard, or the one continuous reading will go on from, shown with a background. */
    readingSentence: Int? = null,
    /** Whether [readingSentence] is being heard right now (a stronger background) or only waits (a faint one). */
    readingHeard: Boolean = false,
    /** Keeps [readingSentence] scrolled into view while reading runs. */
    followReading: Boolean = false,
    /** The margin between the card's edge and this text; a sentence button leaves the same space before the text. */
    edgePadding: Dp = AppSpacing.l,
    /**
     * Practising translation: every sentence is blurred under its translation until it is in
     * [revealed] (sentences counted through the page). With a mouse a sentence shows while the
     * pointer is over it; by touch a tap on a blurred sentence shows it and a tap beside the words
     * of a shown one hides it. [onRevealed] is asked to show or hide a sentence.
     */
    practice: Boolean = false,
    revealed: Set<Int> = emptySet(),
    onRevealed: (sentence: Int, shown: Boolean) -> Unit = { _, _ -> },
) {
    var itemOffset = 0
    val perSentence = splitSentences || translations != null || practice
    var sentenceNo = 0
    val twoColumns = translations != null && sideBySide
    val readingTint = theme.readingText.copy(alpha = if (readingHeard) HEARD_TINT_ALPHA else WAITING_TINT_ALPHA)
    // The highlight reaches out into the card's margin, stopping a little short of its border.
    val highlightReach = (edgePadding - HIGHLIGHT_INSET).coerceAtLeast(0.dp)
    // Counts the spoken sentences across the page, so their places match [RenderedPage.spokenSentences].
    var spoken = 0
    Column(modifier) {
        page.paragraphs.forEach { paragraph ->
            val places: List<Int?> = paragraph.sentences.map { if (it.displayText.isSpoken()) spoken++ else null }
            // A run of items that shares one Text: the whole paragraph, or one sentence each.
            val runs = if (perSentence) paragraph.sentences.mapIndexed { i, s -> Run(s.items, s.displayText, places[i]) } else listOf(Run(paragraph.sentences.flatMap { it.items }, "", null))
            // In a flowing paragraph the play buttons sit inline, before the first item of each sentence.
            val inlinePlay: Map<Int, SpokenSentence> = if (perSentence || onSpeakSentence == null) emptyMap() else buildMap {
                var position = 0
                paragraph.sentences.forEachIndexed { i, sentence ->
                    val place = places[i]
                    if (place != null) {
                        val lead = sentence.items.indexOfFirst { !it.isParagraphMark && it.renderText.isNotBlank() }
                        if (lead >= 0) put(position + lead, SpokenSentence(place, sentence.displayText))
                    }
                    position += sentence.items.size
                }
            }
            // In a flowing paragraph the sentence being read is tinted word by word.
            val readingRange: IntRange? = if (perSentence || readingSentence == null) null else {
                var position = 0
                paragraph.sentences.withIndex().firstNotNullOfOrNull { (i, sentence) ->
                    val range = position until position + sentence.items.size
                    position += sentence.items.size
                    range.takeIf { places[i] == readingSentence }
                }
            }
            runs.forEach { (runItems, sentenceText, place) ->
                val first = itemOffset
                itemOffset += runItems.size
                val isReading = readingSentence != null && (if (perSentence) place == readingSentence else readingRange != null)
                val speakable = perSentence && onSpeakSentence != null && place != null
                val no = sentenceNo++
                val hidden = practice && no !in revealed
                val shown = practice && !hidden
                val reveal = { shown: Boolean -> onRevealed(no, shown) }
                val sentence: @Composable () -> Unit = {
                    Hideable(hidden, theme, onRevealed = reveal) { ParagraphText(
                        items = runItems,
                        // A sentence on its own line starts at its first word, not at the space that follows the previous one.
                        trimLeadingSpace = perSentence,
                        inlinePlay = inlinePlay,
                        onSpeakSentence = onSpeakSentence,
                        playingSentence = playingSentence,
                        preparingSentences = preparingSentences,
                        readingItems = readingRange,
                        readingTint = readingTint,
                        firstItemIndex = first,
                        theme = theme,
                        showHighlights = showHighlights,
                        marked = marked,
                        hovered = hovered,
                        selection = selection,
                        popupItem = popupItem,
                        fontScale = fontScale,
                        lineHeight = lineHeight,
                        justify = justify,
                        rightToLeft = rightToLeft,
                        callbacks = if (shown) callbacks.copy(onBlankTap = { reveal(false) }) else callbacks,
                        fontFamily = fontFamily,
                        readings = readings,
                        toneColors = toneColors,
                    ) }
                }
                val translated = translations != null && sentenceText.any { it.isLetter() }
                // The button is centred on its sentence's text, so a translation under it stays out of the row.
                val withButton: @Composable (Modifier) -> Unit = { rowModifier ->
                    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
                        PlayButton(place.takeIf { speakable }, place == playingSentence, sentenceText in preparingSentences, fontScale, edgePadding, onSpeakSentence!!)
                        Box(Modifier.weight(1f)) { sentence() }
                    }
                }
                // The background takes in the sentence's translation too, beside it or under it.
                if (twoColumns) {
                    Row(Modifier.fillMaxWidth().followed(isReading, followReading, readingTint, highlightReach).padding(vertical = SENTENCE_ROW_PADDING), verticalAlignment = Alignment.Top) {
                        if (onSpeakSentence != null) withButton(Modifier.weight(1f)) else Box(Modifier.weight(1f)) { sentence() }
                        Box(Modifier.width(AppSpacing.xxl))
                        Box(Modifier.weight(1f)) {
                            if (translated) TranslationLine(translations[sentenceText], theme, fontScale, lineHeight, large = true)
                        }
                    }
                } else if (perSentence) {
                    Column(Modifier.fillMaxWidth().followed(isReading, followReading, readingTint, highlightReach).padding(vertical = SENTENCE_ROW_PADDING)) {
                        if (onSpeakSentence != null) withButton(Modifier.fillMaxWidth()) else sentence()
                        if (translated) {
                            Box(Modifier.padding(start = if (onSpeakSentence != null) playGutter(fontScale, edgePadding) else 0.dp)) {
                                TranslationLine(translations[sentenceText], theme, fontScale, lineHeight)
                            }
                        }
                    }
                } else {
                    // A flowing paragraph: its words are tinted instead.
                    Box(Modifier.fillMaxWidth().followed(isReading, followReading, Color.Transparent, highlightReach)) { sentence() }
                }
            }
        }
    }
}

/**
 * A sentence being practised: blurred until shown. The mouse shows it while over it and hides it
 * on leaving; by touch, a tap on the blur shows it. Where blurring does not draw, the text is
 * made invisible over a soft bar instead.
 */
@Composable
private fun Hideable(hidden: Boolean, theme: AppTheme, onRevealed: (Boolean) -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    // Only a change of hovering counts: the first composition is not a pointer leaving.
    var wasHovered by remember { mutableStateOf(false) }
    LaunchedEffect(hovered) {
        if (hovered != wasHovered) {
            wasHovered = hovered
            onRevealed(hovered)
        }
    }
    Box(Modifier.fillMaxWidth().hoverable(interaction)) {
        if (!hidden) {
            content()
        } else {
            val label = tr("Hidden sentence")
            val show = tr("Show the sentence")
            val veil = if (softBlurAvailable) {
                // Unbounded, so the blur fades out past the text's edges instead of being cut off at them.
                Modifier.blur(9.dp, BlurredEdgeTreatment.Unbounded)
            } else {
                Modifier.drawBehind { drawRoundRect(theme.readingText.copy(alpha = AppAlpha.tint), cornerRadius = CornerRadius(8.dp.toPx())) }.graphicsLayer { alpha = 0f }
            }
            Box(veil) { content() }
            // On top of the text, so its words take no taps until it is shown; the tag says the blur is meant and how to lift it.
            Box(
                Modifier.matchParentSize().clickable(onClickLabel = show, onClick = { onRevealed(true) }).semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) { Tag(show, MaterialTheme.colorScheme.primary, size = TagSize.Small) }
        }
    }
}

/** Whether a sentence is read aloud: the ones without a letter get no button and no turn. */
internal fun String.isSpoken(): Boolean = any { it.isLetter() }

/** The texts of the page's spoken sentences, in order; a sentence's index here is its place for [ReadingText]. */
internal fun RenderedPage.spokenSentences(): List<String> = paragraphs.flatMap { it.sentences }.map { it.displayText }.filter { it.isSpoken() }

/** The items that share one Text, with the sentence's text and place when the run is one sentence. */
private data class Run(val items: List<TextItem>, val text: String, val place: Int?)

/** A sentence with a play button: its place among the page's spoken sentences and its text. */
private data class SpokenSentence(val place: Int, val text: String)

/**
 * The button before a sentence. It sits at the start of the text area and leaves [gap] before
 * the text, the same as the card's margin before it, so it is centred between edge and text;
 * an empty slot of the same width keeps sentences without words aligned.
 */
@Composable
private fun PlayButton(place: Int?, playing: Boolean, preparing: Boolean, fontScale: Float, gap: Dp, onSpeak: (Int) -> Unit) {
    Box(Modifier.padding(end = gap).size((PLAY_BUTTON_SIZE * fontScale).dp)) {
        if (place != null) SpeakerCircle(playing, preparing, Modifier.fillMaxSize()) { onSpeak(place) }
    }
}

/** How far the text of a sentence with a play button starts from the edge. */
private fun playGutter(fontScale: Float, gap: Dp) = (PLAY_BUTTON_SIZE * fontScale).dp + gap

private const val PLAY_BUTTON_SIZE = 32

/**
 * The speaker icon in a soft circle: a spinner while the sentence's audio is being made, a stop
 * square while it is read. A preparing sentence can still be clicked; it plays once ready.
 */
@Composable
private fun SpeakerCircle(playing: Boolean, preparing: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val description = if (playing) tr("Stop") else if (preparing) tr("Preparing sentence") else tr("Play sentence")
    Box(
        modifier.clip(AppShapes.circle).background(primary.copy(alpha = AppAlpha.soft)).clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when {
            preparing -> AppSpinner(Modifier.fillMaxSize(0.5f), size = SpinnerSize.Small, color = primary)
            else -> Icon(if (playing) AppIcons.Stop else AppIcons.VolumeUp, contentDescription = null, tint = primary, modifier = Modifier.fillMaxSize(0.58f))
        }
    }
}

/**
 * Marks the sentence being read: a light rounded background drawn [reach] beyond its sides, into
 * the card's margin, so it spans the whole row (none for a flowing paragraph, whose words are
 * tinted instead); kept on screen while [follow] is on, scrolled no further than that takes.
 * Drawn, not laid out, so nothing moves as the highlight moves on.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.followed(isReading: Boolean, follow: Boolean, tint: Color, reach: Dp): Modifier {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(isReading, follow) { if (isReading && follow) requester.bringIntoView() }
    val current = tr(CURRENT_SENTENCE)
    val marked = bringIntoViewRequester(requester)
        .then(if (isReading) Modifier.semantics { stateDescription = current } else Modifier)
    if (!isReading || tint == Color.Transparent) return marked
    return marked.drawBehind {
        val side = reach.toPx()
        drawRoundRect(tint, topLeft = Offset(-side, 0f), size = Size(size.width + side * 2, size.height), cornerRadius = CornerRadius(12.dp.toPx()))
    }
}

/** Read out for the highlighted sentence by screen readers (and found by tests). */
internal const val CURRENT_SENTENCE = "Current sentence"

/** How far the highlight stops short of the card's border. */
private val HIGHLIGHT_INSET = 8.dp

/** Room above and below every sentence on its own line, which the highlight fills too. */
private val SENTENCE_ROW_PADDING = 6.dp

/** How strongly the sentence being heard is tinted with the text colour. */
private const val HEARD_TINT_ALPHA = 0.1f

/** The fainter tint of the sentence continuous reading waits on, paused or auto-paused. */
private const val WAITING_TINT_ALPHA = 0.045f

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
            // It starts under the sentence's first letter.
            modifier = Modifier.fillMaxWidth().padding(bottom = if (large) 0.dp else AppSpacing.m),
        )
        SentenceTranslation.Loading, null -> Row(
            Modifier.padding(top = AppSpacing.xxs, bottom = AppSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            AppSpinner(size = SpinnerSize.Small, color = color)
            Text(tr("Translating\u2026"), style = TextStyle(fontSize = (13 * fontScale).sp, color = color))
        }
        SentenceTranslation.Unavailable -> Spacer(Modifier.height(AppSpacing.s))
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
    fontFamily: FontFamily = FontFamily.Serif,
    justify: Boolean = false,
    /** Local item positions that start a sentence, with the sentence to read, for inline play buttons. */
    inlinePlay: Map<Int, SpokenSentence> = emptyMap(),
    onSpeakSentence: ((Int) -> Unit)? = null,
    playingSentence: Int? = null,
    preparingSentences: Set<String> = emptySet(),
    trimLeadingSpace: Boolean = false,
    /** Local positions of the items of the sentence being read, tinted where they have no colour of their own. */
    readingItems: IntRange? = null,
    readingTint: Color = Color.Transparent,
    readings: Map<String, List<Furigana>> = emptyMap(),
    /** Pinyin readings coloured by tone. */
    toneColors: Boolean = false,
) {
    val spans = remember(items) { mutableListOf<Span>() }
    // The pointer handlers live as long as the paragraph, but its words are laid out anew when they
    // regroup (a Chinese word saved joins the characters it was read as): they read the spans of now.
    val currentSpans by rememberUpdatedState(spans)
    val rubySize = 18 * fontScale * RUBY_SCALE
    val measurer = rememberTextMeasurer()
    // A word whose status changes fades from its old colour to its new one; the words are told apart
    // by their text too, so a page turned, whose words take the same places, fades nothing.
    val known = remember { HashMap<Int, Pair<String, TermStatus>>() }
    val reduceMotion = AppMotion.reduced
    val fading = remember(items) {
        val changed = HashMap<Int, TermStatus>()
        items.forEachIndexed { i, item ->
            if (!item.isWord) return@forEachIndexed
            val key = firstItemIndex + i
            known[key]?.let { (text, status) -> if (text == item.renderText && status != item.status) changed[key] = status }
            known[key] = item.renderText to item.status
        }
        changed
    }
    val fade = remember(items) { Animatable(if (fading.isEmpty() || reduceMotion) 1f else 0f) }
    LaunchedEffect(fade) { if (fade.value < 1f) fade.animateTo(1f, tween(AppMotion.medium, easing = AppMotion.standard)) }
    val fraction = if (fading.isEmpty()) 1f else fade.value
    val text = remember(items, theme, showHighlights, marked, hovered, selection, firstItemIndex, inlinePlay.keys, trimLeadingSpace, readingItems, readingTint, fraction) {
        spans.clear()
        buildParagraph(items, firstItemIndex, theme, showHighlights, marked, hovered, selection, spans, inlinePlay.keys, trimLeadingSpace, readingItems, readingTint, fading, fraction)
    }
    val primary = MaterialTheme.colorScheme.primary
    val inlineContent = remember(inlinePlay, onSpeakSentence, primary, playingSentence, preparingSentences) {
        if (onSpeakSentence == null) emptyMap() else inlinePlay.entries.associate { (position, sentence) ->
            val playing = sentence.place == playingSentence
            // The circle fills the placeholder's height; the extra width is the gap before the sentence.
            "play-$position" to InlineTextContent(Placeholder(1.85.em, 1.35.em, PlaceholderVerticalAlign.TextCenter)) {
                Box(Modifier.fillMaxSize()) {
                    SpeakerCircle(playing, sentence.text in preparingSentences, Modifier.fillMaxHeight().aspectRatio(1f).align(Alignment.CenterStart)) { onSpeakSentence(sentence.place) }
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
        val spans = currentSpans
        fun spanAt(at: Int) = spans.firstOrNull { at >= it.start && at < it.end }
        // The nearest caret comes after the word under the mouse when it is over the far half of its last
        // letter (its left half in a right-to-left line), or over a letter carrying vowel signs or marks.
        val before = if (offset > 0) spanAt(offset - 1) else null
        if (before != null && before.end > before.start && result.getLineForOffset(before.start) == line && result.getLineForOffset(before.end - 1) == line) {
            val bounds = result.getPathForRange(before.start, before.end).getBounds()
            if (position.x >= bounds.left && position.x <= bounds.right) return before.itemIndex
        }
        val span = spanAt(offset) ?: spans.lastOrNull { offset == it.end } ?: return null
        return span.itemIndex
    }

    Box(Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = (18 * fontScale).sp,
                // Lines keep room above the letters for readings, and the letters sit at the bottom of it.
                lineHeight = (18 * fontScale * lineHeight + if (readings.isEmpty()) 0f else rubySize * 1.3f).sp,
                lineHeightStyle = if (readings.isEmpty()) null else LineHeightStyle(LineHeightStyle.Alignment.Bottom, LineHeightStyle.Trim.None),
                color = theme.readingText,
                textDirection = if (rightToLeft) TextDirection.Rtl else TextDirection.Ltr,
                fontFamily = fontFamily,
                textAlign = if (justify) TextAlign.Justify else TextAlign.Start,
            ),
            onTextLayout = { layout = it },
            inlineContent = inlineContent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.s)
                .drawWithCache {
                    val result = layout
                    val marks = if (result != null && result.layoutInput.text == text) highlightMarks(result, (18 * fontScale).sp.toPx(), HIGHLIGHT_SIDE.toPx()) else emptyList()
                    val corner = CornerRadius(HIGHLIGHT_CORNER.toPx())
                    onDrawWithContent {
                        marks.forEach { (rect, color) -> drawRoundRect(color, rect.topLeft, rect.size, corner) }
                        drawContent()
                        if (readings.isNotEmpty() && result != null) drawReadings(result, spans, items, firstItemIndex, readings, measurer, toneColors, TextStyle(fontSize = rubySize.sp, color = theme.readingText.copy(alpha = AppAlpha.muted), fontFamily = fontFamily))
                    }
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
                                up != null -> callbacks.onBlankTap()
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
    readingItems: IntRange? = null,
    readingTint: Color = Color.Transparent,
    /** Words whose status just changed, with the status they had, and how far their colour has come from it to the new one. */
    fading: Map<Int, TermStatus> = emptyMap(),
    fade: Float = 1f,
): AnnotatedString = buildAnnotatedString {
    val colors = theme.statusColors

    // How a word of [status] is coloured: its letters, on the background its status gives.
    fun colouring(status: TermStatus, highlight: Boolean): SpanStyle {
        if (!highlight || status == TermStatus.WELL_KNOWN || status == TermStatus.IGNORED) return SpanStyle()
        val background = colors.background(status)
        return when {
            status == TermStatus.UNKNOWN && colors.unknownAsText -> SpanStyle(color = background)
            background != Color.Transparent -> SpanStyle(background = background, color = if (colors.onHighlight != Color.Unspecified) colors.onHighlight else Color.Unspecified)
            else -> SpanStyle()
        }
    }

    fun Color.orElse(other: Color) = if (this == Color.Unspecified) other else this
    // Leading blank items still get (empty) spans, so item positions stay as they are.
    val firstShown = if (trimLeadingSpace) items.indexOfFirst { it.renderText.isNotBlank() }.coerceAtLeast(0) else 0
    // A sentence's play button goes before an opening quote or bracket that starts it, not between the two.
    var playedEarly = -1
    items.forEachIndexed { i, item ->
        val itemIndex = firstItemIndex + i
        if (i in playBefore && playedEarly != i) appendInlineContent("play-$i", "\u25B6")
        val start = length
        val inSelection = selection != null && item.index in selection
        val isMarked = itemIndex in marked
        val isHovered = itemIndex == hovered
        var style = SpanStyle()
        if (item.isWord) {
            val highlight = showHighlights || isHovered || isMarked
            style = colouring(item.status, highlight)
            val before = fading[itemIndex]
            if (before != null && fade < 1f) {
                val from = colouring(before, highlight)
                val background = lerp(from.background.orElse(Color.Transparent), style.background.orElse(Color.Transparent), fade)
                val letters = lerp(from.color.orElse(theme.readingText), style.color.orElse(theme.readingText), fade)
                style = SpanStyle(background = background, color = letters)
            }
            // The word under the mouse turns plain gray, whatever its status colour, so it reads as "pointed at".
            if (isHovered && !isMarked) style = SpanStyle(background = theme.readingText.copy(alpha = HOVER_ALPHA))
        }
        // Selected words and phrases, including one still being dragged over, keep their backgrounds and change the colour of
        // their letters, and are underlined too, so the choice does not rest on the colour alone.
        val selected = (isMarked && item.isWord) || inSelection
        if (selected) style = style.copy(color = theme.selectedText, textDecoration = if (item.isWord) TextDecoration.Underline else null)
        val coloured = style.background != Color.Unspecified
        // A word's own colour is drawn as a rounded mark around its letters (see highlightMarks), not the line's full height.
        val fill = if (item.isWord && coloured && style.background != Color.Transparent) style.background else null
        if (fill != null) style = style.copy(background = Color.Unspecified)
        if (readingItems != null && i in readingItems && !coloured) style = style.copy(background = readingTint)
        withStyle(style) {
            if (fill != null) pushStringAnnotation(FILL_ANNOTATION, fill.toArgb().toString())
            if (selected) pushStringAnnotation(SELECTED_ANNOTATION, "")
            if (item.isOverlapped) append("⁺")
            if (i >= firstShown) {
                val text = item.renderText
                val opening = text.trimEnd()
                if (!item.isWord && i + 1 in playBefore && opening.lastOrNull()?.let { it in OPENING_MARKS } == true) {
                    append(text.substring(0, opening.length - 1))
                    appendInlineContent("play-${i + 1}", "\u25B6")
                    append(text.substring(opening.length - 1))
                    playedEarly = i + 1
                } else {
                    append(text)
                }
            }
            if (selected) pop()
            if (fill != null) pop()
        }
        spans.add(Span(start, length, itemIndex))
    }
}

/** Readings are drawn at half the size of the text. */
private const val RUBY_SCALE = 0.5f

/**
 * Draws each word's readings centred above the characters they read, in the room the line height
 * leaves above the letters; a reading over characters that wrap is drawn over each line's part.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawReadings(
    result: TextLayoutResult,
    spans: List<Span>,
    items: List<TextItem>,
    firstItemIndex: Int,
    readings: Map<String, List<Furigana>>,
    measurer: TextMeasurer,
    toneColors: Boolean,
    style: TextStyle,
) {
    for (span in spans) {
        val item = items.getOrNull(span.itemIndex - firstItemIndex) ?: continue
        val furigana = readings[item.renderText] ?: continue
        var offset = span.end - item.renderText.length
        for (segment in furigana) {
            val start = offset
            offset += segment.text.length
            if (segment.reading.isEmpty() || offset > result.layoutInput.text.length) continue
            (start until offset).groupBy { result.getLineForOffset(it) }.forEach { (line, positions) ->
                val boxes = positions.map { result.getBoundingBox(it) }
                val left = boxes.minOf { it.left }
                val right = boxes.maxOf { it.right }
                val measured = measurer.measure(if (toneColors) toneColored(segment.reading) else AnnotatedString(segment.reading), style)
                drawText(measured, topLeft = Offset((left + right) / 2 - measured.size.width / 2f, result.getLineTop(line)))
            }
        }
    }
}

/** Marks the selected characters in the text, for tests and accessibility tools. */
internal const val SELECTED_ANNOTATION = "selected"

/** Marks that open a quotation or an aside, and so belong with the sentence after them. */
private const val OPENING_MARKS = "\"“„«‹‘(¿¡["

/** Marks a word drawn on a colour of its own; the value is the colour, as ARGB. */
internal const val FILL_ANNOTATION = "fill"

/** How far a word's colour reaches above its baseline and below it, in ems: round its letters, short of the line's full height, so lines stay apart. */
private const val HIGHLIGHT_ABOVE = 0.98f
private const val HIGHLIGHT_BELOW = 0.32f
private val HIGHLIGHT_CORNER = 4.dp

/** How far a word's colour reaches past its first and last letters, short of the space beside it. */
private val HIGHLIGHT_SIDE = 2.dp

/** Where each coloured word's mark goes, a piece for each line it is on, and its colour. */
private fun highlightMarks(result: TextLayoutResult, fontPx: Float, side: Float): List<Pair<Rect, Color>> {
    val text = result.layoutInput.text
    return text.getStringAnnotations(FILL_ANNOTATION, 0, text.length).flatMap { fill ->
        val color = Color(fill.item.toInt())
        if (fill.end <= fill.start) return@flatMap emptyList()
        (result.getLineForOffset(fill.start)..result.getLineForOffset(fill.end - 1)).mapNotNull { line ->
            val from = maxOf(fill.start, result.getLineStart(line))
            val to = minOf(fill.end, result.getLineEnd(line))
            if (to <= from) return@mapNotNull null
            val box = result.getPathForRange(from, to).getBounds()
            val baseline = result.getLineBaseline(line)
            val top = maxOf(box.top, baseline - fontPx * HIGHLIGHT_ABOVE)
            val bottom = minOf(box.bottom, baseline + fontPx * HIGHLIGHT_BELOW)
            Rect(box.left - side, top, box.right + side, bottom) to color
        }
    }
}

/** How strongly the hovered word is tinted with the text colour: light gray on light themes, soft gray on dark ones. */
private const val HOVER_ALPHA = 0.22f
