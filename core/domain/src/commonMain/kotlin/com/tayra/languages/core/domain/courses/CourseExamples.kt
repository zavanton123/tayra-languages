package com.tayra.languages.core.domain.courses

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.ZWS_STRING
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.parse.parseTokens
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.ExampleLesson
import com.tayra.languages.core.domain.service.ExampleSearchQuery
import com.tayra.languages.core.domain.service.ExampleSearchResult
import com.tayra.languages.core.domain.service.ExampleSentence
import com.tayra.languages.core.domain.service.ExampleSentencesProvider
import com.tayra.languages.core.domain.service.ExampleSort
import com.tayra.languages.core.domain.service.ExampleSource
import com.tayra.languages.core.domain.service.ExampleSourceFeatures
import com.tayra.languages.core.domain.service.LexiconService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/** How a next page of [CourseExamplesProvider] starts, so a router can tell it from the others'. */
const val COURSES_PAGE_PREFIX = "courses:"

/**
 * Example sentences from the lessons of the installed course packs, cut into sentences and words
 * by the language's own parser, as the reader cuts them. A sentence matches when the searched term,
 * or one of its forms saved as such, is one of its words or a run of them. Works offline.
 */
class CourseExamplesProvider(
    private val courses: SampleCourseSource,
    /** The saved terms, whose forms are searched too; without it only the term itself is. */
    private val terms: TermRepository? = null,
    /** Loads the word lists that languages written without spaces are cut by. */
    private val lexicons: LexiconService? = null,
    private val seeds: () -> Int = { Random.nextInt() },
) : ExampleSentencesProvider {

    private class Indexed(val text: String, val words: List<String>, val lesson: ExampleLesson)

    /** A language's sentences, made from the courses with [courseIds]; made again once those change. */
    private class Index(val courseIds: Set<String>, val sentences: List<Indexed>)

    private val indexes = HashMap<Long, Index>()
    private val languages = HashMap<Long, Language>()
    private val lock = Mutex()

    /** A search, as its pages carry it. */
    private data class Search(
        val languageId: Long,
        /** The searched term and its forms, each as its lowercase words. */
        val needles: List<List<String>>,
        val sort: ExampleSort,
        val minWords: Int?,
        val maxWords: Int?,
        val limit: Int,
        val seed: Int,
        val offset: Int = 0,
    )

    // Kept for the session: the pages of a search are asked by their token alone.
    private val searches = LinkedHashMap<String, Search>()
    private var nextToken = 0

    override suspend fun sources(language: Language): List<ExampleSourceFeatures> {
        val code = LanguageCodes.codeFor(language.name) ?: return emptyList()
        return if (runCatching { courses.courseIds(code) }.getOrDefault(emptySet()).isNotEmpty()) listOf(FEATURES) else emptyList()
    }

    override suspend fun search(query: ExampleSearchQuery): ExampleSearchResult {
        val language = query.language
        val text = query.text.replace(ZWS_STRING, "").trim()
        if (text.isEmpty()) return ExampleSearchResult.EMPTY
        index(language) ?: return ExampleSearchResult.EMPTY
        val needles = (listOf(text) + forms(text, language)).map { wordsOf(it, language) }.filter { it.isNotEmpty() }.distinct()
        if (needles.isEmpty()) return ExampleSearchResult.EMPTY
        return page(Search(language.id, needles, query.sort, query.minWords, query.maxWords, query.limit, seeds()))
    }

    override suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult =
        lock.withLock { searches[nextPage] }?.let { page(it) } ?: ExampleSearchResult.EMPTY

    private suspend fun page(search: Search): ExampleSearchResult {
        val language = lock.withLock { languages[search.languageId] } ?: return ExampleSearchResult.EMPTY
        val index = index(language) ?: return ExampleSearchResult.EMPTY
        val matches = index.sentences.filter { sentence ->
            val count = sentence.words.size
            (search.minWords == null || count >= search.minWords) && (search.maxWords == null || count <= search.maxWords) &&
                search.needles.any { matches(sentence.words, it, language) }
        }
        val ordered = when (search.sort) {
            ExampleSort.SHORTEST -> matches.sortedWith(compareBy({ it.words.size }, { it.text.length }))
            ExampleSort.LONGEST -> matches.sortedWith(compareByDescending<Indexed> { it.words.size }.thenByDescending { it.text.length })
            ExampleSort.RANDOM -> matches.shuffled(Random(search.seed))
            else -> matches
        }
        val end = search.offset + search.limit
        val next = if (end < ordered.size) lock.withLock {
            "$COURSES_PAGE_PREFIX${nextToken++}".also {
                searches[it] = search.copy(offset = end)
                if (searches.size > MAX_SEARCHES) searches.remove(searches.keys.first())
            }
        } else null
        return ExampleSearchResult(
            ordered.drop(search.offset).take(search.limit).map { ExampleSentence(it.text, translation = null, lesson = it.lesson) },
            ordered.size,
            next,
        )
    }

    /** The language's sentences, made on first use and again after a pack was installed or removed. */
    private suspend fun index(language: Language): Index? {
        val code = LanguageCodes.codeFor(language.name) ?: return null
        val ids = runCatching { courses.courseIds(code) }.getOrDefault(emptySet())
        if (ids.isEmpty()) return null
        lock.withLock {
            languages[language.id] = language
            indexes[language.id]?.takeIf { it.courseIds == ids }?.let { return it }
        }
        lexicons?.prepare(language)
        val sentences = buildList {
            for (course in runCatching { courses.courses(code) }.getOrDefault(emptyList())) {
                for (lesson in course.lessons) {
                    val ref = ExampleLesson(course.id, lesson.id, course.title, lesson.title)
                    language.parseTokens(lesson.text).groupBy { it.sentenceNumber }.values.forEach { tokens ->
                        val words = tokens.filter { it.isWord }.map { language.lowercase(it.token) }
                        if (words.isEmpty()) return@forEach
                        val text = tokens.filter { !it.isEndOfParagraph }.joinToString("") { it.token }.replace(ZWS_STRING, "").trim()
                        add(Indexed(text, words, ref))
                    }
                }
            }
        }.distinctBy { it.text }
        val index = Index(ids, sentences)
        lock.withLock { indexes[language.id] = index }
        return index
    }

    private fun wordsOf(text: String, language: Language): List<String> =
        language.parseTokens(text).filter { it.isWord }.map { language.lowercase(it.token) }

    /** The forms saved for the term, and for the term it is a form of, as the reader links them. */
    private suspend fun forms(text: String, language: Language): List<String> {
        val repository = terms ?: return emptyList()
        return runCatching {
            val term = repository.findByTextLc(language.id, language.lowercase(text)) ?: return emptyList()
            val parents = repository.parents(term.id)
            (repository.children(term.id) + parents + parents.flatMap { repository.children(it.id) })
                .map { it.text.replace(ZWS_STRING, "") }
        }.getOrDefault(emptyList())
    }

    private fun matches(words: List<String>, needle: List<String>, language: Language): Boolean {
        for (start in 0..words.size - needle.size) {
            if (needle.indices.all { words[start + it] == needle[it] }) return true
        }
        // A language written without spaces may cut the term on its own differently from the sentence: the run's letters decide.
        return segmented(language) && joinedRun(words, needle.joinToString(""))
    }

    private fun joinedRun(words: List<String>, target: String): Boolean {
        for (start in words.indices) {
            val run = StringBuilder()
            for (end in start until words.size) {
                run.append(words[end])
                if (run.length >= target.length) {
                    if (run.toString() == target) return true
                    break
                }
            }
        }
        return false
    }

    private fun segmented(language: Language) = language.parserType in SEGMENTED_PARSERS

    companion object {
        val FEATURES = ExampleSourceFeatures(
            ExampleSource.COURSES,
            audio = false,
            sorts = listOf(ExampleSort.RANDOM, ExampleSort.SHORTEST, ExampleSort.LONGEST, ExampleSort.COURSE_ORDER),
            translations = false,
            phrases = true,
        )

        private const val MAX_SEARCHES = 200
        private val SEGMENTED_PARSERS = setOf("classicalchinese", "thai", "lao", "khmer", "mandarin", "cantonese", "japanese")
    }
}
