package com.tayra.languages.feature.terms.examples

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.parse.parseTokens
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.ui.theme.StatusColors

/**
 * Saved terms in example sentences: which words and phrases are being learned, and saving a
 * word from an example the way the reader's right click does.
 */
class ExampleTerms(private val reading: ReadingService, private val terms: TermService) {

    /** The terms being learned (statuses 1 to 4) found in each of [sentences]. */
    suspend fun learning(sentences: List<String>, language: Language): Map<String, List<Term>> {
        val index = reading.multiwordIndex(language)
        return sentences.distinct().associateWith { sentence ->
            reading.findTermsInTokens(language.parseTokens(sentence.replace(Regex(" +"), " ")), language, index).filter { it.status.isLearning }
        }
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

/**
 * Paints the terms being learned in their status colours, as the reader does. Words already
 * styled, such as the searched term, keep their look; longer terms win over the words in them.
 */
fun AnnotatedString.withLearning(terms: List<Term>, colors: StatusColors): AnnotatedString {
    if (terms.isEmpty()) return this
    val source = text
    val taken = spanStyles.map { it.start until it.end }.toMutableList()
    return buildAnnotatedString {
        append(this@withLearning)
        for (term in terms.sortedByDescending { it.displayText.length }) {
            val needle = term.displayText
            if (needle.isBlank()) continue
            var from = 0
            while (true) {
                val at = source.indexOf(needle, from, ignoreCase = true)
                if (at < 0) break
                val end = at + needle.length
                from = end
                val whole = (at == 0 || !source[at - 1].isLetterOrDigit()) && (end == source.length || !source[end].isLetterOrDigit())
                if (!whole || taken.any { it.first < end && at <= it.last }) continue
                taken += at until end
                addStyle(SpanStyle(background = colors.background(term.status), color = colors.onHighlight), at, end)
            }
        }
    }
}
