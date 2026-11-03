package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.flashcards.CardSchedule
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.FlashcardService
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.flashcards.RetentionAdvisor
import com.tayra.languages.core.domain.flashcards.SimulatorSettings
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The desired retention helper: the learner's ways read from the answers, and every retention simulated. */
class RetentionAdvisorTest {
    private val now = Instant.parse("2026-10-20T18:00:00Z")
    private val clock = object : Clock { override fun now() = now }
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-retention", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val cards = FlashcardRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val advisor = RetentionAdvisor(cards, settings, clock) { TimeZone.UTC }

    private fun close(expected: Double, actual: Double) = assertTrue(abs(expected - actual) < 1e-9, "$expected against $actual")

    @Test
    fun theLearnersWaysAreReadFromTheAnswersAndDrawnTowardsTheDefaults() {
        // Sixty new cards, each answered Good and Good again ten minutes on, the first in ten seconds.
        val reviews = (1L..60L).flatMap { term ->
            val at = now - term.days
            listOf(
                FlashcardReview(termId = term, at = at, rating = Rating.GOOD, stateBefore = CardState.NEW, intervalDays = 0, elapsedDays = 0, answerTime = 10.seconds),
                FlashcardReview(termId = term, at = at + 10.minutes, rating = Rating.GOOD, stateBefore = CardState.LEARNING, intervalDays = 1, elapsedDays = 0),
            )
        }
        val read = advisor.settingsFrom(reviews, FlashcardService.schedulerConfig(settings.current))
        val defaults = SimulatorSettings()
        val weight = 60.0 / 110.0
        close(weight * 1.0 + (1 - weight) * defaults.firstRating[2], read.firstRating[2])
        close((1 - weight) * defaults.firstRating[0], read.firstRating[0])
        close(weight * 10.0 + (1 - weight) * defaults.costs[0][2], read.costs[0][2])
        assertEquals(defaults.costs[1], read.costs[1], "no timed reviews: the defaults")
        // Good after Good sixty times, over one of each added.
        val good = 64.0
        val stepWeight = good / (50 + good)
        close(stepWeight * (61.0 / good) + (1 - stepWeight) * defaults.learningSteps[2][2], read.learningSteps[2][2])
    }

    @Test
    fun everyRetentionIsSimulatedAndTheMostEfficientSuggested() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        repeat(80) { n ->
            val id = terms.save(Term(languageId = pt, text = "w$n", textLc = "w$n", status = TermStatus.NEW_2))
            cards.reconcile(now)
            val card = cards.get(id)!!
            cards.save(card.copy(schedule = CardSchedule(CardState.REVIEW, now + (n % 20).days, MemoryState(5.0 + n, 5.0), 5 + n % 20, 0, 4, 0, now - 3.days)))
        }
        val advice = advisor.advise(pt, 365)
        assertEquals((70..99).toList(), advice.points.map { it.retention })
        assertEquals(90, advice.current)
        assertTrue(advice.suggested in 70..95)
        val (low, high) = advice.points.first() to advice.points.last()
        assertTrue(high.minutesPerDay > low.minutesPerDay * 2, "${low.minutesPerDay} against ${high.minutesPerDay}")
        assertTrue(high.remembered > low.remembered)
        assertEquals(80, advice.cards)
    }
}
