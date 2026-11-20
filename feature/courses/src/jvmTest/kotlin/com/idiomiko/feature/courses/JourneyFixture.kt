package com.idiomiko.feature.courses

import com.idiomiko.core.domain.courses.Course
import com.idiomiko.core.domain.courses.CourseLevel
import com.idiomiko.core.domain.courses.CourseProgress
import com.idiomiko.core.domain.courses.CourseWords
import com.idiomiko.core.domain.courses.Lesson
import com.idiomiko.core.domain.courses.LessonBook
import com.idiomiko.core.domain.courses.LessonProgress
import com.idiomiko.core.domain.frequency.VocabularyProgress
import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.TermStatus
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.time.Instant

/**
 * A Portuguese reader a few months in, as the mockup has them: 1,540 words known, A1 and A2 read to the end, three
 * lessons of "Lugares do Brasil" read and the fourth begun with a recording, and a course of their own. The shares
 * known are worked out from the real lessons with every word up to rank 1,540 known.
 */
object JourneyFixture {
    private val at = Instant.fromEpochMilliseconds(1_763_600_000_000)
    private val portuguese = Language(id = 1, name = "Portuguese")
    const val KNOWN = 1540

    private val courses: List<Course> by lazy { runBlocking { SourceCourses.courses("pt") } }

    /** Every form of the words of the frequency list up to [known], as saved words are. */
    private fun knownWords(known: Int): Map<String, TermStatus> =
        File("../../tools/courses/pt/bands").listFiles()!!.sorted().flatMap { it.readLines() }
            .map { it.split('\t') }.filter { it.size >= 2 && it[0].toInt() <= known }
            .flatMap { listOf(it[1]) + it.getOrElse(2) { "" }.split(' ').filter(String::isNotBlank) }
            .associate { it.lowercase() to TermStatus.WELL_KNOWN }

    /** The share of each course known with [known] words. */
    fun shares(known: Int, list: List<Course> = courses): Map<String, Int> {
        val statuses = knownWords(known)
        return list.mapNotNull { course -> CourseWords.knownPercent(CourseWords.of(course, portuguese), statuses)?.let { course.id to it } }.toMap()
    }

    private fun progress(course: Course, done: Int, opened: Int = done, audio: Boolean = false) = CourseProgress(
        course,
        course.lessons.mapIndexed { i, lesson ->
            LessonProgress(lesson, if (i < opened) LessonBook(lesson.id, 1_000L + i, 1, 1, at, isCompleted = i < done, hasAudio = audio && i == opened - 1) else null)
        },
    )

    /** The reader's own course, tagged by them. */
    private val own = Course(
        id = "own-course-noticias", languageCode = "pt", title = "Minhas notícias", description = "Artigos que eu escolhi.", level = CourseLevel.C1,
        topic = "News I chose", tags = listOf("mine", "news"),
        lessons = listOf(Lesson("own-lesson-1", "O novo parque", "", "A cidade abriu um novo parque perto do rio.")),
    )

    /** The reader the mockup shows. */
    fun underWay(): CoursesUiState {
        val list = courses.map { course ->
            val rank = course.rankUpTo ?: 0
            when {
                rank <= 1000 -> progress(course, done = course.lessons.size)
                course.id == "pt-mini-1900" -> progress(course, done = 3, opened = 4, audio = true)
                else -> progress(course, done = 0)
            }
        } + progress(own, done = 0)
        return CoursesUiState(
            loading = false,
            languageName = "Portuguese",
            courses = list,
            vocabulary = VocabularyProgress.of(KNOWN, List(10_000) { it < KNOWN }),
            knownShares = shares(KNOWN, list.map { it.course }),
        )
    }

    /** A reader starting out: no course started and no words known yet. */
    fun firstTime(): CoursesUiState = CoursesUiState(
        loading = false,
        languageName = "Portuguese",
        courses = courses.map { progress(it, done = 0) },
        vocabulary = VocabularyProgress.of(0, List(10_000) { false }),
        knownShares = emptyMap(),
    )
}
