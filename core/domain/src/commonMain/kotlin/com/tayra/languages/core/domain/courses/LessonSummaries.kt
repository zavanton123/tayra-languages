package com.tayra.languages.core.domain.courses

import com.tayra.languages.core.domain.repository.FlashcardRepository
import com.tayra.languages.core.domain.practice.PracticeProgressRepository
import com.tayra.languages.core.domain.repository.TermRepository

/** What a finished lesson came to, shown when its last stage is checked. */
data class LessonSummary(
    /** The lesson's words now being learned. */
    val learning: Int,
    /** Of those, the ones with a flashcard to review. */
    val flashcards: Int,
    /** The quiz answers given on the lesson's words being learned, and how many were right. */
    val quizRight: Int,
    val quizAsked: Int,
    /** The lesson's distinct words, and how many of them are known. */
    val known: Int,
    val words: Int,
    /** The lesson after this one in its course; null for the last. */
    val next: Lesson?,
)

class LessonSummaries(
    private val terms: TermRepository,
    private val flashcards: FlashcardRepository,
    private val practice: PracticeProgressRepository,
) {
    /** The summary of [lesson], whose page holds the words with [termIds], [known] of its [words] distinct ones known. */
    suspend fun summary(lesson: LessonReading, termIds: Collection<Long>, known: Int, words: Int): LessonSummary {
        val learning = terms.getByIds(termIds.distinct()).filter { it.status.isLearning }.map { it.id }
        val cards = learning.count { id -> flashcards.get(id)?.suspended == false }
        val progress = practice.progress(learning).values
        val lessons = lesson.course.lessons
        val next = lessons.getOrNull(lessons.indexOfFirst { it.id == lesson.lesson.id } + 1)
        return LessonSummary(
            learning = learning.size,
            flashcards = cards,
            quizRight = progress.sumOf { it.right },
            quizAsked = progress.sumOf { it.asked },
            known = known,
            words = words,
            next = next,
        )
    }
}
