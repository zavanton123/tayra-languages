package com.idiomiko.core.data

import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.FlashcardRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.LearningStatsRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.domain.flashcards.CardState
import com.idiomiko.core.domain.flashcards.FlashcardReview
import com.idiomiko.core.domain.flashcards.Rating
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.stats.StudyTime
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/** What the statistics read from the database about a language's study. */
class LearningStatsTest {
    private val file = File.createTempFile("idiomiko-learning-stats", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 11, 20)
    private val noon = today.atStartOfDayIn(zone) + 12.hours
    private val stats = LearningStatsRepositoryImpl(provider, zone)

    private val language = runBlocking { LanguageRepositoryImpl(provider).save(Language(name = "Portuguese")) }
    private val term = runBlocking { TermRepositoryImpl(provider).save(Term(languageId = language, text = "casa", textLc = "casa", status = TermStatus.NEW_1)) }

    private fun review(at: kotlin.time.Instant, taken: kotlin.time.Duration?) = runBlocking {
        FlashcardRepositoryImpl(provider).addReview(
            FlashcardReview(termId = term, at = at, rating = Rating.GOOD, stateBefore = CardState.REVIEW, intervalDays = 1, elapsedDays = 1, answerTime = taken),
        )
    }

    @Test
    fun theAnswersTimeAddsUpByDayAndAnUntimedOneCountsAsUsual() = runBlocking {
        review(noon, 4.seconds)
        review(noon, 6.seconds)
        review(noon - 24.hours, null)
        val activity = stats.activity(language)
        assertEquals(10.seconds, activity.reviewTime[today])
        assertEquals(StudyTime.UNTIMED_ANSWER, activity.reviewTime[LocalDate(2026, 11, 19)])
        // Only the timed answers say how long an answer takes.
        assertEquals(5.seconds, activity.averageAnswer)
    }

    @Test
    fun theTimeListenedAddsUpByDay() = runBlocking {
        val log = com.idiomiko.core.data.repository.ListeningLogImpl(provider)
        log.record(language, noon, 40.seconds)
        log.record(language, noon + 1.hours, 20.seconds)
        log.record(language, noon - 24.hours, 30.seconds)
        val activity = stats.activity(language)
        assertEquals(60.seconds, activity.listenTime[today])
        assertEquals(30.seconds, activity.listenTime[LocalDate(2026, 11, 19)])
    }

    @Test
    fun theWordsSavedLastComeFirstWithTheirSentences() = runBlocking {
        val terms = TermRepositoryImpl(provider)
        val gato = terms.save(Term(languageId = language, text = "gato", textLc = "gato", status = TermStatus.LEARNING_3, sentence = "O gato dorme."))
        terms.save(Term(languageId = language, text = "sim", textLc = "sim", status = TermStatus.WELL_KNOWN))
        val recent = stats.recentlySaved(language, limit = 5)
        // Known on sight is not saved to learn; the later word comes first.
        assertEquals(listOf("gato", "casa"), recent.map { it.text })
        assertEquals("O gato dorme.", recent.first { it.termId == gato }.sentence)
        assertEquals(null, recent.last().sentence)
        assertEquals(1, stats.recentlySaved(language, limit = 1).size)
    }
}
