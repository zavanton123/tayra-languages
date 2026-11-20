package com.idiomiko.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.FlashcardRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.LearningStatsRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.repository.WordsReadRepositoryImpl
import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.Lesson
import com.idiomiko.core.domain.courses.LessonBook
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.domain.flashcards.CardState
import com.idiomiko.core.domain.flashcards.FlashcardReview
import com.idiomiko.core.domain.flashcards.Rating
import com.idiomiko.core.domain.frequency.VocabularyProgress
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.domain.model.BookStats
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.model.WordsReadEntry
import com.idiomiko.core.domain.playlists.ListeningPosition
import com.idiomiko.core.domain.playlists.Playlist
import com.idiomiko.core.domain.playlists.PlaylistItem
import com.idiomiko.core.domain.playlists.PlaylistTextKind
import com.idiomiko.core.domain.service.StatsService
import com.idiomiko.core.domain.stats.SavedWord
import com.idiomiko.core.domain.stats.StudyActivity
import com.idiomiko.core.domain.stats.StudyDay
import com.idiomiko.core.ui.components.AccountBadge
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalAccountBadge
import com.idiomiko.core.ui.components.LocalFlashcardsDue
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.LocalSyncEntry
import com.idiomiko.core.ui.components.LocalVideosEnabled
import com.idiomiko.core.ui.components.LocalWindowWidth
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.PageColumn
import com.idiomiko.core.ui.components.PhoneNavBar
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.components.SyncEntry
import com.idiomiko.core.ui.i18n.UiLanguage
import com.idiomiko.core.ui.navigation.Route
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import java.sql.DriverManager
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus

/** The home page: today's goal and figures from what the database records, one step to take first, and the ways back into the study. */
@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {
    private val file = File.createTempFile("idiomiko-home", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 10, 16)
    private val clock = object : Clock { override fun now(): Instant = today.atStartOfDayIn(zone) + 15.hours }
    private val stats = StatsService(WordsReadRepositoryImpl(provider, zone), LearningStatsRepositoryImpl(provider, zone), clock, zone)

    private fun at(daysAgo: Int): Instant = today.atStartOfDayIn(zone) - (daysAgo * 24).hours + 12.hours

    private var portuguese = 0L

    /** Read three days ago, answered flashcards the day before yesterday and yesterday, and saved two words today. */
    private val activity = runBlocking {
        val id = LanguageRepositoryImpl(provider).save(Language(name = "Portuguese")).also { portuguese = it }
        WordsReadRepositoryImpl(provider, zone).add(WordsReadEntry(languageId = id, pageId = null, readAt = at(3), wordCount = 400))
        val terms = TermRepositoryImpl(provider)
        val saved = listOf("casa" to "house", "gato" to null).map { (text, translation) -> terms.save(Term(languageId = id, text = text, textLc = text, status = TermStatus.NEW_1, translation = translation)) }
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
            saved.forEach { c.createStatement().execute("UPDATE terms SET created_at = ${at(0).toEpochMilliseconds()} WHERE id = $it") }
        }
        val cards = FlashcardRepositoryImpl(provider)
        for (daysAgo in listOf(2, 1)) {
            cards.addReview(FlashcardReview(termId = saved[0], at = at(daysAgo), rating = Rating.GOOD, stateBefore = CardState.REVIEW, intervalDays = 1, elapsedDays = 1))
        }
        stats.studyActivity(id, "Portuguese")
    }

    /** As [activity], with twelve minutes of study today: 480 words read and four minutes of answers. */
    private val busyToday = activity.copy(days = activity.days + (today to StudyDay(wordsRead = 480, reviews = 30, wordsSaved = 2, reviewTime = 4.minutes)))

    /** A season of study, for the pictures: most days of the last months. */
    private val season = StudyActivity(
        (0 until 110).filter { it % 7 != 3 && it % 11 != 5 }.associate { back ->
            today.minus(back, DateTimeUnit.DAY) to StudyDay(wordsRead = 200 + (back * 37) % 900, reviews = (back * 13) % 60, wordsSaved = back % 6)
        } + (today to StudyDay(wordsRead = 480, reviews = 30, wordsSaved = 4, reviewTime = 4.minutes)),
        today,
    )

    private fun lessons(prefix: String) = (1..10).map { Lesson(id = "$prefix-l$it", title = "Lição $it", summary = "", text = List(443) { "palavra" }.joinToString(" "), newWords = List(9) { n -> "w$n" }) }

    private fun course(id: String, title: String, rankUpTo: Int, done: Int, opened: Int = done) = lessons(id).let { list ->
        CourseProgress(
            Course(id = id, languageCode = "pt", title = title, description = "", level = CourseLevel.A2, topic = "", lessons = list, rankUpTo = rankUpTo),
            list.mapIndexed { i, lesson -> LessonProgress(lesson, if (i < opened) LessonBook(lesson.id, 100L + i, currentPage = 1, pageCount = 1, lastOpened = at(3), isCompleted = i < done) else null) },
        )
    }

    private val course = course("pt-mini-0100", "A família", 100, done = 2, opened = 3)
    private val nextBand = course("pt-mini-1600", "Arte e cultura", 1600, done = 0)
    private val book = BookListItem(
        id = 7, title = "O Alquimista", languageId = 1, languageName = "Portuguese", tags = emptyList(), currentPage = 12, pageCount = 48,
        wordCount = 30_000, lastOpened = at(3), isCompleted = false, isArchived = false, stats = null, sourceUri = null,
    )
    private val easyBook = book.copy(
        id = 8, title = "Contos curtos", currentPage = 1, pageCount = 12, lastOpened = null,
        stats = BookStats(distinctTerms = 900, distinctUnknowns = 54, unknownPercent = 6, statusDistribution = emptyMap()),
    )
    private val vocabulary = VocabularyProgress.of(1_532, List(10_000) { it < 1_520 })
    private val words = listOf("saudade" to "longing", "aliás" to null, "desenrascar" to "to get by", "madrugada" to "early morning", "cotidiano" to null, "ao vivo" to "live")
        .mapIndexed { i, (text, translation) -> SavedWord(20L + i, text, translation, TermStatus.NEW_1, at(i), sentence = if (i == 0) "Tenho saudade de casa." else null) }
    private val playlistItems = listOf("Uma compra com problema", "O restaurante novo").mapIndexed { i, title ->
        PlaylistItem(id = i + 1L, bookId = i + 10L, title = title, kind = PlaylistTextKind.LESSON, pageCount = 1, length = 400, finished = false)
    }

    /** Everything under way at once: many cards due, a lesson, a book, a playlist, words saved and suggestions. */
    private val busy = HomeUiState(
        loading = false, languageName = "Portuguese", activity = busyToday, course = course, book = book, flashcardsDue = 310,
        playlist = Playlist(3, 1, "Main", "", 0, at(1), at(0), playlistItems, ListeningPosition(2, 1, 0)),
        dailyGoal = 15, vocabulary = vocabulary, recentWords = words, suggestedCourse = nextBand, suggestedBooks = listOf(easyBook),
        dayPart = DayPart.AFTERNOON,
    )

    /** The page as the design pictures it, for the screenshots: 266 cards due, 13 of 15 minutes, a course, a book and a playlist under way. */
    private val pictured = run {
        val lessons = listOf("Uma visita a Ouro Preto", "O Pelourinho", "Bonito e as grutas").plus((4..10).map { "Lição $it" })
            .mapIndexed { i, title -> Lesson(id = "pt-brasil-l${i + 1}", title = title, summary = "", text = List(443) { "palavra" }.joinToString(" "), newWords = List(9) { n -> "w$n" }) }
        val brasil = CourseProgress(
            Course(id = "pt-brasil", languageCode = "pt", title = "Lugares do Brasil", description = "", level = CourseLevel.A2, topic = "", lessons = lessons, rankUpTo = 1200),
            lessons.mapIndexed { i, lesson -> LessonProgress(lesson, if (i == 0) LessonBook(lesson.id, 200L, currentPage = 1, pageCount = 1, lastOpened = at(1), isCompleted = false) else null) },
        )
        val news = course("pt-noticias", "Notícias do dia a dia", 2000, done = 0).let { it.copy(course = it.course.copy(level = CourseLevel.B1)) }
        val days = season.days.filterKeys { it != today.minus(1, DateTimeUnit.DAY) } + (today to StudyDay(wordsRead = 540, reviews = 30, wordsSaved = 0, reviewTime = 4.minutes))
        busy.copy(
            activity = StudyActivity(days, today, averageAnswer = busy.activity?.averageAnswer),
            course = brasil,
            book = book.copy(title = "Uma amizade antiga", currentPage = 1, pageCount = 2),
            flashcardsDue = 266,
            vocabulary = VocabularyProgress.of(1_539, List(10_000) { it < 1_240 }),
            recentWords = listOf("desconhecida", "juventude", "condenado", "acusados", "acusado", "Mineira", "escravos", "escravo")
                .mapIndexed { i, text -> SavedWord(40L + i, text, null, TermStatus.NEW_1, at(i)) },
            suggestedCourse = news,
        )
    }

    private fun ComposeUiTest.show(
        state: HomeUiState,
        visited: MutableList<Route> = mutableListOf(),
        opened: MutableList<String> = mutableListOf(),
        listened: () -> Unit = {},
        dark: Boolean = false,
        bar: Boolean = false,
    ) = setContent {
        IdiomikoTheme(theme = if (dark) AppThemes.byId("night") else AppThemes.default) {
            ProvideWindowWidth {
                CompositionLocalProvider(
                    LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L, knownWords = state.vocabulary?.known ?: 0) {},
                    LocalSyncEntry provides SyncEntry(running = false, lastSynced = Clock.System.now() - 2.minutes) {},
                    LocalVideosEnabled provides true,
                    LocalFlashcardsDue provides state.flashcardsDue,
                    LocalAccountBadge provides AccountBadge("reader@example.com", "Ana Reader", null) {},
                ) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            if (bar) AppTopBar(title = "Home", onNavigate = { visited += it }, section = NavSection.HOME)
                            PageColumn(PaddingValues(), Modifier.weight(1f)) {
                                HomeContent(state, HomeActions({ visited += it }, onOpenLesson = { _, lesson -> opened += lesson.lesson.id }, onContinuePlaylist = listened))
                            }
                            // The phone's tabs, as the app's frame puts them under every page.
                            if (bar && LocalWindowWidth.current.isCompact) PhoneNavBar(NavSection.HOME, onNavigate = { visited += it }, onMore = {})
                        }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.picture(env: String) {
        System.getenv(env)?.let { path ->
            waitForIdle()
            ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(path))
        }
    }

    /** The count of the words saved today lists them, the latest first; choosing one opens it. */
    @Test
    fun theWordsSavedTodayAreListedFromTheStrip() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val visited = mutableListOf<Route>()
        val words = runBlocking { stats.savedToday(portuguese) }
        setContent {
            var shown by remember { mutableStateOf(false) }
            IdiomikoTheme {
                ProvideWindowWidth {
                    PageColumn(PaddingValues()) {
                        HomeContent(HomeUiState(loading = false, languageName = "Portuguese", activity = activity), HomeActions({}, onSavedToday = { shown = true }))
                    }
                    if (shown) SavedTodayDialog(words, onOpen = { visited += Route.EditTerm(it) }, onDismiss = { shown = false })
                }
            }
        }
        onNodeWithText("2 words saved today").assertExists()
        onNodeWithTag("figure-saved-today").performClick()
        onNodeWithTag("saved-today").assertExists()
        assertEquals(listOf("gato", "casa"), words.map { it.text })
        onNodeWithText("house").assertExists()
        onNodeWithTag("saved-word-${words.last().termId}").performClick()
        assertEquals(listOf<Route>(Route.EditTerm(words.last().termId)), visited)
        onNodeWithTag("saved-today-close").performClick()
        assertEquals(0, onAllNodesWithTag("saved-today").fetchSemanticsNodes().size)
    }

    @Test
    fun everyKindOfStudyCountsTowardsTheStreak() {
        assertEquals(2, activity.savedToday)
        // Today's words saved, then flashcards on the two days before, then reading the day before those.
        assertEquals(4, activity.streak)
    }

    /**
     * With many cards due, the cards lead with their time, a quick session and how near they take the day's goal;
     * the lesson, the book and the playlist's episode follow as rows, and the progress side shows the words known,
     * the words saved and the weeks. A greeting heads the page, in the language being learned.
     */
    @Test
    fun aWideWindowLeadsWithTheCardsAndShowsTheRestBeside() = runDesktopComposeUiTest(width = 1586, height = 1500) {
        val visited = mutableListOf<Route>()
        val opened = mutableListOf<String>()
        var listened = 0
        show(busy, visited, opened, listened = { listened++ })
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Study activity")).fetchSemanticsNodes().isNotEmpty() }
        // Good afternoon in Portuguese, and how the day's goal goes: twelve of fifteen minutes, the streak and the words.
        onNodeWithText("Boa tarde!").assertExists()
        onNodeWithText("Keep going — you're almost at today's goal!").assertExists()
        onNodeWithText("12 of 15 minutes").assertExists()
        onNodeWithText("3 minutes to go").assertExists()
        onNodeWithText("4-day streak").assertExists()
        onNodeWithText("1,532 words known").assertExists()
        // One step first, with its time and the goal it nears, and the page's only filled button; on a wide window it needs no label.
        onNodeWithText("Next up").assertDoesNotExist()
        onNodeWithText("310 cards due today").assertExists()
        onNodeWithText("~31 min").assertExists()
        onNodeWithText("Just 3 minutes to reach today's goal.").assertExists()
        onNodeWithTag("next-up-art").assertExists()
        assertEquals(1, onAllNodesWithTag("next-up-action").fetchSemanticsNodes().size)
        onNodeWithTag("next-up-quick").performClick()
        onNodeWithTag("next-up-action").performClick()
        // The rest under way, the playlist by its episode.
        onNodeWithText("Continue learning").assertExists()
        onNodeWithText("Lesson 3: Lição 3").assertExists()
        onNodeWithText("O restaurante novo").assertExists()
        onNodeWithTag("row-course").performClick()
        onNodeWithTag("row-book").performClick()
        onNodeWithTag("row-playlist").performClick()
        assertEquals(0, onAllNodesWithTag("row-flashcards").fetchSemanticsNodes().size)
        // The level: the words known against the next milestone, and one course recommended for it, with the others a click away.
        onNodeWithText("76% of the 2,000 most common words").assertExists()
        onNodeWithText("Next milestone: 2,000").assertExists()
        onNodeWithText("RECOMMENDED FOR YOU").assertExists()
        onNodeWithText("A2 · Words up to rank 1,600").assertExists()
        assertEquals(0, onAllNodesWithTag("suggested-book-8").fetchSemanticsNodes().size)
        onNodeWithTag("suggested-course").performClick()
        onNodeWithTag("suggestions-browse").performClick()
        // A word saved in a sentence opens in it, on its card; one without, on its own.
        onNodeWithTag("recent-word-20").performClick()
        onNodeWithTag("recent-word-21").performClick()
        // The last 16 weeks here, the year on Statistics.
        onNodeWithText("4 days studied in the last 16 weeks").assertExists()
        onNodeWithTag("see-all-stats").performClick()
        assertEquals(listOf("pt-mini-0100-l3"), opened)
        assertEquals(1, listened)
        assertEquals(
            listOf(Route.Flashcards(limit = 20), Route.Flashcards(), Route.Read(7), Route.Course("pt-mini-1600"), Route.Courses, Route.BrowseFlashcards(20), Route.EditTerm(21), Route.Stats),
            visited,
        )
        // The two columns end level: the recommendation grows to the activity's foot.
        val left = onNodeWithTag("suggestions").fetchSemanticsNode().boundsInRoot.bottom
        val right = onNodeWithTag("activity").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue(abs(left - right) < 1f, "the columns end at $left and $right")
    }

    /** The pictures of the page as the design has it, wide and on a phone, light and dark. */
    @Test
    fun theWidePictures() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        show(pictured, bar = true)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Study activity")).fetchSemanticsNodes().isNotEmpty() }
        picture("HOME_SCREENSHOT")
        onNodeWithText("266 cards due today").assertExists()
        onNodeWithText("0/10").assertExists()
        onNodeWithText("Notícias do dia a dia").assertExists()
        onNodeWithText("B1 · Words up to rank 2,000").assertExists()
    }

    @Test
    fun theWideDarkPicture() = runDesktopComposeUiTest(width = 1600, height = 1000) {
        show(pictured, dark = true, bar = true)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Study activity")).fetchSemanticsNodes().isNotEmpty() }
        picture("HOME_DARK_SCREENSHOT")
        onNodeWithText("Synced 2 minutes ago").assertExists()
    }

    @Test
    fun thePhonePictures() = runDesktopComposeUiTest(width = 400, height = 860) {
        show(pictured, bar = true)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Keep your words fresh")).fetchSemanticsNodes().isNotEmpty() }
        picture("HOME_PHONE_SCREENSHOT")
        onNodeWithText("13 of 15 minutes today").assertExists()
        onNodeWithText("2 minutes to go").assertExists()
        onNodeWithText("1-day streak").assertExists()
        onNodeWithText("1,539 words known").assertExists()
        onNodeWithText("99+", useUnmergedTree = true).assertExists()
    }

    @Test
    fun thePhoneDarkPicture() = runDesktopComposeUiTest(width = 400, height = 860) {
        show(pictured, dark = true, bar = true)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Keep your words fresh")).fetchSemanticsNodes().isNotEmpty() }
        picture("HOME_PHONE_DARK_SCREENSHOT")
    }

    /** The whole phone page, below the fold too. */
    @Test
    fun theWholePhonePicture() = runDesktopComposeUiTest(width = 400, height = 2400) {
        show(pictured)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Study activity")).fetchSemanticsNodes().isNotEmpty() }
        picture("HOME_PHONE_FULL_SCREENSHOT")
    }

    /** On a phone the lesson under way leads, then the rest under way, the vocabulary and the progress, in one column. */
    @Test
    fun aPhoneShowsOneColumnWithTheLessonFirst() = runDesktopComposeUiTest(width = 360, height = 2300) {
        val visited = mutableListOf<Route>()
        val opened = mutableListOf<String>()
        show(busy.copy(flashcardsDue = 12, activity = season), visited, opened = opened)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Study activity")).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("NEXT UP").assertExists()
        onNodeWithText("A família").assertExists()
        onNodeWithText("~8 min").assertExists()
        onNodeWithText("9 new words").assertExists()
        onNodeWithText("2 of 10 lessons").assertExists()
        // No greeting on a phone: its bar names the page.
        onNodeWithText("Boa tarde!").assertDoesNotExist()
        val top = listOf("today", "next-up", "continue", "vocabulary-row", "recent-words", "suggestions", "activity").map { onNodeWithTag(it).fetchSemanticsNode().positionInRoot.y }
        assertEquals(top.sorted(), top)
        // The phone's day card: the streak and the words known, the words saved today listed further down.
        onNodeWithTag("figure-streak").assertExists()
        onNodeWithTag("figure-known").assertExists()
        assertEquals(0, onAllNodesWithTag("figure-saved-today").fetchSemanticsNodes().size)
        onNodeWithTag("next-up").assertIsDisplayed()
        // A few cards wait in the rows, with their time.
        onNodeWithText("12 cards due today").assertExists()
        onNodeWithText("~2 min").assertExists()
        onNodeWithTag("next-up-action").performClick()
        onNodeWithTag("vocabulary-row").performClick()
        assertEquals(listOf("pt-mini-0100-l3"), opened)
        assertEquals(listOf<Route>(Route.Terms()), visited)
    }

    /** A first visit starts here: the course of the reader's level, with nothing else under way and nothing due. */
    @Test
    fun aFirstVisitStartsHere() = runDesktopComposeUiTest(width = 1586, height = 1200) {
        val opened = mutableListOf<String>()
        val first = HomeUiState(
            loading = false, languageName = "Portuguese", activity = StudyActivity(emptyMap(), today), course = course("pt-mini-0100", "A família", 100, done = 0),
            suggestedCourse = nextBand, vocabulary = VocabularyProgress.of(0, List(5_000) { false }),
        )
        show(first, opened = opened, bar = true)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("Start here")).fetchSemanticsNodes().isNotEmpty() }
        picture("HOME_FIRST_SCREENSHOT")
        onNodeWithText("Arte e cultura").assertExists()
        onNodeWithText("Start course").assertExists()
        onNodeWithText("0 of 15 minutes").assertExists()
        onNodeWithText("Ready when you are: today's goal is 15 minutes.").assertExists()
        // Nothing due: the cards are all caught up.
        onNodeWithText("All caught up").assertExists()
        onNodeWithTag("next-up-action").performClick()
        assertEquals(listOf("pt-mini-1600-l1"), opened)
    }

    @Test
    fun withNoCourseAtAllTheCoursesAreOffered() = runDesktopComposeUiTest(width = 400, height = 1400) {
        val visited = mutableListOf<Route>()
        show(HomeUiState(loading = false, languageName = "Portuguese", activity = StudyActivity(emptyMap(), today)), visited)
        onNodeWithText("Start with a course").assertExists()
        onNodeWithTag("next-up-action").performClick()
        assertEquals(listOf<Route>(Route.Courses), visited)
    }

    /** A finished course is said before the next one, which the page offers to start; the cards caught up say so. */
    @Test
    fun aFinishedCourseLeadsToTheNextOne() = runDesktopComposeUiTest(width = 1586, height = 1300) {
        val visited = mutableListOf<Route>()
        val finished = course("pt-mini-0100", "A família", 100, done = 10)
        show(HomeUiState(loading = false, languageName = "Portuguese", activity = activity, finishedCourse = finished, suggestedCourse = nextBand, vocabulary = vocabulary), visited)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasText("You finished A família. Next:")).fetchSemanticsNodes().isNotEmpty() }
        picture("HOME_FINISHED_SCREENSHOT")
        onNodeWithText("Arte e cultura").assertExists()
        onNodeWithText("Course finished").assertExists()
        onNodeWithText("All caught up").assertExists()
        // The course of the band is the next step, not offered again among the suggestions.
        assertEquals(0, onAllNodesWithTag("suggested-course").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithTag("suggestions").fetchSemanticsNodes().size)
        onNodeWithTag("row-finished-course").performClick()
        onNodeWithTag("row-flashcards").performClick()
        assertEquals(listOf(Route.Courses, Route.Flashcards()), visited)
    }

    @Test
    fun aCompletedBookIsNeverOfferedAsAContinuation() = runDesktopComposeUiTest(width = 400, height = 1200) {
        show(HomeUiState(loading = false, languageName = "Portuguese", book = book.copy(isCompleted = true)))
        onNodeWithText("O Alquimista").assertDoesNotExist()
        onNodeWithText("Start with a course").assertExists()
    }

    /** A playlist at a text names it; one at no text with nothing heard has nothing to go on with and is left out. */
    @Test
    fun aPlaylistIsOfferedByItsEpisodeOrNotAtAll() = runDesktopComposeUiTest(width = 400, height = 1600) {
        var state by mutableStateOf(busy)
        var listened = 0
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    PageColumn(PaddingValues()) { HomeContent(state, HomeActions({}, onContinuePlaylist = { listened++ })) }
                }
            }
        }
        onNodeWithText("Continue listening").assertExists()
        onNodeWithText("O restaurante novo").assertExists()
        // Nothing heard yet, the row does not say 0%.
        onNodeWithText("0%").assertDoesNotExist()
        onNodeWithTag("row-playlist").performScrollTo().performClick()
        assertEquals(1, listened)
        state = busy.copy(playlist = busy.playlist!!.copy(position = ListeningPosition(99, 1, 0)))
        waitForIdle()
        onNodeWithText("Continue listening").assertDoesNotExist()
        assertFalse(resumable(busy.playlist!!.copy(position = null)))
    }


    /** The goal reached turns the ring green and says so; the ring leads to the setting. */
    @Test
    fun theDailyGoalReachedSaysSo() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val visited = mutableListOf<Route>()
        show(busy.copy(dailyGoal = 10), visited)
        onNodeWithText("Daily goal reached").assertExists()
        onNodeWithText("You reached today's goal. Well done!").assertExists()
        onNodeWithTag("next-up-facts").assertExists()
        onNodeWithText("reach today's goal", substring = true).assertDoesNotExist()
        onNodeWithText("12 minutes of study today").assertExists()
        onNodeWithTag("daily-goal").performClick()
        assertEquals(listOf<Route>(Route.Settings), visited)
    }

    /** With no course for the level, a book the reader mostly knows is recommended instead, with the books a click away. */
    @Test
    fun aBookIsRecommendedWhenNoCourseFits() = runDesktopComposeUiTest(width = 1586, height = 1500) {
        val visited = mutableListOf<Route>()
        show(busy.copy(suggestedCourse = null), visited)
        onNodeWithText("Contos curtos").assertExists()
        onNodeWithText("94% known · Page 1 of 12").assertExists()
        onNodeWithTag("suggested-book-8").performClick()
        onNodeWithTag("suggestions-browse").performClick()
        assertEquals(listOf(Route.Read(8), Route.Books), visited)
    }

    /** The greeting follows the time of day, in the language being learned where the app knows it, else in the interface's. */
    @Test
    fun theGreetingFitsTheTimeAndTheLanguage() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var state by mutableStateOf(busy.copy(dayPart = DayPart.MORNING))
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    PageColumn(PaddingValues()) { HomeContent(state, HomeActions({})) }
                }
            }
        }
        onNodeWithText("Bom dia!").assertExists()
        state = busy.copy(languageName = "Spanish", dayPart = DayPart.EVENING)
        waitForIdle()
        onNodeWithText("¡Buenas noches!").assertExists()
        state = busy.copy(languageName = "Klingon", dayPart = DayPart.AFTERNOON)
        waitForIdle()
        onNodeWithText("Good afternoon!").assertExists()
        UiLanguage.set("de")
        try {
            state = busy.copy(languageName = "Klingon", dayPart = DayPart.EVENING)
            waitForIdle()
            onNodeWithText("Guten Abend!").assertExists()
        } finally {
            UiLanguage.set("en")
        }
        assertEquals(listOf(DayPart.EVENING, DayPart.MORNING, DayPart.AFTERNOON, DayPart.EVENING), listOf(3, 9, 15, 21).map { DayPart.at(it) })
    }
}
