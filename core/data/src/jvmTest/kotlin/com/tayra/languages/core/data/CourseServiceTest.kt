package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.domain.courses.SampleCourses
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.LessonStatus
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.TermService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Courses list their lessons, a lesson becomes a text when first opened, and reading it is the lesson's progress. */
class CourseServiceTest {
    private val file = File.createTempFile("tayra-courses", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val terms = TermRepositoryImpl(provider)
    private val bookService = BookService(books, languages)
    private val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))
    private val service = CourseService(books, languages, bookService, CourseRepositoryImpl(provider))

    @Test
    fun theSampleCoursesAreWellFormed() {
        val courses = SampleCourses.ALL
        assertEquals(listOf(CourseLevel.A1, CourseLevel.A2, CourseLevel.B1), courses.map { it.level })
        assertEquals(courses.size, courses.map { it.id }.toSet().size)
        val lessons = courses.flatMap { it.lessons }
        assertEquals(lessons.size, lessons.map { it.id }.toSet().size, "lesson ids are unique across courses")
        lessons.forEach { lesson ->
            assertTrue(lesson.wordCount in 60..220, "${lesson.id} has ${lesson.wordCount} words")
            assertTrue(lesson.title.isNotBlank() && lesson.summary.isNotBlank())
            assertTrue(lesson.text.lines().none { it.startsWith(" ") }, "${lesson.id} is not indented")
        }
    }

    @Test
    fun coursesAreListedForTheirLanguageOnly() = runTest {
        val portuguese = languages.save(Language(name = "Portuguese"))
        service.seedSamples()
        val spanish = languages.save(Language(name = "Spanish"))
        assertEquals(listOf("Primeiros passos", "A vida na cidade", "Histórias curtas"), service.observeCourses(portuguese).first().map { it.course.title })
        assertEquals(emptyList(), service.observeCourses(spanish).first())
    }

    @Test
    fun aLessonBecomesATextOnceAndItsReadingIsItsProgress() = runTest {
        val portuguese = languages.save(Language(name = "Portuguese"))
        service.seedSamples()
        val own = bookService.create(BookDraft(languageId = portuguese, title = "My own book", text = "O lobo dorme."))
        val course = service.observeCourses(portuguese).first().first()
        val lesson = course.course.lessons.first()
        assertEquals(LessonStatus.NOT_STARTED, course.status)

        val bookId = assertNotNull(service.openLesson(course.course.id, lesson.id))
        assertEquals(bookId, service.openLesson(course.course.id, lesson.id), "opening again finds the same text")
        val book = books.getBook(bookId)!!
        assertEquals(lesson.title, book.title)
        assertEquals(lesson.text, books.getPages(bookId).joinToString("\n") { it.text })
        // Lessons are kept out of the reader's own books.
        assertEquals(listOf(own), books.observeBooks(archived = false).first().map { it.id })

        suspend fun first() = service.observeCourse(course.course.id).first()!!
        assertEquals(LessonStatus.NOT_STARTED, first().lessons[0].status, "made but not read yet")

        reading.openPage(bookId, 1, trackOpen = true)
        assertEquals(LessonStatus.IN_PROGRESS, first().lessons[0].status)
        assertEquals(LessonStatus.IN_PROGRESS, first().status)
        assertEquals(lesson.id, first().nextLesson?.lesson?.id)

        reading.markPageRead(bookId, books.pageCount(bookId), markRestAsKnown = false)
        val read = first()
        assertEquals(LessonStatus.COMPLETED, read.lessons[0].status)
        assertEquals(1, read.completed)
        assertEquals(course.course.lessons[1].id, read.nextLesson?.lesson?.id, "the next lesson to read")

        // A deleted lesson text makes the lesson new again.
        bookService.delete(bookId)
        assertEquals(LessonStatus.NOT_STARTED, first().lessons[0].status)
        assertNull(books.lessonBookId(lesson.id))
    }

    @Test
    fun aLessonOfALanguageTheAppLacksIsNotOpened() = runTest {
        languages.save(Language(name = "Spanish"))
        assertNull(service.openLesson("pt-primeiros-passos", "pt-primeiros-passos-1"))
        assertNull(service.openLesson("pt-primeiros-passos", "no-such-lesson"))
    }

    @Test
    fun aDatabaseFromBeforeCoursesGainsThem() = runTest {
        val portuguese = languages.save(Language(name = "Portuguese"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use {
            it.createStatement().execute("DROP TABLE course_lessons")
            it.createStatement().execute("PRAGMA user_version = 10")
        }
        val reopened = DatabaseProvider(DatabaseDriverFactory(file))
        val upgradedBooks = BookRepositoryImpl(reopened)
        val upgradedLanguages = LanguageRepositoryImpl(reopened)
        val upgraded = CourseService(upgradedBooks, upgradedLanguages, BookService(upgradedBooks, upgradedLanguages), CourseRepositoryImpl(reopened))
        upgraded.seedSamples()
        assertNotNull(upgraded.openLesson("pt-primeiros-passos", "pt-primeiros-passos-1"))
        assertEquals(3, upgraded.observeCourses(portuguese).first().size)
    }

    @Test
    fun aLessonsTextKnowsItsCourse() = runTest {
        languages.save(Language(name = "Portuguese"))
        service.seedSamples()
        val course = SampleCourses.ALL.first()
        val bookId = assertNotNull(service.openLesson(course.id, course.lessons[2].id))
        val reading = assertNotNull(service.lessonReading(bookId))
        assertEquals(course.id, reading.course.id)
        assertEquals(course.lessons[2].id, reading.lesson.id)
        assertEquals(3, reading.number)

        val own = bookService.create(BookDraft(languageId = languages.getAll().first().id, title = "Mine", text = "Um texto."))
        assertNull(service.lessonReading(own), "the reader's own book belongs to no course")
    }
}
