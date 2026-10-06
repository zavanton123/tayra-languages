package com.tayra.languages.feature.courses

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The frequency courses bundled with the app are read, in rank order. */
class BundledSampleCoursesTest {
    @Test
    fun thePortugueseCoursesAreBundled() = runBlocking {
        val courses = BundledSampleCourses().courses("pt")
        assertEquals((100..1000 step 100).toList(), courses.map { it.rankUpTo })
        courses.forEach { course ->
            assertTrue(course.id.startsWith("pt-freq-") && course.lessons.size == 10, course.id)
            assertTrue(course.lessons.all { it.newWords.isNotEmpty() && it.text.isNotBlank() }, course.id)
        }
        assertEquals(emptyList(), BundledSampleCourses().courses("xx"))
    }
}
