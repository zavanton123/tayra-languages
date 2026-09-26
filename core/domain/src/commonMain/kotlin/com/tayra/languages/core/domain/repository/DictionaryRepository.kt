package com.tayra.languages.core.domain.repository

import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId

/** Read access to the prebuilt dictionary databases. */
interface DictionaryRepository {
    /** True when the dictionary file is bundled and in the expected format; opening it may take a moment the first time. */
    suspend fun isAvailable(dictionary: DictionaryId): Boolean
    suspend fun entries(dictionary: DictionaryId, wordLc: String): List<DictionaryEntry>
    suspend fun lemmas(dictionary: DictionaryId, formLc: String): List<String>
}
