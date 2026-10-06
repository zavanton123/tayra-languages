package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.UserCourseRepositoryImpl
import com.tayra.languages.core.domain.courses.BuiltInCourses
import com.tayra.languages.core.domain.courses.CourseDraft
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.CourseValidationException
import com.tayra.languages.core.domain.courses.LessonDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.service.BookService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The reader's own courses: made, filled with lessons, reordered, changed and deleted, alongside the app's. */
class OwnCoursesTest {
    private val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-own-courses", ".db").also { it.delete() }))
    private val languages = LanguageRepositoryImpl(provider)
    private val books = BookRepositoryImpl(provider)
    private val bookService = BookService(books, languages)
    private val service = CourseService(BuiltInCourses(), books, languages, bookService, UserCourseRepositoryImpl(provider))

    @Test
    fun anOwnCourseIsListedAfterTheAppsAndKeepsItsLessonsInOrder() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        val id = service.createCourse(pt, CourseDraft("  Minhas leituras ", "Textos que eu escolhi.", CourseLevel.B1, "Notícias"))
        val first = service.addLesson(id, LessonDraft("Primeira", "A first one", "Era uma vez um gato."))
        val second = service.addLesson(id, LessonDraft("Segunda", text = "O gato dormia."))
        val third = service.addLesson(id, LessonDraft("Terceira", text = "E acordou."))

        val courses = service.observeCourses(pt).first()
        assertEquals(listOf("Primeiros passos", "A vida na cidade", "Histórias curtas", "Minhas leituras"), courses.map { it.course.title })
        val mine = courses.last().course
        assertTrue(mine.isOwn && !courses.first().course.isOwn)
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
        assertEquals(3, service.observeCourses(pt).first().size, "the app's courses stay")
    }

    @Test
    fun theAppsCoursesCannotBeChangedAndDraftsNeedTheirParts() = runTest {
        val pt = languages.save(Language(name = "Portuguese"))
        assertFailsWith<CourseValidationException> { service.updateCourse("pt-primeiros-passos", CourseDraft("Mine now")) }
        assertFailsWith<CourseValidationException> { service.addLesson("pt-primeiros-passos", LessonDraft("Extra", text = "Texto.")) }
        assertFailsWith<CourseValidationException> { service.createCourse(pt, CourseDraft("  ")) }
        val id = service.createCourse(pt, CourseDraft("Minhas leituras"))
        assertFailsWith<CourseValidationException> { service.addLesson(id, LessonDraft("Vazia", text = " ")) }
        service.updateCourse(id, CourseDraft("Leituras", level = CourseLevel.C1, topic = "Literatura"))
        val course = service.observeCourse(id).first()!!.course
        assertEquals("Leituras", course.title)
        assertEquals(CourseLevel.C1, course.level)
    }
}
