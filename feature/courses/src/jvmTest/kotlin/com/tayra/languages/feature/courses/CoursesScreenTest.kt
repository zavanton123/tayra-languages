package com.tayra.languages.feature.courses

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/** The courses can be searched and filtered, and a course lists its lessons with the reader's progress. */
@OptIn(ExperimentalTestApi::class)
class CoursesScreenTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-courses-ui", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = CourseService(books, languages, BookService(books, languages), CourseRepositoryImpl(provider), SourceCourses)
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))
    private val languageId = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) }; service.seedSamples() } }

    private fun ComposeUiTest.host(section: NavSection = NavSection.COURSES, content: @androidx.compose.runtime.Composable () -> Unit) = setContent {
        TayraTheme {
            ProvideWindowWidth {
                CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {}) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            AppTopBar(title = "Courses", onNavigate = {}, section = section)
                            content()
                        }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    private val ids = (100..10000 step 100).map { "pt-mini-" + it.toString().padStart(4, '0') }

    private fun ComposeUiTest.shown(): List<String> =
        ids.filter { onAllNodesWithTag("course-$it").fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun coursesCanBeSearchedFilteredAndOpened() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = CoursesViewModel(service, languages, settings)
        val opened = mutableListOf<String>()
        host {
            val state by viewModel.state.collectAsState()
            if (!state.loading) CoursesContent(state, viewModel::setSearch, viewModel::setLevel, viewModel::setStatus, onOpen = { opened += it }, onSort = viewModel::setSort, onView = viewModel::setView)
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("100 courses").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(ids.take(2), shown().take(2), "in the order of their ranks")
        onNodeWithText("Guided Portuguese lessons to read, level by level.").assertExists()
        save("COURSES_GRID_SCREENSHOT")

        // The search looks in lesson titles too.
        onNodeWithTag("course-search").performTextInput("O prêmio da Clara")
        waitUntil(timeoutMillis = 5_000) { shown() == listOf("pt-mini-0800") }
        assertTrue(onAllNodesWithText("1 course").fetchSemanticsNodes().isNotEmpty())
        onNodeWithTag("course-search").performTextClearance()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("100 courses").fetchSemanticsNodes().isNotEmpty() }

        onNodeWithText("Level: All").performClick()
        onNodeWithText("A1 · Beginner").performClick()
        waitUntil(timeoutMillis = 5_000) { shown() == ids.take(3) }
        assertTrue(onAllNodesWithText("3 courses").fetchSemanticsNodes().isNotEmpty())
        onNodeWithText("Level: A1 · Beginner").performClick()
        onNodeWithText("All levels").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("100 courses").fetchSemanticsNodes().isNotEmpty() }

        // Nothing is started yet, so the "In progress" tab holds no course and the others all of them.
        onNodeWithTag("status-IN_PROGRESS").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("No courses match").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithText("Continue learning").fetchSemanticsNodes().size)
        onNodeWithTag("status-NOT_STARTED").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("100 courses").fetchSemanticsNodes().isNotEmpty() }

        onNodeWithTag("course-pt-mini-0200").performClick()
        assertEquals(listOf("pt-mini-0200"), opened)

        // Sorted by title, the courses are no longer grouped by level; the list shows them as rows.
        onNodeWithTag("course-sort").performClick()
        onNodeWithText("Title").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Sorted by title").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, onAllNodesWithText("Beginner").fetchSemanticsNodes().size)
        onNodeWithTag("view-LIST").performClick()
        save("COURSES_LIST_SCREENSHOT")

    }

    @Test
    fun aCourseListsItsLessonsAndOpensThemForReading() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = CourseViewModel("pt-mini-0100", service)
        val read = java.util.Collections.synchronizedList(mutableListOf<Long>())
        val collecting = CoroutineScope(Dispatchers.Default).launch { viewModel.events.flow.collect { if (it is CourseEvent.Read) read += it.bookId } }
        host {
            val state by viewModel.state.collectAsState()
            if (!state.loading) CourseContent(state, onOpenLesson = viewModel::openLesson, onCourses = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Em casa e com a família").fetchSemanticsNodes().isNotEmpty() }
        // The first lesson is picked out as the place to start.
        onNodeWithText("Start here").assertExists()
        save("COURSE_NEW_SCREENSHOT")
        for (lesson in listOf("A casa nova", "Onde está o Tom?", "Um computador para dois")) onNodeWithText(lesson).assertExists()
        onNodeWithText("Start course").assertExists()

        onNodeWithTag("lesson-pt-mini-0100-01").performClick()
        waitUntil(timeoutMillis = 5_000) { read.size == 1 }
        val bookId = read.single()
        assertEquals("A casa nova", runBlocking { books.getBook(bookId) }?.title)

        // Reading the lesson to its end completes it, and the course goes on with the next.
        runBlocking {
            reading.openPage(bookId, 1, trackOpen = true)
            reading.markPageRead(bookId, books.pageCount(bookId), markRestAsKnown = false)
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Continue: Onde está o Tom?").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("1 of 10 read").assertExists()
        onNodeWithText("Continue here").assertExists()
        assertEquals(0, onAllNodesWithText("Start here").fetchSemanticsNodes().size)
        assertNotNull(onAllNodesWithText("Completed").fetchSemanticsNodes().singleOrNull())
        save("COURSE_SCREENSHOT")
        collecting.cancel()
    }

    @Test
    fun theCourseBeingReadIsOfferedToContinue() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        // The first lesson of the second course is read to its end.
        runBlocking {
            val bookId = service.openLesson("pt-mini-0200", "pt-mini-0200-01")!!
            reading.openPage(bookId, 1, trackOpen = true)
            reading.markPageRead(bookId, books.pageCount(bookId), markRestAsKnown = false)
        }
        val viewModel = CoursesViewModel(service, languages, settings)
        val read = java.util.Collections.synchronizedList(mutableListOf<Long>())
        val collecting = CoroutineScope(Dispatchers.Default).launch { viewModel.events.flow.collect { if (it is CoursesEvent.Read) read += it.bookId } }
        host {
            val state by viewModel.state.collectAsState()
            if (!state.loading) CoursesContent(state, viewModel::setSearch, viewModel::setLevel, viewModel::setStatus, onOpen = {}, onContinue = viewModel::continueCourse)
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Continue learning").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("1 of 10 lessons").assertExists()
        save("COURSES_SCREENSHOT")
        onNodeWithTag("continue-course").performClick()
        waitUntil(timeoutMillis = 5_000) { read.size == 1 }
        // It opens the course's next lesson.
        assertEquals("O Pedro perde o ônibus", runBlocking { books.getBook(read.single()) }?.title)
        collecting.cancel()
    }

    @Test
    fun withoutCoursesTheReadyMadeOnesAreOfferedForDownload() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var asked = 0
        host { CoursesContent(CoursesUiState(loading = false, languageName = "Portuguese", packAvailable = true), {}, {}, {}, onOpen = {}, onDownloadCourses = { asked++ }) }
        onNodeWithText("No courses yet").assertExists()
        onNodeWithTag("download-courses").performClick()
        assertEquals(1, asked)
    }
}
