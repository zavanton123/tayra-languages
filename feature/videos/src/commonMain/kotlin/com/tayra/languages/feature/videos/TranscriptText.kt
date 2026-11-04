package com.tayra.languages.feature.videos

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.render.TextItem
import com.tayra.languages.core.ui.components.pushFill
import com.tayra.languages.core.ui.components.wordMarks
import com.tayra.languages.core.ui.theme.AppTheme
import com.tayra.languages.core.ui.theme.LocalAppTheme

/** Where an item of a line is in its text, to find the word under a click. */
private class ItemSpan(val start: Int, val end: Int, val item: Int)

/**
 * A subtitle line with its words coloured by status, as the reader colours them. A click on a word
 * reports it ([onWord] with its place among the [items]); a click beside the words reports the line.
 */
@Composable
internal fun TranscriptText(
    items: List<TextItem>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    picked: Int? = null,
    textAlign: TextAlign? = null,
    onWord: (Int) -> Unit,
    onLine: () -> Unit,
) {
    val theme = LocalAppTheme.current
    val built = remember(items, picked, theme) { build(items, theme, picked) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val word by rememberUpdatedState(onWord)
    val line by rememberUpdatedState(onLine)
    val spans by rememberUpdatedState(built.second)
    Text(
        built.first,
        modifier
            .wordMarks({ layout }, built.first, style.fontSize)
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures line()
                    val offset = result.getOffsetForPosition(position)
                    val bounds = result.getBoundingBox(offset.coerceAtMost(result.layoutInput.text.length - 1).coerceAtLeast(0))
                    val hit = spans.firstOrNull { offset in it.start until it.end }
                        ?.takeIf { items.getOrNull(it.item)?.isWord == true && position.x in (bounds.left - SLACK)..(bounds.right + SLACK) }
                    if (hit != null) word(hit.item) else line()
                }
            },
        style = style,
        textAlign = textAlign,
        onTextLayout = { layout = it },
    )
}

private fun build(items: List<TextItem>, theme: AppTheme, picked: Int?): Pair<AnnotatedString, List<ItemSpan>> {
    val colors = theme.statusColors
    val spans = ArrayList<ItemSpan>(items.size)
    val text = buildAnnotatedString {
        items.forEachIndexed { i, item ->
            val start = length
            var style = SpanStyle()
            var fill: Color? = null
            if (item.isWord && item.status != TermStatus.WELL_KNOWN && item.status != TermStatus.IGNORED) {
                val background = colors.background(item.status)
                when {
                    item.status == TermStatus.UNKNOWN && colors.unknownAsText -> style = SpanStyle(color = background)
                    background != Color.Transparent -> {
                        fill = background
                        if (colors.onHighlight != Color.Unspecified) style = SpanStyle(color = colors.onHighlight)
                    }
                }
            }
            if (i == picked && item.isWord) style = style.copy(color = theme.selectedText, textDecoration = TextDecoration.Underline)
            withStyle(style) {
                if (fill != null) pushFill(fill)
                append(item.renderText)
                if (fill != null) pop()
            }
            spans += ItemSpan(start, length, i)
        }
    }
    return text to spans
}

/** How far beside a word's letters a click still counts as on it, in pixels. */
private const val SLACK = 2f
