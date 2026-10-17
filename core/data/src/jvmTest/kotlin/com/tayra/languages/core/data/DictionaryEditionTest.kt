package com.tayra.languages.core.data

import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.data.yomitan.YomitanConverter
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.service.DictionaryService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** FreeDict's packs: published beside the Wiktionaries' ones, looked up with them, and on their own where a pair has no other. */
class DictionaryEditionTest {

    private val directory: File = Files.createTempDirectory("tayra-editions").toFile()
    private val storage = DictionaryPackStorage(DictionaryDownloader(HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })), directory)

    private fun service() = DictionaryService(storage, DictionaryRepositoryImpl(DictionaryDatabaseProvider(storage) { DictionaryPacks.find(it) }))

    /** A pack of one word and its glosses, made as an imported Yomitan dictionary is. */
    private suspend fun pack(word: String, vararg glosses: String): ByteArray {
        val zip = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("index.json")); zip.write("""{"title": "t", "revision": "1", "format": 3}""".toByteArray()); zip.closeEntry()
                zip.putNextEntry(ZipEntry("term_bank_1.json"))
                zip.write("""[["$word", "", "n", "", 0, [${glosses.joinToString { "\"$it\"" }}], 1, ""]]""".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        return YomitanConverter().convert(zip, "pt", "en")
    }

    @Test
    fun aPairsFreeDictPackIsAnEditionBesideItsOwn() {
        val freedict = DictionaryPacks.find(DictionaryId("pt", "en", edition = DictionaryId.FREEDICT))!!
        assertEquals("pt-en-freedict", freedict.id.name)
        assertEquals("Portuguese → English", freedict.title)
        assertTrue(freedict.url.endsWith("/freedict-v1/pt-en-freedict.sqlite.gzip"))
        assertNull(DictionaryPacks.find("pt", "en")!!.id.edition, "the Wiktionaries' pack stays the pair's dictionary")
        assertEquals(DictionaryId.FREEDICT, DictionaryPacks.find("af", "de")!!.id.edition, "a pair only FreeDict has gets its pack")
        assertEquals("FreeDict", DictionaryPacks.editionName(DictionaryId.FREEDICT))
    }

    @Test
    fun anInstalledEditionAnswersForItsPairAfterThePairsOwnPack() = runTest {
        val own = DictionaryPacks.find("pt", "en")!!
        val freedict = DictionaryPacks.find(DictionaryId("pt", "en", edition = DictionaryId.FREEDICT))!!
        storage.installFile(freedict, pack("casa", "house (FreeDict)"))
        val service = service()
        service.refresh()

        // Only the edition is installed: the pair still answers, from it.
        assertTrue(service.isAvailable(own.id))
        assertEquals(listOf("FreeDict"), service.lookup(own.id, "casa").entries.map { it.dictionary })

        storage.installFile(own, pack("casa", "house", "home"))
        service.refresh()
        val found = service.lookup(own.id, "casa").entries
        assertEquals(listOf(null to listOf("house", "home"), "FreeDict" to listOf("house (FreeDict)")), found.map { it.dictionary to it.senses.flatMap { s -> s.glosses } })
        // Looked up on its own, as when testing it, the edition shows only its entries.
        assertEquals(listOf("house (FreeDict)"), service.lookup(freedict.id, "casa").entries.flatMap { it.senses.flatMap { s -> s.glosses } })

        service.remove(freedict)
        assertEquals(listOf(null), service.lookup(own.id, "casa").entries.map { it.dictionary })
        service.remove(own)
        assertFalse(service.isAvailable(own.id))
    }
}
