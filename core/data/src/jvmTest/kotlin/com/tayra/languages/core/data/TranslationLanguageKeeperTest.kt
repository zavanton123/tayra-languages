package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.TayraDatabase
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.TermTranslationProvider
import com.tayra.languages.core.domain.service.TranslationLanguageKeeper
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslationLanguageKeeperTest {

    private class Env(val file: File = File.createTempFile("tayra-keeper", ".db").also { it.delete() }) {
        val settings = SettingsRepositoryImpl(MapSettings())
        val provider = DatabaseProvider(DatabaseDriverFactory(file))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider, translationLanguage = { settings.current.nativeLanguage })
        val asked = mutableListOf<String>()
        val engine = object : TermTranslationProvider {
            override val name = "Fake"
            override suspend fun suggestTranslation(text: String, language: Language): String? {
                asked += text
                return mapOf("maldição" to "проклятие", "no meu país" to "в моей стране")[text]
            }
        }
        val dictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = dictionary == DictionaryId("pt", "ru")
            override suspend fun lookup(dictionary: DictionaryId, text: String) =
                if (text == "ser") DictionaryLookup(listOf(DictionaryEntry("ser", "verb", null, listOf(DictionarySense(listOf("быть"))))), listOf("ser"))
                else DictionaryLookup.EMPTY
        }
        val keeper = TranslationLanguageKeeper(terms, languages, dictionary, engine, settings)

        suspend fun native(code: String) = settings.update { it.copy(nativeLanguage = code) }
        suspend fun portuguese() = languages.save(Language(name = "Portuguese"))
        suspend fun term(languageId: Long, text: String, translation: String?) =
            terms.save(Term(languageId = languageId, text = text, textLc = text.lowercase(), translation = translation))
        fun column(id: Long): String? = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            c.createStatement().executeQuery("SELECT translation_language FROM terms WHERE id = $id").let { it.next(); it.getString(1) }
        }
    }

    @Test
    fun translationsFromTheOldNativeLanguageAreReplacedWhenItChanges() = runTest {
        val env = Env()
        env.native("en")
        val pt = env.portuguese()
        val curse = env.term(pt, "maldição", "curse")
        val ser = env.term(pt, "ser", "to be")
        val untranslatable = env.term(pt, "sétimo", "seventh")
        val blank = env.term(pt, "que", null)
        assertEquals("en", env.column(curse))
        assertEquals(null, env.column(blank))

        env.native("ru")
        assertEquals(2, env.keeper.update("ru"))

        assertEquals("проклятие", env.terms.getById(curse)?.translation)
        assertEquals("ru", env.column(curse))
        // The offline dictionary answers before the engine is asked.
        assertEquals("быть", env.terms.getById(ser)?.translation)
        assertTrue("ser" !in env.asked)
        // Nothing to replace it with: kept, and still due for replacement next time.
        assertEquals("seventh", env.terms.getById(untranslatable)?.translation)
        assertEquals("en", env.column(untranslatable))
        assertEquals(null, env.terms.getById(blank)?.translation)
    }

    @Test
    fun savingOtherFieldsKeepsTheLanguageOfAnUnchangedTranslation() = runTest {
        val env = Env()
        env.native("en")
        val pt = env.portuguese()
        val id = env.term(pt, "maldição", "curse")
        env.native("ru")

        env.terms.save(env.terms.getById(id)!!.copy(status = TermStatus.LEARNING_4))
        assertEquals("en", env.column(id))

        env.terms.save(env.terms.getById(id)!!.copy(translation = "проклятие"))
        assertEquals("ru", env.column(id))
    }

    @Test
    fun untrackedTranslationsAreJudgedByScript() = runTest {
        val env = Env()
        val pt = env.portuguese()
        val russian = env.term(pt, "dizem", "сказать")
        val english = env.term(pt, "no meu país", "in my country")
        DriverManager.getConnection("jdbc:sqlite:${env.file.absolutePath}").use { it.createStatement().execute("UPDATE terms SET translation_language = NULL") }

        env.native("ru")
        env.keeper.update("ru")

        assertEquals("сказать", env.terms.getById(russian)?.translation)
        assertEquals("ru", env.column(russian))
        assertEquals("в моей стране", env.terms.getById(english)?.translation)
        assertEquals("ru", env.column(english))
        assertEquals(listOf("no meu país"), env.asked)
    }

    @Test
    fun anOlderDatabaseGainsTheColumnWithItsTranslationsUntracked() = runTest {
        val env = Env()
        val pt = env.portuguese()
        val id = env.term(pt, "maldição", "curse")
        DriverManager.getConnection("jdbc:sqlite:${env.file.absolutePath}").use {
            it.createStatement().execute("ALTER TABLE terms DROP COLUMN translation_language")
            it.createStatement().execute("ALTER TABLE terms DROP COLUMN sentence")
            it.createStatement().execute("PRAGMA user_version = 5")
        }

        val reopened = Env(env.file)
        assertEquals("curse", reopened.terms.getById(id)?.translation)
        assertEquals(null, reopened.column(id))
        DriverManager.getConnection("jdbc:sqlite:${env.file.absolutePath}").use { c ->
            assertEquals(TayraDatabase.Schema.version, c.createStatement().executeQuery("PRAGMA user_version").let { it.next(); it.getLong(1) })
        }
    }
}
