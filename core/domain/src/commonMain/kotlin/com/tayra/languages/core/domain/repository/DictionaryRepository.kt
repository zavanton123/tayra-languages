package com.tayra.languages.core.domain.repository

import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId

/** Read access to the installed dictionary databases. */
interface DictionaryRepository {
    /** True when the pack is installed and its file is in the expected format. */
    suspend fun isAvailable(dictionary: DictionaryId): Boolean
    suspend fun entries(dictionary: DictionaryId, wordLc: String): List<DictionaryEntry>
    suspend fun lemmas(dictionary: DictionaryId, formLc: String): List<String>

    /** The entries for several lowercase spellings at once, by spelling. */
    suspend fun entries(dictionary: DictionaryId, wordsLc: Collection<String>): Map<String, List<DictionaryEntry>> =
        wordsLc.associateWith { entries(dictionary, it) }.filterValues { it.isNotEmpty() }
    /** Every headword and form the pack has, lowercase: the lexicon a language written without spaces is cut into words with. */
    suspend fun words(dictionary: DictionaryId): Collection<String> = emptyList()

    /** Closes the dictionary so its file can be replaced or deleted. */
    suspend fun close(dictionary: DictionaryId)
}
