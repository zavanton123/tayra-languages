package com.tayra.languages.feature.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
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
import com.tayra.languages.core.domain.flashcards.FlashcardStatsService
import com.tayra.languages.core.domain.flashcards.MemoryState
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.flashcards.StatsPeriod
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The flashcards of a language in figures, over a few months of answers. */
@OptIn(ExperimentalTestApi::class)
class FlashcardStatsScreenTest {
    private val now = Instant.parse("2026-10-20T18:00:00Z")
    private val clock = object : Clock { override fun now() = now }
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-card-stats", ".db").also { it.delete() }))
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

    @Test
    fun theFiguresShowAndThePeriodChangesThem() = runDesktopComposeUiTest(width = 1586, height = 4200) {
        var state by mutableStateOf(FlashcardStatsUiState(loading = false, languageName = "Portuguese", stats = runBlocking { service.statistics(portuguese, StatsPeriod.MONTH) }))
        val visited = mutableListOf<Route>()
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        PageColumn(PaddingValues()) {
                            FlashcardStatsContent(state, onPeriod = { chosen -> state = state.copy(period = chosen, stats = runBlocking { service.statistics(portuguese, chosen) }) }, onNavigate = { visited += it })
                        }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Future due").fetchSemanticsNodes().isNotEmpty() }
        for (tag in listOf("today-studied", "future-due", "card-counts-bar", "reviews-by-day", "added-by-day", "intervals", "stability", "difficulty", "recall", "true-retention", "knowledge", "hours", "answer-buttons", "answered-by")) onNodeWithTag(tag).assertExists()
        onNodeWithTag("card-counts-bar").assertExists()
        System.getenv("FLASHCARD_STATS_SCREENSHOT")?.let { ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }

        onNodeWithTag("period-YEAR").performClick()
        waitUntil(timeoutMillis = 5_000) { state.period == StatsPeriod.YEAR }
        onAllNodesWithText("Statistics")[0].performClick()
        assertEquals(listOf<Route>(Route.Stats), visited)
    }
}
