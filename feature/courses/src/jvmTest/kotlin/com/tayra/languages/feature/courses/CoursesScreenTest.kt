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
import com.tayra.languages.core.data.repository.UserCourseRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.BuiltInCourses
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
import kotlin.test.assertNotNull

/** The courses can be searched and filtered, and a course lists its lessons with the reader's progress. */
@OptIn(ExperimentalTestApi::class)
class CoursesScreenTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-courses-ui", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = CourseService(BuiltInCourses(), books, languages, BookService(books, languages), UserCourseRepositoryImpl(provider))
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))
    private val languageId = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } } }

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

    private fun ComposeUiTest.shown(): List<String> =
        listOf("pt-primeiros-passos", "pt-vida-na-cidade", "pt-historias-curtas").filter { onAllNodesWithTag("course-$it").fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun coursesCanBeSearchedFilteredAndOpened() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = CoursesViewModel(service, languages, settings)
        val opened = mutableListOf<String>()
        host {
            val state by viewModel.state.collectAsState()
            if (!state.loading) CoursesContent(state, viewModel::setSearch, viewModel::setLevel, viewModel::setStatus, onOpen = { opened += it })
        }
        waitUntil(timeoutMillis = 5_000) { shown().size == 3 }
        onNodeWithText("3 courses").assertExists()
        onNodeWithText("Guided Portuguese lessons to read, level by level.").assertExists()
        save("COURSES_SCREENSHOT")

        // The search looks in lesson titles too.
        onNodeWithTag("course-search").performTextInput("fim de semana")
        waitUntil(timeoutMillis = 5_000) { shown() == listOf("pt-vida-na-cidade") }
        onNodeWithText("1 course").assertExists()
        onNodeWithTag("course-search").performTextClearance()
        waitUntil(timeoutMillis = 5_000) { shown().size == 3 }

        onNodeWithText("All levels").performClick()
        onNodeWithText("B1 · Intermediate").performClick()
        waitUntil(timeoutMillis = 5_000) { shown() == listOf("pt-historias-curtas") }
        onNodeWithText("B1 · Intermediate").performClick()
        onNodeWithText("All levels").performClick()
        waitUntil(timeoutMillis = 5_000) { shown().size == 3 }

        // Nothing is started yet, so "In progress" leaves no course.
        onNodeWithText("Any progress").performClick()
        onNodeWithText("In progress").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("No courses match").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("In progress").performClick()
        onNodeWithText("Not started").performClick()
        waitUntil(timeoutMillis = 5_000) { shown().size == 3 }

        onNodeWithTag("course-pt-vida-na-cidade").performClick()
        assertEquals(listOf("pt-vida-na-cidade"), opened)
    }

    @Test
    fun aCourseListsItsLessonsAndOpensThemForReading() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = CourseViewModel("pt-primeiros-passos", service)
        val read = java.util.Collections.synchronizedList(mutableListOf<Long>())
        val collecting = CoroutineScope(Dispatchers.Default).launch { viewModel.events.flow.collect { if (it is CourseEvent.Read) read += it.bookId } }
        host {
            val state by viewModel.state.collectAsState()
            if (!state.loading) CourseContent(state, onOpenLesson = viewModel::openLesson, onCourses = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Primeiros passos").fetchSemanticsNodes().isNotEmpty() }
        // The first lesson is picked out as the place to start.
        onNodeWithText("Start here").assertExists()
        save("COURSE_NEW_SCREENSHOT")
        for (lesson in listOf("Olá! Eu sou a Ana", "Minha família", "Minha casa", "Meu dia", "No café")) onNodeWithText(lesson).assertExists()
        onNodeWithText("Start course").assertExists()
        assertEquals(5, onAllNodesWithText("Not started").fetchSemanticsNodes().size - 1, "five lessons, and the course itself")

        onNodeWithTag("lesson-pt-primeiros-passos-1").performClick()
        waitUntil(timeoutMillis = 5_000) { read.size == 1 }
        val bookId = read.single()
        assertEquals("Olá! Eu sou a Ana", runBlocking { books.getBook(bookId) }?.title)

        // Reading the lesson to its end completes it, and the course goes on with the next.
        runBlocking {
            reading.openPage(bookId, 1, trackOpen = true)
            reading.markPageRead(bookId, books.pageCount(bookId), markRestAsKnown = false)
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Continue: Minha família").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("1 of 5 read").assertExists()
        onNodeWithText("Continue here").assertExists()
        assertEquals(0, onAllNodesWithText("Start here").fetchSemanticsNodes().size)
        assertNotNull(onAllNodesWithText("Completed").fetchSemanticsNodes().singleOrNull())
        save("COURSE_SCREENSHOT")
        collecting.cancel()
    }
}
