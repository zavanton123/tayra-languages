package com.idiomiko.feature.courses

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.playlists.PlaylistSource
import com.idiomiko.core.ui.components.AppTopBar
import com.idiomiko.core.ui.components.LearningLanguageState
import com.idiomiko.core.ui.components.LocalLearningLanguage
import com.idiomiko.core.ui.components.NavSection
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.AppTheme
import com.idiomiko.core.ui.theme.AppThemes
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The courses as a journey: the reader's level and the levels' stations, the next lesson with its path, the courses
 * of the reader's level as tiles, the finished levels folded into one line, and the others to open. COURSES_SHOTS
 * names a folder to save the page's pictures in.
 */
@OptIn(ExperimentalTestApi::class)
class CoursesJourneyTest {
    private fun ComposeUiTest.page(state: CoursesUiState, actions: CoursesActions = CoursesActions(), theme: AppTheme = AppThemes.default, phone: Boolean = false, popup: (@Composable (PlaylistSource, Boolean, () -> Unit) -> Unit)? = null) = setContent {
        IdiomikoTheme(theme) {
            ProvideWindowWidth {
                CompositionLocalProvider(
                    LocalLearningLanguage provides LearningLanguageState(listOf(1L to "Portuguese"), 1L) {},
                    LocalPlaylistPopup provides popup,
                ) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column {
                            AppTopBar(title = "Courses", onNavigate = {}, section = NavSection.COURSES)
                            if (phone) PhoneCoursesContent(state, actions) else CoursesContent(state, actions)
                        }
                    }
                }
            }
        }
    }

    private fun ComposeUiTest.shot(name: String) {
        val dir = System.getenv("COURSES_SHOTS") ?: return
        waitForIdle()
        File(dir).mkdirs()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
    }

    private fun ComposeUiTest.exists(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theDesktopPageShowsTheJourneyTheNextLessonAndTheReadersLevel() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val state = JourneyFixture.underWay()
        page(state)
        // The pane's journey: B1 by the words known, with the words still to go to B2; A1 and A2 ticked.
        onNodeWithTag("top-pane").assertIsDisplayed()
        onNodeWithTag("level-summary").assertIsDisplayed()
        onNodeWithText("1,540 words known · 460 words to B2").assertExists()
        onNodeWithText("3 of 3 courses").assertExists()
        onNodeWithText("7 of 7 courses").assertExists()
        onNodeWithTag("journey-B1").assertTextEquals("B1", "B1 · Intermediate", "0 of 10")
        // The six levels in one line beside the reader's level, and the filters' controls in the pane.
        val levels = CourseLevel.entries.map { onNodeWithTag("journey-${it.name}").fetchSemanticsNode().positionInRoot.y }
        assertTrue(levels.all { it == levels.first() }, "one line of levels: $levels")
        assertTrue(onNodeWithTag("level-summary").fetchSemanticsNode().positionInRoot.x < onNodeWithTag("journey-A1").fetchSemanticsNode().positionInRoot.x)
        listOf("course-search", "new-course", "status-ALL", "tag-filter", "course-sort", "view-GRID").forEach { onNodeWithTag(it).assertIsDisplayed() }
        // The next lesson: the fourth of the course under way, begun, with its recording.
        onNodeWithText("KEEP GOING · LUGARES DO BRASIL").assertExists()
        onNodeWithText("Lesson 4: Uma excursão a Brasília", substring = true).assertExists()
        onNodeWithText("with audio", substring = true).assertExists()
        onNodeWithText("Continue lesson").assertExists()
        assertTrue(exists("hero-scene"), "a wide card has its picture")
        // B1's courses: the one under way and the one picked next marked, the others with the share known.
        onNodeWithText("Picked for your 1,540 words").assertExists()
        assertEquals(1, onAllNodesWithTag("pin-in-progress", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(1, onAllNodesWithTag("pin-up-next", useUnmergedTree = true).fetchSemanticsNodes().size)
        assertTrue(onAllNodesWithTag("known-share", useUnmergedTree = true).fetchSemanticsNodes().size >= 8, "the courses not started say how much is known")
        // A1 and A2 fold into one line; B2 and above wait folded.
        onNodeWithText("A1 and A2 complete", substring = true).assertExists()
        assertTrue(!exists("course-pt-mini-0100") && !exists("course-pt-mini-2100"))
        shot("desktop-light")
    }

    @Test
    fun theDesktopPageInTheDarkTheme() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        page(JourneyFixture.underWay(), theme = AppThemes.night)
        onNodeWithTag("next-lesson").assertIsDisplayed()
        shot("desktop-dark")
    }

    @Test
    fun aFirstVisitStartsAtTheFirstLessonOfTheFirstCourse() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        page(JourneyFixture.firstTime())
        onNodeWithText("START HERE · EM CASA E COM A FAMÍLIA").assertExists()
        onNodeWithText("Start lesson").assertExists()
        // No word known yet: the card names the level rather than counting none.
        assertEquals(2, onAllNodesWithText("A1 · Beginner").fetchSemanticsNodes().size, "on the level summary and the journey")
        onNodeWithText("300 words to A2").assertExists()
        // Nothing is finished, so there is no line of finished levels; the first course is the one to start.
        assertTrue(!exists("finished-levels"))
        assertEquals(1, onAllNodesWithTag("pin-up-next", useUnmergedTree = true).fetchSemanticsNodes().size)
        onNodeWithTag("course-pt-mini-0100").assertIsDisplayed()
        shot("desktop-first-time")
    }

    @Test
    fun choosingALevelOnTheJourneyOpensItsCourses() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        page(JourneyFixture.underWay())
        onNodeWithTag("journey-B2").performClick()
        waitUntil(timeoutMillis = 5_000) { exists("course-pt-mini-2100") }
        onNodeWithTag("course-pt-mini-2100").assertIsDisplayed()
        // A finished level unfolds the finished ones; the pane scrolls with the page, so it is scrolled back to first.
        onNodeWithTag("journey-A1").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { exists("course-pt-mini-0100") }
        onNodeWithTag("course-pt-mini-0100").assertIsDisplayed()
        assertEquals(3, onAllNodesWithText("Hide").fetchSemanticsNodes().size, "B1, the finished levels and B2 can be folded again")
    }

    @Test
    fun theFinishedLevelsAndTheFoldedOnesOpenInPlace() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        page(JourneyFixture.underWay())
        onNodeWithText("Review them").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { exists("course-pt-mini-0100") }
        assertTrue(exists("course-pt-mini-1000"))
        onNodeWithTag("level-header-C1").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { exists("course-own-course-noticias") }
        // The reader's level folds like the others, and its place on the journey opens it again.
        onNodeWithTag("level-header-B1").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { !exists("course-pt-mini-1900") }
        onNodeWithTag("journey-B1").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { exists("course-pt-mini-1900") }
    }

    @Test
    fun theHeroAndTheTilesOpenLessonsAndCourses() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val opened = mutableListOf<String>()
        val lessons = mutableListOf<String>()
        page(JourneyFixture.underWay(), CoursesActions(onOpen = { opened += it }, onOpenLesson = { _, id -> lessons += id }))
        onNodeWithTag("start-lesson").performClick()
        onNodeWithTag("lesson-stop-2").performClick()
        assertEquals(listOf("pt-mini-1900-04", "pt-mini-1900-02"), lessons)
        onNodeWithTag("all-lessons").performClick()
        onNodeWithTag("course-pt-mini-1600").performScrollTo().performClick()
        assertEquals(listOf("pt-mini-1900", "pt-mini-1600"), opened)
    }

    /** A tile's ⋮ adds the course to a playlist; the playlist menu opens where the tile's button is. */
    @Test
    fun aTilesMenuAddsTheCourseToAPlaylist() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val asked = mutableListOf<PlaylistSource>()
        page(JourneyFixture.underWay(), popup = { source, open, _ -> if (open) SideEffect { asked += source } })
        onNodeWithTag("course-pt-mini-1600").performScrollTo()
        onNodeWithTag("course-menu-pt-mini-1600").performClick()
        onNodeWithTag("menu-add-to-playlist").performClick()
        waitUntil(timeoutMillis = 5_000) { asked.isNotEmpty() }
        assertEquals(PlaylistSource.WholeCourse("pt-mini-1600"), asked.first())
    }

    @Test
    fun aSearchShowsTheMatchingCoursesInsteadOfTheJourney() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        var cleared = 0
        page(JourneyFixture.underWay().copy(search = "Lugares"), CoursesActions(onClearFilters = { cleared++ }))
        onNodeWithTag("result-count").assertTextEquals("1 course")
        onNodeWithTag("course-pt-mini-1900").assertIsDisplayed()
        assertTrue(exists("next-lesson"), "the next lesson stays above the search, so the search keeps its place")
        onNodeWithTag("clear-filters").performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun thePhoneShowsAStripTheNextLessonAndTwoColumns() = runDesktopComposeUiTest(width = 400, height = 860) {
        page(JourneyFixture.underWay(), phone = true)
        onNodeWithTag("journey-strip").assertIsDisplayed()
        onNodeWithTag("filters-button").assertIsDisplayed()
        assertTrue(!exists("top-pane") && !exists("hero-scene"), "no pane and no picture on a phone")
        val order = listOf("journey-strip", "course-search", "next-lesson", "course-pt-mini-1100").map { onNodeWithTag(it).fetchSemanticsNode().positionInRoot.y }
        assertEquals(order.sorted(), order)
        // Two tiles side by side.
        val first = onNodeWithTag("course-pt-mini-1100").fetchSemanticsNode().positionInRoot
        val second = onNodeWithTag("course-pt-mini-1200").fetchSemanticsNode().positionInRoot
        assertEquals(first.y, second.y)
        shot("phone-light")
    }

    @Test
    fun thePhoneInTheDarkTheme() = runDesktopComposeUiTest(width = 400, height = 860) {
        page(JourneyFixture.underWay(), theme = AppThemes.night, phone = true)
        onNodeWithTag("journey-strip").assertIsDisplayed()
        shot("phone-dark")
    }

    @Test
    fun aPhoneFirstVisit() = runDesktopComposeUiTest(width = 400, height = 860) {
        page(JourneyFixture.firstTime(), phone = true)
        onNodeWithText("START HERE · EM CASA E COM A FAMÍLIA").assertExists()
        shot("phone-first-time")
    }

    /** The filters sit behind a button on a phone: status, tags, order, and a course of one's own. */
    @Test
    fun thePhoneFiltersOpenInASheet() = runDesktopComposeUiTest(width = 400, height = 860) {
        val statuses = mutableListOf<Any?>()
        val tags = mutableListOf<String>()
        val sorts = mutableListOf<CoursesSort>()
        var created = 0
        page(
            JourneyFixture.underWay(),
            CoursesActions(onStatus = { statuses += it }, onToggleTag = { tags += it }, onSort = { sorts += it }, onNewCourse = { created++ }),
            phone = true,
        )
        onNodeWithTag("filters-button").performClick()
        onNodeWithTag("filters-sheet").assertExists()
        shot("phone-filters")
        onNodeWithTag("status-IN_PROGRESS").performClick()
        onNodeWithTag("tag-chip-mine").performClick()
        onNodeWithTag("sort-TITLE").performClick()
        assertEquals(listOf<Any?>(com.idiomiko.core.domain.courses.LessonStatus.IN_PROGRESS), statuses)
        assertEquals(listOf("mine"), tags)
        assertEquals(listOf(CoursesSort.TITLE), sorts)
        onNodeWithTag("new-course").performClick()
        assertEquals(1, created)
    }

    @Test
    fun theListViewShowsTheCoursesAsRows() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        page(JourneyFixture.underWay().copy(view = CoursesView.LIST))
        onNodeWithTag("course-pt-mini-1100").assertIsDisplayed()
        val first = onNodeWithTag("course-pt-mini-1100").fetchSemanticsNode().positionInRoot
        val second = onNodeWithTag("course-pt-mini-1200").fetchSemanticsNode().positionInRoot
        assertTrue(second.y > first.y && second.x == first.x, "one course a row")
        shot("desktop-list")
    }

    /** The pane's search, filters, order and view, and its way to a course of one's own. */
    @Test
    fun thePanesControlsSearchFilterSortAndMakeACourse() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        val searches = mutableListOf<String>()
        val statuses = mutableListOf<Any?>()
        val tags = mutableListOf<String>()
        val sorts = mutableListOf<CoursesSort>()
        val views = mutableListOf<CoursesView>()
        var created = 0
        page(
            JourneyFixture.underWay(),
            CoursesActions(
                onSearch = { searches += it },
                onStatus = { statuses += it },
                onToggleTag = { tags += it },
                onSort = { sorts += it },
                onView = { views += it },
                onNewCourse = { created++ },
            ),
        )
        onNodeWithTag("course-search").performTextInput("casa")
        onNodeWithTag("status-IN_PROGRESS").performClick()
        onNodeWithTag("tag-chip-mine").performClick()
        onNodeWithTag("course-sort").performClick()
        onNodeWithText("Title").performClick()
        onNodeWithTag("view-LIST").performClick()
        onNodeWithTag("new-course").performClick()
        assertEquals("casa", searches.first())
        assertEquals(listOf<Any?>(com.idiomiko.core.domain.courses.LessonStatus.IN_PROGRESS), statuses)
        assertEquals(listOf("mine"), tags)
        assertEquals(listOf(CoursesSort.TITLE), sorts)
        assertEquals(listOf(CoursesView.LIST), views)
        assertEquals(1, created)
    }

    /** A window just wide enough for the pane puts the levels under the reader's level, in two even rows. */
    @Test
    fun aNarrowerWideWindowWrapsTheLevels() = runDesktopComposeUiTest(width = 1000, height = 1100) {
        page(JourneyFixture.underWay())
        onNodeWithTag("top-pane").assertIsDisplayed()
        val summary = onNodeWithTag("level-summary").fetchSemanticsNode().positionInRoot
        val places = CourseLevel.entries.associateWith { onNodeWithTag("journey-${it.name}").fetchSemanticsNode().positionInRoot }
        assertTrue(places.values.all { it.y > summary.y }, "the levels under the summary")
        assertEquals(places.getValue(CourseLevel.A1).y, places.getValue(CourseLevel.B1).y, "A1 to B1 in the first row")
        assertEquals(places.getValue(CourseLevel.B2).y, places.getValue(CourseLevel.C2).y, "B2 to C2 in the second")
        assertEquals(places.getValue(CourseLevel.A1).x, places.getValue(CourseLevel.B2).x, "the rows line up")
        assertEquals(places.getValue(CourseLevel.B1).x, places.getValue(CourseLevel.C2).x, "in columns")
        assertTrue(!exists("hero-scene"), "no room for the picture beside the lesson path")
        shot("desktop-1000")
    }

    /** A window too narrow for the pane shows the page in one column, the level as a strip and the filters behind a button. */
    @Test
    fun aMiddleSizedWindowHasOneColumn() = runDesktopComposeUiTest(width = 820, height = 1000) {
        page(JourneyFixture.underWay())
        onNodeWithTag("journey-strip").assertIsDisplayed()
        onNodeWithTag("filters-button").assertIsDisplayed()
        assertTrue(!exists("top-pane"))
        val first = onNodeWithTag("course-pt-mini-1100").fetchSemanticsNode().positionInRoot
        val third = onNodeWithTag("course-pt-mini-1300").fetchSemanticsNode().positionInRoot
        assertEquals(first.y, third.y, "three tiles a row")
        shot("medium-light")
    }

    @Test
    fun theLevelsFollowTheCourses() {
        val journey = JourneyFixture.underWay().journey
        assertEquals(CourseLevel.entries, journey.levels.map { it.level })
        assertEquals(CourseLevel.B1, journey.current)
    }
}
