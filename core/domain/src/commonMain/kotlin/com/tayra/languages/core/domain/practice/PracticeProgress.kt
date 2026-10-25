package com.tayra.languages.core.domain.practice

import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * How far practice has taken a word: its [level] on the way from recognising it to producing it,
 * the run of right (positive) or wrong (negative) answers that moves the level, and its tally.
 */
data class WordProgress(
    val termId: Long,
    val level: Int = 0,
    val streak: Int = 0,
    val asked: Int = 0,
    val right: Int = 0,
    val lastPractised: Instant? = null,
) {
    /**
     * The progress after an answer: right ones run the streak up, wrong ones down, and a long
     * enough run moves the level. A slip of a letter counts as right but moves no level up.
     */
    fun after(answer: Answer, at: Instant): WordProgress {
        val right = answer != Answer.WRONG
        val streak = if (right) (if (streak > 0) streak + 1 else 1) else (if (streak < 0) streak - 1 else -1)
        val rule = LEVELS[level]
        val (level, kept) = when {
            right && answer != Answer.SLIP && rule.upAfter != null && streak >= rule.upAfter -> level + 1 to 0
            !right && rule.downAfter != null && -streak >= rule.downAfter -> level - 1 to 0
            else -> level to streak
        }
        return copy(level = level, streak = kept, asked = asked + 1, right = this.right + if (right) 1 else 0, lastPractised = at)
    }

    /** How a question on the word was answered, for its progress. */
    enum class Answer { RIGHT, SLIP, WRONG }

    /** A level's ways of answering, and the runs that move a word up from it or down. */
    class Level(val responses: Set<PracticeResponse>, val upAfter: Int?, val downAfter: Int?)

    companion object {
        /** Recognise (choices), recognise or recall (choices and typing), recall (typing), produce (typing or saying). */
        val LEVELS = listOf(
            Level(setOf(PracticeResponse.CHOOSE), upAfter = 2, downAfter = null),
            Level(setOf(PracticeResponse.CHOOSE, PracticeResponse.TYPE), upAfter = 2, downAfter = 2),
            Level(setOf(PracticeResponse.TYPE), upAfter = 3, downAfter = 1),
            Level(setOf(PracticeResponse.TYPE, PracticeResponse.SPEAK), upAfter = null, downAfter = 1),
        )
        val TOP = LEVELS.lastIndex
    }
}

/** How far practice has taken each word. */
interface PracticeProgressRepository {
    /** The progress of each of [termIds]; words never practised are left out. */
    suspend fun progress(termIds: Collection<Long>): Map<Long, WordProgress>

    suspend fun save(progress: WordProgress)

    /** Records an answer on the word: its progress moves as [WordProgress.after] says. */
    suspend fun record(termId: Long, answer: WordProgress.Answer, at: Instant) {
        save((progress(listOf(termId))[termId] ?: WordProgress(termId)).after(answer, at))
    }
}
