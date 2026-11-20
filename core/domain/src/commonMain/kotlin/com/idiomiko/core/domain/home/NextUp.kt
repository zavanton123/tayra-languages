package com.idiomiko.core.domain.home

import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.domain.courses.LessonStatus
import com.idiomiko.core.domain.model.BookListItem
import com.idiomiko.core.domain.stats.StudyTime
import kotlin.time.Duration

/** The one thing Home offers to do first, with the minutes it will take. */
sealed interface NextUp {
    val minutes: Int?

    /** The flashcards due. */
    data class Review(val due: Int, override val minutes: Int) : NextUp

    /**
     * A lesson: the next of the course under way, or the first of a course to start when [started] is
     * false. [newWords] are the words of the frequency list it brings in.
     */
    data class Lesson(val course: CourseProgress, val lesson: LessonProgress, override val minutes: Int, val newWords: Int, val started: Boolean) : NextUp {
        /** The lesson's place in its course, from 1. */
        val number: Int get() = course.lessons.indexOf(lesson) + 1
    }

    /** The book being read, at its current page; [minutes] are the page's. */
    data class Book(val book: BookListItem, override val minutes: Int) : NextUp

    /** Nothing under way and no course to start: the courses to choose from. */
    data object StartHere : NextUp {
        override val minutes: Int? = null
    }

    companion object {
        /** So many cards due that they come before the lesson and the book under way. */
        const val MANY_DUE = 20

        /** The cards of a quick session, when more than that are due. */
        const val QUICK_SESSION = 20

        /**
         * The best next step: the cards when [MANY_DUE] or more are due, else the next lesson of the [course]
         * under way, else the [book] being read, else the cards still due, else the first lesson of [toStart]
         * (or of [course] when it is not started), else [StartHere]. [averageAnswer] times the cards.
         */
        fun choose(due: Int, course: CourseProgress?, book: BookListItem?, toStart: CourseProgress?, averageAnswer: Duration? = null): NextUp {
            val reviews = Review(due, StudyTime.reviewMinutes(due, averageAnswer))
            val underWay = course?.takeIf { it.status == LessonStatus.IN_PROGRESS }
            val reading = book?.takeIf { !it.isCompleted }
            return when {
                due >= MANY_DUE -> reviews
                underWay?.nextLesson != null -> lesson(underWay, underWay.nextLesson!!, started = true)
                reading != null -> Book(reading, StudyTime.readingMinutes(pageWords(reading)))
                due > 0 -> reviews
                else -> (toStart ?: course?.takeIf { it.status == LessonStatus.NOT_STARTED })
                    ?.let { start -> start.lessons.firstOrNull()?.let { lesson(start, it, started = false) } }
                    ?: StartHere
            }
        }

        private fun lesson(course: CourseProgress, lesson: LessonProgress, started: Boolean) =
            Lesson(course, lesson, StudyTime.readingMinutes(lesson.lesson.wordCount), lesson.lesson.newWords.size, started)

        /** The words of a page of [book], its pages taken as equal. */
        fun pageWords(book: BookListItem): Int = if (book.pageCount <= 1) book.wordCount else (book.wordCount + book.pageCount - 1) / book.pageCount
    }
}
