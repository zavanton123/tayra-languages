package com.tayra.languages.core.domain.service

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
     * of the book read last, else of any book, else the first by name. Returns its id, or 0 when
     * there are no languages.
     */
    suspend fun ensure(): Long {
        val all = languages.getAll()
        val current = settings.current.currentLanguageId
        if (all.any { it.id == current }) return current
        val shelf = books.observeBooks(archived = false).first().filter { book -> all.any { it.id == book.languageId } }
        val pick = shelf.filter { it.lastOpened != null }.maxByOrNull { it.lastOpened!! }?.languageId
            ?: shelf.firstOrNull()?.languageId
            ?: all.minByOrNull { it.name }?.id
            ?: 0L
        settings.update { it.copy(currentLanguageId = pick) }
        return pick
    }

    suspend fun select(languageId: Long) {
        settings.update { it.copy(currentLanguageId = languageId) }
    }
}
