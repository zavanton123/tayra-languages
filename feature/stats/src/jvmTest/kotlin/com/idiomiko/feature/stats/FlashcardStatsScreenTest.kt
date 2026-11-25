package com.idiomiko.feature.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import com.idiomiko.core.ui.components.LocalLearningLevels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.FlashcardRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.flashcards.CardSchedule
import com.idiomiko.core.domain.flashcards.CardState
import com.idiomiko.core.domain.flashcards.FlashcardReview
import com.idiomiko.core.domain.flashcards.FlashcardStatsService
import com.idiomiko.core.domain.flashcards.MemoryState
import com.idiomiko.core.domain.flashcards.Rating
import com.idiomiko.core.domain.flashcards.StatsPeriod
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import com.russhwolf.settings.MapSettings
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone

/** The flashcards of a language in figures, over a few months of answers. */
@OptIn(ExperimentalTestApi::class)
class FlashcardStatsScreenTest {
    private val now = Instant.parse("2026-10-20T18:00:00Z")
    private val clock = object : Clock { override fun now() = now }
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-card-stats", ".db").also { it.delete() }))
    private val service = FlashcardStatsService(FlashcardRepositoryImpl(provider), SettingsRepositoryImpl(MapSettings()), clock) { TimeZone.UTC }

    /** Sixty words learned over the last 120 days, each answered every few days since. */
    private val portuguese = runBlocking {
        val id = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese"))
        val terms = TermRepositoryImpl(provider)
        val cards = FlashcardRepositoryImpl(provider)
        val random = Random(9)
        repeat(60) { n ->
            val term = terms.save(Term(languageId = id, text = "palavra$n", textLc = "palavra$n", status = TermStatus.NEW_2, sentence = "Uma frase com palavra$n."))
            cards.reconcile(now)
            var day = random.nextInt(120)
            val reviews = mutableListOf(FlashcardReview(termId = term, at = now - day.days - 2.hours, rating = Rating.GOOD, stateBefore = CardState.NEW, intervalDays = 0, elapsedDays = 0, answerTime = 8.seconds))
            var interval = 1
            while (day - interval > 0) {
                day -= interval
                val rating = if (random.nextDouble() < 0.85) Rating.GOOD else Rating.AGAIN
                reviews += FlashcardReview(termId = term, at = now - day.days - random.nextInt(12).hours, rating = rating, stateBefore = CardState.REVIEW, intervalDays = interval * 2, elapsedDays = interval, answerTime = random.nextInt(3, 20).seconds)
                interval = if (rating == Rating.GOOD) interval * 2 else 1
            }
            cards.addReviews(reviews)
            val card = cards.get(term)!!
            cards.save(card.copy(schedule = CardSchedule(CardState.REVIEW, now + random.nextInt(0, 40).days, MemoryState(interval * 1.5, random.nextDouble(2.0, 9.0)), interval, 0, reviews.size, 0, reviews.last().at)))
        }
        id
    }

    /** With no cards yet, the page says which words get one, in the statuses the reader sees. */
    @Test
    fun withNoCardsThePageSaysWhichWordsGetOne() = runDesktopComposeUiTest(width = 1200, height = 900) {
        var levels by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalLearningLevels provides levels) {
                PageColumn(PaddingValues()) { FlashcardStatsContent(FlashcardStatsUiState(), onPeriod = {}) }
            }
        }
        onNodeWithText("Words you mark as Learning while reading get a card, and their figures show here.").assertExists()
        levels = true
        waitForIdle()
        onNodeWithText("Words you give a status of 1 to 4 while reading get a card, and their figures show here.").assertExists()
    }

    @Test
    fun theFiguresShowAndThePeriodChangesThem() = runDesktopComposeUiTest(width = 1586, height = 4200) {
        var state by mutableStateOf(FlashcardStatsUiState(loading = false, languageName = "Portuguese", stats = runBlocking { service.statistics(portuguese, StatsPeriod.MONTH) }))
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        PageColumn(PaddingValues()) {
                            FlashcardStatsContent(state, onPeriod = { chosen -> state = state.copy(period = chosen, stats = runBlocking { service.statistics(portuguese, chosen) }) })
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Future due").fetchSemanticsNodes().isNotEmpty() }
        for (tag in listOf("today-studied", "future-due", "card-counts-bar", "reviews-by-day", "added-by-day", "true-retention", "knowledge", "answer-buttons", "answered-by")) onNodeWithTag(tag).assertExists()
        // The calendar shows this year, with no arrow to a year after it.
        onNodeWithTag("calendar-year").assertTextEquals("2026")
        onNodeWithTag("calendar-next-year").assertDoesNotExist()
        // The finer charts wait behind More, and Less puts them away again.
        val finer = listOf("intervals", "stability", "difficulty", "recall", "hours")
        for (tag in finer) onNodeWithTag(tag).assertDoesNotExist()
        onNodeWithTag("flashcard-stats-more").performScrollTo().performClick()
        waitForIdle()
        for (tag in finer) onNodeWithTag(tag).assertExists()
        onNodeWithTag("card-counts-bar").assertExists()
        System.getenv("FLASHCARD_STATS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        // A column pointed at is named, with its value, in a tooltip.
        for ((chart, says) in listOf("future-due" to "due", "hours" to ":00")) {
            onNodeWithTag(chart).performScrollTo().performMouseInput { moveTo(Offset(width * 0.02f, height * 0.6f)) }
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("chart-tooltip").fetchSemanticsNodes().isNotEmpty() }
            val text = onAllNodesWithTag("chart-tooltip").fetchSemanticsNodes().single().config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
            assertTrue(says in text, "$chart: $text")
            onNodeWithTag(chart).performMouseInput { moveTo(Offset(-50f, -50f)) }
            waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("chart-tooltip").fetchSemanticsNodes().isEmpty() }
        }
        onNodeWithTag("period-YEAR").performClick()
        waitUntil(timeoutMillis = 5_000) { state.period == StatsPeriod.YEAR }
        onNodeWithText("Less").performScrollTo().performClick()
        waitForIdle()
        for (tag in finer) onNodeWithTag(tag).assertDoesNotExist()
        // The heading alone: no way back to the statistics above it, no line under it.
        onNodeWithText("Statistics").assertDoesNotExist()
        onNodeWithText("Your flashcards in Portuguese.").assertDoesNotExist()
    }
}
