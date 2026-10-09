package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/** The one language being learned right now, which the book and vocabulary lists follow. */
class LearningLanguageService(
    private val settings: SettingsRepository,
    private val languages: LanguageRepository,
    private val books: BookRepository,
) {
    /**
     * Makes sure a language that still exists is chosen, picking one when none is: the language
     * of the book read last, else of any book, else the first by name, passing over the native
     * language while another one is there. Returns its id, or 0 when there are no languages.
     */
    suspend fun ensure(): Long {
        val every = languages.getAll()
        val current = settings.current.currentLanguageId
        if (every.any { it.id == current }) return current
        val all = every.filterNot { isNative(it) }.ifEmpty { every }
        val shelf = books.observeBooks(archived = false).first().filter { book -> all.any { it.id == book.languageId } }
        val pick = shelf.filter { it.lastOpened != null }.maxByOrNull { it.lastOpened!! }?.languageId
            ?: shelf.firstOrNull()?.languageId
            ?: all.minByOrNull { it.name }?.id
            ?: 0L
        settings.update { it.copy(currentLanguageId = pick) }
        return pick
    }

    /** Makes [languageId] the language being learned; false, and nothing changes, when it is the native language. */
    suspend fun select(languageId: Long): Boolean {
        if (languages.getById(languageId)?.let(::isNative) == true) return false
        settings.update { it.copy(currentLanguageId = languageId) }
        return true
    }

    /** Whether [language] is the one meanings are shown in, which cannot be learned at the same time. */
    fun isNative(language: Language): Boolean = LanguageCodes.codeFor(language.name) == settings.current.nativeLanguage
}
