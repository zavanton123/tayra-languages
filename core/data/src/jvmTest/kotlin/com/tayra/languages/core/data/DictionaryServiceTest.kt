package com.tayra.languages.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.service.DictionaryService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
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
    private val pack = DictionaryPacks.all.single()
    private val enRu = pack.id

    /** Builds a gzip-compressed pack the way tools/build_english_russian_dictionary.py lays it out. */
    private fun buildPack(format: Int = DictionaryId.FORMAT): ByteArray {
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

    private class Env(directory: File, body: ByteArray?, status: HttpStatusCode = HttpStatusCode.OK) {
        var requests = 0
        private val client = HttpClient(MockEngine { request ->
            requests++
            if (body == null) respondError(status) else respond(body, status)
        })
        val storage = DictionaryPackStorage(DictionaryDownloader(client), directory)
        val service = DictionaryService(storage, DictionaryRepositoryImpl(DictionaryDatabaseProvider(storage)))
    }

    @Test
    fun downloadsInstallsAndResolvesForms() = runTest {
        val directory = Files.createTempDirectory("tayra-dict").toFile()
        val env = Env(directory, buildPack())
        env.service.refresh()
        assertEquals(PackState.NotInstalled, env.service.packs.value.single().state)
        assertTrue(!env.service.isAvailable(enRu))

        env.service.download(pack)
        val installed = env.service.packs.value.single().state
        assertTrue(installed is PackState.Installed && installed.sizeBytes > 0, installed.toString())
        assertEquals(1, env.requests)
        assertTrue(File(directory, "en-ru.sqlite").isFile)
        assertTrue(env.service.isAvailable(enRu))

        val cats = env.service.lookup(enRu, "Cats")
        assertEquals(listOf("cat"), cats.lemmas)
        assertEquals("cat", cats.parentSuggestion)
        assertEquals("кошка, кот", cats.suggestedTranslation)
        assertEquals(listOf("derogatory"), cats.entries.single().senses[1].tags)
        assertEquals("[kæt]", cats.entries.single().ipa)
        assertTrue(!cats.entries.single().isOwnEntry)
        assertEquals("do", env.service.lookup(enRu, "done").parentSuggestion)

        val left = env.service.lookup(enRu, "left")
        assertEquals(listOf("left", "leave"), left.entries.map { it.word })
        assertNull(left.parentSuggestion, "a headword of its own is not given a parent")

        assertEquals(listOf("go", "wend"), env.service.lookup(enRu, "went").lemmas)
        assertNull(env.service.lookup(enRu, "went").parentSuggestion, "ambiguous forms get no parent")

        val words = env.service.lookup(enRu, "words")
        assertEquals(listOf("word"), words.lemmas, "the common noun beats the surname for a lowercase word")
        assertEquals("слово", words.suggestedTranslation)
        assertEquals(listOf("Word"), env.service.lookup(enRu, "Words").lemmas)
        assertTrue(env.service.lookup(enRu, "xyzzy").isEmpty)

        // A fresh service over the same directory sees the pack without downloading again.
        val again = Env(directory, null)
        again.service.refresh()
        assertTrue(again.service.packs.value.single().state is PackState.Installed)
        assertEquals("слово", again.service.lookup(enRu, "words").suggestedTranslation)
        assertEquals(0, again.requests)

        again.service.remove(pack)
        assertEquals(PackState.NotInstalled, again.service.packs.value.single().state)
        assertTrue(!File(directory, "en-ru.sqlite").exists())
        assertTrue(again.service.lookup(enRu, "cat").isEmpty)
    }

    @Test
    fun failedDownloadsAndForeignFilesAreNotInstalled() = runTest {
        val failing = Env(Files.createTempDirectory("tayra-dict").toFile(), null, HttpStatusCode.NotFound)
        failing.service.download(pack)
        assertTrue(failing.service.packs.value.single().state is PackState.Failed)
        assertTrue(!failing.service.isAvailable(enRu))

        val foreign = Env(Files.createTempDirectory("tayra-dict").toFile(), buildPack(format = 99))
        foreign.service.download(pack)
        assertTrue(foreign.service.packs.value.single().state is PackState.Installed)
        assertTrue(!foreign.service.isAvailable(enRu), "an unknown format is installed but unusable")
        assertTrue(foreign.service.lookup(enRu, "cat").isEmpty)
    }
}
