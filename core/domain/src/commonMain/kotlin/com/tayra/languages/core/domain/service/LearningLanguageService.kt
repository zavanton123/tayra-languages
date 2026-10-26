package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The one language being learned right now, which the book and vocabulary lists follow, and the
 * few being learned, which the language menu switches between.
 */
class LearningLanguageService(
    private val settings: SettingsRepository,
    private val languages: LanguageRepository,
    private val books: BookRepository,
    /** Tells which languages have saved words, for the languages being learned the first time. */
    private val terms: TermRepository? = null,
) {
    /** The languages being learned, in the order they were added, the current one always among them. */
    fun observeMine(): Flow<List<Long>> = settings.settings.map { s ->
        val mine = decode(s.myLanguages)
        if (s.currentLanguageId == 0L || s.currentLanguageId in mine) mine else mine + s.currentLanguageId
    }.distinctUntilChanged()

    /** The languages being learned now. */
    suspend fun mine(): List<Long> = observeMine().first()

    /** The words known in each language, as last counted while it was the one learned. */
    fun observeKnownWordCounts(): Flow<Map<Long, Int>> = settings.settings.map { counts(it.knownWordCounts) }.distinctUntilChanged()

    /** Keeps [count] as the words known in the language with [languageId], for when another one is learned. */
    suspend fun rememberKnownWords(languageId: Long, count: Int) {
        if (counts(settings.current.knownWordCounts)[languageId] == count) return
        settings.update { it.copy(knownWordCounts = encodeCounts(counts(it.knownWordCounts) + (languageId to count))) }
    }

    /**
     * Fills the languages being learned the first time they are asked for: those with a text that
     * was opened or with saved words, and the current one; the native language never. A text never
     * opened says nothing, as every language starts with its tutorial.
     */
    suspend fun fillMine() {
        if (settings.current.myLanguages.isNotBlank()) return
        val every = languages.getAll().filterNot(::isNative)
        val withTexts = listOf(false, true).flatMap { books.observeBooks(archived = it).first() }.filter { it.lastOpened != null }.map { it.languageId }.toSet()
        val used = every.filter { language ->
            language.id in withTexts || language.id == settings.current.currentLanguageId ||
                terms?.list(TermListFilter(languageId = language.id, minStatus = TermStatus.NEW_1, maxStatus = TermStatus.WELL_KNOWN), TermListSort(), 0, 1)?.items?.isNotEmpty() == true
        }
        if (used.isNotEmpty()) settings.update { it.copy(myLanguages = encode(used.sortedBy { it.name }.map { it.id })) }
    }
    /**
     * Makes sure a language that still exists is chosen, picking one when none is: the language
     * of the book read last, else of any book, else the first by name, passing over the native
     * language while another one is there. Returns its id, or 0 when there are no languages.
     * While none was ever chosen (0, as on first start), one is picked only when [pickUnchosen]:
     * otherwise it is left for the user to choose.
     */
    suspend fun ensure(pickUnchosen: Boolean = true): Long {
        val every = languages.getAll()
        val current = settings.current.currentLanguageId
        if (every.any { it.id == current }) return current
        if (current == 0L && !pickUnchosen) return 0
        val all = every.filterNot { isNative(it) }.ifEmpty { every }
        val shelf = books.observeBooks(archived = false).first().filter { book -> all.any { it.id == book.languageId } }
        val pick = shelf.filter { it.lastOpened != null }.maxByOrNull { it.lastOpened!! }?.languageId
            ?: shelf.firstOrNull()?.languageId
            ?: all.minByOrNull { it.name }?.id
            ?: 0L
        settings.update { s -> s.copy(currentLanguageId = pick, myLanguages = if (s.myLanguages.isBlank() || pick == 0L) s.myLanguages else withMine(s.myLanguages, pick)) }
        return pick
    }

    /** Makes [languageId] the language being learned; false, and nothing changes, when it is the native language. */
    suspend fun select(languageId: Long): Boolean {
        if (languages.getById(languageId)?.let(::isNative) == true) return false
        // A language chosen is one being learned from now on, beside those learned before.
        fillMine()
        settings.update { s -> s.copy(currentLanguageId = languageId, myLanguages = withMine(s.myLanguages, languageId)) }
        return true
    }

    /** Whether [language] is the one meanings are shown in, which cannot be learned at the same time. */
    fun isNative(language: Language): Boolean = LanguageCodes.codeFor(language.name) == settings.current.nativeLanguage

    /**
     * Stops learning the language with [languageId]: it leaves the languages being learned, and when
     * it was the current one, the first of the rest takes its place, or none, to be asked for.
     */
    suspend fun forget(languageId: Long) {
        fillMine()
        settings.update { s ->
            val rest = decode(s.myLanguages) - languageId
            val current = if (s.currentLanguageId == languageId) rest.firstOrNull() ?: 0L else s.currentLanguageId
            s.copy(currentLanguageId = current, myLanguages = encode(rest), knownWordCounts = encodeCounts(counts(s.knownWordCounts) - languageId))
        }
    }

    private fun counts(text: String): Map<Long, Int> = text.split(',').mapNotNull { pair ->
        val (id, count) = pair.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        (id.trim().toLongOrNull() ?: return@mapNotNull null) to (count.trim().toIntOrNull() ?: return@mapNotNull null)
    }.toMap()

    private fun encodeCounts(counts: Map<Long, Int>): String = counts.entries.joinToString(",") { "${it.key}=${it.value}" }

    private fun withMine(text: String, id: Long): String = decode(text).let { if (id in it) text else encode(it + id) }

    private fun decode(text: String): List<Long> = text.split(',').mapNotNull { it.trim().toLongOrNull() }.distinct()

    private fun encode(ids: List<Long>): String = ids.distinct().joinToString(",")
}
