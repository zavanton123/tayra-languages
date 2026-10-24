package com.tayra.languages.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.FlashcardRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.LearningStatsRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.LessonBook
import com.tayra.languages.core.domain.courses.LessonProgress
import com.tayra.languages.core.domain.flashcards.CardState
import com.tayra.languages.core.domain.flashcards.FlashcardReview
import com.tayra.languages.core.domain.flashcards.Rating
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.model.WordsReadEntry
import com.tayra.languages.core.domain.service.StatsService
import com.tayra.languages.core.ui.components.PageColumn
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.navigation.Route
import com.tayra.languages.core.ui.theme.TayraTheme
import java.io.File
import java.sql.DriverManager
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** The home page: today's words and streak from what the database records, and the ways back into the study. */
@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {
    private val file = File.createTempFile("tayra-home", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 10, 16)
    private val clock = object : Clock { override fun now(): Instant = today.atStartOfDayIn(zone) + 15.hours }
    private val stats = StatsService(WordsReadRepositoryImpl(provider, zone), LearningStatsRepositoryImpl(provider, zone), clock, zone)

    private fun at(daysAgo: Int): Instant = today.atStartOfDayIn(zone) - (daysAgo * 24).hours + 12.hours

    /** Read three days ago, answered flashcards the day before yesterday and yesterday, and saved two words today. */
    private val activity = runBlocking {
        val id = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese"))
        WordsReadRepositoryImpl(provider, zone).add(WordsReadEntry(languageId = id, pageId = null, readAt = at(3), wordCount = 400))
        val terms = TermRepositoryImpl(provider)
        val saved = listOf("casa", "gato").map { terms.save(Term(languageId = id, text = it, textLc = it, status = TermStatus.NEW_1)) }
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            saved.forEach { c.createStatement().execute("UPDATE terms SET created_at = ${at(0).toEpochMilliseconds()} WHERE id = $it") }
        }
        val cards = FlashcardRepositoryImpl(provider)
        for (daysAgo in listOf(2, 1)) {
            cards.addReview(FlashcardReview(termId = saved[0], at = at(daysAgo), rating = Rating.GOOD, stateBefore = CardState.REVIEW, intervalDays = 1, elapsedDays = 1))
        }
        stats.studyActivity(id, "Portuguese")
    }

    private val lessons = (1..10).map { Lesson(id = "l$it", title = "Lição $it", summary = "", text = "Texto") }
    private val course = CourseProgress(
        Course(id = "pt-mini-0100", languageCode = "pt", title = "A família", description = "", level = CourseLevel.A1, topic = "", lessons = lessons),
        lessons.mapIndexed { i, lesson ->
            LessonProgress(lesson, if (i < 3) LessonBook(lesson.id, 100L + i, currentPage = 1, pageCount = 1, lastOpened = at(3), isCompleted = i < 2) else null)
        },
    )
    private val book = BookListItem(
        id = 7, title = "O Alquimista", languageId = 1, languageName = "Portuguese", tags = emptyList(), currentPage = 12, pageCount = 48,
        wordCount = 30_000, lastOpened = at(3), isCompleted = false, isArchived = false, stats = null, sourceUri = null,
    )

    private fun ComposeUiTest.show(
        state: HomeUiState,
        visited: MutableList<Route> = mutableListOf(),
        padding: PaddingValues = PaddingValues(),
        continued: () -> Unit = {},
    ) = setContent {
        TayraTheme {
            ProvideWindowWidth {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    PageColumn(padding) { HomeContent(state, onNavigate = { visited += it }, onContinueCourse = continued) }
                }
            }
        }
    }

    @Test
    fun everyKindOfStudyCountsTowardsTheStreak() {
        assertEquals(2, activity.savedToday)
        // Today's words saved, then flashcards on the two days before, then reading the day before those.
        assertEquals(4, activity.streak)
    }

    @Test
    fun theHomePageShowsTodayAndLeadsBackIntoTheStudy() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val visited = mutableListOf<Route>()
        var continued = 0
        show(HomeUiState(loading = false, languageName = "Portuguese", activity = activity, course = course, book = book, flashcardsDue = 19), visited) { continued++ }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Study activity")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("HOME_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }
        onNodeWithText("Words saved today").assertExists()
        onNodeWithText("2 this week").assertExists()
        onNodeWithText("4 days").assertExists()
        onNodeWithText("Longest: 4 days").assertExists()
        onNodeWithText("A família").assertExists()
        onNodeWithText("Lesson 3: Lição 3").assertExists()
        onNodeWithText("2 of 10 lessons").assertExists()
        onNodeWithText("O Alquimista").assertExists()
        onNodeWithText("Page 12 of 48").assertExists()
        onNodeWithText("19 cards due today").assertExists()
        onNodeWithText("4 days studied since", substring = true).assertExists()
        onNodeWithTag("heatmap").assertExists()
        // Pointing at today's square, the last column's Friday, names the day and what it held.
        onNodeWithTag("heatmap").performMouseInput { moveTo(Offset(width - 2f, height * 4.5f / 7f)) }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Oct 16 · 2 words saved")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithTag("action-course").performClick()
        onNodeWithTag("action-book").performClick()
        onNodeWithTag("action-flashcards").performClick()
        assertEquals(1, continued)
        assertEquals(listOf(Route.Read(7), Route.Flashcards), visited)
    }

    @Test
    fun withNothingStartedThePageOffersTheCoursesAndBooks() = runDesktopComposeUiTest(width = 400, height = 1600) {
        val visited = mutableListOf<Route>()
        show(HomeUiState(loading = false, languageName = "Portuguese", activity = activity.copy(days = emptyMap())), visited)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Study activity")).fetchSemanticsNodes().isNotEmpty() }
        System.getenv("HOME_PHONE_SCREENSHOT")?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }
        onNodeWithText("0 days").assertExists()
        onNodeWithText("Study today to start a streak").assertExists()
        onNodeWithText("All caught up").assertExists()
        onNodeWithTag("action-course").performClick()
        onNodeWithTag("action-book").performClick()
        assertEquals(listOf(Route.Courses, Route.Books), visited)
    }

    @Test
    fun phonePrimaryActivityUsesTheLearningPriority() {
        val base = HomeUiState(loading = false, languageName = "Portuguese")
        val notStarted = CourseProgress(course.course, lessons.map { LessonProgress(it, null) })

        assertEquals(PhonePrimaryActivity.COURSE, phonePrimaryActivity(base.copy(course = course, book = book, flashcardsDue = 3)))
        assertEquals(PhonePrimaryActivity.FLASHCARDS, phonePrimaryActivity(base.copy(course = notStarted, book = book, flashcardsDue = 3)))
        assertEquals(PhonePrimaryActivity.BOOK, phonePrimaryActivity(base.copy(course = notStarted, book = book)))
        assertEquals(PhonePrimaryActivity.BROWSE_COURSES, phonePrimaryActivity(base.copy(course = notStarted)))
        assertEquals(PhonePrimaryActivity.BROWSE_COURSES, phonePrimaryActivity(base.copy(book = book.copy(isCompleted = true))))
    }

    /** A lesson under way leads, reviews stay above passive figures, and the last book page is not complete. */
    @Test
    fun thePhoneFirstViewportPutsUrgentWorkFirst() = runDesktopComposeUiTest(width = 400, height = 860) {
        show(
            HomeUiState(loading = false, languageName = "Portuguese", activity = activity, course = course, book = book.copy(currentPage = 48), flashcardsDue = 3),
            padding = PaddingValues(top = 56.dp, bottom = 80.dp),
        )
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Study activity")).fetchSemanticsNodes().isNotEmpty() }
        val top = listOf(
            onNodeWithTag("card-course").fetchSemanticsNode().positionInRoot.y,
            onNodeWithTag("card-flashcards").fetchSemanticsNode().positionInRoot.y,
            onNodeWithTag("tile-words-today").fetchSemanticsNode().positionInRoot.y,
            onNodeWithTag("card-book").fetchSemanticsNode().positionInRoot.y,
        )
        assertEquals(top.sorted(), top)
        onNodeWithTag("primary-activity").assertIsDisplayed()
        onNodeWithTag("card-flashcards").assertIsDisplayed()
        onNodeWithTag("tile-words-today").assertIsDisplayed()
        onNodeWithTag("tile-streak").assertIsDisplayed()
        onNodeWithText("97%").assertExists()
    }

    @Test
    fun aCompletedBookIsNeverOfferedAsAContinuation() = runDesktopComposeUiTest(width = 400, height = 1200) {
        val visited = mutableListOf<Route>()
        show(HomeUiState(loading = false, languageName = "Portuguese", book = book.copy(isCompleted = true)), visited)

        onNodeWithText("O Alquimista").assertDoesNotExist()
        onNodeWithTag("action-book").performClick()
        assertEquals(listOf<Route>(Route.Books), visited)
    }
}
