package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.export.AnkiPackagerImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionarySense
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.export.Sha1
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import com.tayra.languages.core.domain.service.SpeechEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import java.io.ByteArrayInputStream
import java.io.File
import java.sql.DriverManager
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Terms are exported as an Anki package: a zip with a readable collection database and the audio. */
class AnkiExportTest {

    private class Env {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-anki", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
        val dictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = true
            override suspend fun lookup(dictionary: DictionaryId, text: String) =
                if (text == "hesitou") DictionaryLookup(listOf(DictionaryEntry("hesitar", "verb", null, listOf(DictionarySense(listOf("колебаться"))), isOwnEntry = false)), listOf("hesitar"))
                else DictionaryLookup(emptyList(), emptyList())
        }
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language): String? = "<$text>"
        }
        val speech = object : LocalSpeechEngine {
            override val engine = SpeechEngine.PIPER
            override val displayName = "Fake"
            override val description = ""
            override val packagesDescription = ""
            override val hasRuntimeSetup = false
            override val progress = MutableStateFlow<String?>(null)
            override suspend fun status() = "ready"
            override suspend fun isReady() = true
            override suspend fun setUp() = "ready"
            override suspend fun packages() = emptyList<SpeechPackage>()
            override suspend fun installPackage(id: String) {}
            override suspend fun removePackage(id: String) {}
            override suspend fun voices(languageCode: String) = emptyList<SpeechVoice>()
            override suspend fun synthesize(text: String, languageCode: String, voiceId: String?, speed: Float) = "WAV:$text".encodeToByteArray()
        }
        val clock = object : Clock { override fun now() = Instant.parse("2026-10-03T01:00:00Z") }
        val service = AnkiExportService(
            terms, languages, dictionary, translator, SentenceAudio(LocalSpeech(listOf(speech)), settings, MemorySpeechAudioCache()),
            settings, AnkiPackagerImpl(), clock, timeZone = { TimeZone.UTC },
        )
    }

    @Test
    fun theSelectedTermsBecomeClozeNotesWithAudio() = runBlocking<Unit> {
        val env = Env()
        env.settings.update { it.copy(nativeLanguage = "ru", speechEngine = SpeechEngine.PIPER) }
        val language = env.languages.save(Language(name = "Portuguese"))
        val hesitou = env.terms.save(Term(languageId = language, text = "hesitou", textLc = "hesitou", status = TermStatus.NEW_1, translation = "сомневался", sentence = "Ele hesitou antes de responder."))
        val lobo = env.terms.save(Term(languageId = language, text = "lobo", textLc = "lobo", status = TermStatus.NEW_2, translation = "волк"))

        val progress = mutableListOf<Pair<Int, Int>>()
        val export = env.service.export(listOf(hesitou, lobo)) { done, total -> progress += done to total }!!
        assertEquals("2026-10-03_01-00.apkg", export.fileName)
        assertEquals(listOf(hesitou, lobo), export.termIds)
        assertEquals(0, export.skipped)
        assertEquals(listOf(0 to 2, 1 to 2, 2 to 2), progress)

        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(export.bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        assertEquals(setOf("collection.anki2", "media", "0", "1"), entries.keys)
        val media = entries.getValue("media").decodeToString()
        assertTrue(media.contains("\"0\":\"tayra-") && media.endsWith(".wav\"}"), media)
        assertEquals("WAV:Ele hesitou antes de responder.", entries.getValue("0").decodeToString())
        assertEquals("WAV:lobo", entries.getValue("1").decodeToString())

        val file = File.createTempFile("tayra-anki", ".anki2").apply { writeBytes(entries.getValue("collection.anki2")) }
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            val notes = c.createStatement().executeQuery("SELECT flds, tags FROM notes ORDER BY id")
            assertTrue(notes.next())
            val fields = notes.getString(1).split('\u001f')
            assertEquals(AnkiExportService.FIELDS.size, fields.size)
            assertEquals("Ele {{c1::hesitou}} antes de responder.", fields[0])
            assertEquals("WORD|hesitar|pt", fields[1])
            assertEquals("Ele hesitou antes de responder.", fields[2])
            assertEquals("<Ele hesitou antes de responder.>", fields[3])
            assertEquals(listOf("hesitou", "hesitar", "Verb", "", "сомневался", "Tayra Languages", "pt", "ru"), fields.subList(4, 12))
            assertEquals("2026-10-03 01:00", fields[18])
            assertEquals("Ele hesitou antes de responder.", fields[19])
            assertTrue(fields[23].startsWith("[sound:tayra-") && fields[23].endsWith(".wav]"), fields[23])
            assertEquals(" Tayra_Languages::lang::pt Tayra_Languages::source::reading ", notes.getString(2))
            assertTrue(notes.next())
            val second = notes.getString(1).split('\u001f')
            assertEquals("{{c1::lobo}}", second[0])
            assertEquals("lobo", second[2])
            assertEquals("", second[3])
            assertEquals(2, c.createStatement().executeQuery("SELECT count(*) FROM cards").let { it.next(); it.getInt(1) })
            val col = c.createStatement().executeQuery("SELECT ver, models, decks FROM col")
            assertTrue(col.next())
            assertEquals(11, col.getInt(1))
            assertTrue(col.getString(2).contains("\"name\":\"Tayra Languages\"") && col.getString(2).contains("\"type\":1"))
            assertTrue(col.getString(3).contains("Tayra Languages::Portuguese"))
        }
        file.delete()
    }

    /** A word is exported once: after its package was saved it is left out, and nothing is made when all are out. */
    @Test
    fun exportedWordsAreNotExportedAgain() = runBlocking<Unit> {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        val hesitou = env.terms.save(Term(languageId = language, text = "hesitou", textLc = "hesitou", status = TermStatus.NEW_1))
        val lobo = env.terms.save(Term(languageId = language, text = "lobo", textLc = "lobo", status = TermStatus.NEW_2))
        env.service.markExported(env.service.export(listOf(hesitou))!!)

        val export = env.service.export(listOf(hesitou, lobo))!!
        assertEquals(listOf(lobo), export.termIds)
        assertEquals(1, export.skipped)
        assertEquals(env.clock.now(), env.terms.getById(hesitou)?.ankiExportedAt)
        env.service.markExported(export)
        assertEquals(null, env.service.export(listOf(hesitou, lobo)))
    }

    @Test
    fun sha1MatchesTheStandard() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", Sha1.hex("abc".encodeToByteArray()))
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", Sha1.hex(ByteArray(0)))
    }
}
