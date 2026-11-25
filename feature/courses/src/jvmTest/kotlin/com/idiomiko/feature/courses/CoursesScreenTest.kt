package com.idiomiko.feature.courses

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import com.idiomiko.core.data.db.DatabaseDriverFactory
import com.idiomiko.core.data.db.DatabaseProvider
import com.idiomiko.core.data.repository.BookRepositoryImpl
import com.idiomiko.core.data.repository.CourseRepositoryImpl
import com.idiomiko.core.data.repository.LanguageRepositoryImpl
import com.idiomiko.core.data.repository.TermRepositoryImpl
import com.idiomiko.core.data.repository.WordsReadRepositoryImpl
import com.idiomiko.core.data.settings.SettingsRepositoryImpl
import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseDraft
import com.idiomiko.core.domain.courses.CourseKnowledge
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.CoursePack
import com.idiomiko.core.domain.courses.CoursePackService
import com.idiomiko.core.domain.courses.CoursePackStore
import com.idiomiko.core.domain.courses.CourseService
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.dictionary.PackState
import com.idiomiko.core.domain.frequency.FrequencyList
import com.idiomiko.core.domain.frequency.FrequencyLists
import com.idiomiko.core.domain.frequency.FrequencyWord
import com.idiomiko.core.domain.frequency.WordFrequencyService
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.ReadingStage
import com.idiomiko.core.domain.model.Term
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.playlists.PlaylistSource
import com.idiomiko.core.domain.service.BookService
import com.idiomiko.core.domain.service.ReadingService
import com.idiomiko.core.domain.service.TermService
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The courses can be searched and filtered, the next lesson opened, and a course lists its lessons with the reader's progress. */
@OptIn(ExperimentalTestApi::class)
class CoursesScreenTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("idiomiko-courses-ui", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val settings = SettingsRepositoryImpl(MapSettings())
    private val service = CourseService(books, languages, BookService(books, languages), CourseRepositoryImpl(provider), SourceCourses)
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))
    private val languageId = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) }; service.seedSamples() } }

    /** Course packs that are never on the device. */
    private val packs = CoursePackService(
        object : CoursePackStore {
            override suspend fun installedSize(pack: CoursePack): Long? = null
            override suspend fun install(pack: CoursePack, onProgress: (Float?) -> Unit) = Unit
            override suspend fun remove(pack: CoursePack) = Unit
            override suspend fun courseIds(pack: CoursePack): Set<String> = emptySet()
            override suspend fun courses(pack: CoursePack): List<Course> = emptyList()
        },
        service,
    )

    /** A frequency list of the Portuguese courses' band words, as the app's own list ranks them. */
    private val frequency = WordFrequencyService(
        object : FrequencyLists {
            override suspend fun list(languageCode: String): FrequencyList? = if (languageCode != "pt") null else FrequencyList(
                "test",
                File("../../tools/courses/pt/bands").listFiles()!!.sorted().flatMap { it.readLines() }.map { it.split('\t') }.filter { it.size >= 2 }
                    .map { FrequencyWord(it[0].toInt(), it[1], it.getOrElse(2) { "" }.split(' ').filter(String::isNotBlank)) },
            )
        },
        terms,
        languages,
    )

    private fun viewModel(knowing: Boolean = false) =
        if (knowing) CoursesViewModel(service, languages, settings, packs, frequency, CourseKnowledge(terms, languages)) else CoursesViewModel(service, languages, settings, packs)

    private fun ComposeUiTest.host(content: @Composable () -> Unit) = setContent {
        IdiomikoTheme {
            ProvideWindowWidth {
                CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {}) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            AppTopBar(title = "Courses", onNavigate = {}, section = NavSection.COURSES)
                            content()
                        }
                    }
                }
            }
        }
    }

    /** The page as the screen shows it, with its view model's actions and [actions]' openings. */
    private fun ComposeUiTest.page(viewModel: CoursesViewModel, onOpen: (String) -> Unit = {}, phone: Boolean = false) = host {
        val state by viewModel.state.collectAsState()
        val actions = remember { CoursesActions.of(viewModel) {}.let { a -> CoursesActions(a.onSearch, a.onStatus, a.onSort, a.onView, a.onTags, a.onToggleTag, a.onClearFilters, onOpen, null, a.onOpenLesson, a.onNewCourse, a.onDownloadCourses) } }
        if (!state.loading) {
            if (phone) PhoneCoursesContent(state, actions) else CoursesContent(state, actions)
        }
    }

    private fun ComposeUiTest.save(env: String) {
        System.getenv(env)?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(it)) }
    }

    private fun ComposeUiTest.exists(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.waitFor(tag: String) = waitUntil(timeoutMillis = 5_000) { exists(tag) }

    private fun ComposeUiTest.count(): String = onNodeWithTag("result-count").fetchSemanticsNode().config.let { config ->
        config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString { it.text }
    }

    /** Reads [lessons] of [courseId] to their end, every stage checked. */
    private fun complete(courseId: String, vararg lessons: Int) = runBlocking {
        for (n in lessons) {
            val bookId = service.openLesson(courseId, "$courseId-${n.toString().padStart(2, '0')}")!!
            reading.openPage(bookId, 1, trackOpen = true)
            reading.markPageRead(bookId, books.pageCount(bookId), markRestAsKnown = false)
            ReadingStage.entries.forEach { reading.checkStage(bookId, books.pageCount(bookId), it) }
        }
    }

    @Test
    fun coursesCanBeSearchedFilteredSortedAndOpened() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = viewModel()
        val opened = mutableListOf<String>()
        page(viewModel, onOpen = { opened += it })
        waitFor("next-lesson")
        onNodeWithText("Guided Portuguese lessons to read, level by level.").assertDoesNotExist()
        // Nothing is read and no words are counted: the journey starts at A1, with its first lesson.
        onNodeWithText("START HERE · EM CASA E COM A FAMÍLIA").assertExists()
        onNodeWithTag("course-pt-mini-0100").assertIsDisplayed()
        assertTrue(!exists("course-pt-mini-0400"), "the other levels wait folded")

        // The search looks in lesson titles too, and shows the matches instead of the levels.
        onNodeWithTag("course-search").performTextInput("O prêmio da Clara")
        waitUntil(timeoutMillis = 5_000) { exists("result-count") && count() == "1 course" }
        assertTrue(exists("course-pt-mini-0800") && exists("next-lesson"), "the matches, under the next lesson that keeps its place")
        onNodeWithTag("course-search").performTextClearance()
        waitFor("next-lesson")

        // Nothing is started yet, so "In progress" holds no course.
        onNodeWithTag("status-IN_PROGRESS").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("No courses match").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("status-NOT_STARTED").performClick()
        waitUntil(timeoutMillis = 5_000) { count() == "100 courses" }
        onNodeWithTag("course-pt-mini-0200").performClick()
        assertEquals(listOf("pt-mini-0200"), opened)

        // Sorted by title the courses are no longer grouped by level; the list shows them as rows.
        onNodeWithTag("course-sort").performClick()
        onNodeWithText("Title").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Sorted by title").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(!exists("level-header-A1"))
        onNodeWithTag("view-LIST").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.view == CoursesView.LIST }
        save("COURSES_LIST_SCREENSHOT")

        // Clearing the filters goes back to the journey.
        onNodeWithTag("clear-filters").performClick()
        waitFor("next-lesson")
        assertEquals(CoursesSort.RECOMMENDED, viewModel.state.value.sort)
        assertEquals(null, viewModel.state.value.status)
    }

    /** The course under way is offered first, its next lesson opened from the card. */
    @Test
    fun theCourseBeingReadIsOfferedToGoOnWith() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        complete("pt-mini-0200", 1)
        val viewModel = viewModel()
        val read = java.util.Collections.synchronizedList(mutableListOf<Long>())
        val collecting = CoroutineScope(Dispatchers.Default).launch { viewModel.events.flow.collect { if (it is CoursesEvent.Read) read += it.bookId } }
        page(viewModel)
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("KEEP GOING · O DIA A DIA").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Lesson 2: O Pedro perde o ônibus").assertExists()
        assertTrue(onAllNodesWithTag("pin-in-progress", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        save("COURSES_SCREENSHOT")
        onNodeWithTag("start-lesson").performClick()
        waitUntil(timeoutMillis = 5_000) { read.size == 1 }
        assertEquals("O Pedro perde o ônibus", runBlocking { books.getBook(read.single()) }?.title)
        // A stop of the lesson path opens its own lesson.
        onNodeWithTag("lesson-stop-1").performClick()
        waitUntil(timeoutMillis = 5_000) { read.size == 2 }
        assertEquals("A manhã da Ana", runBlocking { books.getBook(read[1]) }?.title)
        collecting.cancel()
    }

    /** The words known place the reader on the levels, and each course says how much of it they know. */
    @Test
    fun theWordsKnownPlaceTheReaderAndTellHowMuchOfEachCourseIsKnown() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        // The first 400 words of the list are known: the reader is at A2, with 600 to go to B1.
        runBlocking {
            val words = File("../../tools/courses/pt/bands").listFiles()!!.sorted().flatMap { it.readLines() }.map { it.split('\t') }.filter { it.size >= 2 && it[0].toInt() <= 400 }
            terms.insertAll(words.map { Term(languageId = languageId, text = it[1], textLc = it[1].lowercase(), status = TermStatus.WELL_KNOWN) })
        }
        val viewModel = viewModel(knowing = true)
        page(viewModel)
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("400 words known · 600 words to B1").fetchSemanticsNodes().isNotEmpty() }
        waitUntil(timeoutMillis = 30_000) { viewModel.state.value.knownShares.size == 100 }
        val shares = viewModel.state.value.knownShares
        assertTrue(shares.getValue("pt-mini-0100") > shares.getValue("pt-mini-1000"), "the first course is known best: $shares")
        assertTrue(onAllNodesWithTag("known-share", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        // The course past the words known is picked to read next.
        assertEquals("pt-mini-0500", viewModel.state.value.suggested?.course?.id)
        assertEquals(CourseLevel.A2, viewModel.state.value.journey.current)
    }

    /** The level on the journey opens its courses; the courses' own tags filter them. */
    @Test
    fun coursesCanBeFilteredByAnyOrAllOfSeveralTags() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val trip = runBlocking { service.createCourse(languageId, CourseDraft("Uma viagem", tags = listOf("travel", "food"))) }
        val market = runBlocking { service.createCourse(languageId, CourseDraft("No mercado", tags = listOf("food"))) }
        val viewModel = viewModel()
        page(viewModel)
        waitFor("tag-chip-food")

        // A tag's chip: the courses with it.
        onNodeWithTag("tag-chip-food").performClick()
        waitUntil(timeoutMillis = 5_000) { exists("result-count") && count() == "2 courses" }
        assertTrue(exists("course-$trip") && exists("course-$market") && !exists("course-pt-mini-0100"))
        onNodeWithTag("tag-chip-food").performClick()
        waitFor("next-lesson")

        // The full filter: all of two tags, only the course with both.
        onNodeWithTag("tag-filter").performClick()
        onNodeWithTag("tag-food").performClick()
        onNodeWithTag("tag-travel").performClick()
        onNodeWithTag("match-all").performClick()
        save("COURSES_TAGS_SCREENSHOT")
        onNodeWithTag("apply-tags").performClick()
        waitUntil(timeoutMillis = 5_000) { exists("result-count") && count() == "1 course" }
        assertTrue(exists("course-$trip") && !exists("course-$market"))
    }

    @Test
    fun aPhoneShowsTheStripTheFiltersAndTheCourses() = runDesktopComposeUiTest(width = 400, height = 860) {
        complete("pt-mini-0200", 1)
        val viewModel = viewModel()
        page(viewModel, phone = true)
        waitFor("next-lesson")
        onNodeWithTag("journey-strip").assertIsDisplayed()
        // The grid or the list, beside the filters as on a wide window.
        onNodeWithTag("view-LIST").assertIsDisplayed().performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.view == CoursesView.LIST }
        onNodeWithTag("view-GRID").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.view == CoursesView.GRID }
        onNodeWithTag("filters-button").performClick()
        onNodeWithTag("status-NOT_STARTED").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.status == LessonStatus.NOT_STARTED }
        onNodeWithTag("sort-TITLE").performClick()
        waitUntil(timeoutMillis = 5_000) { viewModel.state.value.sort == CoursesSort.TITLE }
        save("COURSES_PHONE_SCREENSHOT")
        onNodeWithTag("apply-filters").performClick()
        waitUntil(timeoutMillis = 5_000) { !exists("filters-sheet") }
    }

    @Test
    fun phoneControlsFitAtLargeText() = runDesktopComposeUiTest(width = 400, height = 860) {
        val viewModel = viewModel()
        setContent {
            IdiomikoTheme {
                ProvideWindowWidth {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, 1.3f),
                        LocalLearningLanguage provides LearningLanguageState(listOf(languageId to "Portuguese"), languageId) {},
                    ) {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            val state by viewModel.state.collectAsState()
                            Column {
                                AppTopBar(title = "Courses", onNavigate = {}, section = NavSection.COURSES)
                                if (!state.loading) PhoneCoursesContent(state, CoursesActions())
                            }
                        }
                    }
                }
            }
        }
        waitFor("next-lesson")
        val rootWidth = onAllNodes(isRoot())[0].fetchSemanticsNode().size.width
        listOf("course-search", "filters-button", "start-lesson").forEach { tag ->
            val node = onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
            assertTrue(node.positionInRoot.x >= 0f && node.positionInRoot.x + node.size.width <= rootWidth, "$tag stays inside the viewport at 130% text")
        }
    }

    /** The screen gives each tile's menu the playlist menu, which offers the whole course. */
    @Test
    fun aTileOffersItsCourseToAPlaylist() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = viewModel()
        val asked = mutableListOf<PlaylistSource>()
        host {
            val state by viewModel.state.collectAsState()
            val popup: @Composable (PlaylistSource, Boolean, () -> Unit) -> Unit = { source, open, _ -> if (open) SideEffect { asked += source } }
            CompositionLocalProvider(LocalPlaylistPopup provides popup) {
                if (!state.loading) CoursesContent(state, CoursesActions())
            }
        }
        waitFor("course-menu-pt-mini-0100")
        onNodeWithTag("course-menu-pt-mini-0100").performClick()
        onNodeWithTag("menu-add-to-playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { asked.isNotEmpty() }
        assertEquals(PlaylistSource.WholeCourse("pt-mini-0100"), asked.first())
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
            ReadingStage.entries.forEach { reading.checkStage(bookId, books.pageCount(bookId), it) }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("Continue: Onde está o Tom?").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("1 of 10 read").assertExists()
        onNodeWithText("Continue here").assertExists()
        assertNotNull(onAllNodesWithText("Completed").fetchSemanticsNodes().singleOrNull())
        save("COURSE_SCREENSHOT")
        collecting.cancel()
    }

    /** A course's page offers the whole course to a playlist, and each lesson of it on its own. */
    @Test
    fun aCourseAndEachLessonCanGoIntoAPlaylist() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        val viewModel = CourseViewModel("pt-mini-0100", service)
        val offered = mutableListOf<Pair<PlaylistSource, String?>>()
        host {
            val state by viewModel.state.collectAsState()
            val button: @Composable (PlaylistSource, String?) -> Unit = { source, label -> SideEffect { offered += source to label } }
            CompositionLocalProvider(LocalPlaylistButton provides button) {
                if (!state.loading) CourseContent(state, onOpenLesson = {}, onCourses = {})
            }
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("A casa nova").fetchSemanticsNodes().isNotEmpty() }
        val sources = offered.toMap()
        assertEquals("Add course to playlist", sources[PlaylistSource.WholeCourse("pt-mini-0100")])
        val lessons = sources.keys.filterIsInstance<PlaylistSource.Lesson>()
        assertEquals(10, lessons.size, "every lesson has its own button")
        assertTrue(lessons.all { it.courseId == "pt-mini-0100" } && lessons.none { sources[it] != null }, "a lesson's button is an icon")
    }

    /** On a phone a lesson's title keeps its width beside its tags and the hint; they go under it when the line is full. */
    @Test
    fun aPhoneKeepsTheLessonTitleWhole() = runDesktopComposeUiTest(width = 400, height = 900) {
        val viewModel = CourseViewModel("pt-mini-0100", service)
        host {
            val state by viewModel.state.collectAsState()
            val tagged = state.progress?.let { p -> state.copy(progress = p.copy(lessons = p.lessons.map { it.copy(lesson = it.lesson.copy(tags = listOf("idiomiko"))) })) } ?: state
            if (!state.loading) CourseContent(tagged, onOpenLesson = {}, onCourses = {})
        }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithText("A casa nova").fetchSemanticsNodes().isNotEmpty() }
        save("COURSE_PHONE_SCREENSHOT")
        val title = onNodeWithText("A casa nova", useUnmergedTree = true).fetchSemanticsNode()
        val tag = onAllNodesWithText("idiomiko", useUnmergedTree = true).fetchSemanticsNodes().last()
        assertTrue(title.size.height < 2 * tag.size.height, "the title stays on one line, not a word or letter a line: ${title.size}")
    }

    @Test
    fun withoutCoursesTheReadyMadeOnesAreOfferedForDownload() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var asked = 0
        host { CoursesContent(CoursesUiState(loading = false, languageName = "Portuguese", packAvailable = true), CoursesActions(onDownloadCourses = { asked++ })) }
        onNodeWithText("Get started with courses").assertExists()
        save("COURSES_EMPTY_PACK_SCREENSHOT")
        onNodeWithTag("download-courses").performClick()
        assertEquals(1, asked)
    }

    /** While the ready-made courses download, the button shows it and takes no second click. */
    @Test
    fun theDownloadShowsOnTheButton() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var asked = 0
        host { CoursesContent(CoursesUiState(loading = false, languageName = "Portuguese", packAvailable = true, packState = PackState.Downloading(0.4f)), CoursesActions(onDownloadCourses = { asked++ })) }
        onNodeWithText("Preparing your courses").assertExists()
        onNodeWithTag("courses-progress").assertExists()
        onNodeWithText("Downloading…").assertExists()
        onNodeWithTag("download-courses").assertIsNotEnabled()
        assertEquals(0, asked)
        save("COURSES_LOADING_SCREENSHOT")
    }

    @Test
    fun withoutCoursesOrAPackTheFirstCourseIsCreated() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var created = 0
        host { CoursesContent(CoursesUiState(loading = false, languageName = "Estonian"), CoursesActions(onNewCourse = { created++ })) }
        onNodeWithText("Build a guided reading course from books and texts you choose.").assertExists()
        onNodeWithTag("download-courses").assertDoesNotExist()
        assertEquals(1, onAllNodesWithTag("new-course").fetchSemanticsNodes().size, "only the empty state's button")
        save("COURSES_EMPTY_SCREENSHOT")
        onNodeWithTag("new-course").performClick()
        assertEquals(1, created)
    }

    @Test
    fun theOwnCoursesLinkMakesANewCourse() = runDesktopComposeUiTest(width = 1586, height = 1000) {
        var created = 0
        val viewModel = viewModel()
        host {
            val state by viewModel.state.collectAsState()
            if (!state.loading) CoursesContent(state, CoursesActions(onNewCourse = { created++ }))
        }
        waitFor("new-course")
        onNodeWithTag("new-course").performScrollTo().performClick()
        assertEquals(1, created)
        // Without a word count the summary names the level and the courses read in it.
        onNodeWithText("0 of 3 courses").assertExists()
        onNodeWithTag("journey-A1").assertTextEquals("A1", "A1 · Beginner", "0 of 3")
    }
}
