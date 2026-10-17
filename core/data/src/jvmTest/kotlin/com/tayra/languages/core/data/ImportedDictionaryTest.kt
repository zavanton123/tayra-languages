package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.dictionary.SettingsImportedDictionaryStore
import com.tayra.languages.core.data.mdict.DictionaryFiles
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.data.yomitan.YomitanConverter
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryImporter
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.ImportedDictionaries
import com.tayra.languages.core.domain.dictionary.KanjiInfo
import com.tayra.languages.core.domain.dictionary.WordFrequency
import com.tayra.languages.core.domain.dictionary.PackState
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
import kotlin.test.assertTrue

class ImportedDictionaryTest {

    private fun zipOf(vararg files: Pair<String, String>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { zip ->
            for ((name, text) in files) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
    }.toByteArray()

    private val zip = zipOf(
        "index.json" to """{"title": "My Russian Dictionary", "revision": "3", "format": 3}""",
        "term_bank_1.json" to """[["дом", "", "n", "", 0, ["house", "home"], 1, ""], ["дома", "", "non-lemma", "", 0, [["дом", ["genitive"]]], 2, ""]]""",
    )

    private val settings = MapSettings()
    private val directory: File = Files.createTempDirectory("tayra-imported").toFile()

    private fun service(importer: DictionaryImporter = YomitanConverter()): Pair<DictionaryService, ImportedDictionaries> {
        val storage = DictionaryPackStorage(DictionaryDownloader(HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })), directory)
        val imported = ImportedDictionaries(SettingsImportedDictionaryStore(settings))
        val provider = DictionaryDatabaseProvider(storage) { id -> DictionaryPacks.find(id) ?: imported.pack(id) }
        return DictionaryService(storage, DictionaryRepositoryImpl(provider), imported, importer) to imported
    }

    @Test
    fun anMdictDictionaryIsImportedBesideAYomitanOne() = runTest {
        val (service, _) = service(DictionaryFiles())
        service.refresh()
        service.import(zip, "ru", "en")
        val mdx = MdictTest.Mdx(listOf("дом" to "<b>дом</b><br>house (MDict)<br>building", "дома" to "@@@LINK=дом"), encrypted = 2).build()
        assertEquals("Português–English", service.inspect(mdx).title)
        val dictionary = service.import(mdx, "ru", "en")
        assertEquals("portugu-s-english", dictionary.key)
        // Both answer for the pair, the MDict one finding the word from its form too.
        val found = service.lookup(DictionaryId("ru", "en"), "дома").entries
        assertEquals(listOf("My Russian Dictionary" to listOf("house", "home"), "Português–English" to listOf("house (MDict)", "building")),
            found.map { it.dictionary to it.senses.flatMap { sense -> sense.glosses } })
    }

    @Test
    fun anImportedDictionaryAnswersWithThePackOfItsPair() = runTest {
        val (service, _) = service()
        service.refresh()
        val dictionary = service.import(zip, "ru", "en")
        assertEquals("my-russian-dictionary", dictionary.key)
        assertTrue(service.packs.value.any { it.pack.id == dictionary.id && it.state is PackState.Installed })

        // The app's own ru-en pack is not installed; the imported dictionary still answers, a form finding its word.
        val found = service.lookup(DictionaryId("ru", "en"), "дома")
        assertEquals(listOf("дом"), found.entries.map { it.word })
        assertEquals("My Russian Dictionary", found.entries.single().dictionary)
        assertEquals(listOf("house", "home"), found.entries.single().senses.map { it.glosses.single() })

        // It is there after a restart, and gone once removed.
        val (again, _) = service()
        again.refresh()
        assertEquals("дом", again.lookup(DictionaryId("ru", "en"), "дом").entries.single().word)
        again.remove(again.packs.value.first { it.pack.id == dictionary.id }.pack)
        assertTrue(again.lookup(DictionaryId("ru", "en"), "дом").isEmpty)
        assertTrue(again.packs.value.none { it.pack.id.imported != null })
        assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith("imported-") })
    }

    @Test
    fun frequencyAndKanjiDictionariesAddToTheLookupsOfTheirPair() = runTest {
        val (service, _) = service()
        service.refresh()
        service.import(zip, "ru", "en")
        service.import(zipOf(
            "index.json" to """{"title": "Ranks", "revision": "1", "format": 3, "frequencyMode": "rank-based"}""",
            "term_meta_bank_1.json" to """[["дом", "freq", 120]]""",
        ), "ru", "en")
        val house = service.lookup(DictionaryId("ru", "en"), "дом").entries.single()
        assertEquals(listOf(WordFrequency("Ranks", 120, null, rankBased = true)), house.frequencies)
        assertEquals("#120", house.frequencies.single().label)

        service.import(zipOf(
            "index.json" to """{"title": "Kanji", "revision": "1", "format": 3}""",
            "kanji_bank_1.json" to """[["猫", "ビョウ", "ねこ", "", ["cat"], {"strokes": "11", "grade": "8", "jlpt": "2"}]]""",
        ), "ja", "en")
        assertEquals(listOf(KanjiInfo("猫", listOf("cat"), listOf("ビョウ"), listOf("ねこ"), 11, 8, 2, null)), service.kanji(DictionaryId("ja", "en"), "猫が"))
    }
}
