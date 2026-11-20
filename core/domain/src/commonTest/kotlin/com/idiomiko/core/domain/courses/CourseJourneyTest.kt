package com.idiomiko.core.domain.courses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class CourseJourneyTest {
    private val at = Instant.fromEpochMilliseconds(1_700_000_000_000)

    /** A course of ten lessons up to [rankUpTo] at [level], with [done] lessons completed and [opened] opened. */
    private fun course(rankUpTo: Int?, level: CourseLevel, done: Int = 0, opened: Int = done, id: String = "c$rankUpTo$level", openedAt: Instant = at): CourseProgress {
        val course = Course(
            id = id, languageCode = "pt", title = id, description = "", level = level, topic = "", rankUpTo = rankUpTo,
            lessons = (1..10).map { Lesson("$id-$it", "Lição $it", "", "Uma casa.") },
        )
        return CourseProgress(course, course.lessons.mapIndexed { i, lesson ->
            LessonProgress(lesson, if (i < opened) LessonBook(lesson.id, i.toLong(), 1, 1, openedAt, isCompleted = i < done) else null)
        })
    }

    /** The Portuguese courses' levels: A1 up to rank 300, A2 to 1,000, B1 to 2,000, B2 to 4,000. */
    private fun portuguese(doneUpTo: Int = 0): List<CourseProgress> =
        (100..4000 step 100).map { rank ->
            val level = when {
                rank <= 300 -> CourseLevel.A1
                rank <= 1000 -> CourseLevel.A2
                rank <= 2000 -> CourseLevel.B1
                else -> CourseLevel.B2
            }
            course(rank, level, done = if (rank <= doneUpTo) 10 else 0)
        }

    @Test
    fun theWordsKnownPlaceTheReaderByTheLevelsBands() {
        val journey = CourseJourney.of(portuguese(doneUpTo = 1000), known = 1540)
        assertEquals(CourseLevel.B1, journey.current)
        assertEquals(CourseLevel.B2, journey.next?.level)
        assertEquals(460, journey.wordsToNext, "B1's courses go up to rank 2,000")
        assertEquals(listOf(CourseLevel.A1, CourseLevel.A2), journey.finished.map { it.level })
        assertEquals(listOf(3, 7, 10, 20), journey.levels.map { it.courses })
        assertEquals(LevelProgress(CourseLevel.A2, 7, 7, 70, 70, 1000), journey.levels[1])
    }

    @Test
    fun aLevelsEdgeBelongsToTheNextLevel() {
        assertEquals(CourseLevel.A1, CourseJourney.of(portuguese(), known = 0).current)
        assertEquals(CourseLevel.A1, CourseJourney.of(portuguese(), known = 299).current)
        assertEquals(CourseLevel.A2, CourseJourney.of(portuguese(), known = 300).current)
        // Knowing more than any course teaches leaves the reader at the last level, with nothing further.
        val top = CourseJourney.of(portuguese(), known = 9000)
        assertEquals(CourseLevel.B2, top.current)
        assertNull(top.next)
        assertNull(top.wordsToNext)
    }

    @Test
    fun theCurrentLevelIsNotAmongTheFinishedOnes() {
        val journey = CourseJourney.of(portuguese(doneUpTo = 2000), known = 1990)
        assertEquals(CourseLevel.B1, journey.current)
        assertEquals(listOf(CourseLevel.A1, CourseLevel.A2), journey.finished.map { it.level })
        assertEquals(1f, journey.currentProgress?.share)
    }

    @Test
    fun withoutAWordCountTheCourseReadLastPlacesTheReader() {
        val earlier = Instant.fromEpochMilliseconds(1_600_000_000_000)
        val courses = listOf(
            course(null, CourseLevel.A1, done = 10, id = "own-a1"),
            course(null, CourseLevel.A2, done = 1, opened = 2, id = "own-a2", openedAt = earlier),
            course(null, CourseLevel.B1, done = 0, opened = 1, id = "own-b1"),
        )
        assertEquals(CourseLevel.B1, CourseJourney.of(courses, known = null).current)
        // Own courses have no bands: the count alone does not place the reader.
        assertEquals(CourseLevel.B1, CourseJourney.of(courses, known = 50).current)
        assertNull(CourseJourney.of(courses, known = 50).wordsToNext)
        // Nothing being read: the first level not finished.
        val nothingOpen = listOf(course(null, CourseLevel.A1, done = 10, id = "a"), course(null, CourseLevel.B2, id = "b"))
        assertEquals(CourseLevel.B2, CourseJourney.of(nothingOpen, known = null).current)
    }

    @Test
    fun noCoursesMakeNoJourney() {
        val journey = CourseJourney.of(emptyList(), known = 100)
        assertEquals(emptyList(), journey.levels)
        assertNull(journey.current)
        assertNull(journey.next)
    }

    @Test
    fun aLevelsShareIsItsLessonsRead() {
        val journey = CourseJourney.of(listOf(course(100, CourseLevel.A1, done = 3), course(200, CourseLevel.A1)), known = 10)
        assertEquals(0.15f, journey.levels.single().share)
        assertEquals(false, journey.levels.single().done)
    }
}
