package com.idiomiko.core.domain.frequency

import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class LevelEdgesTest {
    private fun course(level: CourseLevel, rankUpTo: Int?) =
        Course(id = "$level-$rankUpTo", languageCode = "pt", title = "", description = "", level = level, topic = "", lessons = emptyList(), rankUpTo = rankUpTo)

    private fun words(edges: List<LevelEdge>) = edges.map { (it.level?.code ?: "Starter") to it.words }

    @Test
    fun aLevelEndsWhereItsCoursesGo() {
        val courses = listOf(
            course(CourseLevel.A1, 200), course(CourseLevel.A1, 400),
            course(CourseLevel.A2, 1200),
            course(CourseLevel.B1, 2500),
            // A reader's own course is not built on the list and moves no edge.
            course(CourseLevel.B2, null),
        )
        assertEquals(
            listOf("Starter" to 0, "A1" to 400, "A2" to 1200, "B1" to 2500, "B2" to 4000, "C1" to 8000, "C2" to 10_000),
            words(LevelEdges.of(courses, 10_000)),
        )
    }

    @Test
    fun withoutCoursesTheLevelsEndWhereTheTableHasThem() {
        assertEquals(
            listOf("Starter" to 0, "A1" to 300, "A2" to 1000, "B1" to 2000, "B2" to 4000, "C1" to 8000, "C2" to 10_000),
            words(LevelEdges.of(emptyList(), 10_000)),
        )
    }

    @Test
    fun aShortListOffersOnlyTheLevelsItReaches() {
        assertEquals(listOf("Starter" to 0, "A1" to 300), words(LevelEdges.of(emptyList(), 300)))
        assertEquals(listOf("Starter" to 0, "A1" to 300, "A2" to 1000, "B1" to 2000, "B2" to 3500), words(LevelEdges.of(emptyList(), 3500)))
    }

    @Test
    fun aLevelAddingNoWordsIsLeftOut() {
        // B1's courses reach past the table's B2, so B2 would add nothing.
        val edges = LevelEdges.of(listOf(course(CourseLevel.B1, 5000)), 10_000)
        assertEquals(listOf("Starter" to 0, "A1" to 300, "A2" to 1000, "B1" to 5000, "C1" to 8000, "C2" to 10_000), words(edges))
    }

    @Test
    fun theReachedLevelIsTheLastWhoseWordsAreKnown() {
        val edges = LevelEdges.of(emptyList(), 10_000)
        assertEquals(null, LevelEdges.reached(edges, 0).level)
        assertEquals(null, LevelEdges.reached(edges, 299).level)
        assertEquals(CourseLevel.A1, LevelEdges.reached(edges, 300).level)
        assertEquals(CourseLevel.B1, LevelEdges.reached(edges, 2500).level)
        assertEquals(CourseLevel.C2, LevelEdges.reached(edges, 12_000).level)
    }
}
