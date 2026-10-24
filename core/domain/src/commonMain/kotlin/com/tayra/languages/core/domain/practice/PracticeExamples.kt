package com.tayra.languages.core.domain.practice

import kotlin.time.Instant

/**
 * A sentence from elsewhere kept for a word: the [form] of the word it uses, starting at
 * [formStart], its [translation] when known, and where it came from ([source] and its [sourceId],
 * for the credit a source asks for).
 */
data class StoredExample(
    val termId: Long,
    val sentence: String,
    val translation: String?,
    val form: String,
    val formStart: Int,
    val source: String,
    val sourceId: String? = null,
) {
    /** Where the form stands in the sentence. */
    val range: IntRange get() = formStart until formStart + form.length

    /** The sentence as practice asks it. */
    fun asPractice(): PracticeExample = PracticeExample(sentence, range, translation, if (source == PracticeExampleStore.SOURCE_TATOEBA) TATOEBA_CREDIT else null)

    companion object {
        /** Tatoeba's sentences are shared under CC BY 2.0 FR, which asks for the source to be named. */
        const val TATOEBA_CREDIT = "Tatoeba · CC BY 2.0 FR"
    }
}

/** The sentences kept for [words], as practice asks them, by the word's key; words without a saved term or sentences are left out. */
suspend fun PracticeExampleRepository.forPractice(words: List<PracticeWord>): Map<String, List<PracticeExample>> {
    val byTerm = examples(words.mapNotNull { it.termId })
    return words.mapNotNull { word -> byTerm[word.termId]?.let { kept -> word.key to kept.map { it.asPractice() } } }.toMap()
}

/** The example sentences kept for words being learned. */
interface PracticeExampleRepository {
    /** The examples kept for each of [termIds], oldest first; words without any are left out. */
    suspend fun examples(termIds: Collection<Long>): Map<Long, List<StoredExample>>

    /** How many examples each of [termIds] has; words without any are left out. */
    suspend fun counts(termIds: Collection<Long>): Map<Long, Int>

    /** Keeps [examples]; one whose sentence its word has already is skipped. */
    suspend fun add(examples: List<StoredExample>, fetchedAt: Instant)

    suspend fun clear(termId: Long)
}
