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
 * Courses and how far the reader has got in them. All courses are stored alike and can be
 * changed: the app's samples, written in once per language, and the ones the reader makes. A
 * lesson is read like any text: the first time it is opened a text is made from it, kept apart
 * from the reader's own books, and from then on the lesson's progress is that text's.
 */
class CourseService(
    private val books: BookRepository,
    private val languages: LanguageRepository,
    private val bookService: BookService,
    private val repository: CourseRepository,
    private val samples: SampleCourseSource,
) {
    /** The courses for the language with [languageId], oldest first, kept up to date. */
    fun observeCourses(languageId: Long): Flow<List<CourseProgress>> =
        combine(books.observeLessonBooks(), repository.observeCourses(languageId)) { opened, courses -> courses.map { progress(it, opened) } }

    fun observeCourse(courseId: String): Flow<CourseProgress?> =
        combine(books.observeLessonBooks(), repository.observeCourse(courseId)) { opened, course -> course?.let { progress(it, opened) } }

    /**
     * Writes each sample course for each language it is in, once: a sample the reader deleted stays
     * deleted, and one added in a later version is written then. Samples keep their ids, so the
     * place reached in a sample lesson carries over.
     */
    suspend fun seedSamples() {
        for (language in languages.getAll()) {
            val code = LanguageCodes.codeFor(language.name) ?: continue
            val seeded = repository.seededSamples(language.id)
            val new = samples.courses(code).filter { it.id !in seeded }
            if (new.isNotEmpty()) repository.seedSamples(language.id, new)
        }
    }

    private fun progress(course: Course, opened: Map<String, LessonBook>) =
        CourseProgress(course, course.lessons.map { LessonProgress(it, opened[it.id]) })

    /** The lesson that the text with [bookId] was made from, or null for one of the reader's own books. */
    suspend fun lessonReading(bookId: Long): LessonReading? {
        val lessonId = books.observeLessonBooks().first().values.firstOrNull { it.bookId == bookId }?.lessonId ?: return null
        val book = books.getBook(bookId) ?: return null
        return repository.observeCourses(book.languageId).first().firstNotNullOfOrNull { course ->
            course.lessons.firstOrNull { it.id == lessonId }?.let { LessonReading(course, it) }
        }
    }

    /**
     * The text to read for a lesson, made on first use. Null when the app does not have the
     * course's language.
     */
    suspend fun openLesson(courseId: String, lessonId: String): Long? {
        val course = repository.course(courseId) ?: return null
        val lesson = course.lessons.firstOrNull { it.id == lessonId } ?: return null
        books.lessonBookId(lessonId)?.let { return it }
        val language = languages.getById(course.languageId) ?: return null
        val bookId = bookService.create(BookDraft(languageId = language.id, title = lesson.title, text = lesson.text))
        books.linkLesson(lessonId, bookId)
        return bookId
    }

    // Changing courses.

    /** Makes a course for the language with [languageId]; returns its id. */
    suspend fun createCourse(languageId: Long, draft: CourseDraft): String {
        val clean = validated(draft)
        val id = newId("course")
        repository.createCourse(id, languageId, clean)
        return id
    }

    suspend fun updateCourse(courseId: String, draft: CourseDraft) = repository.updateCourse(stored(courseId).id, validated(draft))

    /** Deletes the course, its lessons and the texts made from them. */
    suspend fun deleteCourse(courseId: String) {
        val course = stored(courseId)
        for (lesson in course.lessons) books.lessonBookId(lesson.id)?.let { bookService.delete(it) }
        repository.deleteCourse(course.id)
    }

    /** Adds a lesson at the end of the course; returns its id. */
    suspend fun addLesson(courseId: String, draft: LessonDraft): String {
        val course = stored(courseId)
        val id = newId("lesson")
        repository.addLesson(id, course.id, validated(draft))
        return id
    }

    /**
     * Saves the lesson. A lesson already opened has its text renamed too, and its pages cut again
     * when the words changed, keeping the place reached as far as the new pages allow.
     */
    suspend fun updateLesson(courseId: String, lessonId: String, draft: LessonDraft) {
        val before = stored(courseId).lessons.firstOrNull { it.id == lessonId } ?: throw NoSuchElementException("No lesson $lessonId")
        val clean = validated(draft)
        repository.updateLesson(lessonId, clean)
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
        stored(courseId)
        books.lessonBookId(lessonId)?.let { bookService.delete(it) }
        repository.deleteLesson(lessonId)
    }

    suspend fun moveLesson(courseId: String, lessonId: String, by: Int) {
        stored(courseId)
        repository.moveLesson(lessonId, by)
    }

    private suspend fun stored(courseId: String): Course =
        repository.course(courseId) ?: throw CourseValidationException("This course no longer exists")

    private fun validated(draft: CourseDraft): CourseDraft {
        if (draft.title.isBlank()) throw CourseValidationException("A course needs a title")
        return draft.copy(title = draft.title.trim(), description = draft.description.trim(), topic = draft.topic.trim())
    }

    private fun validated(draft: LessonDraft): LessonDraft {
        if (draft.title.isBlank()) throw CourseValidationException("A lesson needs a title")
        if (draft.text.isBlank()) throw CourseValidationException("A lesson needs a text to read")
        return draft.copy(title = draft.title.trim(), summary = draft.summary.trim(), text = draft.text.trim())
    }

    /** Ids apart from the samples' ("pt-freq-0100-01"), since lessons are found by id. */
    private fun newId(kind: String): String =
        "own-$kind-" + (1..12).map { ID_CHARS[Random.nextInt(ID_CHARS.length)] }.joinToString("")

    private companion object {
        const val ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
