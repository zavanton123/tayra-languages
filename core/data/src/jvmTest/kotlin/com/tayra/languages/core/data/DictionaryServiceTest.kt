package com.tayra.languages.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDriverFactory
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.service.DictionaryService
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DictionaryServiceTest {
    private val enRu = DictionaryId("en", "ru")

    /** Builds a gzip-compressed dictionary file the way tools/build_english_russian_dictionary.py lays it out. */
    private fun buildFile(format: Int = DictionaryId.FORMAT): ByteArray {
        val file = File.createTempFile("tayra-dict-src", ".sqlite").also { it.delete() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        listOf(
            "CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE TABLE entries (id INTEGER NOT NULL PRIMARY KEY, word TEXT NOT NULL, word_lc TEXT NOT NULL, pos TEXT NOT NULL, ipa TEXT, senses TEXT NOT NULL)",
            "CREATE TABLE forms (form_lc TEXT NOT NULL, lemma TEXT NOT NULL, generated INTEGER NOT NULL DEFAULT 0)",
            "CREATE INDEX entries_word_lc ON entries(word_lc)",
            "CREATE INDEX forms_form_lc ON forms(form_lc)",
            "INSERT INTO meta VALUES ('format', '$format'), ('source_language', 'en'), ('target_language', 'ru')",
            """INSERT INTO entries(word, word_lc, pos, ipa, senses) VALUES
                ('Word', 'word', 'noun', NULL, '[{"glosses":["английская фамилия"]}]'),
                ('word', 'word', 'noun', NULL, '[{"glosses":["слово"]}]'),
                ('cat', 'cat', 'noun', '[kæt]', '[{"glosses":["кошка, кот"]},{"glosses":["сварливая женщина"],"tags":["derogatory"]}]'),
                ('do', 'do', 'verb', NULL, '[{"glosses":["делать"]}]'),
                ('left', 'left', 'adj', NULL, '[{"glosses":["левый"]}]'),
                ('leave', 'leave', 'verb', NULL, '[{"glosses":["уходить"]}]'),
                ('go', 'go', 'verb', NULL, '[{"glosses":["идти"]}]'),
                ('wend', 'wend', 'verb', NULL, '[{"glosses":["направляться"]}]')""",
            """INSERT INTO forms VALUES ('cats', 'cat', 1), ('did', 'do', 0), ('done', 'do', 0), ('words', 'Word', 0), ('words', 'word', 0),
                ('left', 'leave', 0), ('went', 'go', 0), ('went', 'wend', 0)""",
            "PRAGMA user_version = $format",
        ).forEach { driver.execute(null, it, 0) }
        driver.close()
        val packed = ByteArrayOutputStream()
        GZIPOutputStream(packed).use { it.write(file.readBytes()) }
        file.delete()
        return packed.toByteArray()
    }

    private class FakeAssets(private val bytes: ByteArray?) : DictionaryAssets {
        var reads = 0
        override suspend fun readBytes(dictionary: DictionaryId): ByteArray? = bytes.also { reads++ }
        override fun uri(dictionary: DictionaryId): String? = null
    }

    private fun service(directory: File, assets: DictionaryAssets): DictionaryService =
        DictionaryService(DictionaryRepositoryImpl(DictionaryDatabaseProvider(DictionaryDriverFactory(directory), assets)))

    @Test
    fun copiesTheFileOnceAndResolvesForms() = runTest {
        val directory = Files.createTempDirectory("tayra-dict").toFile()
        val assets = FakeAssets(buildFile())
        val service = service(directory, assets)
        assertTrue(service.isAvailable(enRu))
        assertEquals(DictionaryId.FORMAT.toString(), File(directory, "en-ru.sqlite.format").readText())

        val cats = service.lookup(enRu, "Cats")
        assertEquals(listOf("cat"), cats.lemmas)
        assertEquals("cat", cats.parentSuggestion)
        assertEquals("кошка, кот", cats.suggestedTranslation)
        assertEquals(listOf("derogatory"), cats.entries.single().senses[1].tags)
        assertEquals("[kæt]", cats.entries.single().ipa)
        assertTrue(!cats.entries.single().isOwnEntry)

        assertEquals("do", service.lookup(enRu, "done").parentSuggestion)

        val left = service.lookup(enRu, "left")
        assertEquals(listOf("left", "leave"), left.entries.map { it.word })
        assertNull(left.parentSuggestion, "a headword of its own is not given a parent")
        assertEquals("левый", left.suggestedTranslation)

        val went = service.lookup(enRu, "went")
        assertEquals(listOf("go", "wend"), went.lemmas)
        assertNull(went.parentSuggestion, "ambiguous forms get no parent")

        val words = service.lookup(enRu, "words")
        assertEquals(listOf("word"), words.lemmas, "the common noun beats the surname for a lowercase word")
        assertEquals("слово", words.suggestedTranslation)
        assertEquals(listOf("Word"), service.lookup(enRu, "Words").lemmas)
        assertEquals(listOf("word", "Word"), service.lookup(enRu, "word").entries.map { it.word })

        assertTrue(service.lookup(enRu, "xyzzy").isEmpty)
        assertTrue(service.lookup(enRu, "  ").isEmpty)
        assertEquals(1, assets.reads)

        // A second provider over the same directory reuses the copied file instead of the asset.
        val untouched = FakeAssets(null)
        assertTrue(service(directory, untouched).isAvailable(enRu))
        assertEquals(0, untouched.reads)
    }

    @Test
    fun missingOrForeignFilesAreUnavailable() = runTest {
        val missing = service(Files.createTempDirectory("tayra-dict").toFile(), FakeAssets(null))
        assertTrue(!missing.isAvailable(enRu))
        assertTrue(missing.lookup(enRu, "cat").isEmpty)

        val foreign = service(Files.createTempDirectory("tayra-dict").toFile(), FakeAssets(buildFile(format = 99)))
        assertTrue(!foreign.isAvailable(enRu))
    }
}
