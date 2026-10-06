package com.tayra.languages.feature.courses

import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.SampleCourseSource
import com.tayra.languages.core.domain.courses.SampleCourses
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.MissingResourceException
import tayra_languages.feature.courses.generated.resources.Res

/**
 * The sample courses: the hand-written ones, and the courses built on the word frequency list,
 * bundled per language as files/courses/<code>.json by tools/build_courses.py.
 */
class BundledSampleCourses : SampleCourseSource {
    override suspend fun courses(languageCode: String): List<Course> =
        frequencyCourses(languageCode) + SampleCourses.ALL.filter { it.languageCode == languageCode }

    private suspend fun frequencyCourses(code: String): List<Course> {
        val bytes = try {
            Res.readBytes("files/courses/$code.json")
        } catch (e: MissingResourceException) {
            return emptyList()
        }
        return json.decodeFromString<Bundle>(bytes.decodeToString()).courses.map { course ->
            Course(
                id = course.id,
                languageCode = code,
                title = course.title,
                description = course.description,
                level = CourseLevel.entries.firstOrNull { it.code == course.level } ?: CourseLevel.A1,
                topic = course.topic,
                lessons = course.lessons.map { Lesson(it.id, it.title, it.summary, it.text, it.newWords) },
                rankUpTo = course.rankUpTo,
            )
        }
    }

    @Serializable
    private class Bundle(val courses: List<BundledCourse>)

    @Serializable
    private class BundledCourse(
        val id: String,
        val rankUpTo: Int,
        val level: String,
        val title: String,
        val description: String,
        val topic: String = "",
        val lessons: List<BundledLesson>,
    )

    @Serializable
    private class BundledLesson(val id: String, val title: String, val summary: String = "", val newWords: List<String> = emptyList(), val text: String)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
