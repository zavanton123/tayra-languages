package com.tayra.languages.feature.courses

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.UserCourseRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.BuiltInCourses
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.LessonDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The reader makes a course of their own through the forms, and its page offers what the app's courses do not: changing it. */
@OptIn(ExperimentalTestApi::class)
class OwnCoursesScreenTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-own-courses-ui", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = CourseService(BuiltInCourses(), books, languages, BookService(books, languages), UserCourseRepositoryImpl(provider))
    private val portuguese = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) } } }

    private fun ComposeUiTest.host(content: @androidx.compose.runtime.Composable () -> Unit) = setContent {
        TayraTheme {
            ProvideWindowWidth {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        AppTopBar(title = "Tayra Languages", onNavigate = {}, section = NavSection.COURSES)
                        content()
                    }
                }
            }
        }
    }

    @Test
    fun aCourseAndALessonAreMadeThroughTheForms() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val courseForm = CourseFormViewModel(null, service, languages, settings)
        var saved: String? = null
        host { CourseFormScreen(courseId = null, onNavigate = {}, onSaved = { saved = it }, onCancel = {}, viewModel = courseForm) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("course-title").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("form-save").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("A course needs a title").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("course-title").performTextInput("Minhas leituras")
        onNodeWithTag("course-description").performTextInput("Textos que eu escolhi.")
        onNodeWithTag("course-topic").performTextInput("Notícias")
        System.getenv("COURSE_FORM_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Create course").performClick()
        waitUntil(timeoutMillis = 5_000) { saved != null }
        val course = runBlocking { service.observeCourse(saved!!).first() }!!.course
        assertEquals("Minhas leituras", course.title)
        assertEquals(portuguese, course.languageId)
    }

    @Test
    fun aLessonIsWrittenInItsForm() = runDesktopComposeUiTest(width = 1586, height = 1100) {
        val courseId = runBlocking { service.createCourse(portuguese, CourseDraft("Minhas leituras")) }
        val lessonForm = LessonFormViewModel(courseId, null, service, languages)
        var done = false
        host { LessonFormScreen(courseId = courseId, lessonId = null, onNavigate = {}, onDone = { done = true }, viewModel = lessonForm) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("lesson-title").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("lesson-title").performTextInput("O gato")
        onNodeWithTag("lesson-summary").performTextInput("Um gato que dorme")
        onNodeWithTag("lesson-text").performTextInput("Era uma vez um gato. Ele dormia o dia todo.")
        onNodeWithText("10 words").assertExists()
        System.getenv("LESSON_FORM_SCREENSHOT")?.let { save(it) }
        onNodeWithText("Add lesson").performClick()
        waitUntil(timeoutMillis = 5_000) { done }
        assertEquals(listOf("O gato"), runBlocking { service.observeCourse(courseId).first() }!!.course.lessons.map { it.title })
    }

    @Test
    fun anOwnCourseCanBeChangedFromItsPageAndTheAppsCannot() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val courseId = runBlocking {
            service.createCourse(portuguese, CourseDraft("Minhas leituras", "Textos que eu escolhi.", topic = "Notícias")).also { id ->
                service.addLesson(id, LessonDraft("O gato", "Um gato que dorme", "Era uma vez um gato. Ele dormia o dia todo."))
                service.addLesson(id, LessonDraft("O cão", "Um cão que late", "O cão latia para a lua."))
            }
        }
        val viewModel = CourseViewModel(courseId, service)
        val actions = mutableListOf<String>()
        host {
            val state = viewModel.state.collectAsStateValue()
            CourseContent(
                state,
                onOpenLesson = {},
                onCourses = {},
                editing = CourseEditing(
                    onEdit = { actions += "edit" },
                    onDelete = viewModel::deleteCourse,
                    onAddLesson = { actions += "add" },
                    onEditLesson = { actions += "edit $it" },
                    onMoveLesson = viewModel::moveLesson,
                    onDeleteLesson = viewModel::deleteLesson,
                ),
            )
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Your course").fetchSemanticsNodes().isNotEmpty() }
        System.getenv("OWN_COURSE_SCREENSHOT")?.let { save(it) }
        onNodeWithTag("edit-course").performClick()
        onNodeWithTag("add-lesson").performClick()
        assertEquals(listOf("edit", "add"), actions)

        val cat = runBlocking { service.observeCourse(courseId).first() }!!.course.lessons.first().id
        onNodeWithTag("lesson-menu-$cat").performClick()
        onNodeWithText("Move down").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.observeCourse(courseId).first() }!!.course.lessons.map { it.title } == listOf("O cão", "O gato") }

        onNodeWithTag("lesson-menu-$cat").performClick()
        onNodeWithText("Delete lesson").performClick()
        onNodeWithText("Delete this lesson?").assertExists()
        onNodeWithTag("confirm-delete").performClick()
        waitUntil(timeoutMillis = 5_000) { runBlocking { service.observeCourse(courseId).first() }!!.course.lessons.map { it.title } == listOf("O cão") }
    }

    @Test
    fun theAppsCoursesHaveNoEditing() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = CourseViewModel("pt-primeiros-passos", service)
        host { CourseContent(viewModel.state.collectAsStateValue(), onOpenLesson = {}, onCourses = {}, editing = CourseEditing()) }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Primeiros passos").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(onAllNodesWithTag("edit-course").fetchSemanticsNodes().isEmpty())
        assertTrue(onAllNodesWithTag("add-lesson").fetchSemanticsNodes().isEmpty())
        assertTrue(onAllNodesWithText("Your course").fetchSemanticsNodes().isEmpty())
    }

    @androidx.compose.runtime.Composable
    private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateValue(): T = collectAsState().value

    private fun ComposeUiTest.save(path: String) {
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(path))
    }
}
