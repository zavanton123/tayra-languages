package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.SqliteQuery
import com.tayra.languages.core.data.db.readSqliteFile
import com.tayra.languages.core.data.yomitan.YomitanConverter
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class YomitanConverterTest {

    private fun zip(vararg files: Pair<String, String>, deflate: Boolean = true): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, text) in files) {
                val bytes = text.toByteArray()
                val entry = ZipEntry(name)
                if (!deflate) {
                    entry.method = ZipEntry.STORED
                    entry.size = bytes.size.toLong()
                    entry.crc = java.util.zip.CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** wiktionary-to-yomitan's layout: glosses lists with tags and examples, non-lemma rows pointing to their lemma. */
    private val wty = zip(
        "index.json" to """{"title": "wty-ru-en", "revision": "2026.10.11", "format": 3, "sourceLanguage": "ru", "targetLanguage": "en", "attribution": "https://kaikki.org/"}""",
        "tag_bank_1.json" to """[["n", "partOfSpeech", 0, "noun", 0], ["fem", "grammar", 0, "feminine", 0]]""",
        "term_bank_1.json" to """[
            ["соба́ка", "", "n fem", "", 0, [{"type": "structured-content", "content": [{"tag": "ol", "data": {"content": "glosses"}, "content": [
                {"tag": "li", "content": [{"tag": "div", "content": ["dog", {"tag": "div", "data": {"content": "example-sentence"}, "content": [
                    {"tag": "div", "data": {"content": "example-sentence-a"}, "content": "Соба́ка ла́ет."},
                    {"tag": "div", "data": {"content": "example-sentence-b"}, "content": "The dog barks."}]}]}]},
                {"tag": "li", "content": ["(colloquial) ", {"tag": "span", "data": {"content": "tag"}, "title": "derogatory", "content": "derog"}, " cur"]}
            ]}]}], 1, ""],
            ["собаки", "", "non-lemma", "", 0, [["собака", ["genitive"]]], 0, ""]
        ]""",
        "term_meta_bank_1.json" to """[["собака", "ipa", {"reading": "собака", "transcriptions": [{"ipa": "[sɐˈbakə]"}]}]]""",
    )

    @Test
    fun aWtyDictionaryBecomesAPackWithSensesFormsAndIpa() = runTest {
        val converter = YomitanConverter()
        val info = converter.inspect(wty)
        assertEquals("wty-ru-en", info.title)
        assertEquals("ru", info.sourceLanguage)
        val pack = converter.convert(wty, "ru", "en")
        val (entries, forms, meta) = readSqliteFile(pack, listOf(
            SqliteQuery("SELECT word, word_lc, pos, ipa, senses FROM entries", 5),
            SqliteQuery("SELECT form_lc, lemma FROM forms", 2),
            SqliteQuery("SELECT value FROM meta WHERE key = 'format'", 1),
        ))
        val (word, wordLc, pos, ipa, senses) = entries.single()
        assertEquals(listOf("собака", "собака", "noun", "[sɐˈbakə]"), listOf(word, wordLc, pos, ipa), "the stress mark is dropped from the headword")
        assertTrue(""""glosses":["dog"]""" in senses!! && """"tags":["feminine"]""" in senses && "The dog barks." in senses, senses)
        assertTrue(""""glosses":["(colloquial) derog cur"]""" in senses && "derogatory" in senses, senses)
        assertEquals(listOf(listOf("собаки", "собака")), forms)
        assertEquals("1", meta.single().single())
    }

    @Test
    fun aGenericDictionaryKeepsItsGlossaryListsAndReadings() = runTest {
        val jitendexLike = zip(
            "index.json" to """{"title": "Generic JA", "revision": "1", "format": 3}""",
            "term_bank_1.json" to """[
                ["使う", "つかう", "v5u", "v5", 0, [{"type": "structured-content", "content": [
                    {"tag": "div", "data": {"content": "sense-label"}, "content": "employ"},
                    {"tag": "ul", "data": {"content": "glossary"}, "content": [{"tag": "li", "content": "to use"}, {"tag": "li", "content": "to make use of"}]}
                ]}], 1, ""],
                ["猫", "ねこ", "n", "", 0, ["cat", {"type": "text", "text": "kitty"}], 2, ""]
            ]""",
            "term_meta_bank_1.json" to """[["使う", "pitch", {"reading": "つかう", "pitches": [{"position": 0}]}]]""",
            deflate = false,
        )
        val converter = YomitanConverter()
        assertEquals(null, converter.inspect(jitendexLike).sourceLanguage)
        val pack = converter.convert(jitendexLike, "ja", "en")
        val (entries, readings, pitch, forms) = readSqliteFile(pack, listOf(
            SqliteQuery("SELECT word, pos, ipa, senses FROM entries ORDER BY word", 4),
            SqliteQuery("SELECT word, reading FROM readings ORDER BY word", 2),
            SqliteQuery("SELECT word, reading, positions FROM pitch", 3),
            SqliteQuery("SELECT form_lc, lemma FROM forms ORDER BY form_lc", 2),
        ))
        assertEquals(listOf("使う", "verb", null), entries[0].take(3))
        assertEquals("""[{"glosses":["to use","to make use of"],"tags":["employ"]}]""", entries[0][3])
        assertEquals("""[{"glosses":["cat"]},{"glosses":["kitty"]}]""", entries[1][3])
        assertEquals(listOf(listOf("使う", "つかう"), listOf("猫", "ねこ")), readings)
        assertEquals(listOf(listOf("使う", "つかう", "0")), pitch)
        assertEquals(listOf(listOf("つかう", "使う"), listOf("ねこ", "猫")), forms, "a kana reading finds its word")
    }

    @Test
    fun aZipWithoutAnIndexIsRefused() = runTest {
        assertFailsWith<Exception> { YomitanConverter().inspect(zip("term_bank_1.json" to "[]")) }
        assertFailsWith<Exception> { YomitanConverter().inspect("not a zip".toByteArray()) }
    }
}
