package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.DatabaseMaintenance
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository

/**
 * On first start the database is loaded with a tutorial and some sample languages.
 */
class DemoDataService(
    private val maintenance: DatabaseMaintenance,
    private val languages: LanguageRepository,
    private val books: BookRepository,
    private val languageService: LanguageService,
    private val settings: SettingsRepository,
) {
    val isDemoData: Boolean get() = settings.current.demoDataLoaded

    suspend fun loadIfEmpty() {
        if (maintenance.hasAnyLanguage()) return
        val available = languageService.predefined().map { it.name }.toSet()
        for (name in DEMO_LANGUAGES) {
            if (name in available) languageService.loadPredefined(name)
        }
        settings.update { it.copy(demoDataLoaded = true, currentLanguageId = 0) }
    }

    suspend fun tutorialBookId(): Long? {
        if (!isDemoData) return null
        val english = languages.findByName("English") ?: return null
        return books.findByTitle(TUTORIAL_TITLE, english.id)?.id
    }

    suspend fun dismissDemoFlag() {
        settings.update { it.copy(demoDataLoaded = false) }
    }

    suspend fun wipeDatabase() {
        maintenance.wipeAllData()
        settings.update { it.copy(demoDataLoaded = false, currentLanguageId = 0) }
    }

    companion object {
        const val TUTORIAL_TITLE = "Tutorial"
        val DEMO_LANGUAGES = listOf(
            "Arabic", "Classical Chinese", "Czech", "English", "French", "German", "Greek",
            "Hindi", "Japanese", "Russian", "Sanskrit", "Spanish", "Turkish",
        )
    }
}
