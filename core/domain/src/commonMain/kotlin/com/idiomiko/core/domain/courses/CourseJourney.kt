package com.idiomiko.core.domain.courses

import kotlin.time.Instant

/** How far the reader has got through one level's courses. */
data class LevelProgress(
    val level: CourseLevel,
    val courses: Int,
    val completed: Int,
    val lessons: Int,
    val lessonsRead: Int,
    /**
     * The rank of the rarest word the level's courses teach, where the level ends on the frequency list
     * (B1's courses going up to rank 2,000 end it there); null for a level without such courses.
     */
    val wordsUpTo: Int?,
) {
    /** Every course of the level completed. */
    val done: Boolean get() = courses > 0 && completed == courses

    /** From 0 to 1: the share of the level's lessons read. */
    val share: Float get() = if (lessons == 0) 0f else lessonsRead.toFloat() / lessons
}

/**
 * The levels of a language's courses as a journey: each level's progress, the level the reader is at, and
 * how many more words lead to the next one. The levels' edges are the frequency bands their courses teach,
 * so [known] words place the reader: 1,540 known is B1 where B1's courses go up to rank 2,000, with 460 to B2.
 */
data class CourseJourney(val levels: List<LevelProgress>, val current: CourseLevel?, val known: Int?) {
    val currentProgress: LevelProgress? get() = levels.firstOrNull { it.level == current }

    /** The level after the current one, if the courses have one. */
    val next: LevelProgress? get() = levels.getOrNull(levels.indexOfFirst { it.level == current } + 1)?.takeIf { current != null }

    /** The words still to learn before the next level, or null when there is none or the words known are not counted. */
    val wordsToNext: Int?
        get() {
            if (next == null) return null
            val edge = currentProgress?.wordsUpTo ?: return null
            return known?.let { edge - it }?.takeIf { it > 0 }
        }

    /** The levels left behind with every course completed, the current one apart. */
    val finished: List<LevelProgress> get() = levels.filter { it.done && it.level != current }

    companion object {
        /** The journey through [courses] for a reader who knows [known] words, or whose words are not counted (null). */
        fun of(courses: List<CourseProgress>, known: Int?): CourseJourney {
            val levels = courses.groupBy { it.course.level }.entries.sortedBy { it.key }.map { (level, list) ->
                LevelProgress(
                    level = level,
                    courses = list.size,
                    completed = list.count { it.status == LessonStatus.COMPLETED },
                    lessons = list.sumOf { it.lessons.size },
                    lessonsRead = list.sumOf { it.completed },
                    wordsUpTo = list.mapNotNull { it.course.rankUpTo }.maxOrNull(),
                )
            }
            return CourseJourney(levels, currentLevel(levels, courses, known), known)
        }

        /**
         * The reader's level: by the words known, the first level whose courses go past them, or the last such
         * level when they know more. Without a count or courses on the frequency list, the level of the course
         * read last, else the first level not finished, else the last level.
         */
        private fun currentLevel(levels: List<LevelProgress>, courses: List<CourseProgress>, known: Int?): CourseLevel? {
            val banded = levels.filter { it.wordsUpTo != null }
            if (known != null && banded.isNotEmpty()) return (banded.firstOrNull { it.wordsUpTo!! > known } ?: banded.last()).level
            val reading = courses.filter { it.status == LessonStatus.IN_PROGRESS }
                .maxByOrNull { progress -> progress.lessons.mapNotNull { it.book?.lastOpened }.maxOrNull() ?: Instant.DISTANT_PAST }
            return reading?.course?.level ?: levels.firstOrNull { !it.done }?.level ?: levels.lastOrNull()?.level
        }
    }
}
