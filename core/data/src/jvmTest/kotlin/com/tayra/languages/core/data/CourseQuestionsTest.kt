package com.tayra.languages.core.data

import com.tayra.languages.core.domain.courses.LessonQuestions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every lesson of the app's courses ends with questions the app can cut off its text. */
class CourseQuestionsTest {
    @Test
    fun everyCourseLessonSplitsIntoAStoryAndItsQuestions() {
        val files = File("../../tools/courses").listFiles().orEmpty().flatMap { language ->
            language.listFiles { file -> file.name.contains("-mini-") && file.name.endsWith(".json") }.orEmpty().toList()
        }
        // The courses are written in the repository; a checkout without them has nothing to check.
        if (files.isEmpty()) return
        val unsplit = mutableListOf<String>()
        var lessons = 0
        for (file in files) {
            val course = Json.parseToJsonElement(file.readText()).jsonObject
            course.getValue("lessons").jsonArray.forEachIndexed { i, lesson ->
                lessons++
                val split = LessonQuestions.split(lesson.jsonObject.getValue("text").jsonPrimitive.content)
                if (split == null || split.questions.size < 2) unsplit += "${file.name} lesson ${i + 1}"
            }
        }
        assertTrue(lessons > 0)
        assertEquals(emptyList(), unsplit)
    }
}
