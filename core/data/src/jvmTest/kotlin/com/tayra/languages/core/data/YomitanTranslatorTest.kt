package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.dictionary.SettingsImportedDictionaryStore
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.data.yomitan.YomitanConverter
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.ImportedDictionaries
import com.tayra.languages.core.domain.service.DictionaryService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Yomitan's own translator tests (test/data/translator-test-*.json and its test dictionary, GPL-3.0),
 * run through the app: the dictionary is imported with [YomitanConverter], and each term lookup must
 * find the words Yomitan finds for the whole text, with their readings and one of the inflection
 * chains Yomitan gives. The test dictionary holds words of several languages, so it is imported once
 * for each language the tests look words up in.
 */
class YomitanTranslatorTest {

    /** Tests of options the app has no counterpart for, by name, with the reason. */
    private val skipped = mapOf(
        "Ignore text inside parentheses" to "text replacements are a Yomitan setting",
        "Remove parentheses around text" to "text replacements are a Yomitan setting",
        "Test non-empty replacement" to "text replacements are a Yomitan setting",
        "Test non-empty replacement at end" to "text replacements are a Yomitan setting",
        "Test non-empty replacement at start" to "text replacements are a Yomitan setting",
        "Test search resolution - word" to "the reader looks up words, not text scanned to the end",
        "Find terms using primary reading 1" to "the reader has no primary reading to prefer",
        "Find terms using primary reading 2" to "the reader has no primary reading to prefer",
        "Search merged mode with non-primary definitions" to "a word typed as one reading shows the entries of that reading only",
    )

    private val inputs = json("/yomitan/translator-test-inputs.json").jsonObject["tests"]!!.jsonArray.map { it.jsonObject }
    private val results = json("/yomitan/translator-test-results.json").jsonArray.map { it.jsonObject }

    @Test
    fun termLookupsFindWhatYomitanFinds() = runTest {
        val service = serviceWith(listOf("ja", "en", "ko", "la"))
        val failures = mutableListOf<String>()
        var checked = 0
        for ((index, test) in inputs.withIndex()) {
            val name = test.string("name")!!
            if (test.string("func") != "findTerms" || name in skipped) continue
            val language = (test["options"] as? JsonArray)?.drop(1)?.firstNotNullOfOrNull { (it as? JsonObject)?.string("language") } ?: "ja"
            val text = test.string("text")!!
            val result = results[index]
            assertEquals(name, result.string("name"))
            val length = result["originalTextLength"]?.jsonPrimitive?.intOrNull
            val expected = result["dictionaryEntries"]!!.jsonArray.map { it.jsonObject }.filter { it["maxOriginalTextLength"]?.jsonPrimitive?.intOrNull == length }
            val found = service.lookup(DictionaryId(language, "en"), text).entries
            checked++
            val expectedWords = expected.flatMap { headwords(it) }.toSet()
            val foundWords = found.flatMap { entry -> entry.reading?.split(", ").orEmpty().ifEmpty { listOf(entry.word) }.map { entry.word to it.takeIf { r -> r != entry.word } } }.toSet()
            if (expectedWords != foundWords) failures += "$name ($text): expected $expectedWords, found $foundWords"
            for (entry in found) {
                val chains = expected.filter { e -> headwords(e).any { it.first == entry.word } }.flatMap { chains(it) }
                if (chains.isNotEmpty() && entry.inflection !in chains) failures += "$name ($text): ${entry.word} through ${entry.inflection}, Yomitan gives $chains"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
        assertEquals(38, checked, "every term test but the skipped ones")
        assertTrue(skipped.keys.all { name -> inputs.any { it.string("name") == name } }, "the skipped tests are still in the fixtures")
    }

    /** A Yomitan entry's headwords, the reading left out where it is the word itself. */
    private fun headwords(entry: JsonObject): List<Pair<String, String?>> = entry["headwords"]!!.jsonArray.map { it.jsonObject }.map { headword ->
        val term = headword.string("term")!!
        term to headword.string("reading")?.takeIf { it != term }
    }

    /**
     * The inflection chains an entry may be reached through: those of Yomitan's algorithm, and none at
     * all where a dictionary row deinflects it, as the app's forms do without naming the inflections.
     */
    private fun chains(entry: JsonObject): List<List<String>> = entry["inflectionRuleChainCandidates"]!!.jsonArray.map { it.jsonObject }.flatMap { candidate ->
        val rules = candidate["inflectionRules"]!!.jsonArray.map { rule -> (rule as? JsonObject)?.string("name") ?: rule.jsonPrimitive.content }
        if (candidate.string("source") == "algorithm") listOf(rules) else listOf(rules, emptyList())
    }

    private suspend fun serviceWith(languages: List<String>): DictionaryService {
        val folder = File(javaClass.getResource("/yomitan/valid-dictionary1")!!.toURI())
        val zip = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { archive ->
                folder.listFiles()!!.sortedBy { it.name }.forEach { file ->
                    archive.putNextEntry(ZipEntry(file.name))
                    archive.write(file.readBytes())
                    archive.closeEntry()
                }
            }
        }.toByteArray()
        val storage = DictionaryPackStorage(DictionaryDownloader(HttpClient(MockEngine { respondError(HttpStatusCode.NotFound) })), Files.createTempDirectory("tayra-yomitan").toFile())
        val imported = ImportedDictionaries(SettingsImportedDictionaryStore(MapSettings()))
        val provider = DictionaryDatabaseProvider(storage) { id -> DictionaryPacks.find(id) ?: imported.pack(id) }
        val service = DictionaryService(storage, DictionaryRepositoryImpl(provider), imported, YomitanConverter())
        service.refresh()
        for (language in languages) service.import(zip, language, "en")
        return service
    }

    private fun json(path: String) = Json.parseToJsonElement(javaClass.getResource(path)!!.readText())

    private fun JsonObject.string(key: String): String? = (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull

    @Suppress("unused")
    private fun DictionaryEntry.describe() = "$word/$reading $inflection"
}
