package com.tayra.languages.core.domain.courses

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Courses and how far the reader has got in them: the ones that come with the app and the ones
 * the reader makes. A lesson is read like any text: the first time it is opened a text is made
 * from it, kept apart from the reader's own books, and from then on the lesson's progress is that
 * text's.
 */
class CourseService(
    private val source: CourseSource,
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val bookService: BookService,
    private val own: UserCourseRepository,
) {
    /** The courses for the language with [languageId], the app's first, then the reader's, kept up to date. */
    fun observeCourses(languageId: Long): Flow<List<CourseProgress>> =
        combine(books.observeLessonBooks(), own.observeCourses(languageId)) { opened, mine ->
            val code = languages.getById(languageId)?.let { LanguageCodes.codeFor(it.name) }
            val builtIn = code?.let { source.courses(it) }.orEmpty()
            (builtIn + mine).map { progress(it, opened) }
        }

    fun observeCourse(courseId: String): Flow<CourseProgress?> =
        combine(books.observeLessonBooks(), own.observeCourse(courseId)) { opened, mine ->
            (mine ?: source.course(courseId))?.let { progress(it, opened) }
        }

    private fun progress(course: Course, opened: Map<String, LessonBook>) =
        CourseProgress(course, course.lessons.map { LessonProgress(it, opened[it.id]) })

    /** The lesson that the text with [bookId] was made from, or null for one of the reader's own books. */
    suspend fun lessonReading(bookId: Long): LessonReading? {
        val lessonId = books.observeLessonBooks().first().values.firstOrNull { it.bookId == bookId }?.lessonId ?: return null
        val book = books.getBook(bookId) ?: return null
        val code = languages.getById(book.languageId)?.let { LanguageCodes.codeFor(it.name) }
        val courses = own.observeCourses(book.languageId).first() + code?.let { source.courses(it) }.orEmpty()
        return courses.firstNotNullOfOrNull { course ->
            course.lessons.firstOrNull { it.id == lessonId }?.let { LessonReading(course, it) }
        }
    }

    /**
     * The text to read for a lesson, made on first use. Null when the app does not have the
     * course's language.
     */
    suspend fun openLesson(courseId: String, lessonId: String): Long? {
        val course = own.course(courseId) ?: source.course(courseId) ?: return null
        val lesson = course.lessons.firstOrNull { it.id == lessonId } ?: return null
        books.lessonBookId(lessonId)?.let { return it }
        val language = course.languageId?.let { languages.getById(it) }
            ?: languages.getAll().firstOrNull { LanguageCodes.codeFor(it.name) == course.languageCode }
            ?: return null
        val bookId = bookService.create(BookDraft(languageId = language.id, title = lesson.title, text = lesson.text))
        books.linkLesson(lessonId, bookId)
        return bookId
    }

    // The reader's own courses; the app's cannot be changed.

    /** Makes a course for the language with [languageId]; returns its id. */
    suspend fun createCourse(languageId: Long, draft: CourseDraft): String {
        val clean = validated(draft)
        val id = newId("course")
        own.createCourse(id, languageId, clean)
        return id
    }

    suspend fun updateCourse(courseId: String, draft: CourseDraft) = own.updateCourse(ownCourse(courseId).id, validated(draft))

    /** Deletes the course, its lessons and the texts made from them. */
    suspend fun deleteCourse(courseId: String) {
        val course = ownCourse(courseId)
        for (lesson in course.lessons) books.lessonBookId(lesson.id)?.let { bookService.delete(it) }
        own.deleteCourse(course.id)
    }

    /** Adds a lesson at the end of the course; returns its id. */
    suspend fun addLesson(courseId: String, draft: LessonDraft): String {
        val course = ownCourse(courseId)
        val id = newId("lesson")
        own.addLesson(id, course.id, validated(draft))
        return id
    }

    /**
     * Saves the lesson. A lesson already opened has its text renamed too, and its pages cut again
     * when the words changed, keeping the place reached as far as the new pages allow.
     */
    suspend fun updateLesson(courseId: String, lessonId: String, draft: LessonDraft) {
        val before = ownCourse(courseId).lessons.firstOrNull { it.id == lessonId } ?: throw NoSuchElementException("No lesson $lessonId")
        val clean = validated(draft)
        own.updateLesson(lessonId, clean)
        val bookId = books.lessonBookId(lessonId) ?: return
        val book = books.getBook(bookId) ?: return
        val textChanged = clean.text != before.text
        bookService.update(
            BookDraft(
                id = bookId,
                languageId = book.languageId,
                title = clean.title,
                text = clean.text,
                sourceUri = book.sourceUri.orEmpty(),
                tags = book.tags,
                wordsPerPage = if (textChanged) bookService.estimatedWordsPerPage(bookId) else BookDraft.DEFAULT_WORDS_PER_PAGE,
                audioFilename = book.audioFilename,
            ),
            rebuildPages = textChanged,
        )
    }

    /** Deletes the lesson and the text made from it. */
    suspend fun deleteLesson(courseId: String, lessonId: String) {
        ownCourse(courseId)
        books.lessonBookId(lessonId)?.let { bookService.delete(it) }
        own.deleteLesson(lessonId)
    }

    suspend fun moveLesson(courseId: String, lessonId: String, by: Int) {
        ownCourse(courseId)
        own.moveLesson(lessonId, by)
    }

    private suspend fun ownCourse(courseId: String): Course =
        own.course(courseId) ?: throw CourseValidationException("Only your own courses can be changed")

    private fun validated(draft: CourseDraft): CourseDraft {
        if (draft.title.isBlank()) throw CourseValidationException("A course needs a title")
        return draft.copy(title = draft.title.trim(), description = draft.description.trim(), topic = draft.topic.trim())
    }

    private fun validated(draft: LessonDraft): LessonDraft {
        if (draft.title.isBlank()) throw CourseValidationException("A lesson needs a title")
        if (draft.text.isBlank()) throw CourseValidationException("A lesson needs a text to read")
        return draft.copy(title = draft.title.trim(), summary = draft.summary.trim(), text = draft.text.trim())
    }

    /** Ids apart from the app's courses' ("pt-primeiros-passos-1"), since lessons are found by id. */
    private fun newId(kind: String): String =
        "own-$kind-" + (1..12).map { ID_CHARS[Random.nextInt(ID_CHARS.length)] }.joinToString("")

    private companion object {
        const val ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
