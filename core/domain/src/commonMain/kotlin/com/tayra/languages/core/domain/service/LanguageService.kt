package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.language.LanguageDefinition
import com.tayra.languages.core.domain.language.PredefinedLanguages
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

    /** Predefined languages not yet in the database. */
    suspend fun predefinedNotLoaded(): List<LanguageDefinition> {
        val existing = languages.getAll().map { it.name.lowercase() }.toSet()
        return predefined().filter { it.name.lowercase() !in existing }
    }

    /** Loads a predefined language and its sample stories; returns the language id. */
    suspend fun loadPredefined(name: String): Long {
        val definition = predefined(name) ?: throw NoSuchElementException("No predefined language '$name'")
        val languageId = languages.findByName(name)?.id ?: languages.save(definition.language)
        for (story in definition.stories) {
            bookService.create(
                BookDraft(
                    languageId = languageId,
                    title = story.title,
                    text = story.text,
                    sourceUri = story.sourceUrl ?: "",
                ),
            )
        }
        settings.update { it.copy(currentLanguageId = languageId) }
        return languageId
    }

    suspend fun save(language: Language): Long {
        validate(language)
        val duplicate = languages.findByName(language.name.trim())
        if (duplicate != null && duplicate.id != language.id) {
            throw LanguageValidationException("Language ${language.name} already exists")
        }
        val id = languages.save(language.copy(name = language.name.trim()))
        if (language.id == 0L) {
            // Force the user to re-pick the default language filter after adding one.
            settings.update { it.copy(currentLanguageId = 0) }
        }
        return id
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

    suspend fun delete(languageId: Long) {
        languages.delete(languageId)
        if (settings.current.currentLanguageId == languageId) settings.update { it.copy(currentLanguageId = 0) }
    }
}
