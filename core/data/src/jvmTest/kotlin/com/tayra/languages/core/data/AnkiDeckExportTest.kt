package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.anki.AnkiArchiveReaderImpl
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.export.AnkiPackagerImpl
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.export.AnkiDeckExportService
import com.tayra.languages.core.domain.export.AnkiExportService
import com.tayra.languages.core.domain.flashcards.AnkiImportService
import com.tayra.languages.core.domain.flashcards.CardSchedule
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.Flashcard
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.FlashcardScheduler
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.NewCardOrder
import com.tayra.languages.core.domain.flashcards.PersonalParameters
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.LocalSpeechEngine
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.SpeechEngine
import com.tayra.languages.core.domain.service.SpeechPackage
import com.tayra.languages.core.domain.service.SpeechVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.File
import java.sql.DriverManager
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * A language's cards go to Anki as a deck with their progress: read back as Anki would read the
 * package, every card keeps its state, due day, interval, steps, memory, reps, lapses and
 * suspension, every answer is in the review log, and the settings make the deck's preset.
 */
class AnkiDeckExportTest {

    private val clock = object : Clock { override fun now() = Instant.parse("2026-11-03T15:00:00Z") }
    private val now = clock.now()

    private class Library(clock: Clock) {
        val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-anki-deck", ".db").also { it.delete() }))
        val languages = LanguageRepositoryImpl(provider)
        val terms = TermRepositoryImpl(provider, clock)
        val cards = FlashcardRepositoryImpl(provider)
        val settings = SettingsRepositoryImpl(MapSettings())
    }

    private val speech = object : LocalSpeechEngine {
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

    private fun exporter(library: Library): AnkiDeckExportService {
        val dictionary = object : OfflineDictionary {
            override suspend fun isAvailable(dictionary: DictionaryId) = false
            override suspend fun lookup(dictionary: DictionaryId, text: String) = DictionaryLookup(emptyList(), emptyList())
        }
        val translator = object : SentenceTranslator {
            override suspend fun translate(text: String, language: Language): String? = "<$text>"
        }
        val audio = SentenceAudio(LocalSpeech(listOf(speech)), library.settings, MemorySpeechAudioCache())
        val notes = AnkiExportService(library.terms, library.languages, dictionary, translator, audio, library.settings, AnkiPackagerImpl(), clock, timeZone = { TimeZone.UTC })
        return AnkiDeckExportService(library.cards, library.terms, library.languages, library.settings, notes, AnkiPackagerImpl(), clock, timeZone = { TimeZone.UTC })
    }

    private suspend fun Library.word(language: Long, text: String, card: CardSchedule, suspended: Boolean = false, sentence: String? = null): Long {
        val id = terms.save(Term(languageId = language, text = text, textLc = text, status = TermStatus.NEW_2, translation = "[$text]", sentence = sentence))
        cards.save(Flashcard(id, card, suspended, seenStatus = TermStatus.NEW_2, createdAt = now - 60.days))
        return id
    }

    private fun answer(termId: Long, at: Instant, rating: Rating, before: CardState, interval: Int = 0, elapsed: Int = 0, took: Int? = null) =
        FlashcardReview(termId = termId, at = at, rating = rating, stateBefore = before, intervalDays = interval, elapsedDays = elapsed, answerTime = took?.seconds)

    @Test
    fun cardsAndTheirHistoryReadBackAsTheyWere() = runBlocking<Unit> {
        val tayra = Library(clock)
        tayra.settings.update {
            it.copy(
                nativeLanguage = "en", speechEngine = SpeechEngine.PIPER, flashcardNewPerDay = 15, flashcardRetention = 87,
                flashcardLearnSteps = "2m 15m 1d", flashcardDayStartHour = 4, flashcardNewOrder = NewCardOrder.NEWEST, flashcardLeechThreshold = 3,
            )
        }
        val portuguese = tayra.languages.save(Language(name = "Portuguese"))
        val weights = List(21) { 0.1 + it / 10.0 }
        tayra.cards.saveParameters(PersonalParameters(portuguese, weights, 400, now - 3.days, 0.02))
        val reviewed = now - 10.days
        val lobo = tayra.word(
            portuguese, "lobo",
            CardSchedule(CardState.REVIEW, now + 5.days, MemoryState(40.5, 5.25), intervalDays = 15, reps = 3, lapses = 3, lastReview = reviewed),
            sentence = "O lobo uivou.",
        )
        val gato = tayra.word(portuguese, "gato", CardSchedule(CardState.LEARNING, now + 15.minutes, MemoryState(1.2, 6.0), remainingSteps = 2, reps = 1, lastReview = now - 2.minutes))
        val casa = tayra.word(portuguese, "casa", CardSchedule(CardState.RELEARNING, now + 1.days, MemoryState(3.0, 7.0), intervalDays = 4, remainingSteps = 1, reps = 6, lapses = 1, lastReview = now - 1.minutes))
        val novo = tayra.word(portuguese, "novo", CardSchedule(CardState.NEW, now - 2.days))
        val velho = tayra.word(portuguese, "velho", CardSchedule(CardState.NEW, now - 1.days), suspended = true)
        tayra.cards.addReviews(
            listOf(
                answer(lobo, reviewed - 30.days, Rating.AGAIN, CardState.NEW, took = 7),
                answer(lobo, reviewed - 30.days + 2.minutes, Rating.GOOD, CardState.LEARNING, interval = 3, took = 95),
                answer(lobo, reviewed, Rating.GOOD, CardState.REVIEW, interval = 15, elapsed = 30),
                answer(gato, now - 2.minutes, Rating.GOOD, CardState.NEW, took = 4),
            ),
        )

        val progress = mutableListOf<Pair<Int, Int>>()
        val export = exporter(tayra).export(portuguese) { done, total -> progress += done to total }!!
        assertEquals("Portuguese 2026-11-03_15-00.apkg", export.fileName)
        assertEquals(5, export.notes)
        assertEquals(setOf(lobo, gato, casa, novo, velho), export.termIds.toSet())
        assertEquals(5 to 5, progress.last())
        System.getenv("ANKI_DECK_FILE")?.let { File(it).writeBytes(export.bytes) }

        // Read back the way the import from Anki reads a package, into a library of its own.
        val anki = Library(clock)
        val there = anki.languages.save(Language(name = "Portuguese"))
        anki.settings.update { it.copy(currentLanguageId = there, flashcardLearnSteps = "2m 15m 1d", flashcardDayStartHour = 4) }
        AnkiImportService(AnkiArchiveReaderImpl(), anki.cards, anki.terms, anki.languages, anki.settings, FlashcardScheduler(timeZone = { TimeZone.UTC }), clock, timeZone = { TimeZone.UTC })
            .import(export.bytes)
        val scheduler = FlashcardScheduler(timeZone = { TimeZone.UTC })
        val config = FlashcardService.schedulerConfig(tayra.settings.current)
        val byWord = anki.cards.list(there).associateBy { card -> anki.terms.getByIds(listOf(card.termId)).single().text }
        for ((word, id) in listOf("lobo" to lobo, "gato" to gato, "casa" to casa, "novo" to novo, "velho" to velho)) {
            val mine = tayra.cards.get(id)!!
            val theirs = byWord.getValue(word)
            assertEquals(mine.schedule.state, theirs.schedule.state, word)
            assertEquals(mine.suspended, theirs.suspended, word)
            assertEquals(mine.schedule.intervalDays, theirs.schedule.intervalDays, word)
            assertEquals(mine.schedule.remainingSteps, theirs.schedule.remainingSteps, word)
            assertEquals(mine.schedule.reps, theirs.schedule.reps, word)
            assertEquals(mine.schedule.lapses, theirs.schedule.lapses, word)
            assertEquals(mine.schedule.lastReview?.epochSeconds, theirs.schedule.lastReview?.epochSeconds, word)
            if (mine.schedule.state != CardState.NEW) {
                assertEquals(mine.schedule.memory, theirs.schedule.memory, word)
                assertEquals(scheduler.day(mine.schedule.due, config), scheduler.day(theirs.schedule.due, config), word)
            }
        }
        // A step later today keeps its time; one on a later day, its day.
        assertEquals((now + 15.minutes).epochSeconds, byWord.getValue("gato").schedule.due.epochSeconds)

        val answers = anki.cards.reviews(byWord.getValue("lobo").termId).sortedBy { it.at }
        assertEquals(listOf(Rating.AGAIN, Rating.GOOD, Rating.GOOD), answers.map { it.rating })
        assertEquals(listOf(CardState.NEW, CardState.LEARNING, CardState.REVIEW), answers.map { it.stateBefore })
        assertEquals(listOf(reviewed - 30.days, reviewed - 30.days + 2.minutes, reviewed), answers.map { it.at })
        assertEquals(listOf(0, 3, 15), answers.map { it.intervalDays })

        val entries = unzip(export.bytes)
        assertTrue(entries.getValue("media").decodeToString().contains(".wav"))
        val file = File.createTempFile("tayra-anki-deck", ".anki2").apply { writeBytes(entries.getValue("collection.anki2")) }
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            fun rows(sql: String, columns: Int) = c.createStatement().executeQuery(sql).let { r -> buildList { while (r.next()) add((1..columns).map { r.getString(it) }) } }
            // The newest new card is learned first, as here; the leech is tagged as Anki tags one.
            val cardRows = rows("SELECT n.sfld, c.type, c.queue, c.due, c.left, c.data, n.tags, n.guid FROM cards c JOIN notes n ON n.id = c.nid ORDER BY n.sfld", 8).associateBy { it[0] }
            assertEquals(listOf("0", "-1", "1"), cardRows.getValue("velho").subList(1, 4))
            assertEquals(listOf("0", "0", "2"), cardRows.getValue("novo").subList(1, 4))
            assertEquals(listOf("2", "2", "5"), cardRows.getValue("O lobo uivou.").subList(1, 4))
            assertEquals(listOf("3", "3", "1", "1001"), cardRows.getValue("casa").subList(1, 5))
            assertEquals(listOf("1", "1", "${(now + 15.minutes).epochSeconds}", "2002"), cardRows.getValue("gato").subList(1, 5))
            assertTrue(cardRows.getValue("O lobo uivou.")[6].contains(" leech "))
            assertTrue(cardRows.getValue("gato")[5].contains("\"dr\":0.87"))
            assertEquals("", cardRows.getValue("novo")[5])

            val log = rows("SELECT r.ease, r.ivl, r.lastIvl, r.time, r.type FROM revlog r JOIN cards c ON c.id = r.cid JOIN notes n ON n.id = c.nid WHERE n.sfld = 'O lobo uivou.' ORDER BY r.id", 5)
            assertEquals(listOf(listOf("1", "-120", "0", "7000", "0"), listOf("3", "3", "-120", "60000", "0"), listOf("3", "15", "3", "0", "1")), log)

            val (conf, decks, dconf, crt) = rows("SELECT conf, decks, dconf, crt FROM col", 4).single()
            val options = Json.parseToJsonElement(conf).jsonObject
            assertEquals(2, options.getValue("schedVer").jsonPrimitive.int)
            assertEquals(4, options.getValue("rollover").jsonPrimitive.int)
            assertEquals(Instant.parse("2026-11-03T04:00:00Z").epochSeconds, crt.toLong())
            val deck = Json.parseToJsonElement(decks).jsonObject.values.map { it.jsonObject }.single { it.getValue("name").jsonPrimitive.content != "Default" }
            assertEquals("Tayra Languages::Portuguese (progress)", deck.getValue("name").jsonPrimitive.content)
            val preset = Json.parseToJsonElement(dconf).jsonObject.getValue(deck.getValue("conf").jsonPrimitive.content).jsonObject
            assertEquals("Tayra Languages: Portuguese", preset.getValue("name").jsonPrimitive.content)
            assertEquals(0.87, preset.getValue("desiredRetention").jsonPrimitive.double)
            assertEquals(weights, preset.getValue("fsrsParams6").jsonArray.map { it.jsonPrimitive.double })
            assertEquals(listOf(2.0, 15.0, 1440.0), preset.getValue("new").jsonObject.getValue("delays").jsonArray.map { it.jsonPrimitive.double })
            assertEquals(15, preset.getValue("new").jsonObject.getValue("perDay").jsonPrimitive.int)
            assertEquals(3, preset.getValue("lapse").jsonObject.getValue("leechFails").jsonPrimitive.int)
            assertEquals(1, preset.getValue("newMix").jsonPrimitive.int)
        }
    }

    @Test
    fun aLanguageWithoutCardsHasNoDeck() = runBlocking<Unit> {
        val tayra = Library(clock)
        val portuguese = tayra.languages.save(Language(name = "Portuguese"))
        assertNull(exporter(tayra).export(portuguese))
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        return entries
    }
}
