package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.SampleCourseSource
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.CourseValidationException
import com.tayra.languages.core.domain.courses.LessonDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The reader's own courses: made, filled with lessons, reordered, changed and deleted, alongside the app's. */
class OwnCoursesTest {
    private val file = File.createTempFile("tayra-own-courses", ".db").also { it.delete() }
    private val provider = DatabaseProvider(DatabaseDriverFactory(file))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val bookService = BookService(books, languages)
    private val repository = CourseRepositoryImpl(provider)
    private val service = CourseService(books, languages, bookService, repository, TestCourses.source)

    @Test
    fun anOwnCourseIsListedAfterTheSamplesAndKeepsItsLessonsInOrder() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        service.seedSamples()
        val id = service.createCourse(pt, CourseDraft("  Minhas leituras ", "Textos que eu escolhi.", CourseLevel.B1, "Notícias"))
        val first = service.addLesson(id, LessonDraft("Primeira", "A first one", "Era uma vez um gato."))
        val second = service.addLesson(id, LessonDraft("Segunda", text = "O gato dormia."))
        val third = service.addLesson(id, LessonDraft("Terceira", text = "E acordou."))

        val courses = service.observeCourses(pt).first()
        assertEquals(listOf("Primeiros passos", "A vida na cidade", "Histórias curtas", "Minhas leituras"), courses.map { it.course.title })
        val mine = courses.last().course
        assertTrue(!mine.builtIn && courses.first().course.builtIn, "the flag tells the samples from the reader's own")
        assertEquals(CourseLevel.B1, mine.level)
        assertEquals(listOf(first, second, third), mine.lessons.map { it.id })

        service.moveLesson(id, third, -2)
        service.moveLesson(id, first, 5)
        assertEquals(listOf("Terceira", "Segunda", "Primeira"), service.observeCourse(id).first()!!.course.lessons.map { it.title })
    }

    @Test
    fun aCourseOfALanguageWithoutCodeWorksToo() = runTest {
        val klingon = languages.save(Language(name = "Klingon"))
        val id = service.createCourse(klingon, CourseDraft("tlhIngan Hol"))
        val lesson = service.addLesson(id, LessonDraft("nuqneH", text = "nuqneH! tlhIngan Hol Dajatlh'a'?"))
        assertEquals(listOf("tlhIngan Hol"), service.observeCourses(klingon).first().map { it.course.title })
        val bookId = assertNotNull(service.openLesson(id, lesson))
        assertEquals(klingon, books.getBook(bookId)!!.languageId)
        assertEquals("nuqneH", service.lessonReading(bookId)?.lesson?.title)
    }

    @Test
    fun changingAnOpenedLessonUpdatesItsTextAndDeletingTakesTheTextAway() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        service.seedSamples()
        val id = service.createCourse(pt, CourseDraft("Minhas leituras"))
        val lesson = service.addLesson(id, LessonDraft("Gato", text = "Era uma vez um gato."))
        val bookId = assertNotNull(service.openLesson(id, lesson))

        service.updateLesson(id, lesson, LessonDraft("O gato", "Sobre um gato", "Era uma vez um gato preto. Ele dormia."))
        assertEquals("O gato", books.getBook(bookId)!!.title)
        assertEquals("Era uma vez um gato preto. Ele dormia.", bookService.text(bookId).trim())
        assertEquals(bookId, service.openLesson(id, lesson), "the lesson keeps its text and progress")

        val other = service.addLesson(id, LessonDraft("Cão", text = "O cão latia."))
        val otherBook = assertNotNull(service.openLesson(id, other))
        service.deleteLesson(id, lesson)
        assertNull(books.getBook(bookId))
        assertEquals(listOf("Cão"), service.observeCourse(id).first()!!.course.lessons.map { it.title })

        service.deleteCourse(id)
        assertNull(books.getBook(otherBook))
        assertNull(service.observeCourse(id).first())
        assertEquals(3, service.observeCourses(pt).first().size, "the samples stay")
    }

    @Test
    fun theSamplesAreWrittenOnceAndCanBeChangedLikeAnyCourse() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val de = languages.save(Language(name = "German"))
        service.seedSamples()
        service.seedSamples()
        assertEquals(listOf("Primeiros passos", "A vida na cidade", "Histórias curtas"), service.observeCourses(pt).first().map { it.course.title }, "once, in their order")
        assertEquals(emptyList(), service.observeCourses(de).first(), "German has no samples")

        service.updateCourse("pt-test-a1", CourseDraft("Os meus primeiros passos", level = CourseLevel.A1))
        service.addLesson("pt-test-a1", LessonDraft("Extra", text = "Mais um texto."))
        val changed = service.observeCourse("pt-test-a1").first()!!.course
        assertEquals("Os meus primeiros passos", changed.title)
        assertEquals(6, changed.lessons.size)
        assertTrue(changed.builtIn, "still marked as a sample")

        service.deleteCourse("pt-test-b1")
        service.seedSamples()
        assertEquals(listOf("pt-test-a1", "pt-test-a2"), service.observeCourses(pt).first().map { it.course.id }, "a deleted sample stays deleted")
    }

    @Test
    fun samplesAddedLaterReachALanguageSeededBefore() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        fun course(id: String, rank: Int?) = Course(id, "pt", id, "", CourseLevel.A1, "", listOf(Lesson("$id-01", "Um", "", "Texto um.", listOf("um"))), rankUpTo = rank)
        var bundled = listOf(course("pt-old", null))
        val seeding = CourseService(books, languages, bookService, repository, SampleCourseSource { bundled })
        seeding.seedSamples()
        seeding.deleteCourse("pt-old")
        bundled = listOf(course("pt-old", null), course("pt-freq-0200", 200), course("pt-freq-0100", 100))
        seeding.seedSamples()
        val courses = seeding.observeCourses(pt).first().map { it.course }
        assertEquals(listOf("pt-freq-0100", "pt-freq-0200"), courses.map { it.id }, "new samples arrive, ordered by rank; the deleted one stays deleted")
        assertEquals(100, courses.first().rankUpTo)
        assertEquals(listOf("um"), courses.first().lessons.single().newWords)
    }

    @Test
    fun draftsNeedTheirParts() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        assertFailsWith<CourseValidationException> { service.updateCourse("no-such-course", CourseDraft("Mine now")) }
        assertFailsWith<CourseValidationException> { service.createCourse(pt, CourseDraft("  ")) }
        val id = service.createCourse(pt, CourseDraft("Minhas leituras"))
        assertFailsWith<CourseValidationException> { service.addLesson(id, LessonDraft("Vazia", text = " ")) }
        service.updateCourse(id, CourseDraft("Leituras", level = CourseLevel.C1, topic = "Literatura"))
        val course = service.observeCourse(id).first()!!.course
        assertEquals("Leituras", course.title)
        assertEquals(CourseLevel.C1, course.level)
    }

    @Test
    fun coursesMadeBeforeTheTablesWereSharedAreMovedOver() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val id = service.createCourse(pt, CourseDraft("Minhas leituras"))
        service.addLesson(id, LessonDraft("Gato", text = "Era uma vez um gato."))
        // Back to version 13, when the reader's courses had tables of their own.
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { db ->
            val st = db.createStatement()
            st.execute("CREATE TABLE user_courses (id TEXT NOT NULL PRIMARY KEY, language_id INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', level TEXT NOT NULL, topic TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)")
            st.execute("CREATE TABLE user_lessons (id TEXT NOT NULL PRIMARY KEY, course_id TEXT NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL DEFAULT '', text TEXT NOT NULL, position INTEGER NOT NULL)")
            st.execute("INSERT INTO user_courses SELECT id, language_id, title, description, level, topic, created_at FROM courses")
            st.execute("INSERT INTO user_lessons SELECT id, course_id, title, summary, text, position FROM lessons")
            for (table in listOf("lessons", "courses", "seeded_courses")) st.execute("DROP TABLE $table")
            st.execute("PRAGMA user_version = 13")
        }
        val reopened = DatabaseProvider(DatabaseDriverFactory(file))
        val upgraded = CourseService(BookRepositoryImpl(reopened), LanguageRepositoryImpl(reopened), BookService(BookRepositoryImpl(reopened), LanguageRepositoryImpl(reopened)), CourseRepositoryImpl(reopened), TestCourses.source)
        val course = upgraded.observeCourse(id).first()!!.course
        assertEquals(listOf("Gato"), course.lessons.map { it.title })
        assertTrue(!course.builtIn)
    }

    @Test
    fun theFirstHandWrittenSamplesAreRemovedWithTheirTexts() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        fun course(id: String, rank: Int?) = Course(id, "pt", id, "", CourseLevel.A1, "", listOf(Lesson("$id-1", "Um", "", "Texto um.")), rankUpTo = rank)
        val retired = listOf("pt-primeiros-passos", "pt-vida-na-cidade", "pt-historias-curtas")
        val seeding = CourseService(books, languages, bookService, repository, SampleCourseSource { retired.map { course(it, null) } + course("pt-freq-0100", 100) })
        seeding.seedSamples()
        val opened = assertNotNull(seeding.openLesson("pt-primeiros-passos", "pt-primeiros-passos-1"))
        val mine = seeding.createCourse(pt, CourseDraft("Minhas leituras"))
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { it.createStatement().execute("PRAGMA user_version = 15") }

        val reopened = DatabaseProvider(DatabaseDriverFactory(file))
        val upgradedBooks = BookRepositoryImpl(reopened)
        val upgraded = CourseService(upgradedBooks, LanguageRepositoryImpl(reopened), BookService(upgradedBooks, LanguageRepositoryImpl(reopened)), CourseRepositoryImpl(reopened), SampleCourseSource { emptyList() })
        assertEquals(listOf("pt-freq-0100", mine), upgraded.observeCourses(pt).first().map { it.course.id })
        assertNull(upgradedBooks.getBook(opened), "the text read from a retired lesson goes too")
        assertNull(upgradedBooks.lessonBookId("pt-primeiros-passos-1"))
    }
}
