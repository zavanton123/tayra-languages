package com.tayra.languages.core.domain.courses

import kotlin.time.Instant

/** How hard a course is, on the European scale. */
enum class CourseLevel(val code: String, val label: String) {
    A1("A1", "Beginner"),
    A2("A2", "Elementary"),
    B1("B1", "Intermediate"),
    B2("B2", "Upper intermediate"),
}

/** A text to read, part of a course. */
data class Lesson(
    /** Unique among all lessons, and stable: a lesson already opened is found again by it. */
    val id: String,
    val title: String,
    val summary: String,
    val text: String,
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
) {
    val wordCount: Int get() = lessons.sumOf { it.wordCount }
}

/** Where courses come from. */
interface CourseSource {
    /** The courses teaching the language with [languageCode], in the order to show them. */
    suspend fun courses(languageCode: String): List<Course>
    suspend fun course(id: String): Course?
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
