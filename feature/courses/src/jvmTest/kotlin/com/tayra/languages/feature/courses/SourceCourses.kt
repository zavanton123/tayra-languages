package com.tayra.languages.feature.courses

import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.SampleCourseSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** The Portuguese courses read from their sources in tools/courses/pt, tagged as tools/build_courses.py tags them in the pack. */
object SourceCourses : SampleCourseSource {
    private val all: List<Course> by lazy {
        File("../../tools/courses/pt").listFiles { file -> file.name.startsWith("pt-mini-") && file.name.endsWith(".json") }!!
            .map { parse(it) }
            .sortedBy { it.rankUpTo }
    }

    override suspend fun courses(languageCode: String): List<Course> = if (languageCode == "pt") all else emptyList()

    private fun parse(file: File): Course {
        val json = Json.parseToJsonElement(file.readText()).jsonObject
        fun text(key: String) = json[key]?.jsonPrimitive?.content.orEmpty()
        val id = text("id")
        return Course(
            id = id,
            languageCode = "pt",
            title = text("title"),
            description = text("description"),
            level = CourseLevel.entries.first { it.code == text("level") },
            topic = text("topic"),
            lessons = json.getValue("lessons").jsonArray.mapIndexed { i, element ->
                val lesson = element.jsonObject
                Lesson(
                    id = "$id-${(i + 1).toString().padStart(2, '0')}",
                    title = lesson.getValue("title").jsonPrimitive.content,
                    summary = lesson["summary"]?.jsonPrimitive?.content.orEmpty(),
                    text = lesson.getValue("text").jsonPrimitive.content.trim(),
                    newWords = lesson["newWords"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                    tags = listOf("tayra"),
                )
            },
            rankUpTo = json.getValue("rankUpTo").jsonPrimitive.int,
            tags = listOf("tayra"),
        )
    }
}
