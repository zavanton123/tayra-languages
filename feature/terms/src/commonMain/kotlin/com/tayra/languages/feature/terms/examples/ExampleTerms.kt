package com.tayra.languages.feature.terms.examples

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.ui.theme.StatusColors

/**
 * Terms in example sentences: the status of every word and saved phrase, and saving a word
 * from an example the way the reader does.
 */
class ExampleTerms(private val reading: ReadingService, private val terms: TermService) {

    /**
     * The words and saved phrases of each of [sentences] with their statuses, worked out as the
     * reader does for a page; a word never saved is unknown. Nothing is stored.
     */
    suspend fun statuses(sentences: List<String>, language: Language): Map<String, List<WordStatus>> {
        val index = reading.multiwordIndex(language)
        return sentences.distinct().associateWith { sentence ->
            var cursor = 0
            reading.renderItems(sentence, language, index).filter { it.isWord && !it.isParagraphMark }.mapNotNull { item ->
                val shown = item.renderText
                // Each item is found in the sentence as written, after the one before it.
                val at = if (shown.isBlank()) -1 else sentence.indexOf(shown, cursor)
                if (at < 0) return@mapNotNull null
                cursor = at + shown.length
                WordStatus(at, cursor, item.status)
            }
        }
    }

    /** Gives [word] the status, with [sentence] as its example when it starts being learned. */
    suspend fun setStatus(word: String, sentence: String?, language: Language, status: TermStatus) {
        // A word that was never saved is unknown already; no term is made just to say so.
        if (status == TermStatus.UNKNOWN && terms.find(language.id, word) == null) return
        reading.setStatusForTexts(language, listOf(word), status, sentence)
    }

    /** Moves [word] one status up or down, an unsaved word counting as unknown. */
    suspend fun shiftStatus(word: String, sentence: String?, language: Language, delta: Int) {
        val current = terms.find(language.id, word)?.status ?: TermStatus.UNKNOWN
        val next = TermStatus.shifted(current, delta)
        if (next != current) setStatus(word, sentence, language, next)
    }

    /**
     * A word not being learned starts at status 1 with [sentence] as its example; one being
     * learned becomes known. Returns the term's id.
     */
    suspend fun toggle(word: String, sentence: String, language: Language): Long? {
        val current = terms.find(language.id, word)?.status ?: TermStatus.UNKNOWN
        val next = if (current.isLearning) TermStatus.WELL_KNOWN else TermStatus.NEW_1
        reading.setStatusForTexts(language, listOf(word), next, sentence)
        return terms.find(language.id, word)?.id
    }
}

/** A word or saved phrase of a sentence, from [start] to [end] (exclusive), and its status. */
data class WordStatus(val start: Int, val end: Int, val status: TermStatus)

/**
 * Paints the words in their status colours as the reader does: unknown and learning words are
 * coloured, known and ignored ones are left plain. [highlight] is the reader's "Highlight terms".
 */
fun AnnotatedString.withStatuses(words: List<WordStatus>, colors: StatusColors, highlight: Boolean = true): AnnotatedString {
    if (!highlight || words.isEmpty()) return this
    return buildAnnotatedString {
        append(this@withStatuses)
        for (word in words) {
            if (word.status == TermStatus.WELL_KNOWN || word.status == TermStatus.IGNORED || word.end > length) continue
            val background = colors.background(word.status)
            val style = when {
                word.status == TermStatus.UNKNOWN && colors.unknownAsText -> SpanStyle(color = background)
                background != Color.Transparent -> SpanStyle(background = background, color = colors.onHighlight)
                else -> continue
            }
            addStyle(style, word.start, word.end)
        }
    }
}

/** Shows the whole-word occurrences of [term], the word or phrase open in the term pane, in the reader's selection colour. */
fun AnnotatedString.withSelected(term: String?, color: Color): AnnotatedString {
    val needle = term?.replace("\u200B", "")?.trim().orEmpty()
    if (needle.isEmpty()) return this
    val source = text
    return buildAnnotatedString {
        append(this@withSelected)
        var from = 0
        while (true) {
            val at = source.indexOf(needle, from, ignoreCase = true)
            if (at < 0) break
            val end = at + needle.length
            from = end
            val whole = (at == 0 || !source[at - 1].isLetterOrDigit()) && (end == source.length || !source[end].isLetterOrDigit())
            if (whole) addStyle(SpanStyle(color = color), at, end)
        }
    }
}
