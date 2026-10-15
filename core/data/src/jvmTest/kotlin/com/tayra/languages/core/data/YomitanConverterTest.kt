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
    fun yomitansTestDictionaryKeepsEveryKindOfFrequencyAndItsKanji() = runTest {
        val folder = java.io.File(javaClass.getResource("/yomitan/valid-dictionary1")!!.toURI())
        val pack = YomitanConverter().convert(zip(*folder.listFiles()!!.sortedBy { it.name }.map { it.name to it.readText() }.toTypedArray()), "ja", "en")
        val (frequencies, kanji) = readSqliteFile(pack, listOf(
            SqliteQuery("SELECT reading, value, display FROM frequencies WHERE word = '打' ORDER BY rowid", 3),
            SqliteQuery("SELECT character, meanings, onyomi, kunyomi, frequency FROM kanji ORDER BY character", 5),
        ))
        // 1, "four", "five (5)", {value}, {reading, frequency}: the number where there is one, the text where it is not just the number.
        assertEquals(listOf(null, "1", null), frequencies[0])
        assertEquals(listOf(null, null, "four"), frequencies[1])
        assertEquals(listOf(null, "5", "five (5)"), frequencies[2])
        assertTrue(listOf("だ", "8", null) in frequencies && listOf("ダース", "9", null) in frequencies, frequencies.toString())
        assertEquals(listOf("打", "込"), kanji.map { it[0] })
        assertEquals(listOf("ダ ダアス", "う.つ う.ち- ぶ.つ", "1"), kanji[0].drop(2))
        assertTrue("utsu meaning 1" in kanji[0][1]!!)
    }

    @Test
    fun aDictionaryOfFrequenciesAloneIsAPack() = runTest {
        val frequencies = zip(
            "index.json" to """{"title": "Word Ranks", "revision": "1", "format": 3, "frequencyMode": "rank-based"}""",
            "term_meta_bank_1.json" to """[["дом", "freq", 120], ["кот", "freq", {"value": 900, "displayValue": "900㋕"}]]""",
        )
        val pack = YomitanConverter().convert(frequencies, "ru", "en")
        val (rows, mode) = readSqliteFile(pack, listOf(
            SqliteQuery("SELECT word, value, display FROM frequencies ORDER BY word", 3),
            SqliteQuery("SELECT value FROM meta WHERE key = 'frequency_mode'", 1),
        ))
        assertEquals(listOf(listOf("дом", "120", null), listOf("кот", "900", "900㋕")), rows)
        assertEquals("rank-based", mode.single().single())
    }

    @Test
    fun aZipWithoutAnIndexIsRefused() = runTest {
        assertFailsWith<Exception> { YomitanConverter().inspect(zip("term_bank_1.json" to "[]")) }
        assertFailsWith<Exception> { YomitanConverter().inspect("not a zip".toByteArray()) }
    }
}
