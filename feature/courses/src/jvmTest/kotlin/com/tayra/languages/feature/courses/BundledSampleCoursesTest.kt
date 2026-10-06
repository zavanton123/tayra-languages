package com.tayra.languages.feature.courses

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The frequency courses bundled with the app are read, in rank order, with the hand-written samples after them. */
class BundledSampleCoursesTest {
    @Test
    fun thePortugueseCoursesAreBundled() = runBlocking {
        val courses = BundledSampleCourses().courses("pt")
        val frequency = courses.filter { it.rankUpTo != null }
        assertTrue(frequency.isNotEmpty(), "at least one frequency course is bundled")
        assertEquals(frequency.sortedBy { it.rankUpTo }, frequency)
        frequency.forEach { course ->
            assertTrue(course.id.startsWith("pt-freq-") && course.lessons.size == 10, course.id)
            assertTrue(course.lessons.all { it.newWords.isNotEmpty() && it.text.isNotBlank() }, course.id)
        }
        assertEquals(listOf("pt-primeiros-passos", "pt-vida-na-cidade", "pt-historias-curtas"), courses.filter { it.rankUpTo == null }.map { it.id })
        assertEquals(emptyList(), BundledSampleCourses().courses("xx"))
    }
}
