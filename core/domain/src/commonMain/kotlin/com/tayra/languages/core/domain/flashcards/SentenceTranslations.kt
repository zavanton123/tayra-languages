package com.tayra.languages.core.domain.flashcards

import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.SentenceTranslator
import kotlin.coroutines.cancellation.CancellationException

/**
 * The translations of the cards' sentences, kept with their words: made by the translator when a
 * sentence has none, and kept unless written over by hand or the sentence changes.
 */
class SentenceTranslations(
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val translator: SentenceTranslator,
) {
    /**
     * Translates every card's sentence still without a translation, one after another. A translator
     * that gives nothing (offline, or out of its day's quota) ends the round; the rest wait for the next.
     */
    suspend fun fillMissing() {
        val byId = mutableMapOf<Long, Language?>()
        for (missing in terms.missingSentenceTranslations()) {
            val language = byId.getOrPut(missing.languageId) { languages.getById(missing.languageId) } ?: continue
            val sentence = missing.sentence.trim()
            val translated = translate(sentence, language) ?: return
            terms.fillSentenceTranslation(missing.termId, missing.sentence, translated)
        }
    }

    /** Translates [sentence] of [termId] and keeps it, if the sentence is still the term's; the translation, or null when none came. */
    suspend fun make(termId: Long, sentence: String, language: Language): String? {
        val translated = translate(sentence.trim(), language) ?: return null
        terms.fillSentenceTranslation(termId, sentence, translated)
        return translated
    }

    private suspend fun translate(sentence: String, language: Language): String? = try {
        translator.translate(sentence, language)?.trim()?.takeIf { it.isNotEmpty() }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
