package com.tayra.languages.cli

import com.tayra.languages.core.domain.courses.CourseProgress
import com.tayra.languages.core.domain.courses.LessonProgress
import com.tayra.languages.core.domain.model.BookListItem
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import kotlinx.serialization.Serializable

// The shapes `--json` prints. Field names are part of the command's contract; add fields, do not rename them.

@Serializable
data class LanguageJson(val id: Long, val name: String, val code: String?, val current: Boolean)

@Serializable
data class BookJson(
    val id: Long,
    val title: String,
    val language: String,
    val tags: List<String>,
    val pages: Int,
    val currentPage: Int,
    val words: Int,
    /** Share of the distinct words on the sampled pages that are not known yet, when worked out. */
    val unknownPercent: Int?,
    val archived: Boolean,
    val completed: Boolean,
    val source: String?,
    val lastOpened: String?,
)

fun BookListItem.toJson() = BookJson(
    id, title, languageName, tags, pageCount, currentPage, wordCount, stats?.unknownPercent, isArchived, isCompleted, sourceUri, lastOpened?.toString(),
)

@Serializable
data class PageJson(val bookId: Long, val page: Int, val pages: Int, val text: String)

@Serializable
data class CourseJson(
    val id: String,
    val title: String,
    val description: String,
    val level: String,
    val topic: String,
    val tags: List<String>,
    val lessons: Int,
    val completedLessons: Int,
    val words: Int,
    val builtIn: Boolean,
    val status: String,
)

fun CourseProgress.toJson() = CourseJson(
    course.id, course.title, course.description, course.level.code, course.topic, course.tags,
    lessons.size, completed, course.wordCount, course.builtIn, status.name.lowercase(),
)

@Serializable
data class LessonJson(val id: String, val number: Int, val title: String, val summary: String, val words: Int, val status: String, val bookId: Long?)

fun LessonProgress.toJson(number: Int) =
    LessonJson(lesson.id, number, lesson.title, lesson.summary, lesson.wordCount, status.name.lowercase(), book?.bookId)

@Serializable
data class CourseDetailJson(val course: CourseJson, val lessonList: List<LessonJson>)

@Serializable
data class TermJson(
    val id: Long,
    val text: String,
    val status: Int,
    val statusName: String,
    val translation: String?,
    val romanization: String?,
    val parents: List<String>,
    val sentence: String?,
    val created: String?,
)

fun Term.toJson() = TermJson(
    id, displayText, status.value, status.cliName, translation, romanization, parents.map { it.text }, sentence, createdAt?.toString(),
)

@Serializable
data class CreatedJson(val id: String)

@Serializable
data class DoneJson(val ok: Boolean = true, val message: String)

/** Statuses as the command names them: 0 to 4 as in the app, then known and ignored. */
val TermStatus.cliName: String
    get() = when (this) {
        TermStatus.UNKNOWN -> "unknown"
        TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4 -> "learning-$value"
        TermStatus.IGNORED -> "ignored"
        TermStatus.WELL_KNOWN -> "known"
    }

const val STATUS_HELP = "0 or unknown, 1 to 4 (learning), 5 or known, i or ignored"

fun parseStatus(text: String): TermStatus = when (text.trim().lowercase()) {
    "0", "u", "unknown" -> TermStatus.UNKNOWN
    "1" -> TermStatus.NEW_1
    "2" -> TermStatus.NEW_2
    "3" -> TermStatus.LEARNING_3
    "4" -> TermStatus.LEARNING_4
    "5", "99", "k", "known", "well-known" -> TermStatus.WELL_KNOWN
    "98", "i", "ignored" -> TermStatus.IGNORED
    else -> invalid("Unknown status '$text'; use $STATUS_HELP")
}
