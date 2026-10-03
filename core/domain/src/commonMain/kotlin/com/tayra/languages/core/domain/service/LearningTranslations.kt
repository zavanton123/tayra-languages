package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Gives every term being learned (statuses 1 to 4) a translation: its parent's, the offline
 * dictionary's gloss or the translation engine's answer, as hover cards show. Terms are queued
 * with [request] as they start being learned and looked up in the background; at start-up the
 * ones still missing a translation are queued again, so a lookup that failed is retried then.
 */
class LearningTranslations(
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val words: WordTranslationService,
) {
    private val queue = Channel<Long>(Channel.UNLIMITED)

    /** Queues the terms; those not being learned, or already translated, are skipped when their turn comes. */
    fun request(termIds: Collection<Long>) {
        for (id in termIds) queue.trySend(id)
    }

    fun start(scope: CoroutineScope): Job = scope.launch {
        request(terms.learningWithoutTranslation())
        for (id in queue) {
            try {
                fill(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Left without a translation; tried again at the next start.
            }
        }
    }

    /** Returns whether a translation was stored. */
    suspend fun fill(termId: Long): Boolean {
        val term = terms.getById(termId) ?: return false
        if (!term.status.isLearning || !term.translation.isNullOrBlank()) return false
        val language = languages.getById(term.languageId) ?: return false
        val translation = words.translate(language, term.text) ?: return false
        return terms.fillTranslation(term.id, translation)
    }
}
