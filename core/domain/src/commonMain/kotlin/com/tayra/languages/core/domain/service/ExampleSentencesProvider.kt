package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.model.Language
import kotlin.coroutines.cancellation.CancellationException

/**
 * An example sentence using a term, with a translation and a recording when available; a sentence
 * taken from a published text links to it ([sourceUrl]) with its date (yyyy-mm-dd), and one from a
 * course to its [lesson].
 */
data class ExampleSentence(
    val text: String,
    val translation: String?,
    val audioUrl: String? = null,
    val sourceUrl: String? = null,
    val date: String? = null,
    val lesson: ExampleLesson? = null,
)

/** The course lesson an example sentence is from. */
data class ExampleLesson(val courseId: String, val lessonId: String, val courseTitle: String, val lessonTitle: String)

/** Where example sentences come from. */
enum class ExampleSource(val label: String) {
    TATOEBA("Tatoeba"),
    COURSES("My courses"),
    LEIPZIG("Leipzig Corpora"),
}

/** What a [source] offers for a language, so a screen shows only the filters it can apply. */
data class ExampleSourceFeatures(
    val source: ExampleSource,
    val audio: Boolean,
    val sorts: List<ExampleSort>,
    val translations: Boolean,
    /** Whether a search for several words finds them together, rather than nothing. */
    val phrases: Boolean,
) {
    companion object {
        val TATOEBA = ExampleSourceFeatures(ExampleSource.TATOEBA, audio = true, sorts = ExampleSort.entries - ExampleSort.COURSE_ORDER, translations = true, phrases = true)
    }
}

enum class YesNo(val apiValue: String, val label: String) { YES("yes", "Yes"), NO("no", "No") }

enum class ExampleSort(val apiValue: String, val label: String) {
    RELEVANCE("relevance", "Relevance"),
    SHORTEST("words", "Shortest first"),
    LONGEST("-words", "Longest first"),
    NEWEST("-created", "Newest first"),
    OLDEST("created", "Oldest first"),
    RECENTLY_MODIFIED("-modified", "Recently modified"),
    RANDOM("random", "Random"),
    /** The order of the course packs' courses and lessons; no service has it. */
    COURSE_ORDER("course", "Course order"),
}

enum class SentenceOrigin(val apiValue: String, val label: String) {
    ORIGINAL("original", "Original sentences"),
    TRANSLATION("translation", "Translations"),
    KNOWN("known", "Known origin"),
    UNKNOWN("unknown", "Unknown origin"),
}

/** Search criteria for example sentences; null means "any". */
data class ExampleSearchQuery(
    val text: String,
    val language: Language,
    val targetLanguage: String,
    val minWords: Int? = null,
    val maxWords: Int? = null,
    val sort: ExampleSort = ExampleSort.RELEVANCE,
    val isOrphan: YesNo? = null,
    val isUnapproved: YesNo? = YesNo.NO,
    val isNative: YesNo? = null,
    val hasAudio: YesNo? = null,
    val tags: List<String> = emptyList(),
    val listId: String? = null,
    val owner: String? = null,
    val origin: SentenceOrigin? = null,
    val transIsDirect: YesNo? = null,
    val transIsNative: YesNo? = null,
    val transHasAudio: YesNo? = null,
    val transIsUnapproved: YesNo? = null,
    val transIsOrphan: YesNo? = null,
    val limit: Int = 30,
    val source: ExampleSource = ExampleSource.TATOEBA,
)

data class ExampleSearchResult(
    val sentences: List<ExampleSentence>,
    val total: Int?,
    /** Opaque link to the next page, or null on the last page. */
    val nextPage: String?,
    /** The source could not be asked (offline, too slow, a server error), so its having no examples is not known. */
    val failed: Boolean = false,
) {
    companion object {
        val EMPTY = ExampleSearchResult(emptyList(), null, null)
        val FAILED = ExampleSearchResult(emptyList(), null, null, failed = true)
    }
}

/** What [block] finds, or a failed result when it throws anything but a cancellation. */
inline fun failedOnError(block: () -> ExampleSearchResult): ExampleSearchResult = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    ExampleSearchResult.FAILED
}

/** Finds example sentences for a term, e.g. from a sentence corpus. */
interface ExampleSentencesProvider {
    /**
     * A random handful of short examples in the term's language, translated into the target language
     * when possible. Never throws.
     */
    suspend fun examples(text: String, language: Language, targetLanguage: String, limit: Int = 10): List<ExampleSentence> =
        search(ExampleSearchQuery(text, language, targetLanguage, minWords = 1, maxWords = 15, sort = ExampleSort.RANDOM, limit = limit)).sentences

    /** Never throws but for cancellation; a source that could not be asked gives a [ExampleSearchResult.failed] result. */
    suspend fun search(query: ExampleSearchQuery): ExampleSearchResult

    suspend fun nextPage(nextPage: String, targetLanguage: String): ExampleSearchResult

    /** The sources with examples in [language], the default first. */
    suspend fun sources(language: Language): List<ExampleSourceFeatures> = listOf(ExampleSourceFeatures.TATOEBA)
}

/** How often a word occurs in a corpus. */
data class CorpusFrequency(val count: Long, val rank: Int?, val frequencyClass: Int?)

/** Counts of a language's words in a large corpus, and the words found near them. */
interface CorpusWords {
    /** The word's frequency, or null when the corpus lacks the language or the word. Never throws. */
    suspend fun frequency(word: String, language: Language): CorpusFrequency?

    /** The words found with [word] most significantly often, strongest first. Never throws. */
    suspend fun cooccurrences(word: String, language: Language, limit: Int = 8): List<String>
}
