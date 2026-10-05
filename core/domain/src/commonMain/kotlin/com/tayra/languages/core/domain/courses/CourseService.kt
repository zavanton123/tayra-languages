package com.tayra.languages.core.domain.courses

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Courses and how far the reader has got in them. A lesson is read like any text: the first time
 * it is opened a text is made from it, kept apart from the reader's own books, and from then on
 * the lesson's progress is that text's.
 */
class CourseService(
    private val source: CourseSource,
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val bookService: BookService,
) {
    /** The courses for the language with [languageId], with the reader's progress, kept up to date. */
    fun observeCourses(languageId: Long): Flow<List<CourseProgress>> = books.observeLessonBooks().map { opened ->
        val code = languages.getById(languageId)?.let { LanguageCodes.codeFor(it.name) } ?: return@map emptyList()
        source.courses(code).map { progress(it, opened) }
    }

    fun observeCourse(courseId: String): Flow<CourseProgress?> = books.observeLessonBooks().map { opened ->
        source.course(courseId)?.let { progress(it, opened) }
    }

    private fun progress(course: Course, opened: Map<String, LessonBook>) =
        CourseProgress(course, course.lessons.map { LessonProgress(it, opened[it.id]) })

    /** The lesson that the text with [bookId] was made from, or null for one of the reader's own books. */
    suspend fun lessonReading(bookId: Long): LessonReading? {
        val lessonId = books.observeLessonBooks().first().values.firstOrNull { it.bookId == bookId }?.lessonId ?: return null
        val code = books.getBook(bookId)?.let { languages.getById(it.languageId) }?.let { LanguageCodes.codeFor(it.name) } ?: return null
        return source.courses(code).firstNotNullOfOrNull { course ->
            course.lessons.firstOrNull { it.id == lessonId }?.let { LessonReading(course, it) }
        }
    }

    /**
     * The text to read for a lesson, made on first use. Null when the app does not have the
     * course's language.
     */
    suspend fun openLesson(courseId: String, lessonId: String): Long? {
        val course = source.course(courseId) ?: return null
        val lesson = course.lessons.firstOrNull { it.id == lessonId } ?: return null
        books.lessonBookId(lessonId)?.let { return it }
        val language = languages.getAll().firstOrNull { LanguageCodes.codeFor(it.name) == course.languageCode } ?: return null
        val bookId = bookService.create(BookDraft(languageId = language.id, title = lesson.title, text = lesson.text))
        books.linkLesson(lessonId, bookId)
        return bookId
    }
}
