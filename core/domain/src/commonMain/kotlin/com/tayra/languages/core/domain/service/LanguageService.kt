package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.language.LanguageCatalog
import com.tayra.languages.core.domain.language.LanguageDefinition
import com.tayra.languages.core.domain.language.PredefinedLanguages
import com.tayra.languages.core.domain.language.Tutorials
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.parse.isSupported
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository

class LanguageValidationException(message: String) : Exception(message)

class LanguageService(
    private val languages: LanguageRepository,
    private val bookService: BookService,
    private val settings: SettingsRepository,
) {

    /** Predefined languages supported on this platform, sorted by name. */
    fun predefined(): List<LanguageDefinition> =
        PredefinedLanguages.all.filter { it.language.isSupported }.sortedBy { it.name }

    fun predefined(name: String): LanguageDefinition? = predefined().firstOrNull { it.name == name }

    /**
     * Makes the database match the catalog: missing target languages are created, optionally
     * with their tutorial book, and languages outside the catalog are removed with their books
     * and terms.
     */
    suspend fun syncWithCatalog(withTutorial: Boolean = true) {
        val existing = languages.getAll()
        for (language in existing) {
            if (!LanguageCatalog.isTarget(language.name)) delete(language.id)
        }
        val names = existing.map { it.name.lowercase() }.toSet()
        for (name in LanguageCatalog.targetLanguages) {
            if (name.lowercase() !in names) loadPredefined(name, withTutorial)
        }
    }

    /** Loads a predefined language, with its tutorial book when asked; returns the language id. */
    suspend fun loadPredefined(name: String, withTutorial: Boolean = true): Long {
        val definition = predefined(name) ?: throw NoSuchElementException("No predefined language '$name'")
        val languageId = languages.findByName(name)?.id ?: languages.save(definition.language)
        if (withTutorial) addTutorial(languageId, name)
        return languageId
    }

    /** Adds the tutorial book, in the language itself, to the language named [name]; returns its id, or null without one. */
    suspend fun addTutorial(languageId: Long, name: String): Long? {
        val tutorial = Tutorials.forLanguage(name) ?: return null
        return bookService.create(BookDraft(languageId = languageId, title = tutorial.title, text = tutorial.text))
    }

    /** Saves the settings of an existing language; the catalog decides which languages exist. */
    suspend fun save(language: Language): Long {
        validate(language)
        val existing = languages.getById(language.id)
            ?: throw LanguageValidationException("Only the languages in the catalog can be used")
        if (LanguageCatalog.isTarget(existing.name) && !existing.name.equals(language.name.trim(), ignoreCase = true)) {
            throw LanguageValidationException("${existing.name} cannot be renamed")
        }
        val duplicate = languages.findByName(language.name.trim())
        if (duplicate != null && duplicate.id != language.id) {
            throw LanguageValidationException("Language ${language.name} already exists")
        }
        return languages.save(language.copy(name = language.name.trim()))
    }

    fun validate(language: Language) {
        if (language.name.isBlank()) throw LanguageValidationException("Name is required")
        if (!language.isSupported) throw LanguageValidationException("Parser '${language.parserType}' is not supported")
        if (language.activeDictionaries(DictionaryUse.TERMS).isEmpty()) {
            throw LanguageValidationException("Please add an active Terms dictionary")
        }
        if (language.activeDictionaries(DictionaryUse.SENTENCES).isEmpty()) {
            throw LanguageValidationException("Please add an active Sentences dictionary")
        }
        if (language.dictionaries.any { it.url.isBlank() }) {
            throw LanguageValidationException("Dictionary URL is required")
        }
    }

    private suspend fun delete(languageId: Long) {
        languages.delete(languageId)
        if (settings.current.currentLanguageId == languageId) settings.update { it.copy(currentLanguageId = 0) }
    }
}
