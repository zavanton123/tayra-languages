package com.tayra.languages.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tayra.languages.core.data.dictionary.DictionaryDatabaseProvider
import com.tayra.languages.core.data.dictionary.DictionaryDownloader
import com.tayra.languages.core.data.dictionary.DictionaryPackStorage
import com.tayra.languages.core.data.repository.DictionaryRepositoryImpl
import com.tayra.languages.core.domain.dictionary.KanjiInfo
import com.tayra.languages.core.domain.dictionary.PitchAccent
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.service.DictionaryService
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.morphology.Furigana
import com.tayra.languages.core.domain.service.WordReadings
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
    /** A pack of [entries] (word, part of speech, gloss or the senses JSON itself) and [forms] (form, lemma) for [source] to English. */
    private suspend fun service(
        source: String,
        entries: List<Triple<String, String, String>>,
        forms: List<Pair<String, String>> = emptyList(),
        /** Statements run on the pack after the entries, for its optional tables. */
        extra: List<String> = emptyList(),
        /** The published pack the file stands in for; [source] to English by default. */
        packOf: () -> DictionaryPack = { DictionaryPacks.find(source, "en")!! },
    ): Pair<DictionaryService, DictionaryId> {
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
            val senses = if (gloss.startsWith("[")) gloss else "[{\"glosses\":[\"$gloss\"]}]"
            driver.execute(null, "INSERT INTO entries(word, word_lc, pos, senses) VALUES (${q(word)}, ${q(word.lowercase())}, ${q(pos)}, ${q(senses)})", 0)
        }
        for ((form, lemma) in forms) driver.execute(null, "INSERT INTO forms VALUES (${q(form)}, ${q(lemma)}, 0)", 0)
        extra.forEach { driver.execute(null, it, 0) }
        driver.close()
        val packed = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(file.readBytes()) } }.toByteArray()
        file.delete()
        val storage = DictionaryPackStorage(DictionaryDownloader(HttpClient(MockEngine { respond(packed) })), Files.createTempDirectory("tayra-morph").toFile())
        val service = DictionaryService(storage, DictionaryRepositoryImpl(DictionaryDatabaseProvider(storage)))
        val pack = packOf()
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
        assertEquals(listOf("ing"), walking.entries.single().inflection)
        assertTrue(!walking.entries.single().isOwnEntry)
        assertEquals("happy", service.lookup(en, "happiest").parentSuggestion)
        // A rule only leads to a word of the part of speech it makes: "walker" is not a form of an adjective.
        assertTrue(service.lookup(en, "happied").isEmpty)
    }

    @Test
    fun koreanWordsAreFoundWithoutTheirParticlesAndEndings() = runTest {
        val (service, ko) = service("ko", listOf(Triple("학교", "noun", "school"), Triple("가다", "verb", "to go")))
        assertEquals("학교", service.lookup(ko, "학교에서").parentSuggestion)
        assertEquals("가다", service.lookup(ko, "갔어요").parentSuggestion)
        assertTrue(service.lookup(ko, "없다").isEmpty)
    }

    @Test
    fun japaneseConjugationsFindTheirDictionaryForm() = runTest {
        val (service, ja) = service("ja", listOf(Triple("使う", "verb", "to use"), Triple("短い", "adj", "short")))
        val used = service.lookup(ja, "使って")
        assertEquals("使う", used.parentSuggestion)
        assertEquals(listOf("-て"), used.entries.single().inflection)
        assertEquals("短い", service.lookup(ja, "短かった").parentSuggestion)
    }

    @Test
    fun aSenseBringsTheDictionarysExampleSentences() = runTest {
        val senses = """[{"glosses":["of"],"examples":[{"text":"os amigos dele","translation":"his friends"},{"text":"fones de ouvido"}]}]"""
        val (service, pt) = service("pt", listOf(Triple("de", "prep", senses)))
        val examples = service.lookup(pt, "de").entries.single().senses.single().examples
        assertEquals(listOf("os amigos dele", "fones de ouvido"), examples.map { it.text })
        assertEquals(listOf("his friends", null), examples.map { it.translation })
    }

    @Test
    fun japaneseAndChineseEntriesCarryTheirReadings() = runTest {
        val readings = listOf("CREATE TABLE readings (word TEXT NOT NULL, reading TEXT NOT NULL)", "INSERT INTO readings VALUES ('使う', 'つかう')")
        val (service, ja) = service("ja", listOf(Triple("使う", "verb", "to use")), extra = readings)
        assertEquals("つかう", service.lookup(ja, "使って").entries.single().reading)
        // A pack without the table still answers, without readings.
        val (plain, zh) = service("zh", listOf(Triple("你好", "intj", "hello")))
        assertEquals(null, plain.lookup(zh, "你好").entries.single().reading)
    }

    @Test
    fun japaneseEntriesCarryTheirPitchAccents() = runTest {
        val pitch = listOf(
            "CREATE TABLE pitch (word TEXT NOT NULL, reading TEXT NOT NULL, positions TEXT NOT NULL)",
            "INSERT INTO pitch VALUES ('箸', 'はし', '1'), ('日本', 'にほん', '2'), ('日本', 'にっぽん', '3'), ('一寸', 'ちょっと', '(副)1,(名;形動)0')",
        )
        val entries = listOf(Triple("箸", "noun", "chopsticks"), Triple("日本", "name", "Japan"), Triple("一寸", "adv", "a little"))
        val (service, ja) = service("ja", entries, extra = pitch)
        assertEquals(listOf(PitchAccent("はし", 1)), service.lookup(ja, "箸").entries.single().pitch)
        assertEquals(listOf(PitchAccent("にほん", 2), PitchAccent("にっぽん", 3)), service.lookup(ja, "日本").entries.single().pitch)
        val accents = service.lookup(ja, "一寸").entries.single().pitch
        assertEquals(listOf(1, 0), accents.map { it.downstep }, "the parts of speech Kanjium marks are dropped")
        assertEquals(listOf("ちょ", "っ", "と"), accents.first().morae)
        assertEquals(listOf("atamadaka", "heiban"), accents.map { it.category(isVerbOrAdjective = false) })
    }

    @Test
    fun aJapaneseTermListsTheKanjiItIsWrittenWith() = runTest {
        val kanji = listOf(
            "CREATE TABLE kanji (character TEXT NOT NULL PRIMARY KEY, meanings TEXT NOT NULL, onyomi TEXT NOT NULL, kunyomi TEXT NOT NULL, strokes INTEGER, grade INTEGER, jlpt INTEGER, frequency INTEGER)",
            "INSERT INTO kanji VALUES ('日', '[\"day\",\"sun\"]', 'ニチ ジツ', 'ひ -び -か', 4, 1, 4, 1), ('本', '[\"book\"]', 'ホン', 'もと', 5, 1, 4, 10), ('𠮟', '[\"scold\"]', 'シツ', 'しか.る', 5, 8, NULL, NULL)",
        )
        val (service, ja) = service("ja", listOf(Triple("日本", "name", "Japan")), extra = kanji)
        val found = service.kanji(ja, "日本の本")
        assertEquals(listOf("日", "本"), found.map { it.character }, "each kanji once, in order, without the kana")
        assertEquals(KanjiInfo("日", listOf("day", "sun"), listOf("ニチ", "ジツ"), listOf("ひ", "-び", "-か"), 4, 1, 4, 1), found.first())
        assertEquals(listOf("𠮟"), service.kanji(ja, "𠮟る").map { it.character }, "a kanji outside the Basic Multilingual Plane")
        val (plain, zh) = service("zh", listOf(Triple("日本", "name", "Japan")))
        assertEquals(emptyList(), plain.kanji(zh, "日本"))
    }

    @Test
    fun theWordFinderGivesTheLearnedLanguagesWordsForANativeOne() = runTest {
        val free = """[{"glosses":["свободный","вольный"],"tags":["not imprisoned"]},{"glosses":["бесплатно"],"tags":["without needing to pay"]}]"""
        val (service, finder) = service("en", listOf(Triple("free", "adj", free)), packOf = { DictionaryPacks.findGlossary("en", "ru")!! })
        assertEquals(DictionaryId("en", "ru", glossary = true), finder)
        assertEquals("en-ru-gloss", finder.name)
        val found = service.lookup(finder, "Free").entries.single()
        assertEquals(listOf("свободный", "вольный"), found.senses.first().glosses)
        assertEquals(listOf("not imprisoned"), found.senses.first().tags)
        assertTrue(DictionaryPacks.all.none { it.id.glossary }, "the dictionaries of words have no word finder among them")
        assertEquals(DictionaryId("en", "ru"), DictionaryPacks.find("en", "ru")!!.id)
    }

    @Test
    fun chineseIsFoundByItsPinyin() = runTest {
        val (service, zh) = service("zh", listOf(Triple("你好", "intj", "hello")), forms = listOf("nǐhǎo" to "你好", "nihao" to "你好"))
        assertEquals("你好", service.lookup(zh, "Nǐ hǎo").parentSuggestion)
        assertEquals("你好", service.lookup(zh, "ni hao").parentSuggestion)
    }

    @Test
    fun wordsGetFuriganaOverTheirKanjiOnly() = runTest {
        val readings = listOf(
            "CREATE TABLE readings (word TEXT NOT NULL, reading TEXT NOT NULL)",
            "INSERT INTO readings VALUES ('使う', 'つかう'), ('日本語', 'にほんご')",
        )
        val (service, _) = service("ja", listOf(Triple("使う", "verb", "to use"), Triple("日本語", "noun", "Japanese")), extra = readings)
        val settings = SettingsRepositoryImpl(MapSettings())
        settings.update { it.copy(nativeLanguage = "en") }
        val japanese = Language(name = "Japanese")
        val furigana = WordReadings(service, settings).of(japanese, listOf("使って", "日本語", "を"))
        assertEquals(listOf(Furigana("使", "つか"), Furigana("って", "")), furigana["使って"])
        assertEquals(listOf(Furigana("日本語", "にほんご")), furigana["日本語"])
        assertTrue("を" !in furigana, "kana need no reading")
    }
}
