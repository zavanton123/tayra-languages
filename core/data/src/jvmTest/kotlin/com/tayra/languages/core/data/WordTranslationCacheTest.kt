package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.SentenceTranslationCacheImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.WordTranslationService
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** A word's translation from the engine is kept in the database, so the next start of the app does not ask for it again. */
class WordTranslationCacheTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-word-cache", ".db").also { it.delete() }))
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val store = SentenceTranslationCacheImpl(provider)
    private val noDictionary = object : OfflineDictionary {
        override suspend fun isAvailable(dictionary: DictionaryId) = false
        override suspend fun lookup(dictionary: DictionaryId, text: String) = error("not installed")
    }
    private var asked = 0
    private val engine = object : TermTranslationProvider {
        override val name = "Fake"
        override suspend fun suggestTranslation(text: String, language: Language): String? { asked++; return "<$text>" }
    }

    @Test
    fun anEnginesWordTranslationOutlastsARestart() = runBlocking {
        val language = Language(id = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese")), name = "Portuguese")
        assertEquals("<lobo>", WordTranslationService(terms, noDictionary, engine, settings, store = store).translate(language, "lobo"))
        // A new service, as after a restart: nothing in memory, the word in the database.
        assertEquals("<lobo>", WordTranslationService(terms, noDictionary, engine, settings, store = store).translate(language, "lobo"))
        assertEquals(1, asked)
    }
}
