package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.language.RetiredSamples
import com.tayra.languages.core.domain.language.Tutorials
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.DatabaseMaintenance
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository

/**
 * Keeps the database in step with the language catalog. On first start every language arrives with
 * its tutorial book, and the home page explains the demo data until dismissed. Databases from before
 * the tutorial lose the sample books they came with and get the tutorial once.
 */
class DemoDataService(
    private val maintenance: DatabaseMaintenance,
    private val languages: LanguageRepository,
    private val books: BookRepository,
    private val languageService: LanguageService,
    private val settings: SettingsRepository,
) {
    val isDemoData: Boolean get() = settings.current.demoDataLoaded

    suspend fun ensureLanguages() {
        val firstStart = !maintenance.hasAnyLanguage()
        languageService.syncWithCatalog()
        // Also after restoring an older backup, which brings its samples back.
        removeRetiredSamples()
        if (firstStart) {
            settings.update { it.copy(demoDataLoaded = true, currentLanguageId = 0, tutorialBooksAdded = true) }
        } else if (!settings.current.tutorialBooksAdded) {
            addMissingTutorials()
            settings.update { it.copy(tutorialBooksAdded = true) }
        }
    }

    /** The tutorial book of the language being learned, while the demo data is shown. */
    suspend fun tutorialBookId(): Long? {
        if (!isDemoData) return null
        val language = languages.getById(settings.current.currentLanguageId) ?: languages.findByName("English") ?: return null
        val tutorial = Tutorials.forLanguage(language.name) ?: return null
        return books.findByTitle(tutorial.title, language.id)?.id
    }

    suspend fun dismissDemoFlag() {
        settings.update { it.copy(demoDataLoaded = false) }
    }

    /** Removes every book and term, then recreates the catalog languages without tutorial books. */
    suspend fun wipeDatabase() {
        maintenance.wipeAllData()
        settings.update { it.copy(demoDataLoaded = false, currentLanguageId = 0, tutorialBooksAdded = true) }
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
