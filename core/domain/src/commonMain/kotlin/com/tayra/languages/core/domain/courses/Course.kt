package com.tayra.languages.core.domain.courses

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/** How hard a course is, on the European scale. */
enum class CourseLevel(val code: String, val label: String) {
    A1("A1", "Beginner"),
    A2("A2", "Elementary"),
    B1("B1", "Intermediate"),
    B2("B2", "Upper intermediate"),
    C1("C1", "Advanced"),
    C2("C2", "Proficient"),
}

/** A text to read, part of a course. */
data class Lesson(
    /** Unique among all lessons, and stable: a lesson already opened is found again by it. */
    val id: String,
    val title: String,
    val summary: String,
    val text: String,
    /** The words of the language's frequency list the lesson introduces, as the list writes them. */
    val newWords: List<String> = emptyList(),
) {
    /** Words in the text, counted at spaces. */
    val wordCount: Int get() = text.split(Regex("""\s+""")).count { part -> part.any { it.isLetterOrDigit() } }
}

/** A course: lessons to read in order. */
data class Course(
    val id: String,
    /** ISO 639-1 code of the language taught. */
    val languageCode: String,
    val title: String,
    val description: String,
    val level: CourseLevel,
    val topic: String,
    val lessons: List<Lesson>,
    /** The language the course is stored for; 0 in the sample data before it is written into the database. */
    val languageId: Long = 0,
    /** Whether the course is one of the app's samples rather than one the reader made; both can be changed. */
    val builtIn: Boolean = false,
    /** The rank in the language's frequency list of the rarest word the course teaches; null for courses not built on it. */
    val rankUpTo: Int? = null,
) {
    val wordCount: Int get() = lessons.sumOf { it.wordCount }
}

/** What the reader writes about a course of their own. */
data class CourseDraft(val title: String, val description: String = "", val level: CourseLevel = CourseLevel.A1, val topic: String = "")

/** What the reader writes for a lesson of their own. */
data class LessonDraft(val title: String, val summary: String = "", val text: String)

class CourseValidationException(message: String) : IllegalArgumentException(message)

/** The courses, the app's samples and the reader's own, with their lessons in order. */
interface CourseRepository {
    fun observeCourses(languageId: Long): Flow<List<Course>>
    fun observeCourse(courseId: String): Flow<Course?>
    suspend fun course(courseId: String): Course?
    suspend fun createCourse(id: String, languageId: Long, draft: CourseDraft)
    suspend fun updateCourse(courseId: String, draft: CourseDraft)
    suspend fun deleteCourse(courseId: String)
    /** Adds the lesson after the course's last one. */
    suspend fun addLesson(id: String, courseId: String, draft: LessonDraft)
    suspend fun updateLesson(lessonId: String, draft: LessonDraft)
    suspend fun deleteLesson(lessonId: String)
    /** Moves the lesson [by] places, later for positive, keeping it within the course. */
    suspend fun moveLesson(lessonId: String, by: Int)

    /** The sample courses written for the language with [languageId] so far, so ones deleted since stay deleted. */
    suspend fun seededSamples(languageId: Long): Set<String>
    /** Writes [courses] with their lessons for the language as samples, and notes each one. */
    suspend fun seedSamples(languageId: Long, courses: List<Course>)
    /** Forgets that the samples with [courseIds] were written, so they are written again when their pack is. */
    suspend fun forgetSeeded(courseIds: Collection<String>)
}

/** The sample courses on the device, for a language by its ISO 639-1 code. */
fun interface SampleCourseSource {
    suspend fun courses(languageCode: String): List<Course>

    /** The ids of [courses], which a source may read without the lessons. */
    suspend fun courseIds(languageCode: String): Set<String> = courses(languageCode).map { it.id }.toSet()
}

/** A lesson that was opened: the text made from it, and how far its reading has got. */
data class LessonBook(
    val lessonId: String,
    val bookId: Long,
    val currentPage: Int,
    val pageCount: Int,
    val lastOpened: Instant?,
    val isCompleted: Boolean,
)

/** A lesson being read, and where it sits in its course. */
data class LessonReading(val course: Course, val lesson: Lesson) {
    /** The lesson's place in the course, from 1. */
    val number: Int get() = course.lessons.indexOfFirst { it.id == lesson.id } + 1
}

enum class LessonStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

data class LessonProgress(val lesson: Lesson, val book: LessonBook?) {
    val status: LessonStatus
        get() = when {
            book == null || (book.lastOpened == null && !book.isCompleted) -> LessonStatus.NOT_STARTED
            book.isCompleted -> LessonStatus.COMPLETED
            else -> LessonStatus.IN_PROGRESS
        }
}

data class CourseProgress(val course: Course, val lessons: List<LessonProgress>) {
    val completed: Int get() = lessons.count { it.status == LessonStatus.COMPLETED }
    val started: Boolean get() = lessons.any { it.status != LessonStatus.NOT_STARTED }
    val status: LessonStatus
        get() = when {
            lessons.isNotEmpty() && completed == lessons.size -> LessonStatus.COMPLETED
            started -> LessonStatus.IN_PROGRESS
            else -> LessonStatus.NOT_STARTED
        }

    /** The lesson to go on with: the first one not completed, or the first of a finished course. */
    val nextLesson: LessonProgress? get() = lessons.firstOrNull { it.status != LessonStatus.COMPLETED } ?: lessons.firstOrNull()
}
