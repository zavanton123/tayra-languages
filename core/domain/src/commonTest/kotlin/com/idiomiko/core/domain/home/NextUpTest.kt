package com.idiomiko.core.domain.home

import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.Lesson
import com.idiomiko.core.domain.courses.LessonBook
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.domain.model.BookStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NextUpTest {
    private val opened = Instant.fromEpochMilliseconds(1_700_000_000_000)
    private val text = List(443) { "palavra" }.joinToString(" ")

    private fun course(id: String, rankUpTo: Int?, done: Int = 0, opened: Int = done) = Course(
        id = id, languageCode = "pt", title = id, description = "", level = CourseLevel.A1, topic = "", rankUpTo = rankUpTo,
        lessons = (1..10).map { Lesson(id = "$id-$it", title = "Lição $it", summary = "", text = text, newWords = List(9) { n -> "w$n" }) },
    ).let { c ->
        CourseProgress(c, c.lessons.mapIndexed { i, lesson ->
            LessonProgress(lesson, if (i < opened) LessonBook(lesson.id, 100L + i, 1, 1, this.opened, isCompleted = i < done) else null)
        })
    }

    private val underWay = course("pt-mini-0300", 300, done = 2, opened = 3)
    private val notStarted = course("pt-mini-1600", 1600)
    private val book = BookListItem(
        id = 7, title = "O Alquimista", languageId = 1, languageName = "Portuguese", tags = emptyList(), currentPage = 12, pageCount = 48,
        wordCount = 14_400, lastOpened = opened, isCompleted = false, isArchived = false, stats = null, sourceUri = null,
    )

    @Test
    fun manyCardsDueComeFirst() {
        val next = NextUp.choose(due = 310, course = underWay, book = book, toStart = notStarted, averageAnswer = 5.seconds)
        assertEquals(NextUp.Review(310, 26), next)
    }

    @Test
    fun aLessonUnderWayComesBeforeTheBookAndAFewCards() {
        val next = assertIs<NextUp.Lesson>(NextUp.choose(due = 5, course = underWay, book = book, toStart = notStarted))
        assertEquals(3, next.number)
        assertEquals(8, next.minutes)
        assertEquals(9, next.newWords)
        assertEquals(true, next.started)
    }

    @Test
    fun theBookBeingReadComesNextAtItsPage() {
        val next = assertIs<NextUp.Book>(NextUp.choose(due = 5, course = notStarted, book = book, toStart = null))
        // A page of 300 words takes five minutes.
        assertEquals(5, next.minutes)
        assertEquals(NextUp.Book(book, 5), next)
    }

    @Test
    fun aFewCardsComeBeforeStartingSomethingNew() {
        assertEquals(NextUp.Review(5, 1), NextUp.choose(due = 5, course = null, book = book.copy(isCompleted = true), toStart = notStarted))
    }

    @Test
    fun withNothingUnderWayTheCourseOfTheReadersLevelStarts() {
        val next = assertIs<NextUp.Lesson>(NextUp.choose(due = 0, course = course("pt-mini-0100", 100), book = null, toStart = notStarted))
        assertEquals(notStarted, next.course)
        assertEquals(1, next.number)
        assertEquals(false, next.started)
        // Without a course of the level, the course to go on with does.
        assertEquals("pt-mini-0100", assertIs<NextUp.Lesson>(NextUp.choose(0, course("pt-mini-0100", 100), null, null)).course.course.id)
    }

    @Test
    fun withNothingAtAllTheCoursesAreOffered() {
        assertEquals(NextUp.StartHere, NextUp.choose(due = 0, course = null, book = null, toStart = null))
        // A finished course is nothing to go on with.
        assertEquals(NextUp.StartHere, NextUp.choose(due = 0, course = course("pt-mini-0100", 100, done = 10), book = null, toStart = null))
    }

    @Test
    fun theCourseOfTheLevelGoesPastTheWordsKnown() {
        val courses = listOf(course("pt-mini-1500", 1500), notStarted, course("pt-mini-1700", 1700), course("own", null), underWay)
        assertEquals("pt-mini-1600", LevelSuggestions.course(courses, known = 1532)?.course?.id)
        assertEquals("pt-mini-1700", LevelSuggestions.course(courses, known = 1600)?.course?.id)
        // Knowing more than any course teaches, the last one is still offered; one under way is not.
        assertEquals("pt-mini-1700", LevelSuggestions.course(courses, known = 9000)?.course?.id)
        assertEquals(null, LevelSuggestions.course(listOf(underWay, course("own", null)), known = 100))
    }

    @Test
    fun theBooksSuggestedAreUnopenedAndMostlyKnown() {
        fun unopened(id: Long, unknownPercent: Int?) = book.copy(
            id = id, lastOpened = null,
            stats = unknownPercent?.let { BookStats(distinctTerms = 100, distinctUnknowns = it, unknownPercent = it, statusDistribution = emptyMap()) },
        )
        val books = listOf(unopened(1, 40), unopened(2, 6), unopened(3, 1), unopened(4, null), unopened(5, 4), book.copy(id = 6), unopened(7, 4).copy(languageId = 2))
        assertEquals(listOf(5L, 2L), LevelSuggestions.books(books, languageId = 1).map { it.id })
        assertEquals(96, unopened(5, 4).knownPercent)
        assertEquals(null, unopened(4, null).knownPercent)
    }
}
