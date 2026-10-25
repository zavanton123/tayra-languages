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

    /**
     * Fills the languages being learned the first time they are asked for: those with texts or
     * saved words, and the current one; the native language never.
     */
    suspend fun fillMine() {
        if (settings.current.myLanguages.isNotBlank()) return
        val every = languages.getAll().filterNot(::isNative)
        val withTexts = books.getBooks().map { it.languageId }.toSet()
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
            s.copy(currentLanguageId = current, myLanguages = encode(rest))
        }
    }

    private fun withMine(text: String, id: Long): String = decode(text).let { if (id in it) text else encode(it + id) }

    private fun decode(text: String): List<Long> = text.split(',').mapNotNull { it.trim().toLongOrNull() }.distinct()

    private fun encode(ids: List<Long>): String = ids.distinct().joinToString(",")
}
