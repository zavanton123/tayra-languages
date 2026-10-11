package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.language.RetiredSamples
import com.tayra.languages.core.domain.language.Tutorials
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.DatabaseMaintenance
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository

/**
 * Keeps the database in step with the language catalog. On first start every language arrives with
 * its tutorial book. Databases from before the tutorial lose the sample books they came with and
 * get the tutorial once.
 */
class DemoDataService(
    private val maintenance: DatabaseMaintenance,
    private val languages: LanguageRepository,
    private val books: BookRepository,
    private val languageService: LanguageService,
    private val settings: SettingsRepository,
) {
    suspend fun ensureLanguages() {
        val firstStart = !maintenance.hasAnyLanguage()
        languageService.syncWithCatalog()
        // Also after restoring an older backup, which brings its samples back.
        removeRetiredSamples()
        if (firstStart) {
            settings.update { it.copy(currentLanguageId = 0, tutorialBooksAdded = true) }
        } else if (!settings.current.tutorialBooksAdded) {
            addMissingTutorials()
            settings.update { it.copy(tutorialBooksAdded = true) }
        }
    }

    /** Removes every book and term, then recreates the catalog languages without tutorial books. */
    suspend fun wipeDatabase() {
        maintenance.wipeAllData()
        settings.update { it.copy(currentLanguageId = 0, tutorialBooksAdded = true) }
        languageService.syncWithCatalog(withTutorial = false)
    }

    /** Deletes the sample books the languages used to come with, where they still hold that sample. */
    private suspend fun removeRetiredSamples() {
        val byName = languages.getAll().associateBy { it.name }
        for (sample in RetiredSamples.all) {
            val language = byName[sample.language] ?: continue
            val book = books.findByTitle(sample.title, language.id) ?: continue
            val opening = books.getPage(book.id, 1)?.text ?: continue
            if (RetiredSamples.opening(opening) == sample.opening) books.deleteBook(book.id)
        }
    }

    private suspend fun addMissingTutorials() {
        for (language in languages.getAll()) {
            val tutorial = Tutorials.forLanguage(language.name) ?: continue
            if (books.findByTitle(tutorial.title, language.id) == null) languageService.addTutorial(language.id, language.name)
        }
    }
}
