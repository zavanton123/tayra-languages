package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.repository.DictionaryRepository

/** Answers lookups from the bundled dictionaries. */
class DictionaryService(private val repository: DictionaryRepository) : OfflineDictionary {

    /** Opens the bundled dictionaries so the first lookup does not pay for copying the files. */
    suspend fun warmUp() {
        DictionaryId.bundled.forEach { repository.isAvailable(it) }
    }

    override suspend fun isAvailable(dictionary: DictionaryId): Boolean = repository.isAvailable(dictionary)

    override suspend fun lookup(dictionary: DictionaryId, text: String): DictionaryLookup {
        val word = text.trim()
        val wordLc = word.lowercase()
        if (wordLc.isEmpty()) return DictionaryLookup.EMPTY
        val own = repository.entries(dictionary, wordLc).sortedBy { caseRank(it.word, word) }
        // Headwords that differ only in case are usually a common noun and a name ("word", "Word");
        // the one whose case matches the clicked word wins, so the name does not become the parent.
        val lemmas = repository.lemmas(dictionary, wordLc)
            .filter { !it.equals(wordLc, ignoreCase = true) }
            .groupBy { it.lowercase() }
            .map { (_, variants) -> variants.minBy { caseRank(it, word) } }
        val inherited = lemmas.flatMap { lemma ->
            repository.entries(dictionary, lemma.lowercase())
                .filter { it.word == lemma }
                .map { it.copy(isOwnEntry = false) }
        }
        return DictionaryLookup(own + inherited, lemmas.filter { lemma -> inherited.any { it.word == lemma } })
    }

    /** 0 for the same case as the clicked word, 1 for the same initial case, 2 otherwise. */
    private fun caseRank(candidate: String, word: String): Int = when {
        candidate == word -> 0
        candidate.first().isUpperCase() == word.first().isUpperCase() -> 1
        else -> 2
    }
}
