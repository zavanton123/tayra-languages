package com.tayra.languages.feature.stats

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.LearningStatsRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.WordsReadEntry
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.io.File
import java.sql.DriverManager
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** The statistics of the language being learned, worked out from what the database records. */
@OptIn(ExperimentalTestApi::class)
class StatsScreenTest {
    private val file = File.createTempFile("tayra-stats", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 10, 7)
    private val clock = object : Clock { override fun now(): Instant = today.atStartOfDayIn(zone) + 15.hours }
    private val stats = StatsService(WordsReadRepositoryImpl(provider, zone), LearningStatsRepositoryImpl(provider, zone), clock, zone)

    private fun at(daysAgo: Int): Instant = today.atStartOfDayIn(zone) - (daysAgo * 24).hours + 12.hours

    private val portuguese = runBlocking {
        val id = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese"))
        val words = WordsReadRepositoryImpl(provider, zone)
        // Read on the last three days, and on four days in a row a month ago.
        for ((daysAgo, count) in listOf(0 to 300, 1 to 450, 2 to 120, 30 to 200, 31 to 200, 32 to 200, 33 to 200)) {
            words.add(WordsReadEntry(languageId = id, pageId = null, readAt = at(daysAgo), wordCount = count))
        }
        val terms = TermRepositoryImpl(provider)
        val saved = listOf(TermStatus.NEW_1, TermStatus.NEW_1, TermStatus.LEARNING_3, TermStatus.WELL_KNOWN, TermStatus.WELL_KNOWN, TermStatus.WELL_KNOWN, TermStatus.IGNORED)
            .mapIndexed { i, status -> terms.save(Term(languageId = id, text = "palavra$i", textLc = "palavra$i", status = status)) }
        // Two words were saved this week, the others three weeks ago.
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            saved.drop(2).forEach { c.createStatement().execute("UPDATE terms SET created_at = ${at(21).toEpochMilliseconds()} WHERE id = $it") }
            saved.take(2).forEach { c.createStatement().execute("UPDATE terms SET created_at = ${at(1).toEpochMilliseconds()} WHERE id = $it") }
        }
        val cards = FlashcardRepositoryImpl(provider)
        for ((i, rating) in listOf(Rating.GOOD, Rating.AGAIN, Rating.EASY, Rating.GOOD).withIndex()) {
            cards.addReview(FlashcardReview(termId = saved[i % 2], at = at(i), rating = rating, stateBefore = CardState.REVIEW, intervalDays = 1, elapsedDays = 1))
        }
        id
    }

    @Test
    fun theOverviewCountsReadingWordsAndReviews() = runDesktopComposeUiTest(width = 1586, height = 1900) {
        val overview = runBlocking { stats.overview(portuguese, "Portuguese") }
        assertEquals(1670, overview.wordsRead)
        assertEquals(870, overview.wordsReadThisWeek)
        assertEquals(7, overview.daysRead)
        assertEquals(3, overview.streak)
        assertEquals(4, overview.longestStreak)
        assertEquals(3, overview.known)
        assertEquals(3, overview.learning)
        assertEquals(1, overview.ignored)
        assertEquals(2, overview.savedThisWeek)
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 2), overview.savedByWeek)
        assertEquals(4, overview.reviewsTotal)
        assertEquals(75, overview.rememberedPercent)

        val summary = runBlocking { stats.summary() }
        setContent {
            TayraTheme {
                ProvideWindowWidth {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        PageColumn(PaddingValues()) { StatsContent(StatsUiState(loading = false, overview = overview, summary = summary)) }
                    }
                }
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Reading activity")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("STATS_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }
        // In its tile, and as the year and total of the language in the table.
        assertEquals(3, onAllNodesWithText("1,670").fetchSemanticsNodes().size)
        onNodeWithText("3 days").assertExists()
        onNodeWithText("Longest: 4 days").assertExists()
        onNodeWithText("4 reviews, remembered 75% of the time").assertExists()
        onNodeWithTag("heatmap").assertExists()
    }
}
