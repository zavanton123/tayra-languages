package com.idiomiko.feature.reading

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.idiomiko.core.domain.courses.Lesson
import com.idiomiko.core.domain.courses.LessonSummary
import com.idiomiko.core.ui.components.ProvideWindowWidth
import com.idiomiko.core.ui.theme.IdiomikoTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The end of a lesson on a phone: the whole screen, its ways on stacked at the foot. */
@OptIn(ExperimentalTestApi::class)
class LessonCompleteTest {
    private val summary = LessonSummary(learning = 1, flashcards = 1, quizRight = 0, quizAsked = 0, known = 102, words = 103, next = Lesson("pt-02", "Na escola", "", "A escola é grande."))

    @Test
    fun aPhoneShowsTheLessonsEndOnTheWholeScreen() = runDesktopComposeUiTest(width = 400, height = 850) {
        var next = false
        setContent {
            IdiomikoTheme { ProvideWindowWidth { LessonCompleteDialog(summary, onClose = {}, onCourse = {}, onNext = { next = true }) } }
        }
        val screen = onNodeWithTag("lesson-complete").getBoundsInRoot()
        assertEquals(400.dp, screen.right - screen.left)
        assertEquals(850.dp, screen.bottom - screen.top)
        val nextButton = onNodeWithTag("lesson-complete-next").getBoundsInRoot()
        val review = onNodeWithTag("lesson-complete-review").getBoundsInRoot()
        val course = onNodeWithTag("lesson-complete-course").getBoundsInRoot()
        assertTrue(nextButton.right - nextButton.left > 300.dp, "Next lesson takes the width, not ${nextButton.right - nextButton.left}")
        assertTrue(nextButton.bottom <= review.top && review.bottom <= course.top, "the next lesson first, then the review, then the course")
        assertTrue(850.dp - course.bottom < 40.dp, "the ways on sit at the screen's foot")
        System.getenv("LESSON_COMPLETE_PHONE_SCREENSHOT")?.let { waitForIdle(); ImageIO.write(onAllNodes(isRoot())[1].captureToImage().toAwtImage(), "png", File(it)) }
        onNodeWithTag("lesson-complete-next").performClick()
        assertTrue(next)
    }
}
