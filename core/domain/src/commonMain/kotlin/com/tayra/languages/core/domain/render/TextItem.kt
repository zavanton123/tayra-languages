package com.tayra.languages.core.domain.render

import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.ZWS_STRING

/**
 * A unit of text to render on the reading screen: either a single token, or a
 * multi-word term covering several tokens.
 */
class TextItem(
    /** Index of the first token in the page's token list. */
    val index: Int,
    /** The original, un-overlapped text (tokens joined by ZWS). */
    val text: String,
    val textLc: String,
    val tokenCount: Int,
    val sentenceNumber: Int,
    val term: Term?,
) {
    /** Number of tokens to display, counted from the end of [text]. */
    var displayCount: Int = tokenCount
        internal set

    var paragraphNumber: Int = 0
        internal set

    var isSentenceStart: Boolean = false
        internal set

    val isWord: Boolean get() = term != null
    val termId: Long? get() = term?.id?.takeIf { it != 0L }
    val status: TermStatus get() = term?.status ?: TermStatus.UNKNOWN
    val isParagraphMark: Boolean get() = text == "¶"

    /** The last [displayCount] tokens, if part of the item is covered by an earlier term. */
    val displayText: String
        get() {
            val tokens = text.split(ZWS_STRING)
            return tokens.takeLast(displayCount).joinToString(ZWS_STRING)
        }

    val isOverlapped: Boolean get() = displayCount != tokenCount

    /** Text as rendered, without token separators. */
    val renderText: String get() = displayText.replace(ZWS_STRING, "")

    override fun toString(): String = "TextItem($index '$text' x$tokenCount show $displayCount)"
}

/** A sentence of rendered items. */
data class RenderedSentence(val number: Int, val items: List<TextItem>)

/** A paragraph of rendered sentences. */
data class RenderedParagraph(val number: Int, val sentences: List<RenderedSentence>)

data class RenderedPage(val paragraphs: List<RenderedParagraph>) {
    val items: List<TextItem> get() = paragraphs.flatMap { p -> p.sentences.flatMap { it.items } }
    val words: List<TextItem> get() = items.filter { it.isWord }

    companion object {
        val EMPTY = RenderedPage(emptyList())
    }
}
