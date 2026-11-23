package com.idiomiko.core.domain.frequency

import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseLevel

/** A level the reader can say they have reached and the most common words they know there; Starter, before A1, has no [level] and no words. */
data class LevelEdge(val level: CourseLevel?, val words: Int)

/**
 * Where the European levels end on a language's frequency list, so a reader who has reached one knows its
 * words. A level with courses built on the list ends at the rank of the rarest word they teach, as the
 * course journey places a reader; one without ends where [FALLBACK] has it.
 */
object LevelEdges {
    val FALLBACK: Map<CourseLevel, Int> = mapOf(
        CourseLevel.A1 to 300,
        CourseLevel.A2 to 1_000,
        CourseLevel.B1 to 2_000,
        CourseLevel.B2 to 4_000,
        CourseLevel.C1 to 8_000,
        CourseLevel.C2 to 10_000,
    )

    /**
     * Starter and the levels of a list of [wordCount] words, each ending at its last word at the latest;
     * a level that would add no words to the one before it is left out, so a 300-word list has Starter and A1.
     */
    fun of(courses: List<Course>, wordCount: Int): List<LevelEdge> {
        val banded = courses.filter { it.rankUpTo != null }.groupBy { it.level }.mapValues { (_, list) -> list.maxOf { it.rankUpTo!! } }
        val edges = mutableListOf(LevelEdge(null, 0))
        for (level in CourseLevel.entries) {
            val words = (banded[level] ?: FALLBACK.getValue(level)).coerceIn(0, wordCount.coerceAtLeast(0))
            if (words > edges.last().words) edges += LevelEdge(level, words)
        }
        return edges
    }

    /** The level a reader who knows [known] words has reached among [edges]: the last whose words they know, else the first. */
    fun reached(edges: List<LevelEdge>, known: Int): LevelEdge = edges.lastOrNull { it.words <= known } ?: edges.first()
}
