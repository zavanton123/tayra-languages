package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.FlashcardScheduler
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.Fsrs
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.PersonalScheduling
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** A language's own scheduling: trained on its answers, kept only when better, its cards' memory worked out again. */
class PersonalSchedulingTest {

    private class MovingClock(var now: Instant) : Clock {
        override fun now() = now
        fun advance(by: Duration) { now += by }
    }

    private val start = Instant.parse("2026-01-05T12:00:00Z")
    private val clock = MovingClock(start)
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-personal", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider, clock)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val cards = FlashcardRepositoryImpl(provider)
    private val personal = PersonalScheduling(cards, settings, clock)
    private val service = FlashcardService(cards, terms, settings, FlashcardScheduler(timeZone = { TimeZone.UTC }), clock)

    /** A learner who forgets sooner than the defaults expect. */
    private val truth = Fsrs.DEFAULT_PARAMETERS.copyOf().also {
        it[0] = 0.1; it[1] = 0.6; it[2] = 1.2; it[3] = 4.0; it[8] = 1.4; it[10] = 0.6; it[20] = 0.3
    }

    /** [count] words of [languageId] learned and reviewed by [truth]'s learner over [days] days. */
    private suspend fun learned(languageId: Long, count: Int, days: Int, seed: Int = 3): List<Long> {
        val random = Random(seed)
        val fsrs = Fsrs(truth)
        return (0 until count).map { n ->
            val termId = terms.save(Term(languageId = languageId, text = "w$n", textLc = "w$n", status = TermStatus.NEW_1, sentence = "Uma frase com w$n."))
            cards.reconcile(clock.now())
            val reviews = mutableListOf<FlashcardReview>()
            var day = random.nextInt(days / 3)
            fun at(day: Int, minutes: Int = 0) = start + day.days + minutes.minutes
            val first = Rating.of(random.nextInt(1, 5))
            var state = fsrs.next(null, 0, first)
            reviews += FlashcardReview(termId = termId, at = at(day), rating = first, stateBefore = CardState.NEW, intervalDays = 0, elapsedDays = 0)
            state = fsrs.next(state, 0, Rating.GOOD)
            reviews += FlashcardReview(termId = termId, at = at(day, 10), rating = Rating.GOOD, stateBefore = CardState.LEARNING, intervalDays = 1, elapsedDays = 0)
            while (true) {
                val interval = fsrs.interval(state.stability, 0.9).toInt().coerceAtLeast(1)
                if (day + interval >= days) break
                day += interval
                val recalled = random.nextDouble() < fsrs.retrievability(interval.toDouble(), state.stability)
                val rating = if (recalled) Rating.GOOD else Rating.AGAIN
                state = fsrs.next(state, interval, rating)
                reviews += FlashcardReview(termId = termId, at = at(day), rating = rating, stateBefore = CardState.REVIEW, intervalDays = interval, elapsedDays = interval)
            }
            cards.addReviews(reviews)
            val card = cards.get(termId)!!
            cards.save(card.copy(schedule = card.schedule.copy(state = CardState.REVIEW, memory = MemoryState(5.0, 5.0), intervalDays = 5, reps = reviews.size, due = at(days), lastReview = reviews.last().at)))
            termId
        }
    }

    @Test
    fun aLanguagesOwnAnswersTrainItsParameters() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val de = languages.save(Language(name = "German"))
        val words = learned(pt, 500, 200)
        clock.now = start + 200.days
        assertTrue(personal.isDue(pt))
        assertFalse(personal.isDue(de), "no answers")

        val trained = assertIs<PersonalScheduling.Outcome.Trained>(personal.optimize(pt))
        assertTrue(trained.gain > 0.01, "${trained.gain}")
        val saved = assertNotNull(personal.current(pt))
        val parameters = assertNotNull(saved.parameters)
        assertEquals(21, parameters.size)
        assertEquals(cards.reviewCount(pt), saved.reviews)
        assertNull(personal.current(de), "each language its own")
        assertFalse(personal.isDue(pt), "just trained")

        // The cards' memory is worked out again from their answers; their due dates stay.
        val fsrs = Fsrs(parameters.toDoubleArray())
        var memory: MemoryState? = null
        for (review in cards.reviews(words[0]).reversed()) memory = fsrs.next(memory, review.elapsedDays, review.rating)
        val card = cards.get(words[0])!!
        assertEquals(memory, card.schedule.memory)
        assertEquals(start + 200.days, card.schedule.due)

        // The scheduling of the language's cards follows its parameters.
        val shown = service.browse(pt).cards.first { it.card.termId == words[0] }
        assertEquals(fsrs.retrievability(((clock.now - card.schedule.lastReview!!).inWholeDays).toDouble(), memory!!.stability), shown.retrievability!!, 1e-9)

        // Trained again on the same answers, they are no better: those in use stay.
        assertIs<PersonalScheduling.Outcome.Kept>(personal.optimize(pt))
        assertEquals(parameters, personal.current(pt)?.parameters)

        personal.useDefaults(pt)
        assertNull(personal.current(pt))
        assertNotEquals(memory, cards.get(words[0])!!.schedule.memory, "worked out with the defaults again")
    }

    @Test
    fun aFewAnswersTeachNothingYet() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        learned(pt, 3, 30)
        assertIs<PersonalScheduling.Outcome.TooFew>(personal.optimize(pt))
        val noted = assertNotNull(personal.current(pt), "the try is noted")
        assertNull(noted.parameters)
        assertFalse(personal.isDue(pt))
    }
}
