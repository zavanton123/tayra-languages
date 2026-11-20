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
        // The level card: B1 by the words known, with the words still to go to B2.
        onNodeWithTag("level-card").assertIsDisplayed()
        onNodeWithText("1,540 words known").assertExists()
        onNodeWithText("460 words to B2").assertExists()
        // The journey: A1 and A2 ticked, the reader at B1.
        onNodeWithText("3 of 3 courses").assertExists()
        onNodeWithText("7 of 7 courses").assertExists()
        onNodeWithText("0 of 10 · you are here").assertExists()
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
        assertEquals(2, onAllNodesWithText("A1 · Beginner").fetchSemanticsNodes().size, "on the level card and the journey")
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
        // A finished level unfolds the finished ones.
        onNodeWithTag("journey-A1").performClick()
        waitUntil(timeoutMillis = 5_000) { exists("course-pt-mini-0100") }
        onNodeWithTag("course-pt-mini-0100").assertIsDisplayed()
        assertEquals(2, onAllNodesWithText("Hide").fetchSemanticsNodes().size, "the finished levels and B2 can be folded again")
    }

    @Test
    fun theFinishedLevelsAndTheFoldedOnesOpenInPlace() = runDesktopComposeUiTest(width = 1600, height = 1100) {
        page(JourneyFixture.underWay())
        onNodeWithText("Review them").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { exists("course-pt-mini-0100") }
        assertTrue(exists("course-pt-mini-1000"))
        onNodeWithTag("level-header-C1").performScrollTo().performClick()
        waitUntil(timeoutMillis = 5_000) { exists("course-own-course-noticias") }
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
        assertTrue(!exists("next-lesson"), "results come first")
        onNodeWithTag("clear-filters").performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun thePhoneShowsAStripTheNextLessonAndTwoColumns() = runDesktopComposeUiTest(width = 400, height = 860) {
        page(JourneyFixture.underWay(), phone = true)
        onNodeWithTag("journey-strip").assertIsDisplayed()
        onNodeWithTag("filters-button").assertIsDisplayed()
        assertTrue(!exists("sidebar") && !exists("hero-scene"), "no sidebar and no picture on a phone")
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

    /** A window too narrow for the sidebar shows the page in one column, the level as a strip and the filters behind a button. */
    @Test
    fun aMiddleSizedWindowHasOneColumn() = runDesktopComposeUiTest(width = 820, height = 1000) {
        page(JourneyFixture.underWay())
        onNodeWithTag("journey-strip").assertIsDisplayed()
        onNodeWithTag("filters-button").assertIsDisplayed()
        assertTrue(!exists("sidebar"))
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
