package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardScheduler
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Flashcards over a real database: which terms have cards, what is shown when, and how answers move a term's status. */
class FlashcardServiceTest {

    private class MovingClock(var now: Instant) : Clock {
        override fun now() = now
        fun advance(by: Duration) { now += by }
    }

    private val file = File.createTempFile("tayra-cards", ".db").also { it.delete() }
    private val clock = MovingClock(Instant.parse("2026-10-05T12:00:00Z"))
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider, clock)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val cards = FlashcardRepositoryImpl(provider)
    private val termService = TermService(terms, languages)
    private val service = FlashcardService(cards, terms, settings, FlashcardScheduler(timeZone = { TimeZone.UTC }), clock)

    private suspend fun language(): Long = languages.save(Language(name = "Portuguese"))

    private suspend fun term(languageId: Long, text: String, status: TermStatus = TermStatus.NEW_1): Long {
        clock.advance(1.minutes)
        return terms.save(Term(languageId = languageId, text = text, textLc = text, status = status, translation = "<$text>"))
    }

    private suspend fun status(id: Long) = terms.getById(id)!!.status

    /** Answers the card on show, which must be [termId]'s. */
    private suspend fun answer(languageId: Long, termId: Long, rating: Rating): FlashcardService.Answered {
        val session = service.next(languageId)
        assertEquals(termId, session.card?.termId, "the card on show")
        return service.answer(session.card!!, rating)!!
    }

    @Test
    fun onlyTermsBeingLearnedHaveCards() = runTest {
        val id = language()
        val learning = term(id, "lobo", TermStatus.NEW_1)
        val later = term(id, "floresta", TermStatus.LEARNING_4)
        val known = term(id, "casa", TermStatus.WELL_KNOWN)
        val ignored = term(id, "de", TermStatus.IGNORED)

        assertEquals(2, service.counts(id).new)
        assertNotNull(service.cardFor(learning))
        assertNotNull(service.cardFor(later))
        assertNull(service.cardFor(known))
        assertNull(service.cardFor(ignored))
        // New cards come in the order their terms were added.
        assertEquals(learning, service.next(id).card?.termId)
    }

    /** A card blanks out the form the sentence holds: the term's own, or a parent's or sibling's when the sentence came through the family. */
    @Test
    fun theCardBlanksOutTheFormOfTheFamilyReadInTheSentence() = runTest {
        val id = language()
        val lemma = term(id, "destruir")
        val past = term(id, "destruíram")
        val future = term(id, "destruirão")
        terms.setParents(past, listOf(lemma))
        terms.setParents(future, listOf(lemma))
        terms.updateSentence(past, "Eu vou destruir esta parede.")
        terms.updateSentence(future, "Eles destruíram a casa.")
        terms.updateSentence(lemma, "Nada aqui.")

        assertEquals(7 until 15, service.clozeRange(terms.getById(past)!!), "the parent's form")
        assertEquals(5 until 15, service.clozeRange(terms.getById(future)!!), "a sibling's form")
        assertNull(service.clozeRange(terms.getById(lemma)!!), "no form of the family in the sentence")
    }

    @Test
    fun cardsBelongToTheirLanguage() = runTest {
        val portuguese = language()
        val spanish = languages.save(Language(name = "Spanish"))
        term(portuguese, "lobo")
        val gato = term(spanish, "gato")
        assertEquals(1, service.counts(portuguese).total)
        assertEquals(gato, service.next(spanish).card?.termId)
        assertEquals(2, service.counts(null).total)
    }

    @Test
    fun aNewCardIsLearnedInStepsAndThenWaitsDays() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val session = service.next(id)
        assertEquals(setOf(Rating.AGAIN, Rating.HARD, Rating.GOOD, Rating.EASY), session.outcomes.keys)

        answer(id, lobo, Rating.GOOD)
        // Ten minutes to wait: nothing else is left, so the card is shown early, as in Anki.
        assertEquals(CardState.LEARNING, service.cardFor(lobo)!!.schedule.state)
        assertEquals(1, service.counts(id).learning)
        clock.advance(10.minutes)
        answer(id, lobo, Rating.GOOD)

        val card = service.cardFor(lobo)!!
        assertEquals(CardState.REVIEW, card.schedule.state)
        assertTrue(card.schedule.intervalDays >= 1)
        assertEquals(TermStatus.NEW_2, status(lobo), "a card that waits a day or more is status 2")
        assertNull(service.next(id).card, "nothing is due until then")
        assertEquals(0, service.counts(id).total)
    }

    @Test
    fun statusRisesAsTheIntervalGrowsAndDropsALevelWhenForgotten() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        answer(id, lobo, Rating.EASY)
        // Easy sends a new card about eight days out: a week or more is status 3.
        assertEquals(TermStatus.LEARNING_3, status(lobo))

        val seen = mutableListOf(status(lobo))
        repeat(6) {
            clock.now = service.cardFor(lobo)!!.schedule.due + 8.hours
            answer(id, lobo, Rating.GOOD)
            seen += status(lobo)
        }
        assertEquals(seen.sortedBy { it.value }, seen, "the status never falls while the card is remembered: $seen")
        assertTrue(TermStatus.LEARNING_4 in seen, "three weeks or more is status 4: $seen")
        assertEquals(TermStatus.WELL_KNOWN, seen.last(), "ninety days or more is known: $seen")
        // A term the flashcards made known keeps its card.
        val card = service.cardFor(lobo)
        assertNotNull(card)
        assertTrue(card.schedule.intervalDays >= 90)

        clock.now = card.schedule.due + 8.hours
        answer(id, lobo, Rating.AGAIN)
        assertEquals(TermStatus.LEARNING_4, status(lobo), "forgetting a review drops one level")
        clock.advance(10.minutes)
        answer(id, lobo, Rating.AGAIN)
        assertEquals(TermStatus.LEARNING_4, status(lobo), "failing again while relearning drops no further")
    }

    @Test
    fun aStatusChangedByHandWins() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        answer(id, lobo, Rating.EASY)
        assertEquals(1, service.cardFor(lobo)!!.schedule.reps)

        // Between learning levels the card carries on.
        termService.setStatus(listOf(lobo), TermStatus.LEARNING_4)
        assertEquals(1, service.cardFor(lobo)!!.schedule.reps)
        assertEquals(TermStatus.LEARNING_4, status(lobo))

        // Known by hand: the card is retired.
        termService.setStatus(listOf(lobo), TermStatus.WELL_KNOWN)
        assertNull(service.cardFor(lobo))
        assertEquals(0, service.counts(id).total)

        // Back to learning: the card starts over.
        termService.setStatus(listOf(lobo), TermStatus.NEW_2)
        val card = service.cardFor(lobo)!!
        assertEquals(CardState.NEW, card.schedule.state)
        assertEquals(0, card.schedule.reps)
        assertEquals(1, service.counts(id).new)
    }

    @Test
    fun aTermTheCardsMadeKnownStartsOverWhenMovedBackToLearning() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        answer(id, lobo, Rating.EASY)
        while (status(lobo) != TermStatus.WELL_KNOWN) {
            clock.now = service.cardFor(lobo)!!.schedule.due + 8.hours
            answer(id, lobo, Rating.EASY)
        }
        termService.setStatus(listOf(lobo), TermStatus.LEARNING_3)
        val card = service.cardFor(lobo)!!
        assertEquals(CardState.NEW, card.schedule.state)
        assertEquals(TermStatus.LEARNING_3, status(lobo))
    }

    @Test
    fun deletingATermRemovesItsCard() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        answer(id, lobo, Rating.GOOD)
        termService.deleteAll(setOf(lobo))
        assertNull(service.cardFor(lobo))
        assertEquals(0, service.counts(id).total)
    }

    @Test
    fun theDailyLimitsHoldBackNewCardsAndReviews() = runTest {
        val id = language()
        val words = listOf("um", "dois", "três").map { term(id, it) }
        settings.update { it.copy(flashcardNewPerDay = 2) }
        assertEquals(2, service.counts(id).new)
        answer(id, words[0], Rating.EASY)
        answer(id, words[1], Rating.EASY)
        assertNull(service.next(id).card, "the third new card waits for tomorrow")
        assertEquals(0, service.counts(id).new)

        clock.advance(1.days)
        assertEquals(1, service.counts(id).new)
        assertEquals(words[2], service.next(id).card?.termId)

        // Once all three reviews are due, a limit of one shows one a day.
        answer(id, words[2], Rating.EASY)
        clock.now = words.maxOf { service.cardFor(it)!!.schedule.due } + 8.hours
        assertEquals(3, service.counts(id).review)
        settings.update { it.copy(flashcardReviewsPerDay = 1) }
        assertEquals(1, service.counts(id).review)
        answer(id, service.next(id).card!!.termId, Rating.GOOD)
        assertNull(service.next(id).card)
        settings.update { it.copy(flashcardReviewsPerDay = 0) }
        assertEquals(2, service.counts(id).review)
    }

    @Test
    fun anAnswerCanBeTakenBack() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val before = service.cardFor(lobo)!!
        val answered = answer(id, lobo, Rating.EASY)
        assertEquals(TermStatus.LEARNING_3, status(lobo))

        service.undo(answered)
        assertEquals(before, service.cardFor(lobo))
        assertEquals(TermStatus.NEW_1, status(lobo))
        assertEquals(1, service.counts(id).new, "the day's allowance of new cards is back too")
    }

    @Test
    fun aSuspendedCardIsNotShownAndRestartingForgetsItsHistory() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        val floresta = term(id, "floresta")
        service.setSuspended(lobo, true)
        assertEquals(floresta, service.next(id).card?.termId)
        assertEquals(1, service.counts(id).new)
        service.setSuspended(lobo, false)
        assertEquals(lobo, service.next(id).card?.termId)

        answer(id, lobo, Rating.EASY)
        clock.advance(1.minutes)
        service.restart(lobo)
        val card = service.cardFor(lobo)!!
        assertEquals(CardState.NEW, card.schedule.state)
        assertNull(card.schedule.memory)
        // Restarted cards queue behind the other new ones.
        assertEquals(floresta, service.next(id).card?.termId)
    }

    @Test
    fun aDatabaseFromBeforeFlashcardsGainsThem() = runTest {
        val id = language()
        val lobo = term(id, "lobo")
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use {
            it.createStatement().execute("DROP TABLE flashcards")
            it.createStatement().execute("DROP TABLE flashcard_reviews")
            it.createStatement().execute("PRAGMA user_version = 9")
        }
        val reopened = DatabaseProvider(DatabaseDriverFactory(file))
        val upgraded = FlashcardService(FlashcardRepositoryImpl(reopened), TermRepositoryImpl(reopened, clock), settings, FlashcardScheduler(timeZone = { TimeZone.UTC }), clock)
        assertEquals(lobo, upgraded.next(id).card?.termId)
    }
}
