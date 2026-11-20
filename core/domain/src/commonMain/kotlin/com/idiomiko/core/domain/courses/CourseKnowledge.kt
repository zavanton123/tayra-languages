package com.idiomiko.core.domain.courses

import com.idiomiko.core.domain.model.Language
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.parse.isSupported
import com.idiomiko.core.domain.parse.lowercase
import com.idiomiko.core.domain.parse.parseTokens
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.repository.TermRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** The words of a course and how many of them the reader knows, counted as Books counts a book's. */
object CourseWords {
    /** The distinct words of the course's lessons, as the language's parser cuts them and as saved words are matched. */
    fun of(course: Course, language: Language): Set<String> =
        course.lessons.flatMapTo(HashSet()) { lesson -> language.parseTokens(lesson.text).filter { it.isWord }.map { language.lowercase(it.token) } }

    /**
     * The share of [words] no longer new to the reader, from 0 to 100, rounded down so a course reads 100% only
     * when every word is known: as on Books, a word counts once it is saved with any status but new. Null without words.
     */
    fun knownPercent(words: Set<String>, statuses: Map<String, TermStatus>): Int? {
        if (words.isEmpty()) return null
        val known = words.count { word -> statuses[word]?.let { it != TermStatus.UNKNOWN } == true }
        return known * 100 / words.size
    }
}

/**
 * How much of each course of a language the reader already knows, kept up to date as words are saved.
 * Cutting the lessons into words is the slow part, so each course's words are kept until its lessons change;
 * a newly saved word only looks the words up again.
 */
class CourseKnowledge(private val terms: TermRepository, private val languages: LanguageRepository) {
    private class Cached(val key: Int, val words: Set<String>)

    private val cache = HashMap<String, Cached>()
    private val lock = Mutex()

    /** The known share of each of [courses] of the language with [languageId], by course id; a course without words is left out. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(languageId: Long, courses: Flow<List<Course>>): Flow<Map<String, Int>> = flow {
        val language = languages.getById(languageId)
        if (language == null || !language.isSupported) {
            emit(emptyMap())
            return@flow
        }
        emitAll(
            combine(courses.distinctUntilChanged(), terms.observeWordStatuses(languageId)) { list, statuses -> list to statuses }
                .mapLatest { (list, statuses) -> knownPercents(language, list, statuses) },
        )
    }

    /** The known share of each of [courses], by course id, cutting only the courses not cut before. */
    suspend fun knownPercents(language: Language, courses: List<Course>, statuses: Map<String, TermStatus>): Map<String, Int> = withContext(Dispatchers.Default) {
        val result = HashMap<String, Int>()
        for (course in courses) {
            val words = words(course, language)
            CourseWords.knownPercent(words, statuses)?.let { result[course.id] = it }
            // A language's hundred courses take a moment to cut; the page stays live meanwhile.
            yield()
        }
        result
    }

    private suspend fun words(course: Course, language: Language): Set<String> {
        val key = 31 * (31 * language.id.hashCode() + language.parserType.hashCode()) + course.lessons.sumOf { it.text.hashCode() }
        lock.withLock { cache[course.id]?.takeIf { it.key == key }?.let { return it.words } }
        val words = CourseWords.of(course, language)
        lock.withLock { cache[course.id] = Cached(key, words) }
        return words
    }
}
