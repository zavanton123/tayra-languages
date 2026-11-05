package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.flashcards.AnkiArchive
import com.tayra.languages.core.domain.flashcards.AnkiArchiveCard
import com.tayra.languages.core.domain.flashcards.AnkiArchiveNote
import com.tayra.languages.core.domain.flashcards.AnkiArchiveReader
import com.tayra.languages.core.domain.flashcards.AnkiArchiveReview
import com.tayra.languages.core.domain.flashcards.AnkiImportService
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardScheduler
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Fsrs
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Anki's cards come in with Anki's scheduling: new words are saved at the status their interval
 * earns, saved words keep a Known or Ignored status and are only raised from learning, Anki's
 * answers join the history once, and notes with no word or in a language not learned are left out.
 */
class AnkiImportServiceTest {

    private class FixedClock(var now: Instant) : Clock {
        override fun now() = now
    }

    private val clock = FixedClock(Instant.parse("2026-11-01T12:00:00Z"))
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-anki-import", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider, clock)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val cards = FlashcardRepositoryImpl(provider)
    private val scheduler = FlashcardScheduler(timeZone = { TimeZone.UTC })
    private val flashcards = FlashcardService(cards, terms, settings, scheduler, clock)

    /** Day 0 of the collection, at Anki's rollover hour. */
    private val dayZero = Instant.parse("2026-02-14T04:00:00Z")

    private var archive = AnkiArchive(dayZero, emptyList())
    private val reader = object : AnkiArchiveReader {
        override val available = true
        override suspend fun read(bytes: ByteArray) = archive
    }
    private val importer = AnkiImportService(reader, cards, terms, languages, settings, scheduler, clock, timeZone = { TimeZone.UTC })

    private suspend fun portuguese(): Long = languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } }

    private fun at(text: String) = Instant.parse(text)

    private fun review(at: String, ease: Int, type: Int, ivl: Int = 0) = AnkiArchiveReview(at(at), ease, ivl, type)

    private fun card(
        type: Int = 2,
        queue: Int = type,
        due: Long = 262,
        ivl: Int = 30,
        reps: Int = 3,
        lapses: Int = 0,
        left: Int = 0,
        reviews: List<AnkiArchiveReview> = emptyList(),
        deck: String = "Tayra Languages::Portuguese",
        stability: Double = 40.5,
    ) = AnkiArchiveCard(deck, type, queue, due, null, ivl, reps, lapses, left, stability.takeIf { type != 0 }, 5.25.takeIf { type != 0 }, reviews.maxOfOrNull { it.at }, reviews)

    /** A note laid out as Tayra and Language Reactor export them. */
    private fun note(id: Long, word: String, card: AnkiArchiveCard, sentence: String = "", translation: String = "", language: String = "pt", created: String = "") =
        AnkiArchiveNote(id, mapOf("Cloze" to "", "Word" to word, "Subtitle" to sentence, "Word Definition" to translation, "Language" to language, "Date Created" to created), listOf("Tayra_Languages::lang::$language"), listOf(card))

    private fun onDay(day: Int) = LocalDate(2026, 2, 14).plus(day, DateTimeUnit.DAY).atTime(4, 0).toInstant(TimeZone.UTC)

    @Test
    fun aNewWordComesWithAnkisScheduleAndHistoryOnce() = runTest {
        val pt = portuguese()
        val answers = listOf(review("2026-10-16T09:00:00Z", 3, 0), review("2026-10-16T09:10:00Z", 3, 0, ivl = 2), review("2026-10-18T09:00:00Z", 3, 1, ivl = 30))
        archive = AnkiArchive(dayZero, listOf(note(1_790_000_000_000, "Pretendo", card(reviews = answers), sentence = "Eu <b>pretendo</b> partir cedo.", translation = "намереваюсь", created = "2026-10-15 21:14")))

        val report = importer.import(ByteArray(0))
        assertEquals(AnkiImportService.Report(added = 1, carded = 0, updated = 0, reviews = 3, unreadable = 0, missingLanguages = emptyList()), report)
        val term = assertNotNull(terms.findByTextLc(pt, "pretendo"))
        assertEquals(TermStatus.WELL_KNOWN, term.status, "forty days of memory are known")
        assertEquals("намереваюсь", term.translation)
        assertEquals("Eu pretendo partir cedo.", term.sentence)
        assertNotNull(term.ankiExportedAt, "in Anki already, so not exported again")
        val card = assertNotNull(cards.get(term.id))
        assertEquals(CardState.REVIEW, card.schedule.state)
        assertEquals(onDay(262), card.schedule.due)
        assertEquals(MemoryState(40.5, 5.25), card.schedule.memory)
        assertEquals(30, card.schedule.intervalDays)
        assertEquals(3, card.schedule.reps)
        assertEquals(at("2026-10-18T09:00:00Z"), card.schedule.lastReview)
        assertEquals(at("2026-10-15T21:14:00Z"), card.createdAt)
        val history = cards.reviews(term.id).sortedBy { it.at }
        assertEquals(listOf(CardState.NEW, CardState.LEARNING, CardState.REVIEW), history.map { it.stateBefore })
        assertEquals(listOf(Rating.GOOD, Rating.GOOD, Rating.GOOD), history.map { it.rating })
        assertEquals(listOf(0, 0, 2), history.map { it.elapsedDays })

        val again = importer.import(ByteArray(0))
        assertEquals(AnkiImportService.Report(added = 0, carded = 0, updated = 1, reviews = 0, unreadable = 0, missingLanguages = emptyList()), again)
        assertEquals(3, cards.reviews(term.id).size)
    }

    @Test
    fun savedWordsTakeAnkisScheduleAndKeepWhatTheyAre() = runTest {
        val pt = portuguese()
        val lobo = terms.save(Term(languageId = pt, text = "lobo", textLc = "lobo", status = TermStatus.NEW_1, sentence = "Uma frase com lobo."))
        val casa = terms.save(Term(languageId = pt, text = "casa", textLc = "casa", status = TermStatus.WELL_KNOWN))
        val gato = terms.save(Term(languageId = pt, text = "gato", textLc = "gato", status = TermStatus.UNKNOWN))
        // Lobo has a card answered here, which keeps the day it was made and its answer.
        val session = flashcards.next(pt)
        val made = session.card!!.createdAt
        flashcards.answer(session.card!!, Rating.GOOD)
        clock.now += 1.days

        val learningDue = at("2026-11-01T13:00:00Z").epochSeconds
        archive = AnkiArchive(
            dayZero,
            listOf(
                note(1, "lobo", card(ivl = 10, stability = 10.0, reps = 6, lapses = 1, reviews = listOf(review("2026-10-20T10:00:00Z", 1, 1, ivl = -600)))),
                note(2, "casa", card(ivl = 3, reps = 2)),
                note(3, "gato", card(type = 1, due = learningDue, ivl = 0, reps = 1, left = 1002)),
            ),
        )
        val report = importer.import(ByteArray(0))
        assertEquals(AnkiImportService.Report(added = 0, carded = 2, updated = 1, reviews = 1, unreadable = 0, missingLanguages = emptyList()), report)

        val loboCard = cards.get(lobo)!!
        assertEquals(TermStatus.LEARNING_4, terms.getById(lobo)!!.status, "raised to what ten days of memory earn")
        assertEquals(6, loboCard.schedule.reps)
        assertEquals(1, loboCard.schedule.lapses)
        assertEquals(onDay(262), loboCard.schedule.due)
        assertEquals(made, loboCard.createdAt)
        assertEquals(2, cards.reviews(lobo).size, "the answer here and Anki's")

        assertEquals(TermStatus.WELL_KNOWN, terms.getById(casa)!!.status)
        assertEquals(CardState.REVIEW, cards.get(casa)!!.schedule.state)

        val gatoCard = cards.get(gato)!!
        assertEquals(TermStatus.NEW_1, terms.getById(gato)!!.status)
        assertEquals(CardState.LEARNING, gatoCard.schedule.state)
        assertEquals(Instant.fromEpochSeconds(learningDue), gatoCard.schedule.due)
        assertEquals(2, gatoCard.schedule.remainingSteps)
        assertEquals(3, flashcards.browse(pt).cards.size)
    }

    @Test
    fun clozeNotesAreReadAndTheRestLeftOut() = runTest {
        val pt = portuguese()
        archive = AnkiArchive(
            dayZero,
            listOf(
                AnkiArchiveNote(10, mapOf("Text" to "Ah, desculpa, é a segunda porta à {{c1::direita::справа}}.", "Back Extra" to ""), emptyList(), listOf(card(type = 0, queue = -1, due = 5, ivl = 0, reps = 0))),
                note(11, "levar", card(ivl = 4, reps = 2)),
                note(12, "levar", card(ivl = 40, stability = 10.0, reps = 7)),
                AnkiArchiveNote(13, mapOf("Front" to "hello", "Back" to "olá"), emptyList(), listOf(card())),
                note(14, "Haus", card(), language = "de"),
            ),
        )
        val report = importer.import(ByteArray(0))
        assertEquals(AnkiImportService.Report(added = 2, carded = 0, updated = 0, reviews = 0, unreadable = 1, missingLanguages = listOf("de")), report)

        val direita = assertNotNull(terms.findByTextLc(pt, "direita"))
        assertEquals("справа", direita.translation)
        assertEquals("Ah, desculpa, é a segunda porta à direita.", direita.sentence)
        val card = cards.get(direita.id)!!
        assertEquals(CardState.NEW, card.schedule.state)
        assertEquals(true, card.suspended)

        val levar = assertNotNull(terms.findByTextLc(pt, "levar"))
        assertEquals(7, cards.get(levar.id)!!.schedule.reps, "the most answered of its notes")
        assertEquals(TermStatus.LEARNING_4, levar.status)
    }

    @Test
    fun aDayCardStillLearningFallsDueOnItsDay() = runTest {
        portuguese()
        archive = AnkiArchive(dayZero, listOf(note(1, "dia", card(type = 3, queue = 3, due = 260, ivl = 5, lapses = 1))))
        importer.import(ByteArray(0))
        val schedule = cards.get(terms.findByTextLc(settings.current.currentLanguageId, "dia")!!.id)!!.schedule
        assertEquals(CardState.RELEARNING, schedule.state)
        assertEquals(LocalDateTime(2026, 11, 1, 4, 0).toInstant(TimeZone.UTC), schedule.due, "day 260 of a collection begun on 14 February")
    }

    /** A card Anki kept no memory for gets one from its answers, played through FSRS. */
    @Test
    fun aCardWithoutMemoryHasItFromItsAnswers() = runTest {
        portuguese()
        val answers = listOf(review("2026-10-01T09:00:00Z", 3, 0), review("2026-10-02T09:00:00Z", 3, 1, ivl = 3), review("2026-10-05T09:00:00Z", 4, 1, ivl = 12))
        val plain = AnkiArchiveCard("Portuguese", 2, 2, 270, null, 12, 3, 0, 0, null, null, null, answers)
        archive = AnkiArchive(dayZero, listOf(note(1, "fogo", plain)))
        importer.import(ByteArray(0))
        val memory = cards.get(terms.findByTextLc(settings.current.currentLanguageId, "fogo")!!.id)!!.schedule.memory
        val fsrs = Fsrs()
        val expected = fsrs.next(fsrs.next(fsrs.next(null, 0, Rating.GOOD), 1, Rating.GOOD), 3, Rating.EASY)
        assertEquals(expected, memory)
    }
}
