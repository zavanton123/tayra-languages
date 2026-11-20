package com.idiomiko.core.domain.home

import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.model.BookListItem
import kotlin.math.abs

/** Courses and books that suit the words the reader knows. */
object LevelSuggestions {
    /** Books further below this share of words known are too hard to suggest. */
    const val MIN_KNOWN_PERCENT = 80

    /** The share of words known that reads most comfortably while still teaching some. */
    const val COMFORT_PERCENT = 96

    /**
     * The course of the reader's level: of the courses built on the frequency list and not started, the
     * first whose words go past the [known] words (1,532 known suggests the course up to rank 1,600), or
     * the last of them when the reader knows more than any teaches.
     */
    fun course(courses: List<CourseProgress>, known: Int): CourseProgress? {
        val banded = courses.filter { it.course.rankUpTo != null && it.status == LessonStatus.NOT_STARTED && it.lessons.isNotEmpty() }
            .sortedBy { it.course.rankUpTo }
        return banded.firstOrNull { it.course.rankUpTo!! > known } ?: banded.lastOrNull()
    }

    /**
     * Up to [limit] books of the language with [languageId] not opened yet whose words the reader mostly
     * knows, those nearest [COMFORT_PERCENT] first. Only books whose word counts were already worked out
     * count: working them out means reading every page.
     */
    fun books(books: List<BookListItem>, languageId: Long, limit: Int = 2): List<BookListItem> =
        books.filter { it.languageId == languageId && !it.isArchived && !it.isCompleted && it.lastOpened == null }
            .mapNotNull { book -> book.knownPercent?.takeIf { it >= MIN_KNOWN_PERCENT }?.let { book to it } }
            .sortedWith(compareBy<Pair<BookListItem, Int>> { abs(it.second - COMFORT_PERCENT) }.thenByDescending { it.second })
            .take(limit)
            .map { it.first }
}

/** The share of the book's distinct words no longer new to the reader, from 0 to 100; null before it was counted. */
val BookListItem.knownPercent: Int?
    get() = stats?.takeIf { it.distinctTerms > 0 }?.let { 100 - it.unknownPercent }
