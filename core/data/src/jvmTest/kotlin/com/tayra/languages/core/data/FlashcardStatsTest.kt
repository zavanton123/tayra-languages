package com.tayra.languages.core.data

import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.flashcards.AnswerMode
import com.tayra.languages.core.domain.flashcards.CardCounts
import com.tayra.languages.core.domain.flashcards.CardSchedule
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.FlashcardStatsService
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.Pass
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.flashcards.RetentionSpan
import com.tayra.languages.core.domain.flashcards.ReviewKind
import com.tayra.languages.core.domain.flashcards.StatsPeriod
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A language's flashcards in figures, from a history small enough to count by hand. */
class FlashcardStatsTest {
    private val now = Instant.parse("2026-10-20T18:00:00Z")
    private val clock = object : Clock { override fun now() = now }
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-stats", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val cards = FlashcardRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val stats = FlashcardStatsService(cards, settings, clock) { TimeZone.UTC }

    /** Today at [hour] o'clock, [daysAgo] days back. */
    private fun at(daysAgo: Int, hour: Int, minute: Int = 0) = Instant.parse("2026-10-20T00:00:00Z") - daysAgo.days + hour.hours + minute.minutes

    private suspend fun card(languageId: Long, text: String, schedule: CardSchedule?, suspended: Boolean = false): Long {
        val id = terms.save(Term(languageId = languageId, text = text, textLc = text, status = TermStatus.NEW_1, sentence = "Uma frase com $text."))
        cards.reconcile(now)
        val card = cards.get(id)!!
        cards.save(card.copy(schedule = schedule ?: card.schedule, suspended = suspended))
        return id
    }

    private fun review(termId: Long, at: Instant, rating: Rating, before: CardState, interval: Int, elapsed: Int, time: Int? = null, by: AnswerMode = AnswerMode.SHOW) =
        FlashcardReview(termId = termId, at = at, rating = rating, stateBefore = before, intervalDays = interval, elapsedDays = elapsed, answeredBy = by, answerTime = time?.seconds)

    @Test
    fun theFiguresOfASmallHistory() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val memory = MemoryState(10.0, 5.5)
        val a = card(pt, "casa", CardSchedule(CardState.REVIEW, now + 10.days, memory, 10, 0, 3, 0, at(0, 9)))
        val b = card(pt, "gato", CardSchedule(CardState.REVIEW, now + 1.days, memory, 1, 0, 2, 0, at(0, 8, 10)))
        val c = card(pt, "lobo", CardSchedule(CardState.REVIEW, now + 4.days, memory, 5, 0, 4, 1, at(1, 15, 10)))
        card(pt, "mesa", null)
        card(pt, "flor", CardSchedule(CardState.LEARNING, now + 10.minutes, memory, 0, 1, 1, 0, now - 1.hours))
        card(pt, "rio", CardSchedule(CardState.REVIEW, now + 3.days, memory, 30, 0, 5, 0, now - 20.days), suspended = true)
        cards.addReviews(listOf(
            review(a, at(2, 10), Rating.GOOD, CardState.NEW, 0, 0, time = 5),
            review(a, at(2, 10, 10), Rating.GOOD, CardState.LEARNING, 3, 0, time = 4),
            review(a, at(0, 9), Rating.GOOD, CardState.REVIEW, 10, 2, time = 6, by = AnswerMode.TYPE),
            review(b, at(0, 8), Rating.AGAIN, CardState.NEW, 0, 0, time = 9),
            review(b, at(0, 8, 10), Rating.GOOD, CardState.LEARNING, 1, 0, time = 3),
            // Mature from the days since it, with no interval before it: remembered long ago, forgotten yesterday.
            review(c, at(40, 12), Rating.GOOD, CardState.REVIEW, 30, 25),
            review(c, at(1, 15), Rating.AGAIN, CardState.REVIEW, 5, 39),
            review(c, at(1, 15, 10), Rating.GOOD, CardState.RELEARNING, 5, 0),
        ))

        val s = stats.statistics(pt, StatsPeriod.MONTH)
        with(s.todayStats) {
            assertEquals(3, studied)
            assertEquals(18.seconds, time)
            assertEquals(1, again)
            assertEquals(mapOf(ReviewKind.LEARNING to 2, ReviewKind.YOUNG to 1), kinds)
            assertEquals(Pass(2, 3), correct)
        }
        assertEquals(CardCounts(new = 1, learning = 1, relearning = 0, young = 3, mature = 0, suspended = 1), s.counts)
        assertEquals(mapOf(0 to 1, 1 to 1, 4 to 1, 10 to 1), s.dueByDay, "the suspended card is left out")
        assertEquals(0, s.overdue)
        assertEquals(Pass(1, 1), s.retention.getValue(RetentionSpan.TODAY)[ReviewKind.YOUNG])
        assertEquals(Pass(0, 1), s.retention.getValue(RetentionSpan.YESTERDAY)[ReviewKind.MATURE])
        assertEquals(Pass(1, 2), s.retention.getValue(RetentionSpan.ALL)[ReviewKind.MATURE])
        assertEquals(listOf(1, 0, 4, 0), s.buttons[ReviewKind.LEARNING], "relearning counted with learning, as Anki's buttons do")
        assertEquals(listOf(0, 0, 1, 0), s.buttons[ReviewKind.YOUNG])
        assertEquals(listOf(1, 0, 0, 0), s.buttons[ReviewKind.MATURE], "the answer forty days ago is outside the month")
        assertEquals(Pass(1, 2), s.hours[8].answers)
        assertEquals(Pass(1, 1), s.answeredBy[AnswerMode.TYPE])
        assertEquals(3, s.reviewsByDay.getValue(s.today).total)
        assertEquals(18.seconds, s.reviewsByDay.getValue(s.today).time)
        assertEquals(listOf(10, 1, 5), s.intervals)
        assertEquals(1, s.calendar[s.today - 40], "the calendar keeps every day")
        assertEquals(4, s.knowledgeOf, "every answered card holding a memory, the suspended one aside")
        assertTrue(s.knowledge in 0.0..4.0)
        assertEquals(s.today - 29, s.firstDay)

        // Over all time, the answer forty days ago counts too, and the period starts with it.
        val all = stats.statistics(pt, StatsPeriod.ALL)
        assertEquals(listOf(1, 0, 1, 0), all.buttons[ReviewKind.MATURE])
        assertEquals(all.today - 40, all.firstDay)
    }
}
