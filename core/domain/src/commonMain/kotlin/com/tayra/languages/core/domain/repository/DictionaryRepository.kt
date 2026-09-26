package com.tayra.languages.core.domain.repository

import com.tayra.languages.core.domain.dictionary.DictionaryEntry
import com.tayra.languages.core.domain.dictionary.DictionaryId

/** Storage for imported dictionaries. */
interface DictionaryRepository {
    suspend fun importedFormat(dictionary: DictionaryId): Int?
    suspend fun entries(dictionary: DictionaryId, wordLc: String): List<DictionaryEntry>
    suspend fun lemmas(dictionary: DictionaryId, formLc: String): List<String>
    /** Replaces the dictionary's content in one transaction. */
    suspend fun replace(dictionary: DictionaryId, format: Int, entries: List<DictionaryEntry>, forms: List<DictionaryForm>)
}

data class DictionaryForm(val form: String, val lemma: String, val generated: Boolean)
