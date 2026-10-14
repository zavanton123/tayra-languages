package com.tayra.languages.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.service.DictionaryService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Lookups that need a language's spelling variants and inflection rules, as Yomitan applies them. */
class MorphologyLookupTest {
    /** A pack of [entries] (word, part of speech, gloss) and [forms] (form, lemma) for [source] to English. */
    private suspend fun service(source: String, entries: List<Triple<String, String, String>>, forms: List<Pair<String, String>> = emptyList()): Pair<DictionaryService, DictionaryId> {
        val file = File.createTempFile("tayra-morph", ".sqlite").also { it.delete() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        val format = DictionaryId.FORMAT
        fun q(s: String) = "'" + s.replace("'", "''") + "'"
        listOf(
            "CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE TABLE entries (id INTEGER NOT NULL PRIMARY KEY, word TEXT NOT NULL, word_lc TEXT NOT NULL, pos TEXT NOT NULL, ipa TEXT, senses TEXT NOT NULL)",
            "CREATE TABLE forms (form_lc TEXT NOT NULL, lemma TEXT NOT NULL, generated INTEGER NOT NULL DEFAULT 0)",
            "INSERT INTO meta VALUES ('format', '$format')",
            "PRAGMA user_version = $format",
        ).forEach { driver.execute(null, it, 0) }
        for ((word, pos, gloss) in entries) {
            driver.execute(null, "INSERT INTO entries(word, word_lc, pos, senses) VALUES (${q(word)}, ${q(word.lowercase())}, ${q(pos)}, ${q("[{\"glosses\":[\"$gloss\"]}]")})", 0)
        }
        for ((form, lemma) in forms) driver.execute(null, "INSERT INTO forms VALUES (${q(form)}, ${q(lemma)}, 0)", 0)
        driver.close()
        val packed = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(file.readBytes()) } }.toByteArray()
        file.delete()
        val storage = DictionaryPackStorage(DictionaryDownloader(HttpClient(MockEngine { respond(packed) })), Files.createTempDirectory("tayra-morph").toFile())
        val service = DictionaryService(storage, DictionaryRepositoryImpl(DictionaryDatabaseProvider(storage)))
        val pack = DictionaryPacks.find(source, "en")!!
        service.refresh()
        service.download(pack)
        return service to pack.id
    }

    @Test
    fun marksTheTextHasButThePackLacksAreIgnored() = runTest {
        val (service, la) = service("la", listOf(Triple("puella", "noun", "girl")))
        val lookup = service.lookup(la, "puellā")
        assertEquals(listOf("puella"), lookup.entries.map { it.word })
        assertTrue(lookup.entries.single().isOwnEntry, "another spelling of the word is the word itself")
        assertTrue(service.lookup(la, "puer").isEmpty)
    }

    @Test
    fun anInflectionThePackDoesNotListFindsItsDictionaryWord() = runTest {
        val (service, en) = service("en", listOf(Triple("walk", "verb", "to go on foot"), Triple("happy", "adj", "glad")))
        val walking = service.lookup(en, "walking")
        assertEquals(listOf("walk"), walking.lemmas)
        assertEquals("walk", walking.parentSuggestion)
        assertTrue(!walking.entries.single().isOwnEntry)
        assertEquals("happy", service.lookup(en, "happiest").parentSuggestion)
        // A rule only leads to a word of the part of speech it makes: "walker" is not a form of an adjective.
        assertTrue(service.lookup(en, "happied").isEmpty)
    }
}
