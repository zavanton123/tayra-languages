package com.idiomiko.core.domain.courses

import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.TermStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CourseWordsTest {
    private val portuguese = Language(id = 1, name = "Portuguese")

    private fun course(vararg texts: String) = Course(
        id = "c", languageCode = "pt", title = "Curso", description = "", level = CourseLevel.A1, topic = "",
        lessons = texts.mapIndexed { i, text -> Lesson("l$i", "Lição", "", text) },
    )

    @Test
    fun aCoursesWordsAreItsLessonsDistinctWordsInLowerCase() {
        val words = CourseWords.of(course("A casa é grande. A Casa!", "O gato está na casa."), portuguese)
        assertEquals(setOf("a", "casa", "é", "grande", "o", "gato", "está", "na"), words)
    }

    @Test
    fun aWordSavedWithAnyStatusButNewCountsAsKnown() {
        val words = setOf("a", "casa", "é", "grande", "o", "gato", "está", "na")
        val statuses = mapOf(
            "a" to TermStatus.WELL_KNOWN, "casa" to TermStatus.NEW_1, "é" to TermStatus.IGNORED,
            "o" to TermStatus.LEARNING_4, "gato" to TermStatus.UNKNOWN, "carro" to TermStatus.WELL_KNOWN,
        )
        assertEquals(50, CourseWords.knownPercent(words, statuses))
        // Rounded down: one word short of all is not 100%.
        assertEquals(99, CourseWords.knownPercent((1..100).map { "w$it" }.toSet(), (1..99).associate { "w$it" to TermStatus.WELL_KNOWN }))
        assertNull(CourseWords.knownPercent(emptySet(), statuses))
    }
}
