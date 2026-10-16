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
import com.tayra.languages.core.domain.export.Crc32
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.client.engine.mock.respond
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
            assertTrue(col.getString(2).contains("\"id\":1"), "the note type keeps one id across exports")
            assertTrue(col.getString(2).contains("tl-card"))
        }
        file.delete()

        // A second export a day later uses the same note type and deck ids, so Anki merges it.
        val later = env.service.export(listOf(lobo))!!
        assertEquals(idsOf(export.bytes), idsOf(later.bytes))
    }

    /** The note type and deck ids of a package. */
    private fun idsOf(apkg: ByteArray): String {
        val database = ZipInputStream(ByteArrayInputStream(apkg)).use { zip ->
            generateSequence { zip.nextEntry }.first { it.name == "collection.anki2" }.let { zip.readBytes() }
        }
        val file = File.createTempFile("tayra-anki", ".anki2").apply { writeBytes(database) }
        return DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            val row = c.createStatement().executeQuery("SELECT models, decks FROM col").also { it.next() }
            val ids = Regex("\"id\":(\\d+)").findAll(row.getString(1) + row.getString(2)).map { it.groupValues[1] }.toList()
            ids.joinToString(",")
        }.also { file.delete() }
    }

    /** A form whose sentence came through its family blanks out the form the sentence holds, inside the sentence. */
    @Test
    fun theClozeBlanksOutTheFamilyFormReadInTheSentence() = runBlocking<Unit> {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        val lemma = env.terms.save(Term(languageId = language, text = "destruir", textLc = "destruir", status = TermStatus.NEW_1))
        val past = env.terms.save(Term(languageId = language, text = "destruíram", textLc = "destruíram", status = TermStatus.NEW_1, sentence = "Eu vou destruir esta parede."))
        env.terms.setParents(past, listOf(lemma))

        val export = env.service.export(listOf(past))!!
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(export.bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        val file = File.createTempFile("tayra-anki", ".anki2").apply { writeBytes(entries.getValue("collection.anki2")) }
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            val notes = c.createStatement().executeQuery("SELECT flds FROM notes")
            assertTrue(notes.next())
            val fields = notes.getString(1).split('\u001f')
            assertEquals("Eu vou {{c1::destruir}} esta parede.", fields[0])
            assertEquals("destruíram", fields[4])
            assertEquals("destruir", fields[5])
        }
        file.delete()
    }

    /** The forms of a family read in one sentence make the same card, so they share one note, in one package or across two. */
    @Test
    fun aFamilySharingASentenceMakesOneNote() = runBlocking<Unit> {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        val sentence = "Eu vou destruir esta parede."
        val lemma = env.terms.save(Term(languageId = language, text = "destruir", textLc = "destruir", status = TermStatus.NEW_1, sentence = sentence))
        val past = env.terms.save(Term(languageId = language, text = "destruíram", textLc = "destruíram", status = TermStatus.NEW_1, sentence = sentence))
        env.terms.setParents(past, listOf(lemma))

        val export = env.service.export(listOf(lemma, past))!!
        assertEquals(listOf(lemma, past), export.termIds, "both terms count as exported")
        assertEquals(1, export.notes)
        val notes = notesOf(export.bytes)
        assertEquals(1, notes.size)
        assertEquals("Eu vou {{c1::destruir}} esta parede.", notes.single().second[0])
        assertEquals("destruir", notes.single().second[4], "the first term keeps the note")

        // Exported on their own, the two forms get the same guid, so Anki keeps one note of them.
        assertEquals(notesOf(env.service.export(listOf(lemma))!!.bytes).single().first, notesOf(env.service.export(listOf(past))!!.bytes).single().first)
    }

    /** The guid and fields of each note of a package. */
    private fun notesOf(apkg: ByteArray): List<Pair<String, List<String>>> {
        val database = ZipInputStream(ByteArrayInputStream(apkg)).use { zip ->
            generateSequence { zip.nextEntry }.first { it.name == "collection.anki2" }.let { zip.readBytes() }
        }
        val file = File.createTempFile("tayra-anki", ".anki2").apply { writeBytes(database) }
        return DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            val rows = c.createStatement().executeQuery("SELECT guid, flds FROM notes ORDER BY id")
            generateSequence { if (rows.next()) rows.getString(1) to rows.getString(2).split('\u001f') else null }.toList()
        }.also { file.delete() }
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
    fun crc32MatchesTheStandard() {
        assertEquals(0xCBF43926.toInt(), Crc32.of("123456789".encodeToByteArray()))
        assertEquals(0, Crc32.of(ByteArray(0)))
    }

    @Test
    fun sha1MatchesTheStandard() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", Sha1.hex("abc".encodeToByteArray()))
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", Sha1.hex(ByteArray(0)))
    }

    /** A note type in the fake Anki: its id, fields, card templates and styling. */
    private class FakeModel(val id: Long, val fields: List<String>, val front: String, val back: String, val css: String)

    /**
     * AnkiConnect as Anki answers: with no Tayra note type unless [models] are given, and the second
     * card already in it.
     */
    private class FakeAnkiConnect(val models: Map<String, FakeModel> = mapOf("Basic" to FakeModel(1, listOf("Front", "Back"), "{{Front}}", "{{Back}}", ""))) {
        val calls = mutableListOf<Pair<String, kotlinx.serialization.json.JsonObject?>>()
        val client = io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { request ->
            val body = kotlinx.serialization.json.Json.parseToJsonElement((request.body as io.ktor.http.content.TextContent).text).jsonObject
            val action = body["action"]!!.jsonPrimitive.content
            val params = body["params"] as? kotlinx.serialization.json.JsonObject
            calls += action to params
            val model = params?.get("modelName")?.jsonPrimitive?.content?.let { models[it] }
            val quote = { text: String -> kotlinx.serialization.json.JsonPrimitive(text).toString() }
            val result = when (action) {
                "version" -> "6"
                "modelNamesAndIds" -> models.entries.joinToString(", ", "{", "}") { (name, m) -> "${quote(name)}: ${m.id}" }
                "modelFieldNames" -> model!!.fields.joinToString(", ", "[", "]") { quote(it) }
                "modelTemplates" -> "{\"Cloze\": {\"Front\": ${quote(model!!.front)}, \"Back\": ${quote(model.back)}}}"
                "modelStyling" -> "{\"css\": ${quote(model!!.css)}}"
                "canAddNotesWithErrorDetail" -> List(params!!["notes"]!!.jsonArray.size) { i ->
                    if (i == 0) """{"canAdd": true}""" else """{"canAdd": false, "error": "cannot create note because it is a duplicate"}"""
                }.joinToString(", ", "[", "]")
                "addNotes" -> "[1700000000000]"
                else -> "null"
            }
            respond("""{"result": $result, "error": null}""")
        })
    }

    @Test
    fun sendingAddsTheNewNotesToTheRunningAnki() = runBlocking<Unit> {
        val env = Env()
        env.settings.update { it.copy(nativeLanguage = "ru", speechEngine = SpeechEngine.PIPER) }
        val language = env.languages.save(Language(name = "Portuguese"))
        val hesitou = env.terms.save(Term(languageId = language, text = "hesitou", textLc = "hesitou", status = TermStatus.NEW_1, translation = "сомневался", sentence = "Ele hesitou antes de responder."))
        val lobo = env.terms.save(Term(languageId = language, text = "lobo", textLc = "lobo", status = TermStatus.NEW_2, translation = "волк"))
        val anki = FakeAnkiConnect()
        val connection = com.tayra.languages.core.data.export.AnkiConnectClient(anki.client)

        assertEquals(6, connection.version())
        val sent = env.service.send(listOf(hesitou, lobo), connection)!!
        assertEquals(1, sent.added)
        assertEquals(1, sent.duplicates)
        assertEquals(listOf("version", "modelNamesAndIds", "createModel", "createDeck", "canAddNotesWithErrorDetail", "storeMediaFile", "addNotes"), anki.calls.map { it.first })
        val model = anki.calls.first { it.first == "createModel" }.second!!
        assertEquals("Tayra Languages", model["modelName"]!!.jsonPrimitive.content)
        assertEquals(AnkiExportService.FIELDS.size, model["inOrderFields"]!!.jsonArray.size)
        val note = anki.calls.first { it.first == "addNotes" }.second!!["notes"]!!.jsonArray.single().jsonObject
        assertEquals("Tayra Languages::Portuguese", note["deckName"]!!.jsonPrimitive.content)
        assertEquals("Ele {{c1::hesitou}} antes de responder.", note["fields"]!!.jsonObject["Cloze"]!!.jsonPrimitive.content)
        // Only the added note's sentence audio is stored.
        assertTrue(anki.calls.single { it.first == "storeMediaFile" }.second!!["filename"]!!.jsonPrimitive.content.startsWith("tayra-"))

        // Both are in Anki now, the duplicate too, so neither is sent again.
        assertEquals(null, env.service.send(listOf(hesitou, lobo), connection))
    }

    /**
     * An Anki that has an old "Tayra Languages" a former version made, and the note type an export
     * brought, renamed "Tayra Languages++" for the name was taken: cards are sent to the export's,
     * and Tayra's note types get the current design, but not one that lacks a field, nor another's.
     */
    @Test
    fun cardsSentGoToTheExportsNoteTypeInTheCurrentDesign() = runBlocking<Unit> {
        val env = Env()
        val language = env.languages.save(Language(name = "Portuguese"))
        val lobo = env.terms.save(Term(languageId = language, text = "lobo", textLc = "lobo", status = TermStatus.NEW_2, translation = "wolf", sentence = "O lobo dorme."))
        val type = AnkiExportService.noteType()
        val fields = AnkiExportService.FIELDS
        val anki = FakeAnkiConnect(
            mapOf(
                "Tayra Languages" to FakeModel(1790997078931, fields, "<div class=\"dc-card\">old</div>", "old back", ".dc-card {}"),
                "Tayra Languages++" to FakeModel(type.id, fields, type.front, type.back, type.css),
                "Tayra Languages+" to FakeModel(1790997420144, fields.dropLast(1), "old", "old", "old"),
                "Tayra Languages Old" to FakeModel(5, fields, "mine", "mine", "mine"),
            ),
        )
        env.service.send(listOf(lobo), com.tayra.languages.core.data.export.AnkiConnectClient(anki.client))

        assertEquals(null, anki.calls.firstOrNull { it.first == "createModel" })
        val note = anki.calls.first { it.first == "addNotes" }.second!!["notes"]!!.jsonArray.single().jsonObject
        assertEquals("Tayra Languages++", note["modelName"]!!.jsonPrimitive.content)
        val restyled = anki.calls.filter { it.first == "updateModelTemplates" || it.first == "updateModelStyling" }
            .map { it.first to it.second!!["model"]!!.jsonObject["name"]!!.jsonPrimitive.content }
        assertEquals(listOf("updateModelTemplates" to "Tayra Languages", "updateModelStyling" to "Tayra Languages"), restyled)
        val templates = anki.calls.first { it.first == "updateModelTemplates" }.second!!["model"]!!.jsonObject["templates"]!!.jsonObject["Cloze"]!!.jsonObject
        assertEquals(type.front, templates["Front"]!!.jsonPrimitive.content)
        assertEquals(type.back, templates["Back"]!!.jsonPrimitive.content)
    }

    @Test
    fun anAnkiWithoutAnkiConnectHasNoVersion() = runBlocking<Unit> {
        val refused = io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { throw java.net.ConnectException("Connection refused") })
        assertEquals(null, com.tayra.languages.core.data.export.AnkiConnectClient(refused).version())
    }
}
