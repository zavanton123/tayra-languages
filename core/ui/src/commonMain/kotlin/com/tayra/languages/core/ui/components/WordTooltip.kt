package com.tayra.languages.core.ui.components

import kotlin.coroutines.cancellation.CancellationException
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import com.tayra.languages.core.ui.theme.TayraTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.tayra.languages.core.domain.text.Words
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Places a popup centred above [target], a rectangle in the anchor layout's own coordinates, and
 * below it when there is no room above. Kept inside the window horizontally.
 */
class AboveTargetPositionProvider(private val target: IntRect, private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val centre = anchorBounds.left + (target.left + target.right) / 2
        val x = (centre - popupContentSize.width / 2).coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width))
        val above = anchorBounds.top + target.top - gap - popupContentSize.height
        val y = if (above >= 0) above else anchorBounds.top + target.bottom + gap
        return IntOffset(x, y)
    }
}

/** The small dark label that holds a word's translation, or an ellipsis while it is looked up. */
@Composable
fun WordTooltip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shape = MaterialTheme.shapes.small,
        shadowElevation = 4.dp,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

/** Shows [WordTooltip] above [target] with the result of [translate], after a short hover. */
@Composable
fun HoverTranslationPopup(target: IntRect, word: String, translate: suspend (String) -> String?) {
    key(word, target) {
        var shown by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            delay(HOVER_DELAY_MS)
            // Only a slow lookup shows the ellipsis, so cached answers appear without a flicker.
            val pending = launch { delay(LOADING_DELAY_MS); if (shown == null) shown = "\u2026" }
            val result = try { translate(word) } catch (e: CancellationException) { throw e } catch (e: Exception) { null }
            pending.cancel()
            shown = result
        }
        val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
        shown?.let { text ->
            Popup(popupPositionProvider = remember(target, gap) { AboveTargetPositionProvider(target, gap) }) { WordTooltip(text) }
        }
    }
}

/**
 * Text whose words show their translation in a tooltip above them while the mouse rests on
 * them. [onWordClick], when given, receives the word under a click or tap, and
 * [onWordSecondaryClick] the word under a right click. [onPhraseSelect] receives the words a
 * mouse drag ran over, from the first to the last, once the button is released; they show in
 * the selection colour meanwhile. [onHover] follows the word under the mouse, null when none.
 */
@Composable
fun HoverTranslatedText(
    text: AnnotatedString,
    translate: suspend (String) -> String?,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    onWordClick: ((String) -> Unit)? = null,
    onWordSecondaryClick: ((String) -> Unit)? = null,
    onPhraseSelect: ((String) -> Unit)? = null,
    onHover: ((String?) -> Unit)? = null,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var hovered by remember(text) { mutableStateOf<HoveredWord?>(null) }
    var selection by remember(text) { mutableStateOf<IntRange?>(null) }
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val selectedColor = TayraTheme.current.selectedText
    val range = hovered?.range
    val picked = selection
    // The same characters with the hovered word tinted gray, so the layout and word positions stay put.
    val shown = remember(text, range, tint, picked, selectedColor) {
        if (range == null && picked == null) text else buildAnnotatedString {
            append(text)
            if (range != null) addStyle(SpanStyle(background = tint), range.first, range.last + 1)
            if (picked != null) addStyle(SpanStyle(color = selectedColor), picked.first, picked.last + 1)
        }
    }
    val secondaryClick by rememberUpdatedState(onWordSecondaryClick)
    val phraseSelect by rememberUpdatedState(onPhraseSelect)
    val hoverChanged by rememberUpdatedState(onHover)
    val wordClick by rememberUpdatedState(onWordClick)
    Box(modifier) {
        // The inner box wraps the text exactly, so the popup's anchor is the text itself.
        Box {
            Text(
                shown,
                style = style,
                onTextLayout = { layout = it },
                modifier = Modifier.then(
                    if (onWordClick == null) Modifier
                    // Keyed by the characters alone: a change of colours must not drop a click under way.
                    else Modifier.pointerInput(text.text) {
                        detectTapGestures { position -> layout?.let { wordUnder(it, text.text, position) }?.let { wordClick?.invoke(it.word) } }
                    },
                ).pointerInput(text) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.type != PointerType.Mouse) continue
                            val next = when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> layout?.let { wordUnder(it, text.text, change.position) }
                                PointerEventType.Exit, PointerEventType.Press, PointerEventType.Scroll -> null
                                else -> hovered
                            }
                            if (next?.range != hovered?.range) {
                                hovered = next
                                hoverChanged?.invoke(next?.word)
                            }
                        }
                    }
                }.then(
                    if (onWordSecondaryClick == null && onPhraseSelect == null) Modifier
                    else Modifier.pointerInput(text) {
                        awaitEachGesture {
                            // A secondary mouse button never sets `pressed`, so wait for the raw press event.
                            var press = awaitPointerEvent()
                            while (press.type != PointerEventType.Press) press = awaitPointerEvent()
                            val down = press.changes.first()
                            if (down.type != PointerType.Mouse) return@awaitEachGesture
                            val start = layout?.let { wordUnder(it, text.text, down.position) } ?: return@awaitEachGesture
                            if (press.buttons.isSecondaryPressed || !press.buttons.isPrimaryPressed) {
                                var release = awaitPointerEvent()
                                while (release.type != PointerEventType.Release) release = awaitPointerEvent()
                                val end = layout?.let { wordUnder(it, text.text, release.changes.first().position) }
                                if (end?.range == start.range) secondaryClick?.invoke(start.word)
                                return@awaitEachGesture
                            }
                            if (phraseSelect == null) return@awaitEachGesture
                            var dragged: IntRange? = null
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()
                                if (event.type == PointerEventType.Release) {
                                    // A drag that picked a phrase is not also a click on its last word.
                                    dragged?.let { change.consume(); phraseSelect?.invoke(text.text.substring(it.first, it.last + 1)) }
                                    break
                                }
                                val over = layout?.let { wordUnder(it, text.text, change.position) } ?: continue
                                dragged = if (over.range == start.range) null
                                else minOf(start.range.first, over.range.first)..maxOf(start.range.last, over.range.last)
                                selection = dragged
                            }
                            selection = null
                        }
                    },
                ),
            )
            hovered?.let { HoverTranslationPopup(it.rect, it.word, translate) }
        }
    }
}

private class HoveredWord(val range: IntRange, val word: String, val rect: IntRect)

/** The word under [position], only when the pointer is actually over its glyphs. */
private fun wordUnder(layout: TextLayoutResult, text: String, position: Offset): HoveredWord? {
    if (position.y < 0 || position.y > layout.size.height) return null
    val range = Words.rangeAt(text, layout.getOffsetForPosition(position)) ?: return null
    val bounds = layout.getPathForRange(range.first, range.last + 1).getBounds()
    val slack = 2f
    if (position.x < bounds.left - slack || position.x > bounds.right + slack || position.y < bounds.top - slack || position.y > bounds.bottom + slack) return null
    val rect = IntRect(bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.right.roundToInt(), bounds.bottom.roundToInt())
    return HoveredWord(range, text.substring(range.first, range.last + 1), rect)
}

private const val HOVER_DELAY_MS = 300L
private const val LOADING_DELAY_MS = 250L
