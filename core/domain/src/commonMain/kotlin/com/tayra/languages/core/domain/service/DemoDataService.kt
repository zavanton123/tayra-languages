package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.DatabaseMaintenance
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository

/**
 * Keeps the database in step with the language catalog. On first start the languages arrive
 * with their sample stories and the tutorial, and the home page explains the demo data until
 * dismissed.
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
        if (firstStart) settings.update { it.copy(demoDataLoaded = true, currentLanguageId = 0) }
    }

    suspend fun tutorialBookId(): Long? {
        if (!isDemoData) return null
        val english = languages.findByName("English") ?: return null
        return books.findByTitle(TUTORIAL_TITLE, english.id)?.id
    }

    suspend fun dismissDemoFlag() {
        settings.update { it.copy(demoDataLoaded = false) }
    }

    /** Removes every book and term, then recreates the catalog languages without sample stories. */
    suspend fun wipeDatabase() {
        maintenance.wipeAllData()
        settings.update { it.copy(demoDataLoaded = false, currentLanguageId = 0) }
        languageService.syncWithCatalog(withStories = false)
    }

    companion object {
        const val TUTORIAL_TITLE = "Tutorial"
    }
}
