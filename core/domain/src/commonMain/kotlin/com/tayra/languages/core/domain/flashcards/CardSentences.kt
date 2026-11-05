package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.practice.PracticeExampleStore
import com.tayra.languages.core.domain.practice.WordForms
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Finds a sentence for the words saved without one, as a flashcard asks its word in a sentence
 * and none is made or shown without it: one from a page already read, else an example from the
 * courses or online. Terms are queued with [request] as they start being learned; at start-up all
 * those still without one are queued again, so a lookup that failed is retried then.
 */
class CardSentences(
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val forms: WordForms,
    private val examples: PracticeExampleStore,
) {
    private val queue = Channel<Long>(Channel.UNLIMITED)

    /** Queues the terms; those that have a sentence by then are skipped when their turn comes. */
    fun request(termIds: Collection<Long>) {
        for (id in termIds) queue.trySend(id)
    }

    fun start(scope: CoroutineScope): Job = scope.launch {
        request(terms.needingSentence())
        for (id in queue) {
            try {
                fill(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Left without a sentence, and so without a card; tried again at the next start.
            }
        }
    }

    /** Returns whether a sentence was stored. */
    suspend fun fill(termId: Long): Boolean {
        val term = terms.getById(termId) ?: return false
        if (!term.sentence.isNullOrBlank()) return false
        val language = languages.getById(term.languageId) ?: return false
        read(term, language)?.let { (sentence, form) -> return terms.fillSentence(term.id, sentence, null, form) }
        val example = examples.examplesFor(term, language).firstOrNull() ?: return false
        return terms.fillSentence(term.id, example.sentence, example.translation, example.form.takeUnless { it.equals(term.displayText, ignoreCase = true) })
    }

    /** A sentence of a read page holding the word as saved, or else another of its forms, which comes with it. */
    private suspend fun read(term: Term, language: Language): Pair<String, String?>? {
        val saved = terms.references(language.id, term.textLc, REFERENCES).firstNotNullOfOrNull { usable(it.sentence, term.textLc) }
        if (saved != null) return saved to null
        for (form in forms.of(term.displayText, language)) {
            val formLc = language.lowercase(form)
            if (formLc == term.textLc) continue
            val sentence = terms.references(language.id, formLc, REFERENCES).firstNotNullOfOrNull { usable(it.sentence, formLc) }
            if (sentence != null) return sentence to form
        }
        return null
    }

    /** A sentence as marked for the reader, unmarked; none if it is little more than the word (a title), or too long for a card. */
    private fun usable(marked: String, word: String): String? {
        val sentence = marked.replace("**", "").trim()
        return sentence.takeIf { it.length <= MAX_LENGTH && it.count(Char::isLetterOrDigit) > word.count(Char::isLetterOrDigit) + MIN_MORE_LETTERS }
    }

    private companion object {
        const val REFERENCES = 20
        const val MAX_LENGTH = 200
        const val MIN_MORE_LETTERS = 2
    }
}
